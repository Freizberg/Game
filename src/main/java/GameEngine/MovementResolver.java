package GameEngine;

import Action.MoveAction;
import Map.GameMap;
import Map.Tile;
import Units.Unit;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Resolves {@link MoveAction} instances during the RESOLVING phase of a turn.
 *
 * <p>Separates movement logic from the {@link GameEngine} to keep each class
 * focused on a single responsibility. Handles pathfinding, reachability
 * queries, and collision detection when multiple units attempt to occupy
 * the same tile in the same round.</p>
 *
 * @author Dzhyhar Volodymyr
 */
public class MovementResolver implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Fully resolves a {@link MoveAction} against the game state: computes the path,
     * checks for collisions, and updates the unit's position on the map via the engine.
     *
     * @param a the move action to resolve
     * @param e the game engine holding authoritative map and unit state
     */
    public void resolve(MoveAction a, GameEngine e) {
        if (a == null || e == null || e.getMap() == null) {
            return;
        }

        GameMap map = e.getMap();
        Unit unit = map.findUnit(a.getUnitId());
        if (unit == null) {
            return;
        }

        List<Tile> path = findPath(
                map.getTile(unit.getPosX(), unit.getPosY()),
                map.getTile(a.getDestX(), a.getDestY()),
                map
        );

        if (path.isEmpty() || path.size() - 1 > unit.getSpeed()) {
            return;
        }

        unit.applyMove(map.getTile(a.getDestX(), a.getDestY()), map);
    }

    /**
     * Returns all tiles that the given unit can legally reach from its current position
     * in a single turn, based on its speed and the map's passability.
     *
     * @param u the unit whose reachable tiles are requested
     * @param m the current game map
     * @return a list of reachable {@link Tile}
     */
    public List<Tile> getReachableTiles(Unit u, GameMap m) {
        if (u == null || m == null) {
            return List.of();
        }
        return m.getReachableTiles(u.getPosX(), u.getPosY(), u.getSpeed());
    }

    /**
     * Computes the shortest passable path between two tiles on the given map.
     *
     * @param from the starting tile
     * @param to   the destination tile
     * @param m    the game map used for passability checks
     * @return an ordered list of tiles from {@code from} to {@code to} (inclusive),
     * or an empty list if no path exists
     */
    public List<Tile> findPath(Tile from, Tile to, GameMap m) {
        if (from == null || to == null || m == null) {
            return List.of();
        }
        return m.findPath(from.getX(), from.getY(), to.getX(), to.getY());
    }
}
