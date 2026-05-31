package Map;

import Map.GameMap;
import Map.Tile;
import Map.TileType;
import Units.Archer;
import Units.Knight;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameMapTest {

    private GameMap map5x5() { return new GameMap(5, 5); }

    // ── Constructor & initialisation

    @Test
    @DisplayName("All tiles initialised to PLAIN")
    public void constructorInitialisesPlain() {
        GameMap m = map5x5();
        for (Tile[] col : m.getTiles())
            for (Tile t : col)
                assertEquals(TileType.PLAIN, t.getType());
    }

    @Test
    @DisplayName("No NullPointerException on fresh map access")
    public void noNpeOnFreshAccess() {
        GameMap m = map5x5();
        assertNotNull(m.getTile(0, 0));
        assertNotNull(m.getTile(4, 4));
    }

    @Test
    @DisplayName("Constructor throws for zero width")
    public void constructorRejectsZeroWidth() {
        assertThrows(IllegalArgumentException.class, () -> new GameMap(0, 5));
    }

    @Test
    @DisplayName("Constructor throws for zero height")
    public void constructorRejectsZeroHeight() {
        assertThrows(IllegalArgumentException.class, () -> new GameMap(5, 0));
    }

    @Test
    @DisplayName("Constructor throws for negative dimensions")
    public void constructorRejectsNegativeDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new GameMap(-1, 5));
        assertThrows(IllegalArgumentException.class, () -> new GameMap(5, -3));
    }

    @Test
    @DisplayName("Tiles know their own coordinates")
    public void tilesStoreCoordinates() {
        GameMap m = map5x5();
        Tile t = m.getTile(3, 2);
        assertEquals(3, t.getX());
        assertEquals(2, t.getY());
    }

    // ── isInBounds / getTile

    @Test
    @DisplayName("getTile returns null for out-of-bounds coordinates")
    public void getTileOutOfBoundsReturnsNull() {
        GameMap m = map5x5();
        assertNull(m.getTile(-1, 0));
        assertNull(m.getTile(0, -1));
        assertNull(m.getTile(5, 0));
        assertNull(m.getTile(0, 5));
    }

    @Test
    @DisplayName("isInBounds returns true for valid coordinates")
    public void isInBoundsValid() {
        GameMap m = map5x5();
        assertTrue(m.isInBounds(0, 0));
        assertTrue(m.isInBounds(4, 4));
        assertTrue(m.isInBounds(2, 3));
    }

    @Test
    @DisplayName("isInBounds returns false for negative coordinates")
    public void isInBoundsNegative() {
        assertFalse(map5x5().isInBounds(-1, 0));
        assertFalse(map5x5().isInBounds(0, -1));
    }

    @Test
    @DisplayName("isInBounds returns false for coordinates >= dimension")
    public void isInBoundsOutOfRange() {
        assertFalse(map5x5().isInBounds(5, 0));
        assertFalse(map5x5().isInBounds(0, 5));
    }

    // ── setTileType

    @Test
    @DisplayName("setTileType changes terrain of specified tile")
    public void setTileTypeChangesTerrain() {
        GameMap m = map5x5();
        m.setTileType(1, 2, TileType.OBSTACLE);
        assertEquals(TileType.OBSTACLE, m.getTile(1, 2).getType());
    }

    @Test
    @DisplayName("setTileType does not affect neighbouring tiles")
    public void setTileTypeDoesNotAffectNeighbours() {
        GameMap m = map5x5();
        m.setTileType(2, 2, TileType.WATER);
        assertEquals(TileType.PLAIN, m.getTile(2, 1).getType());
        assertEquals(TileType.PLAIN, m.getTile(3, 2).getType());
    }

    @Test
    @DisplayName("setTileType throws for out-of-bounds coordinates")
    public void setTileTypeOutOfBoundsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> map5x5().setTileType(10, 0, TileType.WATER));
    }

    // ── isPassable

    @Test
    @DisplayName("Empty PLAIN tile is passable")
    public void plainEmptyIsPassable() {
        assertTrue(map5x5().isPassable(0, 0));
    }

    @Test
    @DisplayName("OBSTACLE tile is not passable")
    public void obstacleTileNotPassable() {
        GameMap m = map5x5();
        m.setTileType(1, 1, TileType.OBSTACLE);
        assertFalse(m.isPassable(1, 1));
    }

    @Test
    @DisplayName("WATER tile is not passable")
    public void waterTileNotPassable() {
        GameMap m = map5x5();
        m.setTileType(0, 0, TileType.WATER);
        assertFalse(m.isPassable(0, 0));
    }

    @Test
    @DisplayName("Occupied PLAIN tile is not passable")
    public void occupiedTileNotPassable() {
        GameMap m = map5x5();
        m.placeUnit(new Archer("A",0, 0), 2, 2);
        assertFalse(m.isPassable(2, 2));
    }

    @Test
    @DisplayName("Out-of-bounds coordinate is not passable")
    public void outOfBoundsNotPassable() {
        assertFalse(map5x5().isPassable(-1, 0));
        assertFalse(map5x5().isPassable(5, 5));
    }

    // ── placeUnit

    @Test
    @DisplayName("placeUnit occupies the target tile")
    public void placeUnitOccupiesTile() {
        GameMap m = map5x5();
        Archer a = new Archer("A",0, 0);
        m.placeUnit(a, 3, 3);
        assertTrue(m.getTile(3, 3).isOccupied());
        assertEquals(a, m.getTile(3, 3).getUnit());
    }

    @Test
    @DisplayName("placeUnit updates unit position")
    public void placeUnitUpdatesUnitPos() {
        GameMap m = map5x5();
        Archer a = new Archer("A",0,0);
        m.placeUnit(a, 3, 4);
        assertEquals(3, a.getPosX());
        assertEquals(4, a.getPosY());
    }

    @Test
    @DisplayName("placeUnit clears previous tile when unit moves")
    public void placeUnitClearsPreviousTile() {
        GameMap m = map5x5();
        Archer a = new Archer("A",0,0);
        m.placeUnit(a, 1, 1);
        m.placeUnit(a, 2, 2);
        assertFalse(m.getTile(1, 1).isOccupied(), "Old tile should be cleared");
        assertTrue(m.getTile(2, 2).isOccupied(), "New tile should be occupied");
    }

    @Test
    @DisplayName("placeUnit throws for out-of-bounds destination")
    public void placeUnitOutOfBoundsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> map5x5().placeUnit(new Archer("A",0,0), 99, 0));
    }

    @Test
    @DisplayName("placeUnit throws for null unit")
    public void placeUnitNullThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> map5x5().placeUnit(null, 1, 1));
    }

    @Test
    @DisplayName("placeUnit throws when destination is an obstacle")
    public void placeUnitOnObstacleThrows() {
        GameMap m = map5x5();
        m.setTileType(2, 2, TileType.OBSTACLE);
        assertThrows(IllegalStateException.class,
                () -> m.placeUnit(new Archer("A",0,0), 2, 2));
    }

    @Test
    @DisplayName("placeUnit throws when tile is occupied by a different unit")
    public void placeUnitOnOccupiedThrows() {
        GameMap m = map5x5();
        Archer a1 = new Archer("A1",0, 0);
        Archer a2 = new Archer("A2",0, 0);
        m.placeUnit(a1, 2, 2);
        assertThrows(IllegalStateException.class, () -> m.placeUnit(a2, 2, 2));
    }

    @Test
    @DisplayName("placeUnit same unit on same tile is a no-op (idempotent)")
    public void placeUnitSameUnitIdempotent() {
        GameMap m = map5x5();
        Archer a = new Archer("A",0,0);
        m.placeUnit(a, 2, 2);
        m.placeUnit(a, 2, 2);
        assertEquals(a, m.getTile(2, 2).getUnit());
    }

    // ── removeUnit

    @Test
    @DisplayName("removeUnit clears the tile")
    public void removeUnitClearsTile() {
        GameMap m = map5x5();
        Archer a = new Archer("A",0, 0);
        m.placeUnit(a, 2, 2);
        m.removeUnit(a);
        assertFalse(m.getTile(2, 2).isOccupied());
    }

    @Test
    @DisplayName("removeUnit(null) is a no-op")
    public void removeUnitNullIsNoop() {
        GameMap m = map5x5();
        m.removeUnit(null);
        assertFalse(m.getTile(0, 0).isOccupied());
    }

    @Test
    @DisplayName("removeUnit on unplaced unit is a no-op (full-grid scan)")
    public void removeUnitUnplacedIsNoop() {
        GameMap m = map5x5();
        Knight k = new Knight("K", 0, 0);
        m.removeUnit(k);
        assertFalse(m.getTile(0, 0).isOccupied());
    }

    // ── findUnit / findUnitTile

    @Test
    @DisplayName("findUnit returns unit by UUID")
    public void findUnitByUuid() {
        GameMap m = map5x5();
        Archer a = new Archer("A", 0, 0);
        m.placeUnit(a, 1, 3);
        assertEquals(a, m.findUnit(a.getId()));
    }

    @Test
    @DisplayName("findUnit returns null for unknown UUID")
    public void findUnitUnknownReturnsNull() {
        assertNull(map5x5().findUnit(java.util.UUID.randomUUID()));
    }

    @Test
    @DisplayName("findUnit returns null for null UUID")
    public void findUnitNullUuidReturnsNull() {
        assertNull(map5x5().findUnit(null));
    }

    @Test
    @DisplayName("findUnitTile returns the tile the unit is on")
    public void findUnitTileReturnsCorrectTile() {
        GameMap m = map5x5();
        Archer a = new Archer("A",0,0);
        m.placeUnit(a, 2, 4);
        Tile t = m.findUnitTile(a.getId());
        assertNotNull(t);
        assertEquals(2, t.getX());
        assertEquals(4, t.getY());
    }

    @Test
    @DisplayName("findUnitTile returns null for unknown UUID")
    public void findUnitTileUnknownReturnsNull() {
        assertNull(map5x5().findUnitTile(java.util.UUID.randomUUID()));
    }

    @Test
    @DisplayName("findUnitTile returns null for null UUID")
    public void findUnitTileNullReturnsNull() {
        assertNull(map5x5().findUnitTile(null));
    }

    @Test
    @DisplayName("findPath returns shortest orthogonal route around obstacles")
    public void findPathAroundObstacles() {
        GameMap m = map5x5();
        m.setTileType(1, 0, TileType.OBSTACLE);
        m.setTileType(1, 1, TileType.OBSTACLE);

        List<Tile> path = m.findPath(0, 0, 2, 0);

        assertEquals(7, path.size(), "Najkrótsza ścieżka powinna mieć 6 kroków i zawierać oba końce");
        assertEquals(0, path.get(0).getX());
        assertEquals(0, path.get(0).getY());
        assertEquals(2, path.get(path.size() - 1).getX());
        assertEquals(0, path.get(path.size() - 1).getY());
        assertTrue(path.stream().noneMatch(Tile::isObstacle), "Ścieżka nie może prowadzić przez przeszkody");
    }

    @Test
    @DisplayName("findPath returns empty when destination is unreachable")
    public void findPathUnreachable() {
        GameMap m = map5x5();
        m.setTileType(1, 0, TileType.OBSTACLE);
        m.setTileType(0, 1, TileType.OBSTACLE);

        assertTrue(m.findPath(0, 0, 2, 2).isEmpty());
    }

    @Test
    @DisplayName("getReachableTiles respects speed and occupied blockers")
    public void reachableTilesRespectBlockers() {
        GameMap m = map5x5();
        Archer blocker = new Archer("Blocker",0, 0);
        m.placeUnit(blocker, 1, 0);

        List<Tile> reachable = m.getReachableTiles(0, 0, 2);

        assertTrue(reachable.stream().anyMatch(tile -> tile.getX() == 0 && tile.getY() == 2));
        assertTrue(reachable.stream().noneMatch(tile -> tile.getX() == 2 && tile.getY() == 0),
                "Jednostka nie może przejść przez zajęty kafelek");
    }

    @Test
    @DisplayName("getDistance uses orthogonal tile distance")
    public void distanceUsesManhattanMetric() {
        assertEquals(5, map5x5().getDistance(0, 0, 2, 3));
    }

    @Test
    @DisplayName("hasLineOfSight returns false when obstacle blocks the shot")
    public void lineOfSightBlockedByObstacle() {
        GameMap m = map5x5();
        m.setTileType(1, 0, TileType.OBSTACLE);

        assertFalse(m.hasLineOfSight(0, 0, 2, 0));
    }

    @Test
    @DisplayName("hasLineOfSight returns true across non-blocking terrain")
    public void lineOfSightAcrossWater() {
        GameMap m = map5x5();
        m.setTileType(1, 0, TileType.WATER);

        assertTrue(m.hasLineOfSight(0, 0, 2, 0));
    }
}
