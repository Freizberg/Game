package GameController;

import Action.*;
import GameEngine.GameEngine;
import GameEngine.GameState;
import GameEngine.Player;
import GameRenderer.GameRenderer;
import GameView.GameView;
import GameView.GameViewMapper;
import Map.Tile;
import NetworkManager.ClientNetworkManager;
import NetworkManager.NetworkManager;
import NetworkManager.ServerNetworkManager;
import Units.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Central controller that coordinates the game loop, user input, rendering,
 * networking, and save/load operations.
 *
 * <p>Acts as the mediator between the {@link GameEngine} (simulation logic),
 * the {@link GameRenderer} (display), the {@link NetworkManager} (multiplayer),
 * the {@link GameSaveManager} (persistence), and the {@link InputHandler}
 * (player commands). Both local and networked game sessions are managed here.</p>
 *
 * <p>Raw user interactions are first translated into {@link Action} objects by
 * the {@link InputHandler}. The controller then routes those actions either to
 * the local authoritative engine or over the network to the authoritative host.</p>
 *
 * <p>The controller also exposes save, load, and autosave functionality.
 * Persistence is delegated to {@link GameSaveManager}, while this class decides
 * when saving and loading are allowed within the game lifecycle.</p>
 *
 * @author Yevhenii Marienko
 * @author Dzhyhar Volodymyr
 * @author Marcin Świerczyński
 */
public class GameController {

    /** The authoritative simulation engine. */
    private GameEngine engine;

    /** The renderer used to display the current game state. */
    private GameRenderer renderer;

    /** The network manager handling multiplayer communication, or {@code null} for local games. */
    private NetworkManager network;

    /** Handles saving and loading of game state. */
    private GameSaveManager saveManager;

    /** Translates raw player input into {@link Action} objects. */
    private InputHandler inputHandler;

    /** Whether automatic saving is enabled. */
    private boolean autosaveEnabled = false;

    /** Optional path used for autosave snapshots. */
    private String autosavePath;

    private UUID selectedUnitId;

    /** Current UI input mode, read by the mouse handler to interpret clicks. */
    private InputMode inputMode = InputMode.NONE;

    /**
     * Perspective player for the local UI. In network mode this is the single
     * player controlled on this machine; in local hot-seat mode it is used only
     * as a fallback (the active planner is derived from turn state instead).
     */
    private Player localPlayer;

    /**
     * Whether this is a local multiplayer (hot-seat) session in which every
     * player plans in turn on the same machine.
     */
    private boolean hotSeat;

    /**
     * Client-side prediction of the local player's queued actions.
     *
     * <p>A networked client does not mutate its (host-synchronised) engine when
     * the player plans; it forwards intents to the host instead. To still give
     * the player immediate visual feedback (planned-action arrows and the
     * per-unit action count), the same intents are mirrored here and drawn over
     * the board. The list is cleared whenever an authoritative
     * {@link GameStateUpdate} or initial engine snapshot arrives.</p>
     */
    private final List<Action> clientPendingActions = new ArrayList<>();

    /**
     * Constructs a GameController with all required collaborators.
     *
     * @param engine the game engine driving the simulation
     * @param renderer the renderer used to display the game
     * @param network the network manager for multiplayer, or {@code null} for local play
     * @param saveManager the save/load manager for game persistence
     * @param inputHandler the handler that translates player input into actions
     */
    public GameController(GameEngine engine,
                          GameRenderer renderer,
                          NetworkManager network,
                          GameSaveManager saveManager,
                          InputHandler inputHandler) {
        this.engine = engine;
        this.renderer = renderer;
        this.network = network;
        this.saveManager = saveManager;
        this.inputHandler = inputHandler;
    }

    /**
     * Starts the appropriate gameplay mode depending on whether the session
     * should be local or networked.
     *
     * @param local {@code true} to start a local game, {@code false} for network mode
     */
    public void gameLoop(boolean local) {
        if (local || network == null) {
            startLocalGame();
        } else {
            startNetworkGame();
        }
    }

    /**
     * Runs the core controller loop.
     *
     * <p>At the current stage this method triggers a full render of the current
     * engine state. Concrete UI layers may call the public input-entry methods
     * of this controller in response to user interaction.</p>
     */
    private void gameLoop() {
        renderFullState();
    }

