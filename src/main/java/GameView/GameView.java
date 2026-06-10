package GameView;

import GameEngine.GameState;
import Map.TileType;

import java.util.List;
import java.util.UUID;

/**
 * Presentation-oriented view model used by the graphical user interface
 * to display the current match state.
 *
 * <p>This class should act as a read-friendly snapshot for rendering logic.
 * It is intended to contain only data needed by the UI layer, such as
 * current phase, round number, visible tiles, units, player summaries,
 * and planned actions.</p>
 *
 * <p>At the current stage this class is only a structural contract for the
 * future graphical interface. Methods are intentionally left without business
 * logic and should be implemented once the GUI requirements are finalized.</p>
 *
 * @author Dzhyhar Volodymyr
 * @author Piotr Gryszka
 */
public final class GameView {

    /** Current high-level match phase shown in the UI. */
    private final GameState state;

    /** Number of the current round displayed by the UI. */
    private final int currentRound;

    /** Winner identifier, or {@code null} if the game is still ongoing. */
    private final UUID winnerId;

    /** Whether autosave is currently enabled. */
    private final boolean autosaveEnabled;

    /** Visible tiles prepared for rendering. */
    private final List<TileView> tiles;

    /** Visible units prepared for rendering. */
    private final List<UnitView> units;

    /** Player summaries prepared for HUD rendering. */
    private final List<PlayerView> players;

    /** Planned actions currently shown in side panels or overlays. */
    private final List<PlannedActionView> plannedActions;

    /** Id of the unit currently selected by the local player, or null. */
    private final UUID selectedUnitId;

    /**
     * Creates a complete game view snapshot.
     *
     * @param state current game phase
     * @param currentRound current round number
     * @param winnerId winner UUID, or {@code null} if there is no winner yet
     * @param autosaveEnabled whether autosave is enabled
     * @param tiles tile views prepared for rendering
     * @param units unit views prepared for rendering
     * @param players player views prepared for HUD display
     * @param plannedActions planned action views prepared for side panels
     * @param selectedUnitId id of the unit currently selected, or {@code null}
     */
    public GameView(GameState state,
                    int currentRound,
                    UUID winnerId,
                    boolean autosaveEnabled,
                    List<TileView> tiles,
                    List<UnitView> units,
                    List<PlayerView> players,
                    List<PlannedActionView> plannedActions,
                    UUID selectedUnitId) {
        this.state = state;
        this.currentRound = currentRound;
        this.winnerId = winnerId;
        this.autosaveEnabled = autosaveEnabled;
        this.tiles = tiles == null ? List.of() : List.copyOf(tiles);
        this.units = units == null ? List.of() : List.copyOf(units);
        this.players = players == null ? List.of() : List.copyOf(players);
        this.plannedActions = plannedActions == null ? List.of() : List.copyOf(plannedActions);
        this.selectedUnitId = selectedUnitId;
    }

    /**
     * Returns the current game phase shown by the UI.
     *
     * @return current game phase
     */
    public GameState getState() {
        return state;
    }

    /**
     * Returns the current round number visible in the GUI.
     *
     * @return round number
     */
    public int getCurrentRound() {
        return currentRound;
    }

    /**
     * Returns the winner identifier.
     *
     * @return winner UUID, or {@code null} if no winner exists
     */
    public UUID getWinnerId() {
        return winnerId;
    }

    /**
     * Returns whether autosave is enabled.
     *
     * @return {@code true} if autosave is enabled
     */
    public boolean isAutosaveEnabled() {
        return this.autosaveEnabled;
    }

    /**
     * Returns all tiles prepared for graphical rendering.
     *
     * @return tile views
     */
    public List<TileView> getTiles() {
        return tiles;
    }

    /**
     * Returns all units prepared for graphical rendering.
     *
     * @return unit views
     */
    public List<UnitView> getUnits() {
        return units;
    }

    /**
     * Returns all player summaries shown in the HUD.
     *
     * @return player views
     */
    public List<PlayerView> getPlayers() {
        return players;
    }

    /**
     * Returns all currently planned actions shown in side panels or overlays.
     *
     * @return planned action views
     */
    public List<PlannedActionView> getPlannedActions() {
        return plannedActions;
    }

    /**
     * Returns the id of the unit currently selected by the local player.
     *
     * @return selected unit UUID, or {@code null} if nothing is selected
     */
    public UUID getSelectedUnitId() {
        return selectedUnitId;
    }

    /**
     * Returns whether this view currently contains enough information
     * to be rendered safely by the GUI.
     *
     * @return {@code true} if the view is render-ready
     */
    public boolean isRenderable() {
        return !tiles.isEmpty() && !units.isEmpty() && !players.isEmpty();
    }

    /**
     * Lightweight tile view for graphical rendering.
     *
     * <p>This class should contain only tile-related presentation data,
     * not domain behavior.</p>
     */
    public static final class TileView {

        /** Tile X coordinate. */
        private final int x;

        /** Tile Y coordinate. */
        private final int y;

        /** Presentation-friendly tile type name. */
        private final TileType type;

        /** Whether the tile is currently occupied. */
        private final boolean occupied;

        /** Whether the tile is highlighted by the GUI. */
        private final boolean highlighted;

        /**
         * Creates a tile view.
         *
         * @param x tile X coordinate
         * @param y tile Y coordinate
         * @param type tile type
         * @param occupied whether the tile is occupied
         * @param highlighted whether the tile is highlighted by the GUI
         */
        public TileView(int x, int y, TileType type, boolean occupied, boolean highlighted) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.occupied = occupied;
            this.highlighted = highlighted;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public TileType getType() {
            return type;
        }

