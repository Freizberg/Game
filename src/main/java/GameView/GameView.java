package GameView;

import Action.Action;
import GameEngine.GameState;
import GameController.*;
import GameEngine.Player;
import Map.GameMap;
import Map.Tile;
import Map.TileType;
import Units.Unit;

import java.util.ArrayList;
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
public class GameView {

    /** Current high-level match phase shown in the UI. */
    private GameState state;

    /** Number of the current round displayed by the UI. */
    private int currentRound;

    /** Winner identifier, or {@code null} if the game is still ongoing. */
    private UUID winnerId;

    /** Whether autosave is currently enabled. */
    private boolean autosaveEnabled;

    /** Visible tiles prepared for rendering. */
    private List<TileView> tiles;

    /** Visible units prepared for rendering. */
    private List<UnitView> units;

    /** Player summaries prepared for HUD rendering. */
    private List<PlayerView> players;

    /** Planned actions currently shown in side panels or overlays. */
    private List<PlannedActionView> plannedActions;

    /** Id of the unit currently selected by the local player, or null. */
    private UUID selectedUnitId;

    /**
     * Creates an empty game view.
     *
     * <p>This constructor is intended for scenarios where the view is created
     * first and populated later by a mapper, builder, or controller adapter.</p>
     */
    public GameView() {

    }

    /**
     * Creates a complete game view snapshot.
     *
     * @param state current game phase
     * @param currentRound current round number
     * @param winnerId winner UUID, or {@code null} if there is no winner yet
     * @param autosaveEnabled whether autosave is enabled
     * @param tiles tile views prepared for rendering
     * @param players player views prepared for HUD display
     * @param plannedActions planned action views prepared for side panels
     */
    public GameView(GameState state,
                    int currentRound,
                    UUID winnerId,
                    boolean autosaveEnabled,
                    List<TileView> tiles,
                    List<UnitView> units,
                    List<PlayerView> players,
                    List<PlannedActionView> plannedActions) {
    }

    /**
     * Builds a graphical snapshot from the current {@link GameController}.
     *
     * <p>This factory method should gather all presentation-relevant data
     * from the controller and convert it into a UI-friendly structure.</p>
     *
     * @param gs GameState, state
     * @param cr Integer, current round
     * @param wid UUID, winner uuid
     * @param ase boolean, auto save enabled
     * @param map GameMap, map
     * @param pl List<Player>, list of players
     * @param al List<Action>, list of planned actions
     * @param sel UUID, selected unit id
     * @return graphical snapshot prepared for rendering
     */
    public static GameView fromController(GameState gs, int cr, UUID wid, boolean ase, GameMap map, List<Player> pl, List<Action> al, UUID sel) {
        GameView gv = new GameView();
        gv.refreshFromController(gs,cr,wid,ase,map,pl,al,sel);
        if (gv.isRenderable()) {
            return gv;
        }
        return null;
    }

