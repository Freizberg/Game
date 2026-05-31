package Map;

import Map.GameMap;
import Map.Tile;
import Map.TileType;
import Units.Archer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TileTest {

    // ── isObstacle

    @Test
    @DisplayName("PLAIN tile is not an obstacle")
    public void plainIsNotObstacle() {
        assertFalse(new Tile(TileType.PLAIN).isObstacle());
    }

    @Test
    @DisplayName("FOREST tile is not an obstacle")
    public void forestIsNotObstacle() {
        assertFalse(new Tile(TileType.FOREST).isObstacle());
    }

    @Test
    @DisplayName("OBSTACLE tile is an obstacle")
    public void obstacleIsObstacle() {
        assertTrue(new Tile(TileType.OBSTACLE).isObstacle());
    }

    @Test
    @DisplayName("WATER tile is an obstacle")
    public void waterIsObstacle() {
        assertTrue(new Tile(TileType.WATER).isObstacle());
    }

    // ── isOccupied / setUnit / getUnit

    @Test
    @DisplayName("New tile is unoccupied")
    public void newTileIsUnoccupied() {
        assertFalse(new Tile(TileType.PLAIN).isOccupied());
    }

    @Test
    @DisplayName("getUnit returns null on empty tile")
    public void getUnitNullOnEmpty() {
        assertNull(new Tile(TileType.PLAIN).getUnit());
    }

    @Test
    @DisplayName("setUnit makes tile occupied")
    public void setUnitMakesOccupied() {
        Tile t = new Tile(TileType.PLAIN);
        t.setUnit(new Archer("A",0, 0));
        assertTrue(t.isOccupied());
    }

    @Test
    @DisplayName("getUnit returns the placed unit")
    public void getUnitReturnsPlaced() {
        Tile t = new Tile(TileType.PLAIN);
        Archer archer = new Archer("A",0, 0);
        t.setUnit(archer);
        assertEquals(archer, t.getUnit());
    }

    @Test
    @DisplayName("setUnit(null) clears the occupant")
    public void setUnitNullClearsOccupant() {
        Tile t = new Tile(TileType.PLAIN);
        t.setUnit(new Archer("A", 0, 0));
        t.setUnit(null);
        assertFalse(t.isOccupied());
        assertNull(t.getUnit());
    }

    // ── getType / setType

    @Test
    @DisplayName("getType returns the correct terrain type")
    public void getTypeReturnsCorrect() {
        assertEquals(TileType.FOREST, new Tile(TileType.FOREST).getType());
    }

    @Test
    @DisplayName("setType changes terrain type in place")
    public void setTypeChangesTerrain() {
        Tile t = new Tile(TileType.PLAIN);
        t.setType(TileType.OBSTACLE);
        assertEquals(TileType.OBSTACLE, t.getType());
        assertTrue(t.isObstacle());
    }

    @Test
    @DisplayName("setType does not clear occupying unit — known caveat")
    public void setTypeDoesNotClearUnit() {
        Tile t = new Tile(TileType.PLAIN);
        t.setUnit(new Archer("A",0, 0));
        t.setType(TileType.OBSTACLE);
        assertTrue(t.isOccupied());
        assertEquals(TileType.OBSTACLE, t.getType());
    }

    // ── Coordinate-aware constructor

    @Test
    @DisplayName("Tile(x, y, type) stores coordinates correctly")
    public void coordinateConstructorStoresXY() {
        Tile t = new Tile(3, 7, TileType.FOREST);
        assertEquals(3, t.getX());
        assertEquals(7, t.getY());
        assertEquals(TileType.FOREST, t.getType());
    }

    @Test
    @DisplayName("Backward-compat Tile(type) sets coordinates to -1,-1")
    public void backwardCompatConstructorSetsMinusOne() {
        Tile t = new Tile(TileType.PLAIN);
        assertEquals(-1, t.getX());
        assertEquals(-1, t.getY());
    }

    @Test
    @DisplayName("Tiles created inside GameMap carry correct coordinates")
    public void mapTilesHaveCorrectCoordinates() {
        GameMap m = new GameMap(4, 4);
        Tile t = m.getTile(2, 3);
        assertEquals(2, t.getX());
        assertEquals(3, t.getY());
    }
}