        public boolean isOccupied() {
            return occupied;
        }

        public boolean isHighlighted() {
            return highlighted;
        }
    }

    /**
     * Lightweight unit view for graphical rendering.
     *
     * <p>This class should contain only unit-related presentation data,
     * not combat, movement, or validation logic.</p>
     */
    public static final class UnitView {

        /** Unit identifier. */
        private final UUID unitId;

        /**Unit name*/
        private final String name;

        /** Owning player identifier. */
        private final UUID ownerId;

        /** Display name or unit type. */
        private final String unitType;

        /** Current unit health. */
        private final int hp;

        /** Current X position. */
        private final int x;

        /** Current Y position. */
        private final int y;

        /** Whether the unit is currently selected in the GUI. */
        private final boolean selected;

        /** Max health points*/
        private final int maxHp;

        /** Tiles the unit can move per action. */
        private final int speed;

        /** Attack range in tiles. */
        private final int attackRange;

        /** Armor value, or {@code 0} if the unit has no armor. */
        private final int armor;

        /** Current mana, or {@code -1} if the unit is not a mana user. */
        private final int mana;

        /** Maximum mana, or {@code -1} if the unit is not a mana user. */
        private final int maxMana;

        /** Total actions this unit may queue per turn. */
        private final int actionsPerTurn;

        /** Whether this unit can currently cast a spell (mage with enough mana). */
        private final boolean canCast;

        /**
         * Creates a unit view.
         *
         * @param unitId unit identifier
         * @param name unit name
         * @param ownerId owning player identifier
         * @param unitType display name or unit type
         * @param hp current unit health
         * @param maxHp max health points
         * @param x current X position
         * @param y current Y position
         * @param selected whether the unit is currently selected in the GUI
         * @param speed tiles the unit can move per action
         * @param attackRange attack range in tiles
         * @param armor armor value, or {@code 0} if none
         * @param mana current mana, or {@code -1} if not a mana user
         * @param maxMana maximum mana, or {@code -1} if not a mana user
         * @param actionsPerTurn total actions this unit may queue per turn
         * @param canCast whether the unit can currently cast a spell
         */
        public UnitView(UUID unitId, String name, UUID ownerId, String unitType,
                        int hp, int maxHp, int x, int y, boolean selected,
                        int speed, int attackRange, int armor, int mana, int maxMana,
                        int actionsPerTurn, boolean canCast) {
            this.unitId = unitId;
            this.name = name;
            this.ownerId = ownerId;
            this.unitType = unitType;
            this.hp = hp;
            this.maxHp = maxHp;
            this.x = x;
            this.y = y;
            this.selected = selected;
            this.speed = speed;
            this.attackRange = attackRange;
            this.armor = armor;
            this.mana = mana;
            this.maxMana = maxMana;
            this.actionsPerTurn = actionsPerTurn;
            this.canCast = canCast;
        }

        public UUID getUnitId() {
            return unitId;
        }

        public UUID getOwnerId() {
            return ownerId;
        }

        public String getUnitType() {
            return unitType;
        }

        public int getHp() {
            return hp;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public boolean isSelected() {
            return selected;
        }

        public int getMaxHp() { return maxHp; }

        public String getName(){
            return name;
        }

        public int getSpeed() { return speed; }

        public int getAttackRange() { return attackRange; }

        public int getArmor() { return armor; }

        public int getMana() { return mana; }

        public int getMaxMana() { return maxMana; }

        public boolean usesMana() { return mana >= 0; }

        public int getActionsPerTurn() { return actionsPerTurn; }

        public boolean canCast() { return canCast; }

    }

    /**
     * Lightweight player summary for the HUD.
     */
    public static final class PlayerView {

        /** Player identifier. */
        private final UUID playerId;

        /** Display name shown in the HUD. */
        private final String name;

        /** Whether this player has already ended their turn. */
        private final boolean turnEnded;

        /** Number of remaining units shown in player summary panels. */
        private final int unitsCount;

        /**
         * Creates a player view.
         *
         * @param playerId player identifier
         * @param name display name shown in the HUD
         * @param turnEnded whether this player has already ended their turn
         * @param unitsCount number of remaining units
         */
        public PlayerView(UUID playerId, String name, boolean turnEnded, int unitsCount) {
            this.playerId = playerId;
            this.name = name;
            this.turnEnded = turnEnded;
            this.unitsCount = unitsCount;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public String getName() {
            return name;
        }

        public boolean isTurnEnded() {
            return turnEnded;
        }

        public int getUnitsCount() {
            return unitsCount;
        }
    }

    /**
     * Lightweight planned action view for side panels, overlays and debug widgets.
     */
    public static final class PlannedActionView {

        /** Presentation-friendly action type name. */
        private final String actionType;

        /** Unit that owns or performs the action. */
        private final UUID unitId;

        /** Optional human-readable action description for the GUI. */
        private final String description;

        /**
         * Creates a planned action view.
         *
         * @param actionType presentation-friendly action type name
         * @param unitId unit that owns or performs the action
         * @param description optional human-readable action description for the GUI
         */
        public PlannedActionView(String actionType, UUID unitId, String description) {
            this.actionType = actionType;
            this.unitId = unitId;
            this.description = description;
        }

        public String getActionType() {
            return actionType;
        }

        public UUID getUnitId() {
            return unitId;
        }

        public String getDescription() {
            return description;
        }
    }
}