package GameEngine;

import Action.MoveAction;
import GameController.GameStateUpdate;
import GameEngine.GameEngine;
import GameEngine.GameState;
import GameEngine.GameConfig;
import GameEngine.Player;
import Map.GameMap;
import Units.Mage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class GameEngineTest {
    private GameEngine engine;
    private GameMap map;
    private Player player1;
    private Player player2;
    private Mage mage1;
    private Mage mage2;

    @BeforeEach
    void setUp() {
        engine = new GameEngine();
        map = new GameMap(10, 10);
        engine.setMap(map);
        player1 = new Player("Player 1");
        player2 = new Player("Player 2");
        mage1 = new Mage("Mage1",1, 1);
        mage2 = new Mage("Mage2",8, 8);

        player1.setUnits(Arrays.asList(mage1));
        player2.setUnits(Arrays.asList(mage2));
        engine.setPlayers(new ArrayList<>(Arrays.asList(player1, player2)));

        map.placeUnit(mage1, 1, 1);
        map.placeUnit(mage2, 8, 8);
    }

    @Test
    void testStateTransitionsAndEndTurn() {
        engine.setState(GameState.PLANNING);

        engine.endTurn(player1);
        assertEquals(GameState.PLANNING, engine.getState(), "Gra powinna nadal być w PLANNING");
        engine.endTurn(player2);
        assertEquals(GameState.RESOLVING, engine.getState(), "Gra powinna przejść do RESOLVING po zakończeniu tury przez wszystkich");
    }

    @Test
    void testCheckWinCondition() {
        assertNull(engine.checkWinCondition(), "Nikt nie powinien wygrać, obaj gracze mają jednostki");
        mage2.applyDamage(999);
        assertEquals(player1, engine.checkWinCondition(), "Gracz 1 powinien wygrać, bo jednostki Gracza 2 zginęły");
    }

    @Test
    void testResolveCollisions() {
        MoveAction move1 = new MoveAction(mage1.getId(), 5, 5);
        MoveAction move2 = new MoveAction(mage2.getId(), 5, 5);

        player1.queueAction(move1);
        player2.queueAction(move2);
        engine.resolveCollisions();

        assertTrue(player1.getPlannedActions().isEmpty(), "Ruch Gracza 1 powinien zostać usunięty");
        assertTrue(player2.getPlannedActions().isEmpty(), "Ruch Gracza 2 powinien zostać usunięty");
    }

    @Test
    void testResolveRoundFlowAndReset() {
        engine.setState(GameState.RESOLVING);
        int initialRound = engine.getCurrentRound();

        mage1.useAction();
        mage1.consumeMana(15);

        engine.resolveRound();

        assertEquals(initialRound + 1, engine.getCurrentRound(), "Licznik rund powinien wzrosnąć o 1");
        assertEquals(0, mage1.getUsedActions(), "Zużyte akcje powinny zostać zresetowane do 0");
        int expectedMana = GameConfig.getMageMaxMana() - 15 + GameConfig.getManaRegenPerRound();
        assertEquals(expectedMana, mage1.getMana(), "Mag powinien poprawnie zregenerować manę");
        assertEquals(GameState.PLANNING, engine.getState(), "Gra powinna wrócić do stanu PLANNING");
    }

    @Test
    void testMageManaRegenerationCapping() {
        engine.setState(GameState.RESOLVING);
        mage1.consumeMana(2);

        engine.resolveRound();

        assertEquals(GameConfig.getMageMaxMana(), mage1.getMana(), "Mana maga nie powinna przekroczyć maksymalnej wartości");
    }

    @Test
    void testPartialCollisions() {
        Player player3 = new Player("Player 3");
        Mage mage3 = new Mage("Mage3",0,0);
        player3.setUnits(new ArrayList<>(Arrays.asList(mage3)));
        engine.getPlayers().add(player3);

        MoveAction move1 = new MoveAction(mage1.getId(), 5, 5);
        MoveAction move2 = new MoveAction(mage2.getId(), 5, 5);
        MoveAction move3 = new MoveAction(mage3.getId(), 6, 6);

        player1.queueAction(move1);
        player2.queueAction(move2);
        player3.queueAction(move3);

        engine.resolveCollisions();

        assertTrue(player1.getPlannedActions().isEmpty(), "Skolidowany ruch 1 usunięty");
        assertTrue(player2.getPlannedActions().isEmpty(), "Skolidowany ruch 2 usunięty");
        assertEquals(1, player3.getPlannedActions().size(), "Bezkolizyjny ruch gracza 3 powinien pozostać w kolejce");
    }

    @Test
    void testMutualDestructionNoWinner() {
        mage1.applyDamage(999);
        mage2.applyDamage(999);
        engine.setState(GameState.RESOLVING);

        engine.resolveRound();

        assertNull(engine.checkWinCondition(), "W przypadku wzajemnego wyniszczenia brak zwycięzcy");
        assertEquals(GameState.DRAW, engine.getState(), "Stan gry powinien zmienić się na DRAW");
        assertTrue(player1.getUnits().isEmpty(), "Lista jednostek G1 powinna być pusta");
        assertTrue(player2.getUnits().isEmpty(), "Lista jednostek G2 powinna być pusta");
    }

    @Test
    void testDeadUnitsListIsClearedEveryRound() {
        mage1.applyDamage(999);
        engine.setState(GameState.RESOLVING);

        engine.resolveRound();
        GameStateUpdate updateRound1 = engine.buildStateUpdate();

        assertEquals(1, updateRound1.deadUnits.size(), "W rundzie 1 powinna być 1 martwa jednostka w paczce");
        assertTrue(updateRound1.deadUnits.contains(mage1.getId()));

        engine.setState(GameState.RESOLVING);
        engine.resolveRound();
        GameStateUpdate updateRound2 = engine.buildStateUpdate();

        assertEquals(0, updateRound2.deadUnits.size(), "W rundzie 2 lista martwych jednostek (do wysłania w DTO) powinna być pusta");
    }

    @Test
    void testInvalidActionsAreIgnoredByEngine() {
        MoveAction invalidMove = new MoveAction(mage1.getId(), 100, 100);
        player1.queueAction(invalidMove);
        engine.setState(GameState.RESOLVING);

        assertDoesNotThrow(() -> engine.resolveRound());

        assertEquals(GameState.PLANNING, engine.getState(), "Silnik powinien bezpiecznie przetworzyć rundę ignorując błędne akcje");
    }

    @Test
    void testBuildStateUpdateCreatesCorrectPayload() {
        mage1.setPos(3, 3);
        mage2.applyDamage(10);

        GameStateUpdate update = engine.buildStateUpdate();

        assertEquals(GameConfig.getMageHp(), update.unitHP.get(mage1.getId()), "Mage 1 powinien mieć pełne HP");
        assertEquals(GameConfig.getMageHp() - 10, update.unitHP.get(mage2.getId()), "Mage 2 powinien mieć HP pomniejszone o 10");

        int[] posM1 = update.unitPositions.get(mage1.getId());
        assertEquals(3, posM1[0]);
        assertEquals(3, posM1[1]);

        assertNull(update.winnerID, "Brak zwycięzcy w trakcie trwania rozgrywki");
    }
}