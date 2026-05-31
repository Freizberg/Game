import GameController.GameStateUpdate;
import GameEngine.GameEngine;
import GameEngine.GameConfig;
import GameEngine.GameState;
import GameEngine.Player;
import Map.GameMap;
import Units.Mage;
import Units.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class GameStateUpdateTest {

    private GameEngine engine;
    private GameMap map;
    private Player player1;
    private Player player2;

    private Mage mage1; // Gracz 1
    private Mage mage2; // Gracz 1
    private Mage mage3; // Gracz 2

    @BeforeEach
    void setUp() {
        engine = new GameEngine();
        map = new GameMap(10, 10);
        engine.setMap(map);

        player1 = new Player("Player 1");
        player2 = new Player("Player 2");

        mage1 = new Mage("Mage1",1,1);
        mage2 = new Mage("Mage2",2,2);
        mage3 = new Mage("Mage3",8,8);

        player1.setUnits(new ArrayList<>(Arrays.asList(mage1, mage2)));
        player2.setUnits(new ArrayList<>(Arrays.asList(mage3)));

        engine.setPlayers(Arrays.asList(player1, player2));

        map.placeUnit(mage1, 1, 1);
        map.placeUnit(mage2, 2, 2);
        map.placeUnit(mage3, 8, 8);
    }

    @Test
    void testApplyEmptyUpdate() {
        GameStateUpdate emptyUpdate = new GameStateUpdate(
                new HashMap<>(), new HashMap<>(), new ArrayList<>(), null, false, 2
        );

        emptyUpdate.apply(engine);

        assertEquals(2, engine.getCurrentRound(), "Runda powinna zaktualizować się do 2");
        assertEquals(GameState.PLANNING, engine.getState(), "Stan gry to PLANNING");

        assertEquals(GameConfig.getMageHp(), mage1.getHp(), "HP powinno pozostać nietknięte");
        assertEquals(1, mage1.getPosX(), "Pozycja X powinna pozostać nietknięta");
        assertEquals(1, mage1.getPosY(), "Pozycja Y powinna pozostać nietknięta");

        assertEquals(2, player1.getUnits().size(), "Gracz 1 wciąż ma 2 jednostki");
        assertEquals(1, player2.getUnits().size(), "Gracz 2 wciąż ma 1 jednostkę");
    }

    @Test
    void testApplyPartialUpdates() {
        Map<UUID, int[]> newPositions = new HashMap<>();
        newPositions.put(mage1.getId(), new int[]{5, 5});

        Map<UUID, Integer> newHPs = new HashMap<>();
        newHPs.put(mage2.getId(), 10);

        GameStateUpdate update = new GameStateUpdate(
                newPositions, newHPs, new ArrayList<>(), null, false, 3
        );

        update.apply(engine);

        assertEquals(5, mage1.getPosX(), "Mage 1 zmienił pozycję X");
        assertEquals(5, mage1.getPosY(), "Mage 1 zmienił pozycję Y");
        assertEquals(GameConfig.getMageHp(), mage1.getHp(), "Mage 1 nie otrzymał obrażeń");

        assertEquals(2, mage2.getPosX(), "Mage 2 pozostał na swoim miejscu X");
        assertEquals(2, mage2.getPosY(), "Mage 2 pozostał na swoim miejscu Y");
        assertEquals(10, mage2.getHp(), "Mage 2 otrzymał obrażenia");
    }

    @Test
    void testApplyMultipleDeadUnits() {
        List<UUID> deadUnits = Arrays.asList(mage1.getId(), mage3.getId());

        GameStateUpdate update = new GameStateUpdate(
                new HashMap<>(), new HashMap<>(), deadUnits, null, false, 4
        );

        update.apply(engine);

        assertEquals(1, player1.getUnits().size(), "Gracz 1 powinien mieć tylko 1 jednostkę");
        assertTrue(player1.getUnits().contains(mage2), "Ocalałą jednostką Gracza 1 powinien być Mage 2");

        assertEquals(0, player2.getUnits().size(), "Gracz 2 stracił wszystkie jednostki");
    }

    @Test
    void testRefreshMapOccupancyWorksCorrectly() {
        assertNotNull(map.getTile(1, 1).getUnit(), "Pole 1,1 powinno być zajęte przed aktualizacją");
        assertNull(map.getTile(0, 0).getUnit(), "Pole 0,0 powinno być puste przed aktualizacją");

        Map<UUID, int[]> newPositions = new HashMap<>();
        newPositions.put(mage1.getId(), new int[]{0, 0});

        GameStateUpdate update = new GameStateUpdate(
                newPositions, new HashMap<>(), new ArrayList<>(), null, false, 5
        );

        update.apply(engine);

        assertNull(map.getTile(1, 1).getUnit(), "Stary kafel 1,1 po odświeżeniu mapy powinien być pusty");

        Unit unitAtNewPos = map.getTile(0, 0).getUnit();
        assertNotNull(unitAtNewPos, "Nowy kafel 0,0 powinien być zajęty");
        assertEquals(mage1.getId(), unitAtNewPos.getId(), "Na nowym kaflu powinien stać Mage 1");
    }

    @Test
    void testApplyDoesNotResurrectDeadUnits() {
        Map<UUID, int[]> newPositions = new HashMap<>();
        newPositions.put(mage3.getId(), new int[]{9, 9});

        Map<UUID, Integer> newHPs = new HashMap<>();
        newHPs.put(mage3.getId(), 50);

        List<UUID> deadUnits = Arrays.asList(mage3.getId());

        GameStateUpdate update = new GameStateUpdate(
                newPositions, newHPs, deadUnits, player1.getUuid(), false, 6
        );

        update.apply(engine);

        assertFalse(player2.getUnits().contains(mage3), "Mage 3 powinien zostać nieodwracalnie usunięty");
        assertNull(map.getTile(9, 9).getUnit(), "Duch Mage 3 nie powinien zająć kafla na mapie");
    }
}