    /**
     * Handles a move request originating from the UI or console layer.
     *
     * <p>The raw interaction is translated into a {@link Action} through the
     * {@link InputHandler}, then passed into the common action-processing path.</p>
     *
     * @param unitId the UUID of the unit that should move
     * @param dest the destination tile selected by the player
     */
    public void onMoveRequested(UUID unitId, Tile dest) {
        Action action = inputHandler.onMoveUnit(unitId, dest);
        handleInput(action);
    }

    /**
     * Handles an attack request originating from the UI or console layer.
     *
     * @param attackerId the UUID of the attacking unit
     * @param targetId the UUID of the targeted unit
     */
    public void onAttackRequested(UUID attackerId, UUID targetId) {
        Action action = inputHandler.onAttack(attackerId, targetId);
        handleInput(action);
    }

    /**
     * Handles a heal request originating from the UI or console layer.
     *
     * @param casterId the UUID of the healing mage
     * @param targetId the UUID of the friendly unit to heal
     */
    public void onHealRequested(UUID casterId, UUID targetId) {
        Action action = inputHandler.onHeal(casterId, targetId);
        handleInput(action);
    }

    /**
     * Handles a wait request originating from the UI or console layer.
     *
     * @param unitId the UUID of the unit that should wait
     */
    public void onWaitRequested(UUID unitId) {
        Action action = inputHandler.onWait(unitId);
        handleInput(action);
    }

    /**
     * Handles a player request to end the planning phase.
     *
     * @param playerId the UUID of the player ending their turn
     */
    public void onEndTurnRequested(UUID playerId) {
        Action action = inputHandler.onEndTurn(playerId);
        handleInput(action);
    }

    /**
     * Handles a raw console command by delegating parsing to the
     * {@link InputHandler} and forwarding the resulting action to the common
     * action-processing path.
     *
     * @param commandLine raw text entered by the player in console mode
     * @param currentPlayerId the UUID of the player issuing the command
     */
    public void onConsoleCommand(String commandLine, UUID currentPlayerId) {
        Action action = inputHandler.parseConsoleCommand(commandLine, currentPlayerId, engine);
        handleInput(action);
    }

    /**
     * Saves the current engine state using the default path configured
     * in {@link GameSaveManager}.
     *
     * <p>Saving is intentionally blocked during the RESOLVING phase to avoid
     * writing a partially processed round state.</p>
     *
     * @throws IllegalStateException if saving is attempted during RESOLVING
     * or if persistence is not configured
     */
    public synchronized void saveGame() {
        ensureSaveAllowed();
        ensureSaveManager();
        saveManager.saveGame(engine);
    }

    /**
     * Saves the current engine state to the specified path.
     *
     * <p>Saving is intentionally blocked during the RESOLVING phase to avoid
     * writing a partially processed round state.</p>
     *
     * @param path destination path for the save file
     * @throws IllegalStateException if saving is attempted during RESOLVING
     * or if persistence is not configured
     */
    public synchronized void saveGame(String path) {
        ensureSaveAllowed();
        ensureSaveManager();
        saveManager.saveGame(engine, path);
    }

    /**
     * Loads a previously saved engine state from the specified path,
     * replaces the current engine instance, and re-renders the game.
     *
     * <p>Loading is blocked during the RESOLVING phase for the same reason as
     * saving: the controller should not swap the engine while a round is being
     * resolved.</p>
     *
     * @param path path of the save file to load
     * @throws IllegalStateException if loading is attempted during RESOLVING
     * or if persistence is not configured
     */
    public synchronized void loadGame(String path) {
        ensureLoadAllowed();
        ensureSaveManager();

        GameEngine loadedEngine = saveManager.loadGame(path);
        if (loadedEngine == null) {
            return;
        }

        this.engine = loadedEngine;
        renderFullState();
    }

    /**
     * Enables automatic saving after each fully resolved round.
     *
     * <p>If a non-blank autosave path is provided, snapshots will be written
     * there. Otherwise the controller will fall back to the default path
     * configured inside {@link GameSaveManager}.</p>
     *
     * @param autosavePath file path where autosave snapshots should be written
     */
    public void enableAutosave(String autosavePath) {
        this.autosaveEnabled = true;
        this.autosavePath = autosavePath;
    }

