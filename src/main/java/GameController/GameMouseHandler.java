package GameController;

import GameEngine.GameEngine;
import GameEngine.Player;
import Action.MoveAction;
import GameRenderer.SwingRenderer;
import Map.GameMap;
import Units.Unit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.UUID;

/**
 * Class used to read mouse inputs inside the game JPanels
 *
 * @author Piotr Gryszka
 */

public class GameMouseHandler implements MouseListener, MouseMotionListener, MouseWheelListener {

    private SwingRenderer gamePanel;
    private GameController gameController;
    private GameMap gameMap;
    private Player player;
    int tileSizeInverse;
    private Point location;
    private UUID selectedUnitID;

    /** Constructs a mousehandler for a JPanel of one instance of a game.
     * 
     * @param gamePanel game instance's SwingRenderer which draws frames.
     * @param player the player to whom actions are attributed.
     * @param tileSize needed for checking which tile the mouse is hovered over
     */
    public GameMouseHandler(SwingRenderer gamePanel, Player player, int tileSize) {
        this.gamePanel = gamePanel;
        this.gameController = gamePanel.getGameController();
        gameMap = gameController.getEngine().getMap();
        this.player = player;
        location = new Point();
        this.tileSizeInverse = 1/tileSize;
    }

    /** Changes selectedUnitID to the id of the unit on the tile of the current mouse location;
     * 
     * @return returns true if the selection changed
     */
    public boolean selectUnit() {
        UUID uuid = gameMap.getTile(location.x, location.y).getUnit().getId();
        if (uuid!=selectedUnitID) {
            selectedUnitID = uuid;
            gameController.setSelectedUnitId(selectedUnitID);
            refreshUnitInfoDisplay();
            return true;
        }
        return false;
    }

    // will be needed for changing the displayed selected unit information
    public void refreshUnitInfoDisplay() {}

    @Override
    public void mouseClicked(MouseEvent e) {
        switch (e.getButton()) {
            case 1: // LMB
                if (selectedUnitID!=null) {
                    if (player.isPlayersUnit(selectedUnitID)) {
                        if (gameMap.getTile(location.x, location.y).getUnit()!=null) {
                            selectUnit();
                        } else {
                            gameController.requestMoveToTile(selectedUnitID,location.x,location.y);
                        }
                    } else {
                        if (gameMap.getTile(location.x, location.y).getUnit()!=null) {
                            selectUnit();
                        } else {
                            selectedUnitID = null;
                            gameController.setSelectedUnitId(selectedUnitID);
                            refreshUnitInfoDisplay();
                        }
                    }
                } else {
                    selectUnit();
                }
                break;
            case 3: // RMB
                if (selectedUnitID!=null && player.isPlayersUnit(selectedUnitID)) {
                    Unit unit = gameMap.getTile(location.x,location.y).getUnit();
                    if (unit.getId()!=null && !player.isPlayersUnit(unit.getId())) {
                        gameController.requestAttackUnit(selectedUnitID,unit.getId());
                    }
                }
                break;
            case 2: // Wheel Press, has no use atm
                selectedUnitID=null;
                gameController.setSelectedUnitId(selectedUnitID);
                refreshUnitInfoDisplay();
                break;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point p = gamePanel.getMousePosition();
        location.setLocation(p.x*tileSizeInverse,p.y*tileSizeInverse);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Point p = gamePanel.getMousePosition();
        location.setLocation(p.x*tileSizeInverse,p.y*tileSizeInverse);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // maybe it will be useful for ui
    }



    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
}
