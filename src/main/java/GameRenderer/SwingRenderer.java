package GameRenderer;

import Action.Action;
import GameController.GameController;
import GameController.GameMouseHandler;
import GameEngine.GameState;
import GameEngine.Player;
import GameView.GameView;
import Map.GameMap;
import Map.TileType;
import Units.Unit;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * @author Filip Glaser
 * @author Marcin Świerczyński
 */
public class SwingRenderer extends JPanel implements GameRenderer {

    private GameController gameController;
    private GameMouseHandler mouseHandler;
    private Player player;
    private Map<UUID, Color> playerColors;

    public static final int TILE_SIZE = 64;

    /**
     * Initializes the graphical panel, calculates window dimensions based on the grid,
     * and attaches mouse event listeners.
     *
     * @param gameController the main game logic controller
     * @param player the local player using this interface
     */
    public SwingRenderer(GameController gameController, Player player) {
        this.gameController = gameController;
        this.player = player;
        this.mouseHandler = new GameMouseHandler(this, player, TILE_SIZE);
        this.playerColors = new HashMap<>();

        this.setPreferredSize(new Dimension(TILE_SIZE * gameController.getEngine().getMap().getWidth(),
                TILE_SIZE * gameController.getEngine().getMap().getHeight()));
        this.setBackground(Color.BLACK);

        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);
        this.addMouseWheelListener(mouseHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderMap(GameMap m) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderUnit(Unit u) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderHUD(List<Player> p, GameState state, UUID winnerId) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderPlannedActions(List<Action> a) {
    }

    /**
     * Returns the controller associated with this view.
     *
     * @return the GameController instance
     */
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
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GameView view = null;
        try {
            view = gameController.getGameView();
        } catch (Exception e) {
            view = null;
        }

        if (view != null) {
            try {
                if (view.getTiles() != null) {
                    drawMap(g2d, view);
                }
                if (view.getUnits() != null) {
                    drawUnits(g2d, view);
                }
            } catch (UnsupportedOperationException e) {
                g2d.setColor(Color.RED);
                g2d.drawString("Waiting for GameView data...", 20, 30);
            }
        } else {
            g2d.setColor(Color.WHITE);
            g2d.drawString("Initializing...", 20, 30);
        }
    }

    /**
     * Draws the static map layer: tiles, terrain textures, and the grid.
     * Maps TileType enum values to specific colors and simple vector shapes.
     *
     * @param g2d the 2D graphics context
     * @param view the current presentation data state (DTO)
     */
    private void drawMap(Graphics2D g2d, GameView view) {
        for (GameView.TileView tile : view.getTiles()) {
            if (tile == null) continue;

            int x = tile.getX() * TILE_SIZE;
            int y = tile.getY() * TILE_SIZE;

            TileType type;
            try {
                type = tile.getType();
                //type = TileType.valueOf(tile.getType()); //tu ci zmieniłem bo zmieniłem getType na tiletype
                //jak chcesz accessować String to wpisz: tile.getType().name
            } catch (Exception e) {
                type = TileType.PLAIN;
            }

            switch (type) {
                case PLAIN:
                    g2d.setColor(new Color(120, 180, 100));
                    break;
                case FOREST:
                    g2d.setColor(new Color(34, 139, 34));
                    break;
                case WATER:
                    g2d.setColor(new Color(65, 105, 225));
                    break;
                case OBSTACLE:
                case MOUNTAIN:
                    g2d.setColor(Color.GRAY);
                    break;
                default:
                    g2d.setColor(Color.BLACK);
                    break;
            }

            g2d.fillRect(x, y, TILE_SIZE, TILE_SIZE);

            if (type == TileType.FOREST) {
                g2d.setColor(new Color(0, 100, 0));
                g2d.fillOval(x + 10, y + 10, TILE_SIZE - 20, TILE_SIZE - 20);
            } else if (type == TileType.OBSTACLE || type == TileType.MOUNTAIN) {
                g2d.setColor(Color.DARK_GRAY);
                int[] xPoints = {x + TILE_SIZE / 2, x + 10, x + TILE_SIZE - 10};
                int[] yPoints = {y + 10, y + TILE_SIZE - 10, y + TILE_SIZE - 10};
                g2d.fillPolygon(xPoints, yPoints, 3);
            }

            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.drawRect(x, y, TILE_SIZE, TILE_SIZE);
        }
    }

    /**
     * Draws units, health bars, and owner-specific indicators on top of the map layer.
     *
     * @param g2d the 2D graphics context
     * @param view the current presentation data state (DTO) containing units
     */
    private void drawUnits(Graphics2D g2d, GameView view) {
        g2d.setFont(new Font("Arial", Font.BOLD, 22));
        FontMetrics metrics = g2d.getFontMetrics();

        for (GameView.UnitView unit : view.getUnits()) {
            if (unit == null || unit.getUnitType() == null) continue;

            int x = unit.getX() * TILE_SIZE;
            int y = unit.getY() * TILE_SIZE;

            int padding = 12;
            int circleSize = TILE_SIZE - (padding * 2);
            int circleX = x + padding;
            int circleY = y + padding + 4;

            Color teamColor = getPlayerColor(unit.getOwnerId());
            g2d.setColor(teamColor);
            g2d.fillOval(circleX, circleY, circleSize, circleSize);

            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(circleX, circleY, circleSize, circleSize);

            String letter = unit.getUnitType().substring(0, 1).toUpperCase();
            int textX = circleX + (circleSize - metrics.stringWidth(letter)) / 2;
            int textY = circleY + ((circleSize - metrics.getHeight()) / 2) + metrics.getAscent();

            g2d.setColor(Color.WHITE);
            g2d.drawString(letter, textX, textY);

            int maxHp = getMaxHpForUnitType(unit.getUnitType());
            double hpRatio = Math.max(0.0, Math.min(1.0, (double) unit.getHp() / maxHp));

            int barWidth = circleSize;
            int barHeight = 6;
            int barX = circleX;
            int barY = y + 4;

            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect(barX, barY, barWidth, barHeight);

            if (hpRatio > 0.5) {
                g2d.setColor(new Color(50, 205, 50));
            } else if (hpRatio > 0.2) {
                g2d.setColor(new Color(255, 215, 0));
            } else {
                g2d.setColor(new Color(220, 20, 60));
            }

            int currentBarWidth = (int) (barWidth * hpRatio);
            g2d.fillRect(barX, barY, currentBarWidth, barHeight);

            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRect(barX, barY, barWidth, barHeight);
        }
    }

    /**
     * Maps an owner UUID to a distinct team color.
     *
     * @param ownerId the UUID of the player
     * @return a distinct color assigned to the player
     */
    private Color getPlayerColor(UUID ownerId) {
        if (ownerId == null) {
            return Color.GRAY;
        }

        if (!playerColors.containsKey(ownerId)) {
            Color[] availableColors = {
                    new Color(30, 144, 255),
                    new Color(220, 20, 60),
                    new Color(255, 140, 0),
                    new Color(138, 43, 226)
            };

            Color assigned = availableColors[playerColors.size() % availableColors.length];
            playerColors.put(ownerId, assigned);
        }

        return playerColors.get(ownerId);
    }

    /**
     * Determines the maximum health points for a given unit type.
     *
     * @param unitType the string identifier of the unit
     * @return the maximum health points
     */
    private int getMaxHpForUnitType(String unitType) {
        String type = unitType.toLowerCase();
        if (type.contains("knight")) {
            return 50;
        }
        if (type.contains("archer")) {
            return 30;
        }
        if (type.contains("mage")) {
            return 20;
        }
        return 50;
    }
}