    /**
     * Disables automatic saving.
     */
    public void disableAutosave() {
        this.autosaveEnabled = false;
    }

    /**
     * Returns whether autosave is currently enabled.
     *
     * @return {@code true} if autosave is enabled
     */
    public boolean isAutosaveEnabled() {
        return autosaveEnabled;
    }

    /**
     * Processes a player-issued {@link Action} by validating it and forwarding it
     * to the engine or network as appropriate.
     *
     * <p>If this peer is a remote client, the action is serialized and sent to
     * the authoritative host. If this peer is the host or the game is running
     * locally, the action is applied to the planning-phase state directly.</p>
     *
     * <p>When all players have ended their turn, the controller transitions the
     * engine to the RESOLVING phase, resolves the round, optionally performs
     * autosave, rebuilds the new {@link GameStateUpdate}, and broadcasts it to
     * connected clients if the session is networked.</p>
     *
     * @param a the action to handle
     */
    public synchronized void handleInput(Action a) {
        if (a == null || engine == null) {
            return;
        }

        if (engine.getState() == GameState.FINISHED || engine.getState() == GameState.DRAW) {
            renderFullState();
            return;
        }

        if (network != null && !network.isHost()) {
            if (network instanceof ClientNetworkManager clientNetworkManager) {
                clientNetworkManager.sendAction(a);
            }
            if (a instanceof SkipTurnAction skipTurnAction) {
                Player p = engine.getPlayer(skipTurnAction.getPlayerId());
                if (p != null) {
                    p.endTurn();
                    renderFullState();
                }
            } else if (engine.getState() == GameState.PLANNING
                    && a.isValid(engine.getMap())
                    && canQueueClientAction(a)) {
                clientPendingActions.add(a);
            }
            return;
        }

        if (a instanceof SkipTurnAction skipTurnAction) {
            Player p = engine.getPlayer(skipTurnAction.getPlayerId());
            if (p != null) engine.endTurn(p);
        } else {
            if (engine.getState() != GameState.PLANNING) {
                return;
            }

            if (!a.isValid(engine.getMap())) {
                return;
            }

            Player owner = findOwnerByUnitId(a.getUnitId());
            if (owner == null) {
                return;
            }

            if (hotSeat) {
                Player active = getActivePlayer();
                if (active == null || !active.getUuid().equals(owner.getUuid())) {
                    return;
                }
            }

            Unit unit = null;
            for (Unit u : owner.getUnits()) {
                if (u.getId().equals(a.getUnitId())) {
                    unit = u;
                    break;
                }
            }

            if (unit != null) {
                long queuedActionsCount = 0;
                boolean hasEndedTurnAction = false;

                for (Action queued : owner.getPlannedActions()) {
                    if (queued.getUnitId() != null && queued.getUnitId().equals(unit.getId())) {
                        queuedActionsCount++;
                        if (queued instanceof AttackAction || queued instanceof HealAction) {
                            hasEndedTurnAction = true;
                        }
                    }
                }

                if (hasEndedTurnAction) { return; }
                if (queuedActionsCount >= unit.getActionsPerTurn()) { return; }
            }

            owner.getPlannedActions().add(a);
        }

        renderFullState();

        if (engine.allPlayersEndedTurn()) {
            engine.setState(GameState.RESOLVING);
            renderFullState();

            engine.resolveRound();
            GameStateUpdate update = engine.buildStateUpdate();

            autosaveIfEnabled();

            renderFullState();

            if (network != null && network.isHost()) {
                if (network instanceof ServerNetworkManager serverNetworkManager) {
                    serverNetworkManager.broadcastStateUpdate(update);
                }
            }
        }
    }

    /**
     * Applies an incoming {@link GameStateUpdate} received over the network
     * and refreshes the rendered view.
     *
     * @param u the update snapshot to apply to the local engine
     */
    public synchronized void applyStateUpdate(GameStateUpdate u) {
        if (u == null || engine == null) {
            return;
        }

        clientPendingActions.clear();
        u.apply(engine);
        renderFullState();
    }

