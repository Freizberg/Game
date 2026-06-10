package GameController;

import GameEngine.GameEngine;
import GameEngine.Player;
import Action.MoveAction;
import GameRenderer.SwingRenderer;
import Map.GameMap;
import Units.Unit;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.UUID;

/**
 * Class used to read mouse inputs inside the game JPanels
 *
 * @author Piotr Gryszka
 * @author Marcin Świerczyński
 */
public class GameMouseHandler implements MouseListener, MouseMotionListener, MouseWheelListener {

    private SwingRenderer gamePanel;
    private GameController gameController;
    private GameMap gameMap;
    private Player player;
    private int tileSize;
    private Point location;
    private UUID selectedUnitID;

    /**
     * Constructs a mousehandler for a JPanel of one instance of a game.
     * * @param gamePanel game instance's SwingRenderer which draws frames.
     * @param player the player to whom actions are attributed.
     * @param tileSize needed for checking which tile the mouse is hovered over
     */
    public GameMouseHandler(SwingRenderer gamePanel, Player player, int tileSize) {
        this.gamePanel = gamePanel;
        this.gameController = gamePanel.getGameController();
        this.gameMap = gameController.getEngine().getMap();
        this.player = player;
        this.location = new Point();
        this.tileSize = tileSize;
    }

    /**
     * Changes selectedUnitID to the id of the unit on the tile of the current mouse location;
     *
     * @return returns true if the selection changed
     */
    public boolean selectUnit() {
        if (!gameMap.isInBounds(location.x, location.y)) {
            return false;
        }

        Unit unit = gameMap.getTile(location.x, location.y).getUnit();
        if (unit != null) {
            UUID uuid = unit.getId();
            if (!uuid.equals(selectedUnitID)) {
                selectedUnitID = uuid;
                gameController.setSelectedUnitId(selectedUnitID);
                refreshUnitInfoDisplay();
                return true;
            }
        }
        return false;
    }

    /**
     * Repaints the game panel so selection-dependent visuals (such as the
     * selected unit's outline and reachable-tile highlights) update immediately
     * after a click, even when no game action was dispatched.
     */
    public void refreshUnitInfoDisplay() {
        gamePanel.repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        gameMap = gameController.getEngine().getMap();

        Point tile = gamePanel.screenToTile(e.getX(), e.getY());
        int clickX = tile.x;
        int clickY = tile.y;

        if (!gameMap.isInBounds(clickX, clickY)) {
            return;
        }

        location.setLocation(clickX, clickY);

        Player active = gameController.getActivePlayer();
        if (active == null) {
            return;
        }
        selectedUnitID = gameController.getSelectedUnitId();
        if (selectedUnitID != null && !active.isPlayersUnit(selectedUnitID)) {
            selectedUnitID = null;
            gameController.setSelectedUnitId(null);
        }

        if (e.getButton() == MouseEvent.BUTTON1) {
            Unit clickedUnit = gameMap.getTile(clickX, clickY).getUnit();
            InputMode mode = gameController.getInputMode();

            switch (mode) {
                case MOVE -> {
                    if (selectedUnitID != null) {
                        gameController.requestMoveToTile(selectedUnitID, clickX, clickY);
                    }
                    gameController.setInputMode(InputMode.NONE);
                }
                case ATTACK -> {
                    if (selectedUnitID != null && clickedUnit != null
                            && !active.isPlayersUnit(clickedUnit.getId())) {
                        gameController.requestAttackUnit(selectedUnitID, clickedUnit.getId());
                    }
                    gameController.setInputMode(InputMode.NONE);
                }
                case HEAL -> {
                    if (selectedUnitID != null && clickedUnit != null
                            && active.isPlayersUnit(clickedUnit.getId())) {
                        gameController.requestHealUnit(selectedUnitID, clickedUnit.getId());
                    }
                    gameController.setInputMode(InputMode.NONE);
                }
                default -> {
                    if (clickedUnit != null && active.isPlayersUnit(clickedUnit.getId())) {
                        selectUnit();
                    } else if (selectedUnitID != null) {
                        gameController.requestMoveToTile(selectedUnitID, clickX, clickY);
                    }
                }
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            if (selectedUnitID != null && active.isPlayersUnit(selectedUnitID)) {
                Unit targetUnit = gameMap.getTile(clickX, clickY).getUnit();
                if (targetUnit != null && !active.isPlayersUnit(targetUnit.getId())) {
                    gameController.requestAttackUnit(selectedUnitID, targetUnit.getId());
                }
            }
        } else if (e.getButton() == MouseEvent.BUTTON2) {
            selectedUnitID = null;
            gameController.setSelectedUnitId(null);
            gameController.setInputMode(InputMode.NONE);
        }

        gamePanel.repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point tile = gamePanel.screenToTile(e.getX(), e.getY());
        location.setLocation(tile.x, tile.y);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Point tile = gamePanel.screenToTile(e.getX(), e.getY());
        location.setLocation(tile.x, tile.y);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}