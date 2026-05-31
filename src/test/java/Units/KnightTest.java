package Units;

import GameEngine.GameConfig;
import Units.Knight;
import Map.Tile;
import Map.TileType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KnightTest {

    private Knight knight = new Knight("Sir Lancelot", 150,1);

    @Test
    @DisplayName("getArmor returns constructor value")
    public void armorCorrect() {
        assertEquals(GameConfig.getKnightArmor(), knight.getArmor());
    }

    @Test
    @DisplayName("applyMove places knight on destination tile")
    public void applyMovePlacesOnTile() {
        Knight k = knight;
        Tile dest = new Tile(TileType.PLAIN);
        k.applyMove(dest);
        assertEquals(k, dest.getUnit());
    }

    @Test
    @DisplayName("Knight survives hits reduced by armor (conceptual)")
    public void armorReducesDamageConceptual() {
        Knight k = knight;
        int rawDamage = 15;
        int reducedDamage = Math.max(0, rawDamage - k.getArmor());
        k.applyDamage(reducedDamage);
        assertEquals(GameConfig.getKnightHp()-15+GameConfig.getKnightArmor(), k.getHp());
    }
}