    /**
     * Adopts the authoritative engine snapshot sent by the host when this peer
     * joins a networked game.
     *
     * <p>The client originally starts with its own engine built from
     * independent ids; once the host's authoritative engine arrives, the local
     * engine is replaced so that all unit and player ids match the host. The
     * player this client controls is identified by the position (turn order) of
     * its previous local player, so no extra id needs to be transmitted.</p>
     *
     * @param newEngine the authoritative engine snapshot to adopt
     */
    public synchronized void applyInitialState(GameEngine newEngine) {
        if (newEngine == null) {
            return;
        }

        int playerIndex = 1;
        if (engine != null && localPlayer != null && engine.getPlayers() != null) {
            int found = engine.getPlayers().indexOf(localPlayer);
            if (found >= 0) {
                playerIndex = found;
            }
        }

        List<Player> newPlayers = newEngine.getPlayers();
        Player adopted = null;
        if (newPlayers != null && !newPlayers.isEmpty()) {
            int idx = Math.min(playerIndex, newPlayers.size() - 1);
            adopted = newPlayers.get(idx);
        }

        this.engine = newEngine;
        this.hotSeat = false;
        this.localPlayer = adopted;
        this.selectedUnitId = null;
        this.inputMode = InputMode.NONE;
        this.clientPendingActions.clear();

        renderFullState();
    }

    /**
     * Initializes and starts a single-machine game session with no network
     * communication.
     *
     * <p>If the engine is still in the WAITING state, the controller transitions
     * it into the PLANNING phase before the first render.</p>
     */
    public void startLocalGame() {
        if (engine.getState() == GameState.WAITING) {
            engine.setState(GameState.PLANNING);
        }

        gameLoop();
    }

    /**
     * Initializes and starts a networked game session.
     *
     * <p>If this peer is the host, the server begins accepting incoming client
     * connections and the engine becomes authoritative for turn resolution.
     * If this peer is a client, it starts listening for incoming
     * {@link GameStateUpdate} snapshots from the host.</p>
     *
     * @throws IllegalStateException if network mode is requested without
     * a configured {@link NetworkManager}
     */
    public void startNetworkGame() {
        if (network == null) {
            throw new IllegalStateException("NetworkManager is required for network game.");
        }

        if (network.isHost()) {
            if (network instanceof ServerNetworkManager serverNetworkManager) {
                serverNetworkManager.startAccepting();
            }

            if (engine.getState() == GameState.WAITING) {
                engine.setState(GameState.PLANNING);
            }

            renderFullState();
        } else {
            if (network instanceof ClientNetworkManager clientNetworkManager) {
                clientNetworkManager.startListening(this);
            }
        }

        gameLoop();
    }

    /**
     * Performs autosave if the feature is enabled and persistence is configured.
     *
     * <p>If a dedicated autosave path is configured, it is used. Otherwise
     * the method falls back to the default path managed by
     * {@link GameSaveManager}.</p>
     */
    private void autosaveIfEnabled() {
        if (!autosaveEnabled || saveManager == null || engine == null) {
            return;
        }

        if (engine.getState() == GameState.RESOLVING) {
            return;
        }

        if (autosavePath == null || autosavePath.isBlank()) {
            saveManager.saveGame(engine);
        } else {
            saveManager.saveGame(engine, autosavePath);
        }
    }

    /**
     * Ensures that saving is currently allowed.
     *
     * <p>Saving is forbidden while the engine is resolving a round because the
     * internal game state may be in a transient intermediate form.</p>
     *
     * @throws IllegalStateException if the engine is not initialized or if the
     * game is currently in the RESOLVING phase
     */
    private void ensureSaveAllowed() {
        if (engine == null) {
            throw new IllegalStateException("GameEngine is not initialized.");
        }

        if (engine.getState() == GameState.RESOLVING) {
            throw new IllegalStateException("Saving during resolving phase is not allowed.");
        }
    }

    /**
     * Ensures that loading is currently allowed.
     *
     * <p>Loading is forbidden during the RESOLVING phase because swapping the
     * active engine mid-resolution would invalidate the turn lifecycle.</p>
     *
     * @throws IllegalStateException if the game is currently in the
     * RESOLVING phase
     */
    private void ensureLoadAllowed() {
        if (engine != null && engine.getState() == GameState.RESOLVING) {
            throw new IllegalStateException("Loading during resolving phase is not allowed.");
        }
    }

