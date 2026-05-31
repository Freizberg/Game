import Action.MoveAction;
import Action.WaitAction;
import GameEngine.Player;
import Units.Mage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerTest {
    private Player player;
    private Mage mage1;
    private Mage mage2;

    @BeforeEach
    void setUp() {
        player = new Player("TestPlayer");
        mage1 = new Mage("Gandalf",0,0);
        mage2 = new Mage("Saruman",5,5);

        List<Units.Unit> units = new ArrayList<>();
        units.add(mage1);
        units.add(mage2);
        player.setUnits(units);
    }

    @Test
    void testPlayerInitialization() {
        assertEquals("TestPlayer", player.getName(), "Nazwa gracza powinna być poprawnie przypisana");
        assertNotNull(player.getUuid(), "UUID gracza nie może być null");
        assertTrue(player.getPlannedActions().isEmpty(), "Nowy gracz powinien mieć pustą listę akcji");
        assertFalse(player.isTurnEnded(), "Flaga końca tury powinna domyślnie wynosić false");
    }

    @Test
    void testQueueAndClearActions() {
        WaitAction waitAction = new WaitAction(mage1.getId());

        player.queueAction(waitAction);
        player.endTurn();

        assertEquals(1, player.getPlannedActions().size(), "Gracz powinien mieć jedną zaplanowaną akcję");
        assertTrue(player.isTurnEnded(), "Flaga końca tury powinna być podniesiona");

        player.clearActions();

        assertTrue(player.getPlannedActions().isEmpty(), "Kolejka akcji powinna być pusta");
        assertFalse(player.isTurnEnded(), "Flaga końca tury powinna być opuszczona");
    }

    @Test
    void testHasUnitsAlive() {
        assertTrue(player.hasUnitsAlive(), "Gracz z żywym magiem powinien zwrócić true");

        mage1.applyDamage(50);
        mage2.applyDamage(50);

        assertFalse(player.hasUnitsAlive(), "Gracz bez żywych jednostek powinien zwrócić false");
    }

    @Test
    void testQueueNullActionIsIgnored() {
        player.queueAction(null);
        assertTrue(player.getPlannedActions().isEmpty(), "Gracz nie powinien dodawać wartości null do kolejki akcji");
    }

    @Test
    void testQueueMultipleActions() {
        WaitAction wait = new WaitAction(mage1.getId());
        MoveAction move = new MoveAction(mage1.getId(), 2, 2);

        player.queueAction(wait);
        player.queueAction(move);

        assertEquals(2, player.getPlannedActions().size(), "Gracz powinien mieć zapisane 2 akcje");
        assertEquals(wait, player.getPlannedActions().get(0), "Pierwsza akcja powinna być WaitAction");
        assertEquals(move, player.getPlannedActions().get(1), "Druga akcja powinna być MoveAction");
    }

    @Test
    void testHasUnitsAliveWithMixedStates() {
        player.setUnits(new ArrayList<>(Arrays.asList(mage1, mage2)));

        assertTrue(player.hasUnitsAlive(), "Gracz z dwiema żywymi jednostkami zwraca true");

        mage1.applyDamage(999);
        assertTrue(player.hasUnitsAlive(), "Gracz z jedną żywą i jedną martwą jednostką nadal zwraca true");

        mage2.applyDamage(999);
        assertFalse(player.hasUnitsAlive(), "Gracz, którego wszystkie jednostki zginęły, zwraca false");
    }

    @Test
    void testHasUnitsAliveWithEmptyList() {
        player.setUnits(new ArrayList<>());

        assertFalse(player.hasUnitsAlive(), "Gracz bez żadnych jednostek (pusta lista) powinien zwrócić false");
    }

    @Test
    void testIsPlayersUnit() {
        player.setUnits(new ArrayList<>(Arrays.asList(mage1)));

        assertTrue(player.isPlayersUnit(mage1.getId()), "Metoda powinna zwrócić true dla jednostki należącej do gracza");
        assertFalse(player.isPlayersUnit(mage2.getId()), "Metoda powinna zwrócić false dla jednostki, której gracz nie posiada");
        assertFalse(player.isPlayersUnit(UUID.randomUUID()), "Metoda powinna zwrócić false dla losowego UUID");
    }

    @Test
    void testSetLocalAndIsLocal() {
        player.setLocal(true);
        assertTrue(player.isLocal(), "Flaga isLocal powinna zwracać true po ustawieniu na true");

        player.setLocal(false);
        assertFalse(player.isLocal(), "Flaga isLocal powinna zwracać false po ustawieniu na false");
    }
}