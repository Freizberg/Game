package Action;

import GameEngine.GameEngine;
import Map.GameMap;

import java.io.Serial;
import java.util.UUID;

/**
 * Instructs a unit to move to the given tile coordinates.
 *
 * <p>Valid only when the destination tile exists, is passable (not an obstacle),
 * and is unoccupied at resolution time. The path between the unit's current position
 * and the destination is computed by {@link GameEngine.MovementResolver} using BFS;
 * tiles occupied by any unit (friendly or hostile) are treated as impassable, so a
 * unit cannot pass through another unit.</p>
 *
 * <p>If two units queue moves to the same destination tile in the same round,
 * {@code GameEngine.resolveCollisions()} cancels both moves and both units remain
 * in place.</p>
 *
 * @author Dzhyhar Volodymyr
 */
public class MoveAction implements Action {
    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID unitId;
    private final int destX;
    private final int destY;

    /**
     * Constructs a MoveAction for the given unit and destination coordinates.
     *
     * @param unitId the UUID of the unit to move
     * @param destX  the destination column index
     * @param destY  the destination row index
     */
    public MoveAction(UUID unitId, int destX, int destY) {
        this.unitId = unitId;
        this.destX  = destX;
        this.destY  = destY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UUID getUnitId() {
        return unitId;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code true} if the destination tile is inside the map bounds,
     * is not an obstacle, and is not currently occupied. The BFS path length
     * vs. unit speed check is delegated to {@link GameEngine.MovementResolver}.</p>
     */
    @Override
    public boolean isValid(GameMap map) {
        return map.isPassable(destX, destY);
    }

    /**
     * Returns the destination column index.
     *
     * @return destination X coordinate
     */
    public int getDestX() {
        return destX;
    }

    /**
     * Returns the destination row index.
     *
     * @return destination Y coordinate
     */
    public int getDestY() {
        return destY;
    }
}
