package NetworkManager;

import Action.Action;
import Action.WaitAction;
import GameController.GameController;
import GameController.GameSaveManager;
import GameController.GameStateUpdate;
import GameController.InputHandler;
import GameEngine.GameEngine;
import GameEngine.GameState;
import GameEngine.Player;
import GameRenderer.GameRenderer;
import Map.GameMap;
import Units.Unit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Yevhenii Marienko
 */

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ServerNetworkManagerTest {

    @Test
    void isHost_should_always_return_true() {
        RecordingController controller = new RecordingController();
        ServerNetworkManager manager = new ServerNetworkManager(0, controller);

        assertTrue(manager.isHost());
    }

    @Test
    void startAccepting_should_accept_client_connection() throws Exception {
        RecordingController controller = new RecordingController();
        int port = findFreePort();
        ServerNetworkManager manager = new ServerNetworkManager(port, controller);

        manager.startAccepting();

        try (ClientProbe client = ClientProbe.connect(port)) {
            assertTimeout(Duration.ofSeconds(2), () -> waitForClientCount(manager, 1));
            assertEquals(1, manager.getClients().size());
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void broadcastStateUpdate_should_send_update_to_all_connected_clients() throws Exception {
        RecordingController controller = new RecordingController();
        int port = findFreePort();
        ServerNetworkManager manager = new ServerNetworkManager(port, controller);

        manager.startAccepting();

        try (ClientProbe clientOne = ClientProbe.connect(port);
             ClientProbe clientTwo = ClientProbe.connect(port)) {

            assertTimeout(Duration.ofSeconds(2), () -> waitForClientCount(manager, 2));

            GameStateUpdate update = new GameStateUpdate();
            manager.broadcastStateUpdate(update);

            Object messageOne = clientOne.readObject();
            Object messageTwo = clientTwo.readObject();

            assertNotNull(messageOne);
            assertNotNull(messageTwo);
            assertInstanceOf(GameStateUpdate.class, messageOne);
            assertInstanceOf(GameStateUpdate.class, messageTwo);
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void incoming_action_from_client_should_be_forwarded_to_controller_handleInput() throws Exception {
        RecordingController controller = new RecordingController();
        int port = findFreePort();
        ServerNetworkManager manager = new ServerNetworkManager(port, controller);

        manager.startAccepting();

        try (ClientProbe client = ClientProbe.connect(port)) {
            assertTimeout(Duration.ofSeconds(2), () -> waitForClientCount(manager, 1));

            UUID unitId = UUID.randomUUID();
            WaitAction action = new WaitAction(unitId);
            client.sendObject(action);

            assertTrue(controller.awaitHandledAction());

            Action handledAction = controller.lastHandledAction.get();
            assertNotNull(handledAction);
            assertInstanceOf(WaitAction.class, handledAction);
            assertEquals(unitId, handledAction.getUnitId());
            assertEquals(1, controller.handleInputCalls.get());
        } finally {
            manager.shutdown();
        }
    }
    @Test
    void disconnected_client_should_be_removed_from_client_list() throws Exception {
        RecordingController controller = new RecordingController();
        int port = findFreePort();
        ServerNetworkManager manager = new ServerNetworkManager(port, controller);

        manager.startAccepting();

        ClientProbe client = ClientProbe.connect(port);
        try {
            assertTimeout(Duration.ofSeconds(2), () -> waitForClientCount(manager, 1));

            client.close();

            assertTimeout(Duration.ofSeconds(2), () -> waitForClientCount(manager, 0));
            assertEquals(0, manager.getClients().size());
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void shutdown_should_close_server_socket_and_clear_client_list() throws Exception {
        RecordingController controller = new RecordingController();
        int port = findFreePort();
        ServerNetworkManager manager = new ServerNetworkManager(port, controller);

        manager.startAccepting();

        try (ClientProbe clientOne = ClientProbe.connect(port);
             ClientProbe clientTwo = ClientProbe.connect(port)) {

            assertTimeout(Duration.ofSeconds(2), () -> waitForClientCount(manager, 2));

            manager.shutdown();

            assertTrue(manager.getClients().isEmpty());
            assertTrue(readServerSocket(manager).isClosed());
        }
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void waitForClientCount(ServerNetworkManager manager, int expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);

        while (System.nanoTime() < deadline) {
            if (manager.getClients().size() == expected) {
                return;
            }
            Thread.sleep(25);
        }

        fail("Expected client count " + expected + " but was " + manager.getClients().size());
    }

    private static ServerSocket readServerSocket(ServerNetworkManager manager) {
        try {
            Field field = ServerNetworkManager.class.getDeclaredField("serverSocket");
            field.setAccessible(true);
            return (ServerSocket) field.get(manager);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to read server socket.", e);
        }
    }

    private static final class RecordingController extends GameController {
        private final AtomicReference<Action> lastHandledAction = new AtomicReference<>();
        private final AtomicInteger handleInputCalls = new AtomicInteger();
        private final CountDownLatch actionLatch = new CountDownLatch(1);

        private RecordingController() {
            super(new GameEngine(), new NoOpRenderer(), null, new GameSaveManager(), new InputHandler());
        }

        @Override
        public synchronized void handleInput(Action action) {
            lastHandledAction.set(action);
            handleInputCalls.incrementAndGet();
            actionLatch.countDown();
        }

        private boolean awaitHandledAction() throws InterruptedException {
            return actionLatch.await(2, TimeUnit.SECONDS);
        }
    }

    private static final class NoOpRenderer implements GameRenderer {
        @Override
        public void renderMap(GameMap map) {
        }

        @Override
        public void renderUnit(Unit unit) {
        }

        @Override
        public void renderHUD(List<Player> players, GameState state, UUID winnerId) {
        }

        @Override
        public void renderPlannedActions(List<Action> actions) {
        }
    }

    private static final class ClientProbe implements Closeable {
        private final Socket socket;
        private final ObjectOutputStream out;
        private final ObjectInputStream in;

        private ClientProbe(Socket socket, ObjectOutputStream out, ObjectInputStream in) {
            this.socket = socket;
            this.out = out;
            this.in = in;
        }

        private static ClientProbe connect(int port) throws IOException {
            Socket socket = new Socket("127.0.0.1", port);
            socket.setSoTimeout(2000);

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            return new ClientProbe(socket, out, in);
        }

        private void sendObject(Object object) throws IOException {
            out.writeObject(object);
            out.flush();
        }

        private Object readObject() throws IOException, ClassNotFoundException {
            try {
                return in.readObject();
            } catch (SocketTimeoutException e) {
                fail("Timed out while waiting for server message.");
                return null;
            }
        }

        @Override
        public void close() throws IOException {
            IOException first = null;

            try {
                in.close();
            } catch (EOFException ignored) {
            } catch (IOException e) {
                first = e;
            }

            try {
                out.close();
            } catch (IOException e) {
                if (first == null) {
                    first = e;
                }
            }

            try {
                socket.close();
            } catch (IOException e) {
                if (first == null) {
                    first = e;
                }
            }

            if (first != null) {
                throw first;
            }
        }
    }
}