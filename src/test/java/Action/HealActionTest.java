package Action;

import Map.GameMap;
import Units.Archer;
import Units.Knight;
import Units.Mage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class HealActionTest {

    private GameMap map;
    private Mage mage;
    private Knight target;

    @BeforeEach
    void setUp() {
        map = new GameMap(5, 5);
        mage = new Mage("Merlin", 1, 1);
        target = new Knight("Arthur", 1, 2);

        map.placeUnit(mage, 1, 1);
        map.placeUnit(target, 1, 2);
    }

    @Test
    @DisplayName("Getter methods return correct IDs")
    public void gettersReturnCorrectIds() {
        HealAction heal = new HealAction(mage.getId(), target.getId());

        assertEquals(mage.getId(), heal.getCasterId());
        assertEquals(target.getId(), heal.getTargetId());
        assertEquals(mage.getId(), heal.getUnitId());
    }

    @Test
    @DisplayName("isValid true when Mage has mana and target is alive")
    public void isValidStandardSuccess() {
        HealAction heal = new HealAction(mage.getId(), target.getId());
        assertTrue(heal.isValid(map));
    }

    @Test
    @DisplayName("isValid false when caster is not a Mage")
    public void isValidFailsIfCasterNotMage() {
        Archer archer = new Archer("Robin", 2, 2);
        map.placeUnit(archer, 2, 2);

        // Archer tries to cast heal
        HealAction heal = new HealAction(archer.getId(), target.getId());
        assertFalse(heal.isValid(map));
    }

    @Test
    @DisplayName("isValid false when Mage has insufficient mana")
    public void isValidFailsIfNoMana() {
        // Drain all mana
        mage.consumeMana(mage.getMaxMana());

        HealAction heal = new HealAction(mage.getId(), target.getId());
        assertFalse(heal.isValid(map));
    }

    @Test
    @DisplayName("isValid false when target is dead")
    public void isValidFailsIfTargetDead() {
        target.applyDamage(9999); // Kill target

        HealAction heal = new HealAction(mage.getId(), target.getId());
        assertFalse(heal.isValid(map));
    }

    @Test
    @DisplayName("isValid false when caster or target are missing from map")
    public void isValidFailsIfUnitsMissing() {
        // Missing caster
        HealAction healMissingCaster = new HealAction(UUID.randomUUID(), target.getId());
        assertFalse(healMissingCaster.isValid(map));

        // Missing target
        HealAction healMissingTarget = new HealAction(mage.getId(), UUID.randomUUID());
        assertFalse(healMissingTarget.isValid(map));
    }
}