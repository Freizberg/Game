package GameRenderer;

import Action.Action;
import GameController.GameController;
import GameController.GameMouseHandler;
import GameEngine.GameState;
import GameEngine.Player;
import Map.GameMap;
import Units.Unit;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.UUID;

/**
 * {@link GameRenderer} implementation that draws the game using the Java Swing framework.
 *
 * <p>Extends a Swing component and overrides {@link #paintComponent(Graphics)} to
 * perform all rendering. The game map, units, HUD, and planned actions are each
 * drawn as layers within a single component. {@link #tileSize} controls the pixel
 * dimensions of each map tile, allowing the view to be scaled.</p>
 *
 * @author Dzhyhar Volodymyr
 */
public class SwingRenderer extends JPanel implements GameRenderer {
    private GameController gameController;
    private GameMouseHandler mouseHandler;
    private Player player;

    /** The side length in pixels of each map tile. */
    int tileSize;

    public SwingRenderer(GameController gameController, int tileSize, Player player) {
        this.gameController = gameController;
        this.tileSize = tileSize;
        this.player = player;
        this.mouseHandler = new GameMouseHandler(this, player, tileSize);

        this.setPreferredSize(new Dimension(tileSize*gameController.getEngine().getMap().getWidth(),tileSize*gameController.getEngine().getMap().getHeight()));
        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);
        this.addMouseWheelListener(mouseHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderMap(GameMap m) {
        //TODO
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderUnit(Unit u) {
        //TODO
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderHUD(List<Player> p, GameState state, UUID winnerId) {
        //TODO
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderPlannedActions(List<Action> a) {
        //TODO
    }

    public GameController getGameController() {
        return gameController;
    }

    /**
     * Paints all game layers onto the component's graphics context.
     * Called automatically by Swing's repaint mechanism and should not
     * be invoked directly; use {@code repaint()} instead.
     *
     * @param g the graphics context provided by Swing
     */
    public void paintComponent(Graphics g) {
        //TODO
    }
}
