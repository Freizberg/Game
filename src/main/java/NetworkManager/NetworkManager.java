package NetworkManager;

/**
 * Abstraction layer for all network communication in a multiplayer game session.
 *
 * <p>Implementations differ in topology: {@link ServerNetworkManager} acts as the
 * authoritative host that broadcasts to all clients, while
 * {@link ClientNetworkManager} connects to the server and sends actions on behalf
 * of the local player.</p>
 *
 * @see ServerNetworkManager
 * @see ClientNetworkManager
 *
 * @author Dzhyhar Volodymyr
 * @author Yevhenii Marienko
 */
public interface NetworkManager {

    /**
     * Returns whether this manager is the authoritative game host.
     *
     * @return {@code true} if this peer is the host (server), {@code false} if it is a client
     */
    boolean isHost();
}
