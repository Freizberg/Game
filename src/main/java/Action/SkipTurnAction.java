package Action;

import GameEngine.GameEngine;
import GameEngine.Player;
import Map.GameMap;

import java.io.Serial;
import java.util.UUID;

/**
 * Signals that a player ends their planning phase without issuing further
 * unit commands. This is a player-level action rather than a unit-level one,
 * so {@link #getUnitId()} ()} returns {@code null}.
 *
 * <p>Distinct from {@link WaitAction}: WaitAction targets a specific unit,
 * while SkipTurnAction marks the owning player's turn as ended entirely.</p>
 *
 * @author Dzhyhar Volodymyr
 */
public class SkipTurnAction implements Action {
    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID playerId;

    public SkipTurnAction(UUID playerId) {
        this.playerId = playerId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UUID getUnitId() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(GameMap map) {
        // Skipping a turn is always a legal choice.
        return true;
    }

    /**
     * Returns the UUID of the player who is skipping.
     *
     * @return player UUID
     */
    public UUID getPlayerId() { return playerId; }
}
