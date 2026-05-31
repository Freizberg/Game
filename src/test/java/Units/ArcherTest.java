package Units;

import Units.Archer;
import Map.Tile;
import Map.TileType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArcherTest {

    private Archer archer() {
        return new Archer("Robin", 80, 3);
    }

    @Test
    @DisplayName("getAttackRange returns constructor value")
    public void attackRangeCorrect() {
        assertEquals(5, archer().getAttackRange());
    }

    @Test
    @DisplayName("applyMove places unit on destination tile")
    public void applyMovePlacesUnitOnTile() {
        Archer a = archer();
        Tile dest = new Tile(TileType.PLAIN);
        a.applyMove(dest);
        assertTrue(dest.isOccupied());
        assertEquals(a, dest.getUnit());
    }

    @Test
    @DisplayName("Two archers with same stats have different UUIDs")
    public void uniqueIds() {
        Archer a1 = archer();
        Archer a2 = archer();
        assertNotEquals(a1.getId(), a2.getId());
    }
}