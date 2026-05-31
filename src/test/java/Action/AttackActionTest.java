package Action;

import Map.GameMap;
import Units.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AttackActionTest {

    private GameMap mapWithTwo(Archer attacker, Knight target) {
        GameMap m = new GameMap(5, 5);
        m.placeUnit(attacker, 0, 0);
        m.placeUnit(target, 2, 2);
        return m;
    }

    @Test
    @DisplayName("getAttackerId and getTargetId return correct UUIDs")
    public void getterIds() {
        UUID atk = UUID.randomUUID(), tgt = UUID.randomUUID();
        AttackAction aa = new AttackAction(atk, tgt);
        assertEquals(atk, aa.getAttackerId());
        assertEquals(tgt, aa.getTargetId());
    }

    @Test
    @DisplayName("getUnitId returns attackerId")
    public void getUnitIdIsAttacker() {
        UUID atk = UUID.randomUUID();
        assertEquals(atk, new AttackAction(atk, UUID.randomUUID()).getUnitId());
    }

    @Test
    @DisplayName("isValid true when both units are alive on map")
    public void isValidBothAlive() {
        Archer a = new Archer("A", 0, 0);
        Knight k = new Knight("K", 0, 1);
        assertTrue(new AttackAction(a.getId(), k.getId()).isValid(mapWithTwo(a, k)));
    }

    @Test
    @DisplayName("isValid false when attacker is dead")
    public void isValidAttackerDead() {
        Archer a = new Archer("A", 0, 0);
        Knight k = new Knight("K", 0, 0);
        GameMap m = mapWithTwo(a, k);
        a.applyDamage(9999);
        assertFalse(new AttackAction(a.getId(), k.getId()).isValid(m));
    }

    @Test
    @DisplayName("isValid false when target is dead")
    public void isValidTargetDead() {
        Archer a = new Archer("A",0, 0);
        Knight k = new Knight("K",0, 0);
        GameMap m = mapWithTwo(a, k);
        k.applyDamage(9999);
        assertFalse(new AttackAction(a.getId(), k.getId()).isValid(m));
    }

    @Test
    @DisplayName("isValid false when attacker not on map")
    public void isValidAttackerNotOnMap() {
        Knight k = new Knight("K",0, 0);
        GameMap m = new GameMap(5, 5);
        m.placeUnit(k, 1, 1);
        assertFalse(new AttackAction(UUID.randomUUID(), k.getId()).isValid(m));
    }

    @Test
    @DisplayName("isValid false when target not on map")
    public void isValidTargetNotOnMap() {
        Archer a = new Archer("A",0, 0);
        GameMap m = new GameMap(5, 5);
        m.placeUnit(a, 0, 0);
        assertFalse(new AttackAction(a.getId(), UUID.randomUUID()).isValid(m));
    }
}