    /**
     * Ensures that a {@link GameSaveManager} is available.
     *
     * @throws IllegalStateException if no save manager has been configured
     */
    private void ensureSaveManager() {
        if (saveManager == null) {
            throw new IllegalStateException("GameSaveManager is not configured.");
        }
    }

    /**
     * Finds the player that owns the unit referenced by the given unit UUID.
     *
     * @param unitId UUID of the unit whose owner should be found
     * @return the owning player, or {@code null} if no matching unit exists
     */
    private Player findOwnerByUnitId(UUID unitId) {
        if (unitId == null) {
            return null;
        }

        for (Player player : engine.getPlayers()) {
            for (Unit unit : player.getUnits()) {
                if (unit.getId().equals(unitId)) {
                    return player;
                }
            }
        }

        return null;
    }

    /**
     * Decides whether another action may be predicted locally for the unit
     * referenced by the given action, mirroring the per-unit action economy the
     * host enforces (at most {@code actionsPerTurn} actions, and no further
     * actions once an attack or heal has been queued).
     *
     * @param a the candidate action
     * @return {@code true} if the action fits within the unit's remaining budget
     */
    private boolean canQueueClientAction(Action a) {
        Player owner = findOwnerByUnitId(a.getUnitId());
        if (owner == null) {
            return false;
        }

        Unit unit = null;
        for (Unit u : owner.getUnits()) {
            if (u.getId().equals(a.getUnitId())) {
                unit = u;
                break;
            }
        }
        if (unit == null) {
            return false;
        }

        long queued = 0;
        boolean hasFinalizing = false;
        for (Action q : clientPendingActions) {
            if (q.getUnitId() != null && q.getUnitId().equals(unit.getId())) {
                queued++;
                if (q instanceof AttackAction || q instanceof HealAction) {
                    hasFinalizing = true;
                }
            }
        }

        if (hasFinalizing) {
            return false;
        }
        return queued < unit.getActionsPerTurn();
    }

    /**
     * Renders the complete visible game state.
     *
     * <p>The controller first renders the map, then each unit, then the HUD,
     * and finally the list of planned actions gathered from all players.</p>
     */
    private void renderFullState() {
        if (renderer == null || engine == null) {
            return;
        }

        renderer.renderMap(engine.getMap());

        for (Player player : engine.getPlayers()) {
            for (Unit unit : player.getUnits()) {
                renderer.renderUnit(unit);
            }
        }

        renderer.renderHUD(engine.getPlayers(), engine.getState(), engine.getWinnerId());
        renderer.renderPlannedActions(collectPlannedActions());
    }

    /**
     * Collects all actions currently planned by all players in the current round.
     *
     * @return a flat list containing every queued action from every player
     */
    private List<Action> collectPlannedActions() {
        List<Action> plannedActions = new ArrayList<>();

        if (engine == null || engine.getPlayers() == null) {
            return plannedActions;
        }

        for (Player player : engine.getPlayers()) {
            if (player.getPlannedActions() != null) {
                plannedActions.addAll(player.getPlannedActions());
            }
        }

        return plannedActions;
    }
    /**
     * Returns a read-only snapshot of the current game for graphical rendering.
     *
     * <p>This method is intended for graphical user interface layers such as
     * Swing, JavaFX, libGDX adapters, or any custom visual renderer that needs
     * one aggregated state object to paint the current screen.</p>
     *
     * <p>The returned view object should contain only data needed for rendering
     * and HUD presentation, not full mutable domain objects.</p>
     *
     * @return renderable game snapshot
     * @throws IllegalStateException if the engine is not initialized
     */
    public synchronized GameView getGameView() {
        if (engine == null) {
            throw new IllegalStateException("GameEngine is not initialized.");
        }

        return GameViewMapper.fromController(this);
    }

    /**
     * Returns a flat list of actions currently queued by all players.
     *
     * <p>This is useful for graphical HUD panels, debug overlays, sidebars,
     * replay widgets, or turn-preview windows.</p>
     *
     * @return defensive copy of currently planned actions
     */
    public synchronized List<Action> getPlannedActionsView() {
        List<Action> all = new ArrayList<>(collectPlannedActions());
        all.addAll(clientPendingActions);
        return all;
    }

