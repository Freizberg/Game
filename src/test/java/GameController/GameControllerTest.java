package GameController;

import Action.Action;
import Action.AttackAction;
import Action.HealAction;
import Action.MoveAction;
import Action.SkipTurnAction;
import Action.WaitAction;
import GameEngine.GameEngine;
import GameEngine.GameState;
import GameEngine.Player;
import GameRenderer.GameRenderer;
import Map.GameMap;
import Map.Tile;
import Map.TileType;
import NetworkManager.ClientNetworkManager;
import NetworkManager.ServerNetworkManager;
import Units.Mage;
import Units.Unit;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Yevhenii Marienko
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GameControllerTest {

    private GameEngine engine;
    private RecordingRenderer renderer;
    private RecordingSaveManager saveManager;
    private StubInputHandler inputHandler;

    @BeforeEach
    void setUp() {
        engine = TestGameFactory.createEngine(GameState.WAITING);
        renderer = new RecordingRenderer();
        saveManager = new RecordingSaveManager();
        inputHandler = new StubInputHandler();
    }

    @Nested
    class GameStartTests {

        @Test
        void gameLoop_true_starts_local_game_and_switches_waiting_to_planning() {
            GameController controller = new GameController(engine, renderer, null, saveManager, inputHandler);

            controller.gameLoop(true);

            assertEquals(GameState.PLANNING, engine.getState());
            assertEquals(1, renderer.renderMapCalls);
            assertEquals(4, renderer.renderUnitCalls);
            assertEquals(1, renderer.renderHudCalls);
            assertEquals(1, renderer.renderPlannedActionsCalls);
        }

        @Test
        void gameLoop_false_without_network_falls_back_to_local_game() {
            GameController controller = new GameController(engine, renderer, null, saveManager, inputHandler);

            controller.gameLoop(false);

            assertEquals(GameState.PLANNING, engine.getState());
            assertEquals(1, renderer.renderMapCalls);
        }

        @Test
        void startNetworkGame_without_network_throws_exception() {
            GameController controller = new GameController(engine, renderer, null, saveManager, inputHandler);

            IllegalStateException exception = assertThrows(IllegalStateException.class, controller::startNetworkGame);

            assertEquals("NetworkManager is required for network game.", exception.getMessage());
        }

        @Test
        void startNetworkGame_as_host_starts_accepting_sets_planning_and_renders() {
            FakeServerNetworkManager hostNetwork = new FakeServerNetworkManager();
            GameController controller = new GameController(engine, renderer, hostNetwork, saveManager, inputHandler);

            controller.startNetworkGame();

            assertTrue(hostNetwork.startAcceptingCalled);
            assertEquals(GameState.PLANNING, engine.getState());
            assertEquals(2, renderer.renderMapCalls);
        }

        @Test
        void startNetworkGame_as_client_starts_listening() {
            FakeClientNetworkManager clientNetwork = new FakeClientNetworkManager();
            GameController controller = new GameController(engine, renderer, clientNetwork, saveManager, inputHandler);

            controller.startNetworkGame();

            assertTrue(clientNetwork.startListeningCalled);
            assertSame(controller, clientNetwork.listeningController);
            assertEquals(1, renderer.renderMapCalls);
        }
    }

    @Nested
    class SaveLoadTests {

        @Test
        void saveGame_uses_default_save_manager_path() {
            engine.setState(GameState.PLANNING);
            GameController controller = new GameController(engine, renderer, null, saveManager, inputHandler);

            controller.saveGame();

            assertSame(engine, saveManager.savedEngineDefault);
            assertNull(saveManager.savedPath);
        }

        @Test
        void saveGame_with_explicit_path_passes_path_to_save_manager() {
            engine.setState(GameState.PLANNING);
            GameController controller = new GameController(engine, renderer, null, saveManager, inputHandler);

            controller.saveGame("save-01.dat");

            assertSame(engine, saveManager.savedEngineWithPath);
            assertEquals("save-01.dat", saveManager.savedPath);
        }

        @Test
        void saveGame_during_resolving_throws_exception() {
            engine.setState(GameState.RESOLVING);
            GameController controller = new GameController(engine, renderer, null, saveManager, inputHandler);

            IllegalStateException exception = assertThrows(IllegalStateException.class, controller::saveGame);

            assertEquals("Saving during resolving phase is not allowed.", exception.getMessage());
        }

        @Test
        void loadGame_during_resolving_throws_exception() {
            engine.setState(GameState.RESOLVING);
            GameController controller = new GameController(engine, renderer, null, saveManager, inputHandler);

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> controller.loadGame("save-01.dat"));

            assertEquals("Loading during resolving phase is not allowed.", exception.getMessage());
        }

        @Test
        void loadGame_replaces_engine_and_renders_loaded_state() {
            GameEngine loadedEngine = TestGameFactory.createEngine(GameState.PLANNING);
            saveManager.engineToLoad = loadedEngine;
            GameController controller = new GameController(engine, renderer, null, saveManager, inputHandler);

            controller.loadGame("save-01.dat");

            assertSame(loadedEngine, readEngine(controller));
            assertEquals("save-01.dat", saveManager.loadedPath);
            assertEquals(1, renderer.renderMapCalls);
        }

        @Test
        void loadGame_when_save_manager_returns_null_keeps_existing_engine() {
            GameController controller = new GameController(engine, renderer, null, saveManager, inputHandler);

            controller.loadGame("missing.dat");

            assertSame(engine, readEngine(controller));
            assertEquals(0, renderer.renderMapCalls);
        }

        @Test
        void saveGame_without_save_manager_throws_exception() {
            GameController controller = new GameController(engine, renderer, null, null, inputHandler);

            IllegalStateException exception = assertThrows(IllegalStateException.class, controller::saveGame);

            assertEquals("GameSaveManager is not configured.", exception.getMessage());
        }
    }

    @Nested
    class AutosaveTests {

        @Test
        void autosave_can_be_enabled_and_disabled() {
            GameController controller = new GameController(engine, renderer, null, saveManager, inputHandler);

            controller.enableAutosave("autosave.dat");
            assertTrue(controller.isAutosaveEnabled());

            controller.disableAutosave();
            assertFalse(controller.isAutosaveEnabled());
        }

        @Test
        void autosave_runs_after_round_resolution_when_all_players_end_turn() {
            engine.setState(GameState.PLANNING);
            GameController controller = new GameController(engine, renderer, null, saveManager, inputHandler);
            controller.enableAutosave("autosave.dat");

            List<Player> players = engine.getPlayers();
            controller.handleInput(new SkipTurnAction(players.get(0).getUuid()));
            controller.handleInput(new SkipTurnAction(players.get(1).getUuid()));

            assertSame(engine, saveManager.savedEngineWithPath);
            assertEquals("autosave.dat", saveManager.savedPath);
            assertEquals(GameState.PLANNING, engine.getState());
            assertEquals(2, engine.getCurrentRound());
        }
    }

    @Nested
    class InputEntryTests {

        @Test
        void onMoveRequested_delegates_to_input_handler_and_routes_action() {
            FakeClientNetworkManager clientNetwork = new FakeClientNetworkManager();
            GameController controller = new GameController(engine, renderer, clientNetwork, saveManager, inputHandler);
            Unit unit = engine.getPlayers().get(0).getUnits().get(0);
            Tile destination = engine.getMap().getTile(2, 0);
            MoveAction expected = new MoveAction(unit.getId(), destination.getX(), destination.getY());
            inputHandler.moveAction = expected;

            controller.onMoveRequested(unit.getId(), destination);

            assertEquals(unit.getId(), inputHandler.lastMoveUnitId);
            assertSame(destination, inputHandler.lastMoveDestination);
            assertSame(expected, clientNetwork.sentAction);
        }

        @Test
        void onAttackRequested_delegates_to_input_handler_and_routes_action() {
            FakeClientNetworkManager clientNetwork = new FakeClientNetworkManager();
            GameController controller = new GameController(engine, renderer, clientNetwork, saveManager, inputHandler);
            Unit attacker = engine.getPlayers().get(0).getUnits().get(0);
            Unit target = engine.getPlayers().get(1).getUnits().get(0);
            AttackAction expected = new AttackAction(attacker.getId(), target.getId());
            inputHandler.attackAction = expected;

            controller.onAttackRequested(attacker.getId(), target.getId());

            assertEquals(attacker.getId(), inputHandler.lastAttackAttackerId);
            assertEquals(target.getId(), inputHandler.lastAttackTargetId);
            assertSame(expected, clientNetwork.sentAction);
        }

        @Test
        void onWaitRequested_delegates_to_input_handler_and_routes_action() {
            FakeClientNetworkManager clientNetwork = new FakeClientNetworkManager();
            GameController controller = new GameController(engine, renderer, clientNetwork, saveManager, inputHandler);
            Unit unit = engine.getPlayers().get(0).getUnits().get(0);
            WaitAction expected = new WaitAction(unit.getId());
            inputHandler.waitAction = expected;

            controller.onWaitRequested(unit.getId());

            assertEquals(unit.getId(), inputHandler.lastWaitUnitId);
            assertSame(expected, clientNetwork.sentAction);
        }

        @Test
        void onEndTurnRequested_delegates_to_input_handler_and_routes_action() {
            FakeClientNetworkManager clientNetwork = new FakeClientNetworkManager();
            GameController controller = new GameController(engine, renderer, clientNetwork, saveManager, inputHandler);
            UUID playerId = engine.getPlayers().get(0).getUuid();
            SkipTurnAction expected = new SkipTurnAction(playerId);
            inputHandler.endTurnAction = expected;

            controller.onEndTurnRequested(playerId);

            assertEquals(playerId, inputHandler.lastEndTurnPlayerId);
            assertSame(expected, clientNetwork.sentAction);
        }

        @Test
        void onConsoleCommand_delegates_to_parser_and_routes_action() {
            FakeClientNetworkManager clientNetwork = new FakeClientNetworkManager();
            GameController controller = new GameController(engine, renderer, clientNetwork, saveManager, inputHandler);
            UUID playerId = engine.getPlayers().get(0).getUuid();
            SkipTurnAction expected = new SkipTurnAction(playerId);
            inputHandler.consoleAction = expected;

            controller.onConsoleCommand("endturn", playerId);

            assertEquals("endturn", inputHandler.lastCommandLine);
            assertEquals(playerId, inputHandler.lastConsolePlayerId);
            assertSame(engine, inputHandler.lastConsoleEngine);
            assertSame(expected, clientNetwork.sentAction);
        }
    }

    @Nested
    class HandleInputTests {

        @Test
        void handleInput_null_action_does_nothing() {
            GameController controller = new GameController(engine, renderer, null, saveManager, inputHandler);

            controller.handleInput(null);

            assertEquals(0, renderer.renderMapCalls);
        }

        @Test
        void handleInput_when_game_is_finished_only_renders() {
            engine.setState(GameState.FINISHED);
            GameController controller = new GameController(engine, renderer, null, saveManager, inputHandler);

            controller.handleInput(new SkipTurnAction(UUID.randomUUID()));

            assertEquals(1, renderer.renderMapCalls);
            assertEquals(1, renderer.renderHudCalls);
        }

        @Test
        void handleInput_on_remote_client_sends_action_to_host_without_mutating_engine() {
            engine.setState(GameState.PLANNING);
            FakeClientNetworkManager clientNetwork = new FakeClientNetworkManager();
            GameController controller = new GameController(engine, renderer, clientNetwork, saveManager, inputHandler);
            Unit unit = engine.getPlayers().get(0).getUnits().get(0);
            MoveAction action = new MoveAction(unit.getId(), 2, 0);

            controller.handleInput(action);

            assertSame(action, clientNetwork.sentAction);
            assertTrue(engine.getPlayers().get(0).getPlannedActions().isEmpty());
            assertEquals(0, renderer.renderMapCalls);
        }

        @Test
        void handleInput_skip_turn_marks_player_turn_as_ended() {
            engine.setState(GameState.PLANNING);
            GameController controller = new GameController(engine, renderer, null, saveManager, inputHandler);
            Player player = engine.getPlayers().get(0);

            controller.handleInput(new SkipTurnAction(player.getUuid()));

            assertTrue(player.isTurnEnded());
            assertEquals(1, renderer.renderMapCalls);
        }

        @Test
        void handleInput_non_skip_action_outside_planning_is_ignored() {
            engine.setState(GameState.WAITING);
            GameController controller = new GameController(engine, renderer, null, saveManager, inputHandler);
            Unit unit = engine.getPlayers().get(0).getUnits().get(0);
            MoveAction action = new MoveAction(unit.getId(), 2, 0);

            controller.handleInput(action);

            assertTrue(engine.getPlayers().get(0).getPlannedActions().isEmpty());
            assertEquals(0, renderer.renderMapCalls);
        }

        @Test
        void handleInput_invalid_action_is_ignored() {
            engine.setState(GameState.PLANNING);
            GameController controller = new GameController(engine, renderer, null, saveManager, inputHandler);
            Unit unit = engine.getPlayers().get(0).getUnits().get(0);
            MoveAction action = new MoveAction(unit.getId(), 99, 99);

            controller.handleInput(action);

            assertTrue(engine.getPlayers().get(0).getPlannedActions().isEmpty());
            assertEquals(0, renderer.renderMapCalls);
        }

        @Test
        void handleInput_action_for_unknown_unit_owner_is_ignored() {
            engine.setState(GameState.PLANNING);
            GameController controller = new GameController(engine, renderer, null, saveManager, inputHandler);
            MoveAction action = new MoveAction(UUID.randomUUID(), 2, 0);

            controller.handleInput(action);

            assertTrue(engine.getPlayers().stream().allMatch(player -> player.getPlannedActions().isEmpty()));
            assertEquals(0, renderer.renderMapCalls);
        }

        @Test
        void handleInput_valid_non_skip_action_is_added_to_owners_queue() {
            engine.setState(GameState.PLANNING);
            GameController controller = new GameController(engine, renderer, null, saveManager, inputHandler);
            Unit unit = engine.getPlayers().get(0).getUnits().get(0);
            MoveAction action = new MoveAction(unit.getId(), 2, 0);

            controller.handleInput(action);

            assertEquals(1, engine.getPlayers().get(0).getPlannedActions().size());
            assertSame(action, engine.getPlayers().get(0).getPlannedActions().get(0));
            assertEquals(1, renderer.renderMapCalls);
        }

        @Test
        void handleInput_when_host_finishes_round_broadcasts_state_update() {
            engine.setState(GameState.PLANNING);
            FakeServerNetworkManager hostNetwork = new FakeServerNetworkManager();
            GameController controller = new GameController(engine, renderer, hostNetwork, saveManager, inputHandler);
            List<Player> players = engine.getPlayers();

            controller.handleInput(new SkipTurnAction(players.get(0).getUuid()));
            controller.handleInput(new SkipTurnAction(players.get(1).getUuid()));

            assertNotNull(hostNetwork.broadcastedUpdate);
            assertEquals(GameState.PLANNING, engine.getState());
            assertEquals(2, engine.getCurrentRound());
            assertTrue(renderer.renderMapCalls >= 3);
        }
    }

    @Nested
    class StateUpdateTests {

        @Test
        void applyStateUpdate_applies_update_and_renders() {
            engine.setState(GameState.WAITING);
            GameController controller = new GameController(engine, renderer, null, saveManager, inputHandler);
            Unit unit = engine.getPlayers().get(0).getUnits().get(0);

            GameStateUpdate update = new GameStateUpdate();
            update.currentRound = 7;
            update.unitHP.put(unit.getId(), 3);
            update.unitPositions.put(unit.getId(), new int[]{2, 2});

            controller.applyStateUpdate(update);

            assertEquals(7, engine.getCurrentRound());
            assertEquals(3, unit.getHp());
            assertEquals(2, unit.getPosX());
            assertEquals(2, unit.getPosY());
            assertSame(unit, engine.getMap().getTile(2, 2).getUnit());
            assertEquals(GameState.PLANNING, engine.getState());
            assertEquals(1, renderer.renderMapCalls);
        }
    }

    private static GameEngine readEngine(GameController controller) {
        try {
            Field field = GameController.class.getDeclaredField("engine");
            field.setAccessible(true);
            return (GameEngine) field.get(controller);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to read controller engine.", e);
        }
    }

    private static final class RecordingRenderer implements GameRenderer {
        private int renderMapCalls;
        private int renderUnitCalls;
        private int renderHudCalls;
        private int renderPlannedActionsCalls;

        @Override
        public void renderMap(GameMap m) {
            renderMapCalls++;
        }

        @Override
        public void renderUnit(Unit u) {
            renderUnitCalls++;
        }

        @Override
        public void renderHUD(List<Player> p, GameState state, UUID winnerId) {
            renderHudCalls++;
        }

        @Override
        public void renderPlannedActions(List<Action> a) {
            renderPlannedActionsCalls++;
        }
    }

    private static final class RecordingSaveManager extends GameSaveManager {
        private GameEngine savedEngineDefault;
        private GameEngine savedEngineWithPath;
        private String savedPath;
        private String loadedPath;
        private GameEngine engineToLoad;

        @Override
        public void saveGame(GameEngine engine) {
            this.savedEngineDefault = engine;
            this.savedPath = null;
        }

        @Override
        public void saveGame(GameEngine engine, String path) {
            this.savedEngineWithPath = engine;
            this.savedPath = path;
        }

        @Override
        public GameEngine loadGame(String path) {
            this.loadedPath = path;
            return engineToLoad;
        }
    }

    private static final class StubInputHandler extends InputHandler {
        private UUID lastMoveUnitId;
        private Tile lastMoveDestination;
        private UUID lastAttackAttackerId;
        private UUID lastAttackTargetId;
        private UUID lastHealCasterId;
        private UUID lastHealTargetId;
        private UUID lastWaitUnitId;
        private UUID lastEndTurnPlayerId;
        private String lastCommandLine;
        private UUID lastConsolePlayerId;
        private GameEngine lastConsoleEngine;

        private MoveAction moveAction;
        private AttackAction attackAction;
        private HealAction healAction;
        private WaitAction waitAction;
        private SkipTurnAction endTurnAction;
        private Action consoleAction;

        @Override
        public MoveAction onMoveUnit(UUID unitId, Tile dest) {
            this.lastMoveUnitId = unitId;
            this.lastMoveDestination = dest;
            return moveAction;
        }

        @Override
        public AttackAction onAttack(UUID attackerId, UUID targetId) {
            this.lastAttackAttackerId = attackerId;
            this.lastAttackTargetId = targetId;
            return attackAction;
        }

        @Override
        public HealAction onHeal(UUID casterId, UUID targetId) {
            this.lastHealCasterId = casterId;
            this.lastHealTargetId = targetId;
            return healAction;
        }

        @Override
        public WaitAction onWait(UUID unitId) {
            this.lastWaitUnitId = unitId;
            return waitAction;
        }

        @Override
        public SkipTurnAction onEndTurn(UUID playerId) {
            this.lastEndTurnPlayerId = playerId;
            return endTurnAction;
        }

        @Override
        public Action parseConsoleCommand(String commandLine, UUID currentPlayerId, GameEngine engine) {
            this.lastCommandLine = commandLine;
            this.lastConsolePlayerId = currentPlayerId;
            this.lastConsoleEngine = engine;
            return consoleAction;
        }
    }

    private static final class FakeClientNetworkManager extends ClientNetworkManager {
        private Action sentAction;
        private boolean startListeningCalled;
        private GameController listeningController;

        private FakeClientNetworkManager() {
            super("127.0.0.1", 65535);
        }

        @Override
        public void connect(String host, int port) {
        }

        @Override
        public synchronized void sendAction(Action a) {
            this.sentAction = a;
        }

        @Override
        public void startListening(GameController ctrl) {
            this.startListeningCalled = true;
            this.listeningController = ctrl;
        }

        @Override
        public void disconnect() {
        }
    }

    private static final class FakeServerNetworkManager extends ServerNetworkManager {
        private boolean startAcceptingCalled;
        private GameStateUpdate broadcastedUpdate;

        private FakeServerNetworkManager() {
            super(9999, null);
        }

        @Override
        public void startAccepting() {
            this.startAcceptingCalled = true;
        }

        @Override
        public void broadcastStateUpdate(GameStateUpdate u) {
            this.broadcastedUpdate = u;
        }

        @Override
        public void shutdown() {
        }
    }

    private static final class TestUnit extends Unit {
        private TestUnit(String name, int posX, int posY) {
            super(name, 10, 3, 3, posX, posY);
            setAttackRange(1);
        }
    }

    private static final class TestGameFactory {
        private static GameEngine createEngine(GameState initialState) {
            GameEngine engine = new GameEngine();
            GameMap map = new GameMap(5, 5);

            Player playerOne = new Player("Player One");
            Player playerTwo = new Player("Player Two");

            TestUnit p1UnitA = new TestUnit("P1-A", 0, 0);
            TestUnit p1UnitB = new TestUnit("P1-B", 1, 0);
            TestUnit p2UnitA = new TestUnit("P2-A", 4, 4);
            TestUnit p2UnitB = new TestUnit("P2-B", 3, 4);

            playerOne.setUnits(new ArrayList<>(List.of(p1UnitA, p1UnitB)));
            playerTwo.setUnits(new ArrayList<>(List.of(p2UnitA, p2UnitB)));

            map.placeUnit(p1UnitA, 0, 0);
            map.placeUnit(p1UnitB, 1, 0);
            map.placeUnit(p2UnitA, 4, 4);
            map.placeUnit(p2UnitB, 3, 4);
            map.setTileType(2, 1, TileType.FOREST);

            engine.setMap(map);
            engine.setPlayers(new ArrayList<>(List.of(playerOne, playerTwo)));
            engine.setState(initialState);
            engine.setCurrentRound(1);
            return engine;
        }
    }
    @Nested
    @DisplayName("Action Queuing Rules")
    class ActionQueuingLogicTests {

        private Player player;
        private Unit unit1;
        private Unit unit2;
        private Unit enemyUnit;
        private GameController controller;

        @BeforeEach
        void setUpQueueTests() {
            // Używamy gotowej fabryki z GameControllerTest, która ustawia stan na PLANNING
            engine = TestGameFactory.createEngine(GameState.PLANNING);
            controller = new GameController(engine, renderer, null, saveManager, inputHandler);

            player = engine.getPlayers().get(0);
            Player enemy = engine.getPlayers().get(1);

            unit1 = player.getUnits().get(0); // TestUnit, domyślnie 3 actionsPerTurn
            unit2 = player.getUnits().get(1); // TestUnit, domyślnie 3 actionsPerTurn
            enemyUnit = enemy.getUnits().get(0);
        }

        @Test
        @DisplayName("Lets to que actions till limit of actionsPerTurn.")
        void should_allow_moves_up_to_actions_per_turn_limit() {
            int maxActions = unit1.getActionsPerTurn(); // dla TestUnit to 3

            for (int i = 0; i < maxActions; i++) {
                controller.handleInput(new MoveAction(unit1.getId(), 2, i));
            }

            assertEquals(maxActions, player.getPlannedActions().size(), "Should make possible adding maximal amount of allowed actions.");

        }

        @Test
        @DisplayName("Revokes actions above the limit actionsPerTurn")
        void should_reject_actions_exceeding_actions_per_turn_limit() {
            int maxActions = unit1.getActionsPerTurn();

            for (int i = 0; i < maxActions + 1; i++) {
                controller.handleInput(new MoveAction(unit1.getId(), 2, i));
            }

            assertEquals(maxActions, player.getPlannedActions().size(),
                    "Actions above the limit should be ignored");
        }

        @Test
        @DisplayName("Lets making move and than attack in the same round.")
        void should_allow_move_then_attack() {
            MoveAction move = new MoveAction(unit1.getId(), 2, 2);
            AttackAction attack = new AttackAction(unit1.getId(), enemyUnit.getId());

            controller.handleInput(move);
            controller.handleInput(attack);

            assertEquals(2, player.getPlannedActions().size());
            assertInstanceOf(MoveAction.class, player.getPlannedActions().get(0));
            assertInstanceOf(AttackAction.class, player.getPlannedActions().get(1));
        }

        @Test
        @DisplayName("Reject every action after the attack was planned, (attack ends the round)")
        void should_reject_any_action_after_attack() {
            AttackAction attack = new AttackAction(unit1.getId(), enemyUnit.getId());
            MoveAction move = new MoveAction(unit1.getId(), 2, 2);
            WaitAction wait = new WaitAction(unit1.getId());

            controller.handleInput(attack);

            controller.handleInput(move);
            controller.handleInput(wait);
            controller.handleInput(new AttackAction(unit1.getId(), enemyUnit.getId())); // kolejny atak

            assertEquals(1, player.getPlannedActions().size(),
                    "Que should have only one attack!");
            assertInstanceOf(AttackAction.class, player.getPlannedActions().get(0));
        }

        @Test
        @DisplayName("Rejects every action after heal, (no actions are allowed after heal)")
        void should_reject_any_action_after_heal() {
            Mage mage = new Mage("TestMage", 2, 0);
            player.getUnits().add(mage);
            engine.getMap().placeUnit(mage, 2, 0);

            HealAction heal = new HealAction(mage.getId(), unit2.getId());
            MoveAction move = new MoveAction(mage.getId(), 2, 1);

            controller.handleInput(heal);
            controller.handleInput(move);

            List<Action> mageActions = player.getPlannedActions().stream()
                    .filter(a -> a.getUnitId().equals(mage.getId()))
                    .toList();

            assertEquals(1, mageActions.size(),
                    "There should be no actions after heal for mage, in the que");
            assertInstanceOf(HealAction.class, mageActions.get(0));
        }

        @Test
        @DisplayName("Block after the attack of one unit has no effect on other units of the same player")
        void should_allow_independent_action_queuing_for_multiple_units() {
            controller.handleInput(new AttackAction(unit1.getId(), enemyUnit.getId()));

            controller.handleInput(new MoveAction(unit2.getId(), 3, 3));

            controller.handleInput(new MoveAction(unit1.getId(), 1, 1));

            List<Action> queued = player.getPlannedActions();
            assertEquals(2, queued.size(), "Only attack from Unit1 and move from Unit2 should enter the que.");

            assertEquals(unit1.getId(), queued.get(0).getUnitId());
            assertInstanceOf(AttackAction.class, queued.get(0));

            assertEquals(unit2.getId(), queued.get(1).getUnitId());
            assertInstanceOf(MoveAction.class, queued.get(1));
        }

        @Test
        @DisplayName("WaitAction correctly adds action count to the limit")
        void should_count_wait_action_towards_limit() {
            int maxActions = unit1.getActionsPerTurn();

            controller.handleInput(new WaitAction(unit1.getId()));
            controller.handleInput(new WaitAction(unit1.getId()));
            controller.handleInput(new MoveAction(unit1.getId(), 1, 1));

            controller.handleInput(new MoveAction(unit1.getId(), 2, 2));

            assertEquals(maxActions, player.getPlannedActions().size());
            assertInstanceOf(MoveAction.class, player.getPlannedActions().get(2),
                    "Last right action should be move.");
        }
    }
}