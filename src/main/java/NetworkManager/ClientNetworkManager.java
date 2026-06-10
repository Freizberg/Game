package NetworkManager;

import Action.Action;
import GameController.GameController;
import GameController.GameStateUpdate;
import GameEngine.GameEngine;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * NetworkManager implementation for the client side of a networked game.
 *
 * <p>Connects to a remote {@link ServerNetworkManager} over a TCP socket,
 * sends locally produced {@link Action} objects to the host, and listens
 * for incoming {@link GameStateUpdate} messages that synchronize the local
 * game state with the authoritative server.</p>
 *
 * <p>A client is never the authoritative host, so {@link #isHost()} always
 * returns {@code false}. Incoming state updates are forwarded to the
 * {@link GameController}, which applies them to the local engine and triggers
 * re-rendering.</p>
 *
 * @author Dzhyhar Volodymyr
 * @author Yevhenii Marienko
 */
public class ClientNetworkManager implements NetworkManager {
    /** The TCP socket used to communicate with the server. */
    private Socket socket;
    /** Stream used to send data to the server*/
    private ObjectOutputStream out;
    /** Stream used to collect data from the server*/
    private ObjectInputStream in;

    public String hostAddress;
    public int port;

    public ClientNetworkManager(String host, int port) {
        this.hostAddress = host;
        this.port = port;
        connect(host, port);
    }

    /**
     * Serializes and sends an {@link Action} to the server.
     *
     * @param a the action to send
     */
    public synchronized void sendAction(Action a) {
        try {
            out.writeObject(a);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to send action to server.", e);
        }
    }

    /**
     * Establishes the network connection.
     *
     * @param host address of the server
     * @param port port on which server is listening
     */
    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to connect to server.", e);
        }
    }

    /**
     * Closes the network connection and releases all associated resources.
     */
    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to disconnect client.", e);
        }
    }

    @Override
    public boolean isHost() {
        return false;
    }

    /**
     * Starts a background thread that reads incoming messages from the server
     * and forwards them to the given {@link GameController} for processing.
     *
     * @param ctrl the controller that will handle incoming updates
     */
    public void startListening(GameController ctrl) {
        Thread listener = new Thread(() -> {
            try {
                while (socket != null && !socket.isClosed()) {
                    Object msg = in.readObject();

                    if (msg instanceof GameEngine initialEngine) {
                        ctrl.applyInitialState(initialEngine);
                    } else if (msg instanceof GameStateUpdate update) {
                        ctrl.applyStateUpdate(update);
                    }
                }
            } catch (EOFException ignored) {
                // Server closed connection normally.
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException("Client listener stopped unexpectedly.", e);
            }
        });

        listener.setDaemon(true);
        listener.setName("client-network-listener");
        listener.start();
    }
}