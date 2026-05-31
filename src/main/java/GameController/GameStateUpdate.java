package GameController;

import GameEngine.GameEngine;
import GameEngine.GameState;
import GameEngine.Player;
import Units.Unit;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A serialisable snapshot of changes that occurred during a resolved round,
 * transmitted from the host to all clients in a networked game.
 *
 * <p>Each field captures a specific category of state change. After receiving
 * this object over the network, clients call {@link #apply(GameEngine)} to
 * synchronize their local engine with the authoritative server state.</p>
 *
 * @author Dzhyhar Volodymyr
 * @author Cybulski Mikołaj
 */
public class GameStateUpdate implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * Maps each unit's UUID to its new {@code [x, y]} position on the map.
     */
    public Map<UUID, int[]> unitPositions;
    /**
     * Maps each unit's UUID to its current hit-point value after resolution.
     */
    public Map<UUID, Integer> unitHP;
    /**
     * UUIDs of units that were killed during this round.
     */
    public List<UUID> deadUnits;
    /**
     * UUID of the winning player, or {@code null} if the game is still ongoing.
     */
    public UUID winnerID;
    /**
     * Flaga oznaczająca, że runda zakończyła się wzajemnym wyniszczeniem.
     */
    public boolean isDraw;
    /**
     * The round number this update corresponds to.
     */
    public int currentRound;

    /**
     * Constructs an empty GameStateUpdate.
     * Fields should be populated by the host before the update is sent.
     */
    public GameStateUpdate() {
        this.unitPositions = new HashMap<>();
        this.unitHP = new HashMap<>();
        this.deadUnits = new ArrayList<>();
        this.winnerID = null;
        this.isDraw = false;
        this.currentRound = 0;
    }

    /**
     * Konstruktor parametryczny ułatwiający tworzenie obiektu w GameEngine.buildStateUpdate().
     */
    public GameStateUpdate(Map<UUID, int[]> unitPositions, Map<UUID, Integer> unitHP,
                           List<UUID> deadUnits, UUID winnerID, boolean isDraw, int currentRound) {
        this.unitPositions = unitPositions;
        this.unitHP = unitHP;
        this.deadUnits = deadUnits;
        this.winnerID = winnerID;
        this.isDraw = isDraw;
        this.currentRound = currentRound;
    }

    /**
     * Applies this update to the given {@link GameEngine}, synchronising unit
     * positions, HP values, removing dead units, and advancing the round counter.
     *
     * @param engine the local game engine to update
     */
    public void apply(GameEngine engine) {
        engine.setCurrentRound(this.currentRound);

        for (Player player : engine.getPlayers()) {

            player.getUnits().removeIf(unit -> deadUnits.contains(unit.getId()));

            for (Unit unit : player.getUnits()) {
                UUID unitId = unit.getId();

                if (unitHP.containsKey(unitId)) {
                    unit.setHp(unitHP.get(unitId));
                }

                if (unitPositions.containsKey(unitId)) {
                    int[] pos = unitPositions.get(unitId);
                    unit.setPosX(pos[0]);
                    unit.setPosY(pos[1]);
                }
            }
        }

        engine.refreshMapOccupancy();

        if (this.winnerID != null) {
            engine.setWinner(this.winnerID);
            engine.setState(GameState.FINISHED);
        } else if (this.isDraw) {
            engine.setState(GameState.DRAW);
        } else {
            engine.setState(GameState.PLANNING);
        }
    }
}
