package GameEngine;

import Action.Action;
import Units.Unit;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a participant in the game session, either local or remote.
 *
 * <p>Each player owns a set of {@link Unit} objects and accumulates
 * {@link Action} objects during the PLANNING phase. When the player calls
 * {@link #endTurn()}, the engine can proceed once all players have done so.</p>
 *
 * @author Dzhyhar Volodymyr
 * @author Cybulski Mikołaj
 */
public class Player implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * Unique identifier for this player.
     */
    private UUID uuid;
    /**
     * Display name of this player.
     */
    private final String name;
    /**
     * The units owned by this player.
     */
    private List<Unit> units;
    /**
     * Actions queued by this player during the current planning phase.
     */
    private List<Action> plannedActions;
    /**
     * {@code true} if this player is controlled on the local machine;
     * {@code false} if their input arrives over the network.
     */
    private boolean isLocal;
    /**
     * {@code true} once the player has signalled the end of their planning phase.
     */
    private boolean turnEnded;

    /**
     * Constructs a player with the given display name.
     * A unique UUID is assigned automatically.
     *
     * @param name the display name of the player
     */
    public Player(String name) {
        this.name = name;
        uuid = UUID.randomUUID();
        units = new ArrayList<>();
        plannedActions = new ArrayList<>();
        turnEnded = false;
    }

    /**
     * Adds an {@link Action} to this player's queue for the current round.
     * Should only be called during the PLANNING phase.
     *
     * @param a the action to queue
     */
    public void queueAction(Action a) {
        if (a != null) {
            plannedActions.add(a);
        }
    }

    /**
     * Marks this player's planning phase as complete.
     * Once all players have called this, the engine moves to the RESOLVING phase.
     */
    public void endTurn() {
        turnEnded = true;
    }

    /**
     * Removes all queued actions, typically called at the start of each new round.
     */
    public void clearActions() {
        plannedActions.clear();
        turnEnded = false;
    }

    /**
     * Returns whether this player has ended their turn for the current round.
     *
     * @return {@code true} if the player has called {@link #endTurn()}
     */
    public boolean isTurnEnded() {
        return turnEnded;
    }

    /**
     * Returns whether this player has at least one living unit remaining.
     *
     * @return {@code true} if any unit in {@link #units} is alive
     */
    public boolean hasUnitsAlive() {
        if (units == null || units.isEmpty()) {
            return false;
        }

        for (Unit unit : units) {
            if (unit.isAlive()) {
                return true;
            }
        }
        return false;
    }

    public String getName() { return name; }

    public UUID getUuid() { return uuid; }

    public List<Unit> getUnits() {
        return units;
    }

    public void setUnits(List<Unit> units) {
        this.units = units;
    }

    public List<Action> getPlannedActions() {
        return plannedActions;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public void setLocal(boolean local) {
        isLocal = local;
    }

    public boolean isPlayersUnit(UUID unitId) {
        for (Unit unit : units) {
            if(unit.getId().equals(unitId)){return true;}
        }
        return false;
    }
}
