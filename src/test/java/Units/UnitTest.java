package Units;

import Units.Archer;
import Units.Unit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for behaviour defined in the abstract {@link Unit} class,
 * exercised through {@link Archer} as the simplest concrete subclass.
 */
public class UnitTest {

    private Archer archer() {
        return new Archer("Legolas",0, 0);
    }

    // ── Construction

    @Test
    @DisplayName("HP starts at maxHp")
    public void hpStartsAtMaxHp() {
        Archer a = archer();
        assertEquals(GameEngine.GameConfig.getArcherHp(), a.getHp());
        assertEquals(GameEngine.GameConfig.getArcherHp(), a.getMaxHp());
    }

    @Test
    @DisplayName("Unit has non-null UUID after construction")
    public void idIsNotNull() {
        assertNotNull(archer().getId());
    }

    @Test
    @DisplayName("Name is stored correctly")
    public void nameStored() {
        assertEquals("Legolas", archer().getName());
    }

    @Test
    @DisplayName("Initial position matches constructor arguments")
    public void initialPosition() {
        Archer a = new Archer("A",2,4);
        assertEquals(2, a.getPosX());
        assertEquals(4, a.getPosY());
    }

    @Test
    @DisplayName("usedActions starts at 0")
    public void usedActionsZeroOnConstruction() {
        assertEquals(0, archer().getUsedActions());
    }

    // ── applyDamage

    @Test
    @DisplayName("applyDamage reduces HP by the given amount")
    public void applyDamageReducesHp() {
        Archer a = archer();
        a.applyDamage(30);
        assertEquals(Math.max(0, GameEngine.GameConfig.getArcherHp() - 30), a.getHp());
    }

    @Test
    @DisplayName("applyDamage does not reduce HP below 0")
    public void applyDamageNotBelowZero() {
        Archer a = archer();
        a.applyDamage(999);
        assertEquals(0, a.getHp());
    }

    @Test
    @DisplayName("applyDamage ignores negative values")
    public void applyDamageIgnoresNegative() {
        Archer a = archer();
        a.applyDamage(-10);
        assertEquals(GameEngine.GameConfig.getArcherHp(), a.getHp());
    }

    @Test
    @DisplayName("applyDamage(0) leaves HP unchanged")
    public void applyDamageZeroNoOp() {
        Archer a = archer();
        a.applyDamage(0);
        assertEquals(GameEngine.GameConfig.getArcherHp(), a.getHp());
    }

    // ── isAlive

    @Test
    @DisplayName("Unit with HP > 0 is alive")
    public void aliveWhenHpPositive() {
        assertTrue(archer().isAlive());
    }

    @Test
    @DisplayName("Unit with HP = 0 is dead")
    public void deadWhenHpZero() {
        Archer a = archer();
        a.applyDamage(100);
        assertFalse(a.isAlive());
    }

    @Test
    @DisplayName("Unit is still alive after partial damage")
    public void aliveAfterPartialDamage() {
        Archer a = archer();
        a.applyDamage(20);
        assertTrue(a.isAlive());
    }

    // ── useAction / getRemainingActions / resetActions

    @Test
    @DisplayName("getRemainingActions equals actionsPerTurn initially")
    public void remainingActionsInitial() {
        assertEquals(GameEngine.GameConfig.getActionsPerTurn(), archer().getRemainingActions());
    }

    @Test
    @DisplayName("useAction decrements remaining actions")
    public void useActionDecrements() {
        Archer a = archer();
        a.useAction();
        assertEquals(1, a.getRemainingActions());
    }

    @Test
    @DisplayName("useAction cannot exceed actionsPerTurn")
    public void useActionCapped() {
        Archer a = archer();
        for (int i = 0; i < GameEngine.GameConfig.getActionsPerTurn() + 1; i++) {
            a.useAction();
        }
        assertEquals(0, a.getRemainingActions());
    }

    @Test
    @DisplayName("resetActions restores full action count")
    public void resetActionsRestores() {
        Archer a = archer();
        a.useAction();
        a.useAction();
        a.resetActions();
        assertEquals(GameEngine.GameConfig.getActionsPerTurn(), a.getRemainingActions());
    }

    // ── setPos

    @Test
    @DisplayName("setPos updates posX and posY")
    public void setPosUpdates() {
        Archer a = archer();
        a.setPos(7, 3);
        assertEquals(7, a.getPosX());
        assertEquals(3, a.getPosY());
    }

    // ── speed

    @Test
    @DisplayName("getSpeed returns constructor value")
    public void getSpeedReturnsCorrect() {
        assertEquals(GameEngine.GameConfig.getArcherSpeed(), archer().getSpeed());
    }
}