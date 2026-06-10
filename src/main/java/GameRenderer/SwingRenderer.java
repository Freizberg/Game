package GameRenderer;

import Action.Action;
import Action.AttackAction;
import Action.HealAction;
import Action.MoveAction;
import GameController.GameController;
import GameController.GameMouseHandler;
import GameEngine.GameState;
import GameEngine.Player;
import GameView.GameView;
import Map.GameMap;
import Map.Tile;
import Map.TileType;
import Units.Unit;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@link GameRenderer} implementation that draws the game using the Java Swing framework.
 *
 * <p>Extends a Swing component and overrides {@link #paintComponent(Graphics)} to
 * perform all rendering. The game map, units, HUD, and planned actions are each
 * drawn as layers within a single component. {@link #{tileSize} controls the pixel
 * dimensions of each map tile, allowing the view to be scaled.</p>
 *
 * @author Dzhyhar Volodymyr
 * @author Filip Glaser
 * @author Marcin Świerczyński
 * @author Piotr Gryszka
 */
public class SwingRenderer extends JPanel implements GameRenderer {

    private GameController gameController;
    private GameMouseHandler mouseHandler;
    private Player player;
    private Map<UUID, Color> playerColors;

    public static final int TILE_SIZE = 64;

    /** Empty margin (in screen pixels) kept around the centered map. */
    private static final int VIEW_PADDING = 16;

    /** Warm dark background filling the area around the centered map. */
    private static final Color BACKGROUND = new Color(28, 26, 23);

    /** Current map-to-screen scale factor, recomputed on every paint. */
    private double renderScale = 1.0;

    /** Current horizontal offset (px) that centers the map, recomputed on every paint. */
    private int renderOffsetX = 0;

    /** Current vertical offset (px) that centers the map, recomputed on every paint. */
    private int renderOffsetY = 0;

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
        this.setBackground(BACKGROUND);

        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);
        this.addMouseWheelListener(mouseHandler);
    }


    @Override
    public void renderMap(GameMap m) {
        this.repaint();
    }


    @Override
    public void renderUnit(Unit u) {
        this.repaint();
    }


    @Override
    public void renderHUD(List<Player> p, GameState state, UUID winnerId) {
        this.repaint();
    }


    @Override
    public void renderPlannedActions(List<Action> a) {
        this.repaint();
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

        if (view == null) {
            g2d.setColor(Color.WHITE);
            g2d.drawString("Initializing...", 20, 30);
            return;
        }

        updateViewTransform();

        AffineTransform original = g2d.getTransform();
        boolean dataError = false;
        g2d.translate(renderOffsetX, renderOffsetY);
        g2d.scale(renderScale, renderScale);
        try {
            if (view.getTiles() != null) {
                drawMap(g2d, view);
                drawHighlights(g2d, view);
            }

            drawPlannedActions(g2d);

            if (view.getUnits() != null) {
                drawUnits(g2d, view);
            }
        } catch (UnsupportedOperationException e) {
            dataError = true;
        } finally {
            g2d.setTransform(original);
        }

        if (dataError) {
            g2d.setColor(Color.RED);
            g2d.drawString("Waiting for GameView data...", 20, 30);
            return;
        }

        if (view.getState() == GameState.FINISHED || view.getState() == GameState.DRAW) {
            drawEndScreen(g2d, view);
        }
    }

    /**
     * Recomputes the scale and offsets used to draw the map as large as possible while keeping it centered and fully visible within the panel.
     *
     */
    private void updateViewTransform() {
        int mapW = 1;
        int mapH = 1;
        try {
            GameMap map = gameController.getEngine().getMap();
            mapW = Math.max(1, map.getWidth());
            mapH = Math.max(1, map.getHeight());
        } catch (Exception ignored) {
            // Fall back to a 1x1 area if the map is not available yet.
        }

        double logicalW = TILE_SIZE * mapW;
        double logicalH = TILE_SIZE * mapH;
        double availW = Math.max(1, getWidth() - 2 * VIEW_PADDING);
        double availH = Math.max(1, getHeight() - 2 * VIEW_PADDING);

        renderScale = Math.max(0.01, Math.min(availW / logicalW, availH / logicalH));
        renderOffsetX = (int) Math.round((getWidth() - logicalW * renderScale) / 2.0);
        renderOffsetY = (int) Math.round((getHeight() - logicalH * renderScale) / 2.0);
    }

    /**
     * Converts a point in component (screen) coordinates into map tile coordinates, accounting for the current scale and centering offset.
     *
     * @param screenX x coordinate within the panel
     * @param screenY y coordinate within the panel
     * @return the tile column/row under the given point
     */
    public Point screenToTile(int screenX, int screenY) {
        double tile = TILE_SIZE * renderScale;
        if (tile <= 0) {
            return new Point(-1, -1);
        }
        int tx = (int) Math.floor((screenX - renderOffsetX) / tile);
        int ty = (int) Math.floor((screenY - renderOffsetY) / tile);
        return new Point(tx, ty);
    }

    private void drawMap(Graphics2D g2d, GameView view) {
        for (GameView.TileView tile : view.getTiles()) {
            if (tile == null) continue;

            int x = tile.getX() * TILE_SIZE;
            int y = tile.getY() * TILE_SIZE;

            TileType type;
            try {
                type = tile.getType();
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

    private void drawHighlights(Graphics2D g2d, GameView view) {
        UUID selectedId = view.getSelectedUnitId();
        if (selectedId == null) return;

        Player active = gameController.getActivePlayer();
        if (active == null) return;

        Unit selectedUnit = gameController.getEngine().getMap().findUnit(selectedId);
        if (selectedUnit == null || !active.isPlayersUnit(selectedId)) return;

        GameMap map = gameController.getEngine().getMap();

        List<Tile> reachable = map.getReachableTiles(selectedUnit.getPosX(), selectedUnit.getPosY(), selectedUnit.getSpeed());
        g2d.setColor(new Color(30, 144, 255, 100));
        for (Tile t : reachable) {
            g2d.fillRect(t.getX() * TILE_SIZE, t.getY() * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }

        g2d.setColor(new Color(220, 20, 60, 100));
        for (Player p : gameController.getEngine().getPlayers()) {
            if (p.getUuid().equals(active.getUuid())) continue;
            for (Unit enemy : p.getUnits()) {
                if (!enemy.isAlive()) continue;
                int dist = map.getDistance(selectedUnit.getPosX(), selectedUnit.getPosY(), enemy.getPosX(), enemy.getPosY());
                if (dist <= selectedUnit.getAttackRange()) {
                    if (selectedUnit.getAttackRange() <= 1 || map.hasLineOfSight(selectedUnit.getPosX(), selectedUnit.getPosY(), enemy.getPosX(), enemy.getPosY())) {
                        g2d.fillRect(enemy.getPosX() * TILE_SIZE, enemy.getPosY() * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
        }
    }

    private void drawPlannedActions(Graphics2D g2d) {
        List<Action> actions = gameController.getPlannedActionsView();
        GameMap map = gameController.getEngine().getMap();

        Player viewer = gameController.getActivePlayer();
        if (viewer == null) {
            return;
        }

        for (Action a : actions) {
            if (!viewer.isPlayersUnit(a.getUnitId())) continue;

            Unit u = map.findUnit(a.getUnitId());
            if (u == null) continue;

            int startX = u.getPosX() * TILE_SIZE + TILE_SIZE / 2;
            int startY = u.getPosY() * TILE_SIZE + TILE_SIZE / 2;

            if (a instanceof MoveAction move) {
                int endX = move.getDestX() * TILE_SIZE + TILE_SIZE / 2;
                int endY = move.getDestY() * TILE_SIZE + TILE_SIZE / 2;
                g2d.setColor(Color.WHITE);
                drawArrow(g2d, startX, startY, endX, endY);
            } else if (a instanceof AttackAction atk) {
                Unit target = map.findUnit(atk.getTargetId());
                if (target != null) {
                    int endX = target.getPosX() * TILE_SIZE + TILE_SIZE / 2;
                    int endY = target.getPosY() * TILE_SIZE + TILE_SIZE / 2;
                    g2d.setColor(Color.RED);
                    drawArrow(g2d, startX, startY, endX, endY);
                }
            } else if (a instanceof HealAction heal) {
                Unit target = map.findUnit(heal.getTargetId());
                if (target != null) {
                    int endX = target.getPosX() * TILE_SIZE + TILE_SIZE / 2;
                    int endY = target.getPosY() * TILE_SIZE + TILE_SIZE / 2;
                    g2d.setColor(Color.GREEN);
                    drawArrow(g2d, startX, startY, endX, endY);
                }
            }
        }
    }

    private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{10}, 0));
        g2d.drawLine(x1, y1, x2, y2);

        double angle = Math.atan2(y2 - y1, x2 - x1);
        int arrowSize = 10;
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(x2, y2, (int) (x2 - arrowSize * Math.cos(angle - Math.PI / 6)), (int) (y2 - arrowSize * Math.sin(angle - Math.PI / 6)));
        g2d.drawLine(x2, y2, (int) (x2 - arrowSize * Math.cos(angle + Math.PI / 6)), (int) (y2 - arrowSize * Math.sin(angle + Math.PI / 6)));
    }

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

            if (unit.isSelected()) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(4));
                g2d.drawOval(circleX - 4, circleY - 4, circleSize + 8, circleSize + 8);
            }

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

            int maxHp = unit.getMaxHp();
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

    private void drawEndScreen(Graphics2D g2d, GameView view) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        boolean finished = view.getState() == GameState.FINISHED;
        String msg = finished ? "ZWYCIĘSTWO!" : "REMIS";
        String sub = finished ? winnerName(view) : "Wzajemne wyniszczenie – nikt nie przetrwał.";

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 52));
        FontMetrics fm = g2d.getFontMetrics();
        int msgX = (getWidth() - fm.stringWidth(msg)) / 2;
        int msgY = getHeight() / 2;
        g2d.drawString(msg, msgX, msgY);

        if (sub != null && !sub.isBlank()) {
            g2d.setColor(new Color(217, 160, 74));
            g2d.setFont(new Font("Segoe UI", Font.PLAIN, 24));
            FontMetrics fm2 = g2d.getFontMetrics();
            int subX = (getWidth() - fm2.stringWidth(sub)) / 2;
            g2d.drawString(sub, subX, msgY + 48);
        }
    }

    /**
     * Resolves the display text, naming the winning player from the view.
     *
     * @param view current game view snapshot
     * @return text like {@code "Wygrywa: Gracz 1"}, or {@code null} if unknown
     */
    private String winnerName(GameView view) {
        UUID winnerId = view.getWinnerId();
        if (winnerId == null || view.getPlayers() == null) {
            return null;
        }
        for (GameView.PlayerView p : view.getPlayers()) {
            if (p != null && winnerId.equals(p.getPlayerId())) {
                return "Wygrywa: " + p.getName();
            }
        }
        return null;
    }

    /** Colour used for the units of the player currently viewing the board. */
    public static final Color OWN_COLOR = new Color(30, 144, 255); // niebieski

    /** Colours assigned to opponents, in their order among the other players. */
    public static final Color[] ENEMY_PALETTE = {
            new Color(220, 20, 60),
            new Color(255, 140, 0),
            new Color(138, 43, 226),
            new Color(60, 179, 113)
    };

    /**
     * Picks a colour for a unit's owner from the perspective of a viewing player.
     * The viewer's own units are always drawn in OWN_COLOR so they are easy to recognise.
     *
     * @param orderedPlayerIds player ids in turn order
     * @param viewerId id of the player viewing the board, or {@code null}
     * @param ownerId id of the unit's owner
     * @return the colour to draw the owner's units in
     */
    public static Color colorFor(List<UUID> orderedPlayerIds, UUID viewerId, UUID ownerId) {
        if (ownerId == null) {
            return Color.GRAY;
        }
        if (viewerId != null && ownerId.equals(viewerId)) {
            return OWN_COLOR;
        }
        int enemyIndex = 0;
        for (UUID id : orderedPlayerIds) {
            if (viewerId != null && id.equals(viewerId)) {
                continue;
            }
            if (id.equals(ownerId)) {
                return ENEMY_PALETTE[enemyIndex % ENEMY_PALETTE.length];
            }
            enemyIndex++;
        }
        return Color.GRAY;
    }

    /**
     * Returns the colour for the given player's units from the local viewer's perspective (the player whose turn it currently is on this screen).
     *
     * @param ownerId id of the owner
     * @return the player's colour, or grey if the owner is unknown
     */
    private Color getPlayerColor(UUID ownerId) {
        if (ownerId == null) {
            return Color.GRAY;
        }

        Player viewer = gameController.getActivePlayer();
        UUID viewerId = (viewer != null) ? viewer.getUuid() : null;

        List<UUID> ids = new ArrayList<>();
        List<Player> players = gameController.getEngine().getPlayers();
        if (players != null) {
            for (Player p : players) {
                ids.add(p.getUuid());
            }
        }
        return colorFor(ids, viewerId, ownerId);
    }
}