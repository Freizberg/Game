package Map;

import Units.Unit;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Represents the game map as a 2D grid of {@link Tile} objects.
 * Responsible for tile access, unit placement, and passability checks.
 *
 * @author Mateusz Pacek
 */
public class GameMap implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final int[][] ORTHOGONAL_DIRECTIONS = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };
    private final Tile[][] tiles;
    private final int width;
    private final int height;

    /**
     * Constructs a GameMap of the given dimensions.
     *
     * @param width  number of columns
     * @param height number of rows
     *
     * @author Mateusz Pacek
     */
    public GameMap(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Map dimensions must be positive.");
        }

        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = new Tile(x, y, TileType.PLAIN);
            }
        }
    }

    /**
     * Returns the tile at the given grid coordinates.
     *
     * @param x the column index
     * @param y the row index
     * @return the {@link Tile} at (x, y), or {@code null} if out of bounds
     *
     * @author Mateusz Pacek
     */
    public Tile getTile(int x, int y) {
        if (!isInBounds(x, y)) {
            return null;
        }
        return tiles[x][y];
    }

    /**
     * Checks whether a tile at (x, y) can be moved through.
     * A tile is passable if it is not an obstacle and not occupied.
     *
     * @param x the column index
     * @param y the row index
     * @return {@code true} if the tile is passable, {@code false} otherwise
     *
     * @author Mateusz Pacek
     */
    public boolean isPassable(int x, int y) {
        Tile tile = getTile(x, y);
        return tile != null && !tile.isObstacle() && !tile.isOccupied();
    }

    /**
     * Places the given unit on the tile at (x, y) and updates the unit's position.
     *
     * @param u the unit to place
     * @param x the column index
     * @param y the row index
     *
     * @author Mateusz Pacek
     */
    public void placeUnit(Unit u, int x, int y) {
        if (u == null) {
            throw new IllegalArgumentException("Unit cannot be null.");
        }
        Tile destination = getTile(x, y);
        if (destination == null) {
            throw new IllegalArgumentException("Destination tile is out of bounds.");
        }
        if (destination.isObstacle()) {
            throw new IllegalStateException("Cannot place a unit on an obstacle tile.");
        }
        if (destination.isOccupied() && destination.getUnit() != u) {
            throw new IllegalStateException("Destination tile is already occupied.");
        }

        removeUnit(u);
        destination.setUnit(u);
        u.setPos(x, y);
    }

    /**
     * Removes the given unit from whichever tile it currently occupies.
     *
     * @param u the unit to remove
     *
     * @author Mateusz Pacek
     */
    public void removeUnit(Unit u) {
        if (u == null) {
            return;
        }
        int x = u.getPosX();
        int y = u.getPosY();
        if (isInBounds(x, y) && tiles[x][y].getUnit() == u) {
            tiles[x][y].setUnit(null);
        }
    }

    public Tile[][] getTiles() {
        return tiles;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Computes the orthogonal grid distance between two coordinates.
     * Distance used by movement range and attack range checks.
     *
     * @param fromX starting column
     * @param fromY starting row
     * @param toX   destination column
     * @param toY   destination row
     *
     * @author Mateusz Pacek
     */
    public int getDistance(int fromX, int fromY, int toX, int toY) {
        if (!isInBounds(fromX, fromY) || !isInBounds(toX, toY)) {
            return Integer.MAX_VALUE;
        }
        return Math.abs(fromX - toX) + Math.abs(fromY - toY);
    }

    /**
     * Computes the shortest passable orthogonal path between two map coordinates.
     *
     * @param fromX starting column
     * @param fromY starting row
     * @param toX   destination column
     * @param toY   destination row
     * @return an ordered path including both endpoints, or an empty list when unreachable
     *
     * @author Mateusz Pacek
     */
    public List<Tile> findPath(int fromX, int fromY, int toX, int toY) {
        if (!isInBounds(fromX, fromY) || !isInBounds(toX, toY)) {
            return Collections.emptyList();
        }

        if (fromX == toX && fromY == toY) {
            return List.of(getTile(fromX, fromY));
        }

        if (!isPassable(toX, toY)) {
            return Collections.emptyList();
        }

        boolean[][] visited = new boolean[width][height];
        int[][] prevX = new int[width][height];
        int[][] prevY = new int[width][height];
        ArrayDeque<int[]> queue = new ArrayDeque<>();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                prevX[x][y] = -1;
                prevY[x][y] = -1;
            }
        }

        visited[fromX][fromY] = true;
        queue.add(new int[]{fromX, fromY});

        while (!queue.isEmpty()) {
            int[] current = queue.removeFirst();
            int currentX = current[0];
            int currentY = current[1];

            for (int[] direction : ORTHOGONAL_DIRECTIONS) {
                int nextX = currentX + direction[0];
                int nextY = currentY + direction[1];

                if (!isInBounds(nextX, nextY) || visited[nextX][nextY] || !isPassable(nextX, nextY)) {
                    continue;
                }

                visited[nextX][nextY] = true;
                prevX[nextX][nextY] = currentX;
                prevY[nextX][nextY] = currentY;

                if (nextX == toX && nextY == toY) {
                    return rebuildPath(prevX, prevY, fromX, fromY, toX, toY);
                }

                queue.addLast(new int[]{nextX, nextY});
            }
        }

        return Collections.emptyList();
    }

    /**
     * Returns all passable tiles reachable from the given coordinate within the supplied range.
     *
     * @param fromX      starting column
     * @param fromY      starting row
     * @param maxSteps   maximum number of orthogonal steps
     * @return reachable tiles excluding the starting tile
     *
     * @author Mateusz Pacek
     */
    public List<Tile> getReachableTiles(int fromX, int fromY, int maxSteps) {
        if (!isInBounds(fromX, fromY) || maxSteps <= 0) {
            return Collections.emptyList();
        }

        boolean[][] visited = new boolean[width][height];
        int[][] distance = new int[width][height];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        List<Tile> reachable = new ArrayList<>();

        visited[fromX][fromY] = true;
        queue.add(new int[]{fromX, fromY});

        while (!queue.isEmpty()) {
            int[] current = queue.removeFirst();
            int currentX = current[0];
            int currentY = current[1];

            for (int[] direction : ORTHOGONAL_DIRECTIONS) {
                int nextX = currentX + direction[0];
                int nextY = currentY + direction[1];

                if (!isInBounds(nextX, nextY) || visited[nextX][nextY] || !isPassable(nextX, nextY)) {
                    continue;
                }

                int nextDistance = distance[currentX][currentY] + 1;
                if (nextDistance > maxSteps) {
                    continue;
                }

                visited[nextX][nextY] = true;
                distance[nextX][nextY] = nextDistance;
                reachable.add(getTile(nextX, nextY));
                queue.addLast(new int[]{nextX, nextY});
            }
        }

        return reachable;
    }

    /**
     * Returns whether there is a clear line-of-sight between two map coordinates.
     * Only obstacle and mountain tiles block sight; units and water do not.
     *
     * @param fromX starting column
     * @param fromY starting row
     * @param toX   destination column
     * @param toY   destination row
     * @return {@code true} when no blocking terrain lies between the endpoints
     *
     * @author Mateusz Pacek
     */
    public boolean hasLineOfSight(int fromX, int fromY, int toX, int toY) {
        if (!isInBounds(fromX, fromY) || !isInBounds(toX, toY)) {
            return false;
        }

        int x = fromX;
        int y = fromY;
        int deltaX = Math.abs(toX - fromX);
        int deltaY = Math.abs(toY - fromY);
        int stepX = Integer.compare(toX, fromX);
        int stepY = Integer.compare(toY, fromY);
        int error = deltaX - deltaY;

        while (x != toX || y != toY) {
            int doubledError = 2 * error;

            if (doubledError > -deltaY) {
                error -= deltaY;
                x += stepX;
            }
            if (doubledError < deltaX) {
                error += deltaX;
                y += stepY;
            }

            if ((x != toX || y != toY) && blocksLineOfSight(x, y)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Changes the terrain type of the tile at (x, y) in place.
     *
     * @apiNote Intended for map initialisation before units are placed.
     *          If the tile already has a unit, that unit stays even if the new
     *          terrain is impassable — avoid calling this after deployment.
     *
     * @author Mateusz Pacek
     */
    public void setTileType(int x, int y, TileType type) {
        Tile tile = getTile(x, y);
        if (tile == null) {
            throw new IllegalArgumentException("Tile is out of bounds.");
        }
        tile.setType(type);
    }

    public Unit findUnit(UUID unitId) {
        if (unitId == null) {
            return null;
        }

        for (Tile[] column : tiles) {
            for (Tile tile : column) {
                if (tile.isOccupied() && unitId.equals(tile.getUnit().getId())) {
                    return tile.getUnit();
                }
            }
        }
        return null;
    }

    public Tile findUnitTile(UUID unitId) {
        if (unitId == null) {
            return null;
        }

        for (Tile[] column : tiles) {
            for (Tile tile : column) {
                if (tile.isOccupied() && unitId.equals(tile.getUnit().getId())) {
                    return tile;
                }
            }
        }
        return null;
    }

    private boolean blocksLineOfSight(int x, int y) {
        Tile tile = getTile(x, y);
        if (tile == null) {
            return true;
        }

        TileType type = tile.getType();
        return type == TileType.OBSTACLE || type == TileType.MOUNTAIN;
    }

    private List<Tile> rebuildPath(int[][] prevX, int[][] prevY, int fromX, int fromY, int toX, int toY) {
        List<Tile> path = new ArrayList<>();
        int currentX = toX;
        int currentY = toY;

        while (currentX != -1 && currentY != -1) {
            path.add(getTile(currentX, currentY));

            if (currentX == fromX && currentY == fromY) {
                Collections.reverse(path);
                return path;
            }

            int nextX = prevX[currentX][currentY];
            int nextY = prevY[currentX][currentY];
            currentX = nextX;
            currentY = nextY;
        }

        return Collections.emptyList();
    }
}
