package Action;

import Action.MoveAction;
import Map.GameMap;
import Map.TileType;
import Units.Archer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MoveActionTest {

    private GameMap map() {
        return new GameMap(5, 5);
    }

    @Test
    @DisplayName("getUnitId returns the unit UUID (no typo)")
    public void getUnitIdReturnsUuid() {
        UUID id = UUID.randomUUID();
        assertEquals(id, new MoveAction(id, 2, 2).getUnitId());
    }

    @Test
    @DisplayName("getDestX / getDestY return constructor values")
    public void destCoordinates() {
        MoveAction ma = new MoveAction(UUID.randomUUID(), 3, 4);
        assertEquals(3, ma.getDestX());
        assertEquals(4, ma.getDestY());
    }

    @Test
    @DisplayName("isValid returns true for empty PLAIN tile")
    public void isValidPlainEmpty() {
        assertTrue(new MoveAction(UUID.randomUUID(), 1, 1).isValid(map()));
    }

    @Test
    @DisplayName("isValid returns false for OBSTACLE tile")
    public void isValidObstacle() {
        GameMap m = map();
        m.setTileType(2, 2, TileType.OBSTACLE);
        assertFalse(new MoveAction(UUID.randomUUID(), 2, 2).isValid(m));
    }

    @Test
    @DisplayName("isValid returns false for WATER tile")
    public void isValidWater() {
        GameMap m = map();
        m.setTileType(0, 0, TileType.WATER);
        assertFalse(new MoveAction(UUID.randomUUID(), 0, 0).isValid(m));
    }

    @Test
    @DisplayName("isValid returns false for occupied tile")
    public void isValidOccupied() {
        GameMap m = map();
        Archer occupier = new Archer("A",0,0);
        m.placeUnit(occupier, 2, 3);
        assertFalse(new MoveAction(UUID.randomUUID(), 2, 3).isValid(m));
    }

    @Test
    @DisplayName("isValid returns false for out-of-bounds destination")
    public void isValidOutOfBounds() {
        assertFalse(new MoveAction(UUID.randomUUID(), 99, 0).isValid(map()));
        assertFalse(new MoveAction(UUID.randomUUID(), 0, -1).isValid(map()));
    }
}