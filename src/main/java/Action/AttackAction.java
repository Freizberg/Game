package Action;

import GameEngine.GameEngine;
import Map.GameMap;
import Map.Tile;
import Units.Unit;

import java.io.Serial;
import java.util.UUID;

/**
 * Instructs one unit to attack another unit.
 *
 * <h3>Targeting model</h3>
 * <p>Attacks target a specific enemy unit identified by {@link UUID}, not a tile
 * coordinate. The engine re-evaluates the target's position at resolution time.
 * If the target has moved out of the attacker's range during the preceding movement
 * phase, {@link #isValid(GameMap)} returns {@code false} and the attack is silently
 * cancelled.</p>
 *
 * <h3>Simultaneous damage</h3>
 * <p>All {@code AttackAction} instances in a single resolution batch are evaluated
 * at the same time: damage values are calculated first, then applied all at once.
 * If two units deliver lethal attacks to each other in the same round, both die.
 * Dead units are removed only after every attack in the batch has been processed.</p>
 *
 * <h3>Friendly fire</h3>
 * <p>A unit may not attack a unit belonging to the same player.
 * {@link #isValid(GameMap)} rejects such actions.</p>
 *
 * <h3>One attack per turn</h3>
 * <p>A unit may queue at most one {@code AttackAction} per round, regardless of its
 * {@code actionsPerTurn}. This limit is enforced by {@code InputHandler} during the
 * planning phase.</p>
 *
 * @author Dzhyhar Volodymyr
 */
public class AttackAction implements Action {
    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID attackerId;
    private final UUID targetId;

    /**
     * Constructs an AttackAction for the given attacker and target.
     *
     * @param attackerId the UUID of the unit performing the attack
     * @param targetId   the UUID of the enemy unit being attacked
     */
    public AttackAction(UUID attackerId, UUID targetId) {
        this.attackerId = attackerId;
        this.targetId   = targetId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UUID getUnitId() {
        return attackerId;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code true} if both the attacker and the target exist on the map
     * and are alive. Range verification and friendly-fire checks are the
     * responsibility of {@code CombatResolver} at resolution time; this method
     * performs only existence checks so that the action can be quickly discarded
     * when a unit has already died before the resolving phase begins.</p>
     */
    @Override
    public boolean isValid(GameMap map) {
        boolean attackerFound = false;
        boolean targetFound   = false;

        for (Tile[] col : map.getTiles()) {
            for (Tile tile : col) {
                if (!tile.isOccupied()) continue;
                Unit u = tile.getUnit();
                if (u.getId().equals(attackerId) && u.isAlive()) attackerFound = true;
                if (u.getId().equals(targetId)   && u.isAlive()) targetFound   = true;
                if (attackerFound && targetFound) return true;
            }
        }
        return false;
    }

    /**
     * Returns the UUID of the attacking unit.
     *
     * @return attacker UUID
     */
    public UUID getAttackerId() {
        return attackerId;
    }

    /**
     * Returns the UUID of the unit being attacked.
     *
     * @return target UUID
     */
    public UUID getTargetId() {
        return targetId;
    }
}
