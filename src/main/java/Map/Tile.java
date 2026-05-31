package Map;

import Units.Unit;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents a single cell on the {@link GameMap}.
 * Each tile has a terrain type and can hold at most one {@link Unit}.
 *
 * @author Mateusz Pacek
 */
public class Tile implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final int x;
    private final int y;
    private TileType type;
    private Unit unit;

    /**
     * Constructs a tile with the given coordinates, terrain type and no occupying unit.
     *
     * @param x    X coordinate on the map
     * @param y    Y coordinate on the map
     * @param type the terrain type of this tile
     *
     * @author Mateusz Pacek
     */
    public Tile(int x, int y, TileType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    /**
     * Backward-compatible constructor for standalone tile creation.
     * Coordinates default to {@code -1, -1} when the tile is not attached to a map.
     *
     * @param type the terrain type of this tile
     */
    public Tile(TileType type) {
        this(-1, -1, type);
    }

    /**
     * Returns whether this tile blocks movement entirely.
     * Tiles of type {@link TileType#OBSTACLE}, {@link TileType#WATER},
     * or {@link TileType#MOUNTAIN} are considered obstacles.
     *
     * @return {@code true} if the tile cannot be entered
     * @author Mateusz Pacek
     */
    public boolean isObstacle() {
        return type == TileType.OBSTACLE
                || type == TileType.WATER
                || type == TileType.MOUNTAIN;
    }

    /**
     * Returns whether a unit is currently standing on this tile.
     *
     * @return {@code true} if the tile holds a unit
     *
     * @author Mateusz Pacek
     */
    public boolean isOccupied() {
        return unit != null;
    }

    /**
     * Places a unit on this tile.
     *
     * @param u the unit to place, or {@code null} to remove the current occupant
     *
     * @author Mateusz Pacek
     */
    public void setUnit(Unit u) {
        this.unit = u;
    }

    /**
     * Returns whether this tile provides cover to a defending unit.
     * Currently only {@link TileType#FOREST} provides cover (–1 flat damage reduction).
     *
     * @return {@code true} if the tile grants a cover bonus
     */
    public boolean providesCover() {
        return type == TileType.FOREST;
    }

    public Unit getUnit() {
        return unit;
    }

    public TileType getType() {
        return type;
    }

    public void setType(TileType type) { this.type = type;}

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
