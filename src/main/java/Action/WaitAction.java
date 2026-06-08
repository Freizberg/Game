package Action;

import GameEngine.GameEngine;
import Map.GameMap;
import Map.Tile;

import java.io.Serial;
import java.util.UUID;

/**
 * Instructs a unit to stay in place for this round.
 * Always valid as long as the unit exists; used to explicitly signal
 * that the unit is not idle by mistake.
 *
 * @author Dzhyhar Volodymyr
 */
public class WaitAction implements Action {
    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID unitId;

    public WaitAction(UUID unitId) {
        this.unitId = unitId;
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
     */
    @Override
    public boolean isValid(GameMap map) {
        Tile[][] tiles = map.getTiles();
        for (Tile[] row : tiles) {
            for (Tile tile : row) {
                if (tile.isOccupied()) {
                    if ((tile.getUnit().getId()).equals(unitId)) {
                        return tile.getUnit().isAlive();
                    }
                }
            }
        }
        return false;
    }

    public String type() { return "Wait";}

    public String description() { return "Wait description.";}
}
