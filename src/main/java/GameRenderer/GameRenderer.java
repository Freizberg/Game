package GameRenderer;

import Action.Action;
import GameEngine.GameState;
import GameEngine.Player;
import Map.GameMap;
import Units.Unit;

import java.util.List;
import java.util.UUID;

/**
 * Abstraction layer for all game rendering concerns.
 *
 * <p>Implementations are responsible for presenting the game state to the player
 * in a specific medium (e.g. console text, Swing graphics). The controller calls
 * these methods after each state change to keep the display up to date.</p>
 *
 * @see ConsoleRenderer
 * @see SwingRenderer
 *
 * @author Dzhyhar Volodymyr
 */
public interface GameRenderer {

    /**
     * Renders the full game map, including terrain and unit positions.
     *
     * @param m the game map to render
     */
    void renderMap(GameMap m);

    /**
     * Renders a single unit, typically showing its name, position, and HP.
     *
     * @param u the unit to render
     */
    void renderUnit(Unit u);

    /**
     * Renders the heads-up display containing player status information
     * such as remaining units and turn state.
     *
     * @param p the list of players whose information should be displayed
     */
    void renderHUD(List<Player> p, GameState state, UUID winnerId);

    /**
     * Renders the actions that have been queued by players during the
     * current planning phase.
     *
     * @param a the list of planned actions to display
     */
    void renderPlannedActions(List<Action> a);
}