    /**
     * Refreshes this view from the given controller.
     *
     * <p>This method may later be used by GUI refresh cycles, HUD updates,
     * or manual synchronization after user interaction.</p>
     *
     * @param gs GameState, state
     * @param cr Integer, current round
     * @param wid UUID, winner uuid
     * @param ase boolean, auto save enabled
     * @param map GameMap, map
     * @param pl List<Player>, list of players
     * @param al List<Action>, list of planned actions
     * @param sel UUID, selected unit id
     */
    public void refreshFromController(GameState gs, int cr, UUID wid, boolean ase, GameMap map, List<Player> pl, List<Action> al, UUID sel) {
        this.setState(gs);
        this.setCurrentRound(cr);
        this.setWinnerId(wid);
        this.setAutosaveEnabled(ase);
        this.setTiles(map);
        this.setPlayers(pl);
        this.setPlannedActions(al);
        this.selectedUnitId=sel;
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
     * Updates the current game phase displayed by the UI.
     *
     * @param state new game phase
     */
    public void setState(GameState state) {
        this.state=state;
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
     * Updates the current round number displayed in the GUI.
     *
     * @param currentRound round number
     */
    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
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
     * Updates the winner identifier shown in the result overlay or HUD.
     *
     * @param winnerId winner UUID, or {@code null} if there is no winner yet
     */
    public void setWinnerId(UUID winnerId) {
        this.winnerId=winnerId;
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
     * Updates the autosave indicator shown in settings or HUD widgets.
     *
     * @param autosaveEnabled autosave flag
     */
    public void setAutosaveEnabled(boolean autosaveEnabled) {
        this.autosaveEnabled=autosaveEnabled;
    }

    /**
     * Returns all tiles prepared for graphical rendering.
     *
     * @return tile views
     */
    public List<TileView> getTiles() {
        if (tiles == null) {
            ArrayList<TileView> list = new ArrayList<>();
            return list;
        }
        return tiles;
    }

    /**
     * Replaces the current tile snapshot used by the renderer.
     *
     * @param map GameMap object for tiles
     */
    public void setTiles(GameMap map) {
        List<TileView> tiles = new ArrayList<>();
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                tiles.add(new TileView(map.getTiles()[x][y]));
            }
        }
        this.tiles=tiles;
    }

    /**
     * Returns all units prepared for graphical rendering.
     *
     * @return unit views
     */
    public List<UnitView> getUnits() {
        if (units == null) {
            List<UnitView> list = new ArrayList<>();
            return list;
        }
        return units;
    }

    /**
     * Replaces the current unit snapshot used by the renderer.
     *
     * @param players list of Player objects
     */
    public void setUnits(List<Player> players) {
        List<UnitView> units = new ArrayList<>();
        for (Player p : players) {
            if (p.getUnits() == null) continue;
            for (Unit u : p.getUnits()) {
                boolean selected = u.getId().equals(this.selectedUnitId);
                units.add(new UnitView(u, p, selected));
            }
        }
        this.units = units;
    }

    /**
     * Returns all player summaries shown in the HUD.
     *
     * @return player views
     */
    public List<PlayerView> getPlayers() {
        if(players == null) {
            List<PlayerView> list = new ArrayList<>();
            return list;
        }
        return players;
    }

    /**
     * Replaces the current player summary snapshot shown in the HUD.
     *
     * @param players player views
     */
    public void setPlayers(List<Player> players) {
        List<PlayerView> playersViews = new ArrayList<>();
        for (Player p : players) {
            playersViews.add(new PlayerView(p));
        }
        this.players=playersViews;
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
     * Replaces the planned action snapshot currently shown by the GUI.
     *
     * @param actions planned actions
     */
    public void setPlannedActions(List<Action> actions) {
        List<PlannedActionView> plannedActionViews = new ArrayList<>();
        for (Action a : actions) {
            plannedActionViews.add(new PlannedActionView(a));
        }
        this.plannedActions=plannedActionViews;
    }

    /**
     * Selects one unit in the graphical view.
     *
     * <p>This method is intended for GUI interactions such as clicking a unit
     * on the map or navigating units with the keyboard.</p>
     *
     * @param unitId identifier of the selected unit
     */
    public void selectUnit(UUID unitId) {
        // need a window to implement
        throw new UnsupportedOperationException("Selection logic is not implemented yet.");
    }

    /**
     * Clears all current selections and highlights in the graphical view.
     *
     * <p>This method may be used when the player cancels an action,
     * closes a context menu, or changes the active interaction mode.</p>
     */
    public void clearSelection() {
        // need a window to implement
        throw new UnsupportedOperationException("Selection clearing is not implemented yet.");
    }

    /**
     * Highlights candidate destination tiles in the graphical interface.
     *
     * <p>This method is intended for previewing movement, healing, or attack
     * ranges before the player confirms an action.</p>
     *
     * @param tiles tile identifiers or tile coordinates to highlight
     */
    public void highlightTiles(List<TileView> tiles) {
        // need a window to implement
        throw new UnsupportedOperationException("Tile highlighting is not implemented yet.");
    }

    /**
     * Clears any currently highlighted tiles.
     */
    public void clearHighlightedTiles() {
        // need a window to implement
        throw new UnsupportedOperationException("Tile highlight clearing is not implemented yet.");
    }

    /**
     * Returns whether this view currently contains enough information
     * to be rendered safely by the GUI.
     *
     * @return {@code true} if the view is render-ready
     */
    public boolean isRenderable() {
        if (tiles.isEmpty() || units.isEmpty() || players.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * Lightweight tile view for graphical rendering.
     *
     * <p>This class should contain only tile-related presentation data,
     * not domain behavior.</p>
     */
    public static class TileView {

        /** Tile X coordinate. */
        private int x;

        /** Tile Y coordinate. */
        private int y;

        /** Presentation-friendly tile type name. */
        private TileType type;

        /** Whether the tile is currently occupied. */
        private boolean occupied;

        /** Whether the tile is highlighted by the GUI. */
        private boolean highlighted;

        /**
         * Creates an empty tile view.
         */
        public TileView() {

        }

        /**
         * Creates a tile view.
         *
         * @param t Tile which is converted into information for frontend
         */
        public TileView(Tile t) {
            setX(t.getX());
            setY(t.getY());
            setType(t.getType());
            setOccupied(t.isOccupied());
            setHighlighted(false); //trzeba dorobić jak będzie okno jakieś
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public TileType getType() {
            return type;
        }

        public void setType(TileType type) {
            this.type = type;
        }

        public boolean isOccupied() {
            return occupied;
        }

        public void setOccupied(boolean occupied) {
            this.occupied = occupied;
        }

        public boolean isHighlighted() {
            return highlighted;
        }

        public void setHighlighted(boolean highlighted) {
            this.highlighted=highlighted;
        }
    }

    /**
     * Lightweight unit view for graphical rendering.
     *
     * <p>This class should contain only unit-related presentation data,
     * not combat, movement, or validation logic.</p>
     */
    public static class UnitView {

        /** Unit identifier. */
        private UUID unitId;

        /**Unit name*/
        private String name;

        /** Owning player identifier. */
        private UUID ownerId;

        /** Display name or unit type. */
        private String unitType;

        /** Current unit health. */
        private int hp;

        /** Current X position. */
        private int x;

        /** Current Y position. */
        private int y;

        /** Whether the unit is currently selected in the GUI. */
        private boolean selected;

        /** Max health points*/
        private int maxHp;


        /**
         * Creates an empty unit view.
         */
        public UnitView() {

        }

        /**
         * Creates a unit view.
         *
         * @param u Unit object made into information for frontend
         * @param p unit's owner
         */
        public UnitView(Unit u, Player p, boolean selected) {
            setUnitId(u.getId());
            setName(u.getName());
            setOwnerId(p.getUuid());
            setUnitType(u.getClass().getSimpleName());
            setHp(u.getHp());
            setX(u.getPosX());
            setY(u.getPosY());
            setMaxHp(u.getMaxHp());
            this.selected = selected;
        }

        public UUID getUnitId() {
            return unitId;
        }

        public void setUnitId(UUID unitId) {
            this.unitId=unitId;
        }

        public UUID getOwnerId() {
            return ownerId;
        }

        public void setOwnerId(UUID ownerId) {
            this.ownerId=ownerId;
        }

        public String getUnitType() {
            return unitType;
        }

        public void setUnitType(String unitType) {
            this.unitType=unitType;
        }

        public int getHp() {
            return hp;
        }

        public void setHp(int hp) {
            this.hp=hp;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x=x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y=y;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
        public int getMaxHp() { return maxHp; }

        public void setMaxHp(int maxHp) { this.maxHp = maxHp; }

        public String getName(){
            return name;
        }
        public void setName(String name){
            this.name = name;
        }

    }

    /**
     * Lightweight player summary for the HUD.
     */
    public static class PlayerView {

        /** Player identifier. */
        private UUID playerId;

        /** Display name shown in the HUD. */
        private String name;

        /** Whether this player has already ended their turn. */
        private boolean turnEnded;

        /** Number of remaining units shown in player summary panels. */
        private int unitsCount;

        /**
         * Creates an empty player view.
         */
        public PlayerView() {

        }

        /**
         * Creates a player view.
         *
         * @param p Player object which is made into information for frontend
         */
        public PlayerView(Player p) {
            setPlayerId(p.getUuid());
            setName(p.getName());
            setTurnEnded(p.isTurnEnded());
            setUnitsCount(p.getUnits().size());
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public void setPlayerId(UUID playerId) {
            this.playerId=playerId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name=name;
        }

        public boolean isTurnEnded() {
            return turnEnded;
        }

        public void setTurnEnded(boolean turnEnded) {
            this.turnEnded=turnEnded;
        }

        public int getUnitsCount() {
            return unitsCount;
        }

        public void setUnitsCount(int unitsCount) {
            this.unitsCount=unitsCount;
        }
    }

    /**
     * Lightweight planned action view for side panels, overlays and debug widgets.
     */
    public static class PlannedActionView {

        /** Presentation-friendly action type name. */
        private String actionType;

        /** Unit that owns or performs the action. */
        private UUID unitId;

        /** Optional human-readable action description for the GUI. */
        private String description;

        /**
         * Creates an empty planned action view.
         */
        public PlannedActionView() {

        }

        /**
         * Creates a planned action view.
         *
         * @param a Class implementing Action interface
         */
        public PlannedActionView(Action a) {
            setActionType(a.type());
            setUnitId(a.getUnitId());
            setDescription(a.description());
        }

        public String getActionType() {
            return actionType;
        }

        public void setActionType(String actionType) {
            this.actionType=actionType;
        }

        public UUID getUnitId() {
            return unitId;
        }

        public void setUnitId(UUID unitId) {
            this.unitId=unitId;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description=description;
        }
    }
}