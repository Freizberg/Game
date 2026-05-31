package Units;

import java.io.Serial;

import GameEngine.GameConfig;
import Map.Tile;

/**
 * A ranged unit that attacks from a distance.
 * The {@code range} field determines how many tiles away a valid target can be.
 *
 * @author Dzhyhar Volodymyr
 */
public class Archer extends Unit {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs an Archer with the given stats.
     *
     * @param name           display name
     * @param posX           starting column position
     * @param posY           starting row position
     */
    public Archer(String name, int posX, int posY) {
        super(name, GameConfig.getArcherHp(), GameConfig.getArcherSpeed(), GameConfig.getActionsPerTurn(), posX, posY);
        this.setAttackRange(GameConfig.getArcherRange());
    }
}
