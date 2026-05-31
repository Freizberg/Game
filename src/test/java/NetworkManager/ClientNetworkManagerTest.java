package NetworkManager;

import Action.Action;
import GameController.GameController;
import GameController.GameSaveManager;
import GameController.GameStateUpdate;
import GameController.InputHandler;
import GameEngine.GameEngine;
import GameEngine.GameState;
import GameEngine.Player;
import Map.GameMap;
import GameRenderer.GameRenderer;
import Units.Unit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.TestFixtures;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * These tests show the networking contract expected by the client:
 * - client is never host
 * - sendAction writes serialized Action to server
 * - listener forwards GameStateUpdate to GameController
 */
class ClientNetworkManagerTest {

    @Test
    @DisplayName("Client network manager should always report isHost = false")
    void clientShouldNeverBeHost() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Thread acceptThread = new Thread(() -> {
                try (Socket socket = serverSocket.accept();
                     ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                    out.flush();
                    Thread.sleep(200);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            acceptThread.start();

            ClientNetworkManager client = new ClientNetworkManager("127.0.0.1", serverSocket.getLocalPort());
            assertFalse(client.isHost());
            client.disconnect();
        }
    }

    @Test
    @DisplayName("sendAction should serialize and send an Action object to the server")
    void sendActionShouldReachServer() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            AtomicReference<Object> received = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            Thread serverThread = new Thread(() -> {
                try (Socket socket = serverSocket.accept();
                     ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                    out.flush();
                    received.set(in.readObject());
                    latch.countDown();
                } catch (EOFException ignored) {
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            serverThread.start();

            ClientNetworkManager client = new ClientNetworkManager("127.0.0.1", serverSocket.getLocalPort());
            client.sendAction(new ProbeAction(UUID.randomUUID()));

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertNotNull(received.get());
            assertTrue(received.get() instanceof Action);

            client.disconnect();
        }
    }

    @Test
    @DisplayName("startListening should forward incoming GameStateUpdate to GameController")
    void listenerShouldForwardUpdateToController() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            CountDownLatch latch = new CountDownLatch(1);

            Thread serverThread = new Thread(() -> {
                try (Socket socket = serverSocket.accept();
                     ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                    out.flush();

                    GameStateUpdate update = TestFixtures.sampleUpdate(TestFixtures.newPlanningEngine());
                    out.writeObject(update);
                    out.flush();

                    Thread.sleep(200);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            serverThread.start();

            ClientNetworkManager client = new ClientNetworkManager("127.0.0.1", serverSocket.getLocalPort());
            RecordingGameController controller = new RecordingGameController(latch);

            client.startListening(controller);

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertNotNull(controller.lastUpdate);

            client.disconnect();
        }
    }

    static class ProbeAction implements Action {
        private final UUID unitId;

        ProbeAction(UUID unitId) {
            this.unitId = unitId;
        }


        public void execute(GameEngine engine) {
        }

        @Override
        public UUID getUnitId() {
            return unitId;
        }

        @Override
        public boolean isValid(GameMap map) {
            return true;
        }
    }

    static class RecordingGameController extends GameController {
        volatile GameStateUpdate lastUpdate;
        private final CountDownLatch latch;

        RecordingGameController(CountDownLatch latch) {
            super(
                    null,
                    new NoOpRenderer(),
                    null,
                    new GameSaveManager(),
                    new InputHandler()
            );
            this.latch = latch;
        }

        @Override
        public synchronized void applyStateUpdate(GameStateUpdate u) {
            this.lastUpdate = u;
            latch.countDown();
        }
    }

    static class NoOpRenderer implements GameRenderer {
        @Override public void renderMap(GameMap m) {}
        @Override public void renderUnit(Unit u) {}
        @Override public void renderHUD(List<Player> p, GameState state, UUID winnerId) {}
        @Override public void renderPlannedActions(List<Action> a) {}
    }
}