    /**
     * Returns the currently selected gameplay phase for UI state switching.
     *
     * <p>The graphical layer may use this to switch between planning, resolving,
     * waiting, draw, or finished screens.</p>
     *
     * @return current game phase
     * @throws IllegalStateException if the engine is not initialized
     */
    public synchronized GameState getCurrentPhase() {
        if (engine == null) {
            throw new IllegalStateException("GameEngine is not initialized.");
        }

        return engine.getState();
    }

    /**
     * Returns the identifier of the winner, if the game has already ended.
     *
     * <p>The graphical interface may use this to display victory banners,
     * result screens, or match summary widgets.</p>
     *
     * @return winner UUID, or {@code null} if there is no winner yet
     * @throws IllegalStateException if the engine is not initialized
     */
    public synchronized UUID getWinnerIdForView() {
        if (engine == null) {
            throw new IllegalStateException("GameEngine is not initialized.");
        }

        return engine.getWinnerId();
    }

    /**
     * Returns a snapshot of all players for UI panels and overlays.
     *
     * <p>This method is intended for scoreboards, side panels, turn indicators,
     * and player summary widgets.</p>
     *
     * @return defensive copy of players
     * @throws IllegalStateException if the engine is not initialized
     */
    public synchronized List<Player> getPlayersView() {
        if (engine == null) {
            throw new IllegalStateException("GameEngine is not initialized.");
        }

        return new ArrayList<>(engine.getPlayers());
    }

    public synchronized GameEngine getEngine() {
        if (engine == null) {
            throw new IllegalStateException("GameEngine is not initialized.");
        }

        return engine;
    }

    /**
     * Handles a tile click from the graphical interface as a move request.
     *
     * <p>This is a GUI-friendly convenience method for interfaces where the user
     * selects a unit first and then clicks a destination tile on the map.</p>
     *
     * @param unitId UUID of the unit selected in the GUI
     * @param x destination tile X coordinate
     * @param y destination tile Y coordinate
     * @throws IllegalStateException if the engine or map is not initialized
     * @throws IllegalArgumentException if the clicked tile does not exist
     */
    public synchronized void requestMoveToTile(UUID unitId, int x, int y) {
        if (engine == null || engine.getMap() == null) {
            throw new IllegalStateException("GameEngine is not initialized.");
        }

        Tile destination = engine.getMap().getTile(x, y);
        if (destination == null) {
            throw new IllegalArgumentException("Destination tile does not exist.");
        }

        onMoveRequested(unitId, destination);
    }

    /**
     * Handles a unit-to-unit attack selection from the graphical interface.
     *
     * <p>This is intended for GUI flows where the player selects an attacking
     * unit and then clicks an enemy unit on the board.</p>
     *
     * @param attackerId UUID of the selected attacking unit
     * @param targetId UUID of the clicked target unit
     */
    public synchronized void requestAttackUnit(UUID attackerId, UUID targetId) {
        onAttackRequested(attackerId, targetId);
    }

    /**
     * Handles a mage-to-ally heal selection from the graphical interface.
     *
     * <p>Intended for GUI flows where the player selects a mage, presses the
     * heal action button, and then clicks a friendly target unit.</p>
     *
     * @param casterId UUID of the selected healing mage
     * @param targetId UUID of the clicked friendly target unit
     */
    public synchronized void requestHealUnit(UUID casterId, UUID targetId) {
        onHealRequested(casterId, targetId);
    }

    /**
     * Handles a wait command from the graphical interface.
     *
     * <p>This method is intended for action bars, context menus, or keyboard
     * shortcuts in the GUI.</p>
     *
     * @param unitId UUID of the unit that should wait
     */
    public synchronized void requestUnitWait(UUID unitId) {
        onWaitRequested(unitId);
    }

    /**
     * Handles an end-turn request from the graphical interface.
     *
     * <p>This method is intended for turn buttons, hotkeys, or turn-control
     * panels in a graphical user interface.</p>
     *
     * @param playerId UUID of the player ending the turn
     */
    public synchronized void requestPlayerEndTurn(UUID playerId) {
        onEndTurnRequested(playerId);
        selectedUnitId = null;
        inputMode = InputMode.NONE;
    }

