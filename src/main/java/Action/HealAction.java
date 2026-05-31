package Action;

import GameEngine.GameEngine;
import GameEngine.GameConfig;
import Map.GameMap;
import Map.Tile;
import Units.Mage;
import Units.Unit;

import java.io.Serial;
import java.util.UUID;

/**
 * Instructs a {@link Mage} to cast a healing spell on a friendly unit.
 *
 * <p>Healing costs mana (value determined by config). The action is invalid if the
 * caster is not a Mage, has insufficient mana, or the target does not exist
 * on the map. HP restoration equals the config value, capped at
 * the target's maximum HP.</p>
 *
 * <p>A Mage may not queue both an {@link AttackAction} and a {@code HealAction}
 * in the same round; the {@code InputHandler} enforces this during planning.</p>
 *
 * @author Dzhyhar Volodymyr
 */
public class HealAction implements Action {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID casterId;
    private final UUID targetId;

    /**
     * Constructs a HealAction for the given Mage and target.
     *
     * @param casterId the UUID of the Mage performing heal
     * @param targetId the UUID of the friendly unit to be healed
     */
    public HealAction(UUID casterId, UUID targetId) {
        this.casterId = casterId;
        this.targetId = targetId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UUID getUnitId() {
        return casterId;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code true} if:
     * <ul>
     *   <li>The caster exists on the map and is a {@link Mage}.</li>
     *   <li>The Mage has at least the required spell mana cost.</li>
     *   <li>The target exists on the map and is alive.</li>
     * </ul>
     * Range checking is delegated to {@code CombatResolver} at resolution time.
     * </p>
     */
    @Override
    public boolean isValid(GameMap map) {
        Tile[][] tiles = map.getTiles();
        Unit casterUnit = null;
        Unit targetUnit = null;

        for (Tile[] row : tiles) {
            for (Tile tile : row) {
                if (!tile.isOccupied()) continue;
                Unit u = tile.getUnit();
                if (u.getId().equals(casterId)) casterUnit = u;
                if (u.getId().equals(targetId))  targetUnit = u;
            }
        }

        if (casterUnit == null || !(casterUnit instanceof Mage mage)) return false;
        if (mage.getMana() < GameConfig.getSpellManaCost()) return false;
        return targetUnit != null && targetUnit.isAlive();
    }

    /**
     * Returns the UUID of the Mage performing the heal.
     *
     * @return caster UUID
     */
    public UUID getCasterId() {
        return casterId;
    }

    /**
     * Returns the UUID of the unit being healed.
     *
     * @return target UUID
     */
    public UUID getTargetId() {
        return targetId;
    }
}
