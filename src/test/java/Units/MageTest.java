package Units;

import Map.Tile;
import Map.TileType;
import GameEngine.GameConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MageTest {

    private Mage mage() {
        return new Mage("Gandalf", 6, 2);
    }

    @Test
    @DisplayName("Mana starts at maxMana")
    public void manaStartsAtMax() {
        Mage m = mage();
        assertEquals(m.getMaxMana(), m.getMana());
    }

    @Test
    @DisplayName("getMaxMana returns correct value")
    public void maxManaCorrect() {
        assertEquals(GameConfig.getMageMaxMana(), mage().getMaxMana());
    }

    @Test
    @DisplayName("consumeMana reduces mana")
    public void consumeManaReduces() {
        Mage m = mage();
        m.consumeMana(30);
        assertEquals(GameConfig.getMageMaxMana()-30, m.getMana());
    }

    @Test
    @DisplayName("consumeMana does not go below 0")
    public void consumeManaNotBelowZero() {
        Mage m = mage();
        m.consumeMana(9999);
        assertEquals(0, m.getMana());
    }

    @Test
    @DisplayName("consumeMana ignores negative values")
    public void consumeManaIgnoresNegative() {
        Mage m = mage();
        m.consumeMana(-10);
        assertEquals(GameConfig.getMageMaxMana(), m.getMana());
    }

    @Test
    @DisplayName("restoreMana increases mana")
    public void restoreManaIncreases() {
        Mage m = mage();
        m.consumeMana(40);
        m.restoreMana(20);
        assertEquals(30, m.getMana());
    }

    @Test
    @DisplayName("restoreMana does not exceed maxMana")
    public void restoreManaNotAboveMax() {
        Mage m = mage();
        m.restoreMana(9999);
        assertEquals(GameConfig.getMageMaxMana(), m.getMana());
    }

    @Test
    @DisplayName("canCast is true when mana >= MIN_CAST_MANA")
    public void canCastTrue() {
        Mage m = mage();
        assertTrue(m.canCast());
    }

    @Test
    @DisplayName("canCast is false when mana < MIN_CAST_MANA")
    public void canCastFalse() {
        Mage m = mage();
        m.consumeMana(45);
        assertFalse(m.canCast());
    }

    @Test
    @DisplayName("applyMove places mage on destination tile")
    public void applyMoveOnTile() {
        Mage m = mage();
        Tile dest = new Tile(TileType.PLAIN);
        m.applyMove(dest);
        assertEquals(m, dest.getUnit());
    }

    @Test
    @DisplayName("Heal should not exceed maxHp")
    public void healNotAboveMax() {
        Knight k = new Knight("Target", 50, 2);
        k.applyDamage(5);
        int healAmount = 50;
        k.heal(healAmount);
        assertEquals(50, k.getHp());
    }
}