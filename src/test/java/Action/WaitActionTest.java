package Action;

import Action.WaitAction;
import Map.GameMap;
import Units.Archer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class WaitActionTest {

    @Test
    @DisplayName("getUnitId returns the correct UUID")
    public void getUnitIdCorrect() {
        UUID id = UUID.randomUUID();
        assertEquals(id, new WaitAction(id).getUnitId());
    }

    @Test
    @DisplayName("isValid true when unit is alive on map")
    public void isValidAliveUnit() {
        Archer a = new Archer("A",0,0);
        GameMap m = new GameMap(5, 5);
        m.placeUnit(a, 2, 2);
        assertTrue(new WaitAction(a.getId()).isValid(m));
    }

    @Test
    @DisplayName("isValid false when unit is dead")
    public void isValidDeadUnit() {
        Archer a = new Archer("A",0,0);
        GameMap m = new GameMap(5, 5);
        m.placeUnit(a, 2, 2);
        a.applyDamage(9999);
        assertFalse(new WaitAction(a.getId()).isValid(m));
    }

    @Test
    @DisplayName("isValid false when unit not on map")
    public void isValidUnitNotOnMap() {
        assertFalse(new WaitAction(UUID.randomUUID()).isValid(new GameMap(5, 5)));
    }
}