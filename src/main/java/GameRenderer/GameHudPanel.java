package GameRenderer;

import GameController.GameController;
import GameEngine.Player;
import GameView.GameView;

import javax.swing.*;
import java.awt.*;

public class GameHudPanel {
    GameController gameController;
    Player player;

    public GameHudPanel(GameController gameController, Player player){
        this.player = player;
        this.gameController = gameController;
    }

    public static JPanel createHudPanel(GameController gameController, Player player,JFrame frame){
        GameHudPanel gameHudPanel = new GameHudPanel(gameController, player);
        JPanel hudPanel = new JPanel();
        hudPanel.setLayout(new BoxLayout(hudPanel, BoxLayout.Y_AXIS));
        hudPanel.setBackground(Color.DARK_GRAY);

        JLabel round = new JLabel("Runda " + 0); // + gameHudPanel.gameController.getGameView().getCurrentRound() - Wstawić to zamiast 0
        round.setForeground(Color.WHITE);
        round.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel unitInfo = new JPanel();
        unitInfo.setBackground(Color.DARK_GRAY);
        unitInfo.setLayout(new BoxLayout(unitInfo, BoxLayout.Y_AXIS));

        JLabel ownerName = new JLabel();
        ownerName.setForeground(Color.WHITE);
        ownerName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel unitHP = new JLabel();
        unitHP.setForeground(Color.WHITE);
        unitHP.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel unitName = new JLabel();
        unitName.setForeground(Color.WHITE);
        unitName.setAlignmentX(Component.CENTER_ALIGNMENT);

        Timer timer = new Timer(100, e -> {
            GameView.UnitView sel = getSelectedUnit(gameController);
            if (sel != null) {
                Player owner = gameController.getEngine().getPlayer(sel.getOwnerId());
                ownerName.setText("Owner: " + (owner != null ? owner.getName() : "?"));
                unitHP.setText("HP: " + sel.getHp() + "/" + sel.getMaxHp());
                unitName.setText(sel.getClass().getSimpleName() + sel.getName());
            } else {
                ownerName.setText("?");
            }
        });
        timer.start();




        frame.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                hudPanel.setPreferredSize(new Dimension(frame.getWidth() / 6, 0));
                hudPanel.setBorder(BorderFactory.createEmptyBorder(frame.getHeight() / 15, 10, 10, 10));
                round.setFont(round.getFont().deriveFont((float) frame.getHeight()/ 30));

                unitInfo.setPreferredSize(new Dimension(frame.getWidth()/6, frame.getHeight()*2/3));

                ownerName.setFont(ownerName.getFont().deriveFont((float) frame.getHeight()/60));
                unitName.setFont(ownerName.getFont().deriveFont((float) frame.getHeight()/60));
                unitHP.setFont(ownerName.getFont().deriveFont((float) frame.getHeight()/60));

                hudPanel.revalidate();
            }
        });



        hudPanel.add(round);
        unitInfo.add(ownerName);
        unitInfo.add(unitName);
        unitInfo.add(unitHP);
        hudPanel.add(Box.createVerticalGlue());
        hudPanel.add(unitInfo);


        return hudPanel;
    }

    private static GameView.UnitView getSelectedUnit(GameController gc) {
        try {
            GameView view = gc.getGameView();
            if (view == null || view.getUnits() == null) return null;
            for (GameView.UnitView u : view.getUnits()) {
                if (u != null && u.isSelected()) return u;
            }
        } catch (Exception ex) {
            return null;
        }
        return null;
    }


    public static void main(String[] args) {
        JFrame frame = new JFrame("gra");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel hud = createHudPanel(null, null, frame);

        frame.add(hud, BorderLayout.EAST);

        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.setVisible(true);
    }
}