    /**
     * Handles a textual debug or developer command entered from a GUI console.
     *
     * <p>This may be used by debug panels, host tools, or in-game admin consoles.</p>
     *
     * @param commandLine raw command text entered in the GUI console
     * @param playerId UUID of the player issuing the command
     */
    public synchronized void submitGuiCommand(String commandLine, UUID playerId) {
        onConsoleCommand(commandLine, playerId);
    }

    /**
     * Requests a manual save from the graphical interface using the default path.
     *
     * <p>This method is intended for pause menus, toolbar buttons, or host-only
     * configuration panels in the GUI.</p>
     */
    public synchronized void requestSaveGame() {
        saveGame();
    }

    /**
     * Requests a manual save from the graphical interface using an explicit path.
     *
     * <p>This is useful for desktop GUI save dialogs or developer tooling.</p>
     *
     * @param path target save path chosen by the user
     */
    public synchronized void requestSaveGame(String path) {
        saveGame(path);
    }

    /**
     * Requests loading a saved game from the graphical interface.
     *
     * <p>This is intended for load-game menus, desktop file pickers, or host tools.</p>
     *
     * @param path source save path chosen by the user
     */
    public synchronized void requestLoadGame(String path) {
        loadGame(path);
    }

    /**
     * Enables autosave through the graphical interface.
     *
     * <p>This is intended for settings screens and match configuration windows.</p>
     *
     * @param path autosave path, or blank to use the default save path
     */
    public synchronized void enableAutosaveFromSettings(String path) {
        enableAutosave(path);
    }

    /**
     * Disables autosave through the graphical interface.
     *
     * <p>This is intended for settings screens and match configuration windows.</p>
     */
    public synchronized void disableAutosaveFromSettings() {
        disableAutosave();
    }

    public synchronized UUID getSelectedUnitId(){return selectedUnitId;}
    public synchronized void setSelectedUnitId(UUID id) { this.selectedUnitId = id; }

    /**
     * Configures which player the local UI represents.
     *
     * @param localPlayer the perspective/fallback player on this machine
     * @param hotSeat {@code true} for a local hot-seat session where players
     * take turns planning on the same machine
     */
    public synchronized void configureLocalPlayer(Player localPlayer, boolean hotSeat) {
        this.localPlayer = localPlayer;
        this.hotSeat = hotSeat;
    }

    /**
     * Returns whether this session is a local hot-seat game.
     *
     * @return {@code true} for local multiplayer on a single machine
     */
    public synchronized boolean isHotSeat() {
        return hotSeat;
    }

    /**
     * Returns whether this peer is a non-authoritative network client.
     *
     * <p>Used by the UI to restrict save/load to the host (or local games),
     * since a client only holds a synchronised copy of the host's state.</p>
     *
     * @return {@code true} if connected as a client, {@code false} for host or local play
     */
    public synchronized boolean isNetworkClient() {
        return network != null && !network.isHost();
    }

    /**
     * Returns the player the UI should currently treat as "the local player".
     *
     *
     * @return the active player for the UI, or {@code null} if none is available
     */
    public synchronized Player getActivePlayer() {
        if (engine == null) {
            return localPlayer;
        }
        if (hotSeat && engine.getState() == GameState.PLANNING && engine.getPlayers() != null) {
            for (Player p : engine.getPlayers()) {
                if (!p.isTurnEnded()) {
                    return p;
                }
            }
        }
        if (localPlayer != null) {
            return localPlayer;
        }
        List<Player> ps = engine.getPlayers();
        return (ps == null || ps.isEmpty()) ? null : ps.get(0);
    }

    /**
     * Returns the current UI input mode used to interpret the next map click.
     *
     * @return current input mode, never {@code null}
     */
    public synchronized InputMode getInputMode() { return inputMode; }

    /**
     * Sets the current UI input mode. Used by action buttons to switch the
     * interface into move/attack/heal target-selection mode.
     *
     * @param mode the new input mode; {@code null} is treated as {@link InputMode#NONE}
     */
    public synchronized void setInputMode(InputMode mode) {
        this.inputMode = (mode == null) ? InputMode.NONE : mode;
    }
}