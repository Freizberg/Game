package GameEngine;

import Action.Action;
import Action.AttackAction;
import Action.HealAction;
import Action.MoveAction;
import GameController.GameStateUpdate;
import Map.GameMap;
import Map.Tile;
import Units.Mage;
import Units.Unit;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * Central simulation engine. Owns the authoritative game state and drives
 * the PLANNING → RESOLVING → (repeat) turn cycle.
 *
 * <p>The engine is the single source of truth for unit positions, HP values,
 * and the current round. All {@link Action} executions and win-condition
 * checks are coordinated here. Delegates combat resolution to
 * {@link CombatResolver} and movement resolution to {@link MovementResolver}.</p>
 *
 * @author Dzhyhar Volodymyr
 * @author Cybulski Mikołaj
 */
public class GameEngine implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private List<Player> players;
    private GameMap map;
    private int currentRound;
    private GameState state;
    private CombatResolver combatResolver = null;
    private MovementResolver movementResolver;
    private UUID winnerId = null;

    private List<UUID> deadUnitsThisRound;

    public GameEngine() {
        this.players = new ArrayList<>();
        this.currentRound = 1;
        this.state = GameState.WAITING;
        this.deadUnitsThisRound = new ArrayList<>();
        this.movementResolver = new MovementResolver();
        this.combatResolver = new CombatResolver(new ArrayList<>());
    }

    /**
     * Marks the given player's planning phase as ended.
     * If {@link #allPlayersEndedTurn()} returns {@code true} after this call,
     * the engine should transition to the RESOLVING phase.
     *
     * @param p the player who is ending their turn
     */
    public void endTurn(Player p) {
        if (p != null) {
            p.endTurn();
        }

        if (this.state == GameState.PLANNING && allPlayersEndedTurn()) {
            this.state = GameState.RESOLVING;
        }
    }

    /**
     * Looks up and returns the player with the given UUID.
     *
     * @param playerId the UUID of the player to retrieve
     * @return the matching {@link Player}
     */
    public Player getPlayer(UUID playerId) {
        if (players != null) {
            for (Player p : players) {
                if (p.getUuid().equals(playerId)) {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * Returns whether every player in the session has ended their turn,
     * indicating that the engine may proceed to the RESOLVING phase.
     *
     * @return {@code true} if all players have called {@link Player#endTurn()}
     */
    public boolean allPlayersEndedTurn() {
        if (players == null || players.isEmpty()) return false;

        for (Player p : players) {
            if (!p.isTurnEnded()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Executes all queued {@link Action} objects for the current round in order,
     * delegating movement to {@link MovementResolver} and attacks to
     * {@link CombatResolver}. Advances {@code currentRound} and resets
     * player turn states when done.
     */
    public void resolveRound() {
        if (state != GameState.RESOLVING) {
            state = GameState.RESOLVING;
        }
        deadUnitsThisRound.clear();

        resolveCollisions();

        // Movement phase
        for (Player p : players) {
            List<Action> actions = new ArrayList<>(p.getPlannedActions());
            for (Action a : actions) {
                if (a instanceof MoveAction) {
                    if (a.isValid(map)) {
                        movementResolver.resolve((MoveAction) a, this);
                    }
                }
            }
        }

        // Combat phase
        List<Action> combatActions = new ArrayList<>();
        for (Player p : players) {
            for (Action a : p.getPlannedActions()) {
                if (a instanceof AttackAction || a instanceof HealAction) {
                    if (a.isValid(map)) {
                        combatActions.add(a);
                    }
                }
            }
        }

        if (combatResolver != null) {
            combatResolver.setActions(combatActions);
        }

        if (combatResolver != null && !combatActions.isEmpty()) {
            combatResolver.resolveAll(this);
        }

        // Cleanup i regeneracja many magów
        for (Player p : players) {
            List<Unit> survivingUnits = new ArrayList<>();

            for (Unit u : p.getUnits()) {
                if (u.isAlive()) {
                    survivingUnits.add(u);

                    if (u instanceof Mage) {
                        ((Mage) u).restoreMana(GameConfig.getManaRegenPerRound());
                    }
                } else {
                    deadUnitsThisRound.add(u.getId());
                    map.removeUnit(u);
                }
            }
            p.setUnits(survivingUnits);
        }

        Player winner = checkWinCondition();

        boolean draw = false;
        if (winner == null) {
            int alivePlayers = 0;
            for (Player p : players) {
                if (p.hasUnitsAlive()) alivePlayers++;
            }
            if (alivePlayers == 0) draw = true;
        }

        // Reset rundy
        for (Player p : players) {
            p.clearActions();
            for (Unit u : p.getUnits()) {
                u.resetActions();
            }
        }
        currentRound++;

        if (winner != null) {
            state = GameState.FINISHED;
        } else if (draw) {
            state = GameState.DRAW;
        } else {
            state = GameState.PLANNING;
        }
    }

    /**
     * Checks whether a win condition has been reached (e.g. only one player
     * has units remaining) and returns the winning player.
     *
     * @return the winning {@link Player}, or {@code null} if the game is still ongoing
     */
    public Player checkWinCondition() {
        Player lastAlive = null;
        int playersWithUnits = 0;

        for (Player p : players) {
            if (p.hasUnitsAlive()) {
                lastAlive = p;
                playersWithUnits++;
            }
        }

        return playersWithUnits == 1 ? lastAlive : null;
    }

    /**
     * Resolves tile conflicts that arise when two or more units attempt to move
     * to the same destination in the same round.
     * Called by {@link #resolveRound()} before individual move actions are applied.
     */
    public void resolveCollisions() {
        Map<String, List<Action>> targetTiles = new HashMap<>();

        for (Player p : players) {
            for (Action a : p.getPlannedActions()) {
                if (a instanceof MoveAction) {
                    MoveAction move = (MoveAction) a;
                    String coordKey = move.getDestX() + "," + move.getDestY();
                    targetTiles.computeIfAbsent(coordKey, k -> new ArrayList<>()).add(move);
                }
            }
        }

        // Usuwanie kolizji
        for (List<Action> conflictedMoves : targetTiles.values()) {
            if (conflictedMoves.size() > 1) {
                for (Action conflictedAction : conflictedMoves) {
                    for (Player p : players) {
                        p.getPlannedActions().remove(conflictedAction);
                    }
                }
            }
        }
    }

    /**
     * Builds a {@link GameStateUpdate} snapshot reflecting the current engine state
     * (unit positions, HP values, dead units, winner, and round number).
     * Used by the host to synchronise all clients after each resolved round.
     *
     * @return a fully populated {@link GameStateUpdate} ready to be broadcast
     */
    public GameStateUpdate buildStateUpdate() {
        Map<UUID, int[]> currentPositions = new HashMap<>();
        Map<UUID, Integer> currentHPs = new HashMap<>();

        for (Player p : players) {
            for (Unit u : p.getUnits()) {
                currentPositions.put(u.getId(), new int[]{u.getPosX(), u.getPosY()});
                currentHPs.put(u.getId(), u.getHp());
            }
        }

        UUID winnerUuid = null;
        Player winner = checkWinCondition();
        if (winner != null) {
            winnerUuid = winner.getUuid();
        }

        return new GameStateUpdate(
                currentPositions,
                currentHPs,
                new ArrayList<>(deadUnitsThisRound),
                winnerUuid,
                this.state == GameState.DRAW,
                currentRound
        );
    }

    public List<Player> getPlayers() { return players; }

    public void setPlayers(List<Player> players) { this.players = players; }

    public GameMap getMap() { return map; }

    public void setMap(GameMap map) { this.map = map; }

    public int getCurrentRound() { return currentRound; }

    public void setCurrentRound(int currentRound) { this.currentRound = currentRound; }

    public GameState getState() { return state; }

    public void setState(GameState state) { this.state = state; }

    public void setWinner(UUID winnerId) {
        this.winnerId = winnerId;
    }

    public UUID getWinnerId() {
        return this.winnerId;
    }

    /**
     * Odświeża układ jednostek na Tiles po otrzymaniu aktualizacji z sieci.
     * Ponieważ GameStateUpdate nadpisuje koordynaty posX, posY wewnątrz samych
     * obiektów Unit, siatka mapy mogłaby nadal trzymać referencje na starych kafelkach.
     * Ta metoda czyści całą mapę, a następnie ustawia jednostki na ich nowych pozycjach.
     */
    public void refreshMapOccupancy() {
        if (map == null) return;

        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                if (tile != null) {
                    tile.setUnit(null); // Ustawiamy brak okupanta
                }
            }
        }

        for (Player p : players) {
            for (Unit u : p.getUnits()) {
                if (u.isAlive()) {
                    map.placeUnit(u, u.getPosX(), u.getPosY());
                }
            }
        }
    }
}
