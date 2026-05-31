package Action;

import GameEngine.GameEngine;
import Map.GameMap;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents a single player command queued during the PLANNING phase
 * and executed during the RESOLVING phase.
 *
 * @author Dzhyhar Volodymyr
 */
public interface Action extends Serializable {


    /**
     * Returns the UUID of the unit this action belongs to.
     * For actions that are not unit-specific (e.g. SkipTurnAction),
     * implementations should return {@code null}.
     *
     * @return unit UUID, or {@code null} if not applicable
     */
    UUID getUnitId();

    /**
     * Checks whether this action is legal given the current map state.
     *
     * @param map the current game map
     * @return {@code true} if the action can be legally executed
     */
    boolean isValid(GameMap map);
}
