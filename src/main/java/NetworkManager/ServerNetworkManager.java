package NetworkManager;

import Action.Action;
import GameController.GameController;
import GameController.GameStateUpdate;
import GameEngine.GameEngine;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NetworkManager implementation for the server host side of a networked game.
 *
 * <p>Accepts incoming client connections, receives {@link Action} objects from
 * remote players, and broadcasts {@link GameStateUpdate} snapshots back to all
 * connected clients after the authoritative game state has been updated.</p>
 *
 * <p>The server acts as the authoritative host for the multiplayer session, so
 * {@link #isHost()} always returns {@code true}. It is responsible only for
 * transport and synchronization; final action validation and turn resolution
 * remain the responsibility of the {@link GameController}.
 *
 * @author Dzhyhar Volodymyr
 * @author Yevhenii Marienko
 */
public class ServerNetworkManager implements NetworkManager {

    /** The port number on which the server listens for incoming connections. */
    private final int port;

    /** The server socket used to accept new client connections. */
    private ServerSocket serverSocket;

    /** The authoritative game controller. */
    private final GameController controller;

    /** The list of currently connected client connections. */
    private final List<ClientConnection> clients = new CopyOnWriteArrayList<>();

    private int nextSlot = 1;

    public ServerNetworkManager(int port, GameController controller) {
        this.port = port;
        this.controller = controller;
    }

    /**
     *
     * @return list of clients.
     */
    public List<ClientConnection> getClients() {
        return clients;
    }

    /**
     *
     * @return port number.
     */
    public int getPort() {
        return port;
    }

    @Override
    public boolean isHost() {
        return true;
    }

    /**
     * Broadcasts a {@link GameStateUpdate} to every connected client socket.
     *
     * @param u the state update to broadcast
     */
    public void broadcastStateUpdate(GameStateUpdate u) {
        for (ClientConnection client : clients) {
            try {
                synchronized (client.out) {
                    client.out.reset();
                    client.out.writeObject(u);
                    client.out.flush();
                }
            } catch (IOException e) {
                removeClient(client);
            }
        }
    }

    /**
     * Sends the engine snapshot to a freshly connected client.
     *
     * @param client the connection to send the snapshot to
     */
    private void sendInitialState(ClientConnection client) {
        try {
            GameEngine engine = controller.getEngine();
            synchronized (client.out) {
                client.out.reset();
                client.out.writeObject(Integer.valueOf(client.playerIndex));
                client.out.writeObject(engine);
                client.out.flush();
            }
        } catch (IOException e) {
            removeClient(client);
        }
    }

    /**
     * Starts the accept loop that listens for incoming client connections.
     */
    public void startAccepting() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            throw new IllegalStateException("Server is already accepting connections.");
        }

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start server socket.", e);
        }

        Thread acceptThread = new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();

                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.flush();
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                    int maxIndex = controller.getEngine().getPlayers().size() - 1;
                    int slot = nextSlot++;
                    if (slot > maxIndex) {
                        socket.close();
                        continue;
                    }
                    ClientConnection client = new ClientConnection(socket, in, out, slot);
                    clients.add(client);

                    sendInitialState(client);
                    startClientListener(client);
                }
            } catch (IOException e) {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    throw new RuntimeException("Server accept loop stopped unexpectedly.", e);
                }
            }
        });

        acceptThread.setDaemon(true);
        acceptThread.setName("server-accept-thread");
        acceptThread.start();
    }

    private void handleIncomingAction(Action action) {
        if (action == null) {
            return;
        }
        controller.handleInput(action);
    }

    public void shutdown() {
        for (ClientConnection client : clients) {
            closeClient(client);
        }
        clients.clear();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to shut down server.", e);
        }
    }

    private void startClientListener(ClientConnection client) {
        Thread clientThread = new Thread(() -> {
            try {
                while (!client.socket.isClosed()) {
                    Object msg = client.in.readObject();

                    if (msg instanceof Action action) {
                        handleIncomingAction(action);
                    }
                }
            } catch (EOFException ignored) {
                removeClient(client);
            } catch (IOException | ClassNotFoundException e) {
                removeClient(client);
            }
        });

        clientThread.setDaemon(true);
        clientThread.setName("server-client-listener-" + client.socket.getPort());
        clientThread.start();
    }


    private void removeClient(ClientConnection client) {
        clients.remove(client);
        closeClient(client);
    }

    private void closeClient(ClientConnection client) {
        try {
            if (client.in != null) client.in.close();
        } catch (IOException ignored) { }

        try {
            if (client.out != null) client.out.close();
        } catch (IOException ignored) { }

        try {
            if (client.socket != null && !client.socket.isClosed()) client.socket.close();
        } catch (IOException ignored) { }
    }

    private static class ClientConnection {
        private final Socket socket;
        private final ObjectInputStream in;
        private final ObjectOutputStream out;
        private final int playerIndex;

        private ClientConnection(Socket socket, ObjectInputStream in, ObjectOutputStream out,  int playerIndex) {
            this.socket = socket;
            this.in = in;
            this.out = out;
            this.playerIndex = playerIndex;
        }
    }
}