import Action.Action;
import GameController.GameController;
import GameController.GameSaveManager;
import GameController.InputHandler;
import GameEngine.GameEngine;
import GameEngine.GameState;
import GameEngine.Player;
import GameRenderer.GameHudPanel;
import GameRenderer.GameRenderer;
import GameRenderer.SwingRenderer;
import Map.GameMap;
import Map.MapConfig;
import NetworkManager.ClientNetworkManager;
import NetworkManager.NetworkManager;
import NetworkManager.ServerNetworkManager;
import Units.Archer;
import Units.Knight;
import Units.Mage;
import Units.Unit;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Główny punkt wejścia dla graficznej wersji gry.
 * Inicjuje Menu Główne, z którego można uruchomić grę lokalną
 * lub sieciową (jako Host lub Klient).
 *
 * @author Marcin Świerczyński
 */
public class GuiMain {

    /** Tło głównego okna i menu. */
    private static final Color BG        = new Color(28, 26, 23);
    /** Tło karty menu. */
    private static final Color PANEL     = new Color(40, 36, 30);
    /** Kolor akcentu – bursztyn/złoto. */
    private static final Color ACCENT    = new Color(217, 160, 74);
    /** Podstawowy kolor tekstu. */
    private static final Color TEXT      = new Color(232, 224, 211);
    /** Stonowany kolor tekstu pomocniczego. */
    private static final Color SUBTLE    = new Color(150, 142, 126);
    /** Tło przycisków menu. */
    private static final Color BTN       = new Color(58, 51, 42);
    /** Tło przycisku menu pod kursorem. */
    private static final Color BTN_HOVER = new Color(74, 66, 52);

    private static JFrame frame;

    public static void main(String[] args) {
        // Konfiguracja głównego okna aplikacji
        frame = new JFrame("Strategiczna Gra Turowa");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(900, 640);
        frame.setMinimumSize(new Dimension(640, 480));
        frame.setLocationRelativeTo(null); // Wyśrodkowanie na ekranie
        frame.getContentPane().setBackground(BG);

        showMainMenu();

        frame.setVisible(true);
    }

    /**
     * Buduje i wyświetla Menu Główne z przyciskami trybów gry.
     */
    private static void showMainMenu() {
        frame.getContentPane().removeAll();

        // Tło wypełniające całe okno, na środku wyśrodkowana karta menu.
        JPanel menuPanel = new JPanel(new GridBagLayout());
        menuPanel.setBackground(BG);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1, true),
                BorderFactory.createEmptyBorder(40, 56, 40, 56)));

        JLabel title = new JLabel("STRATEGICZNA GRA TUROWA");
        title.setFont(new Font("Segoe UI", Font.BOLD, 34));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);

        card.add(Box.createRigidArea(new Dimension(0, 12)));

        // Cienki akcentowy separator pod tytułem.
        JPanel rule = new JPanel();
        rule.setBackground(ACCENT);
        rule.setMaximumSize(new Dimension(120, 3));
        rule.setPreferredSize(new Dimension(120, 3));
        rule.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(rule);

        card.add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel subtitle = new JLabel("Turowa gra strategiczna • lokalnie lub przez sieć");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitle.setForeground(SUBTLE);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);

        card.add(Box.createRigidArea(new Dimension(0, 36)));

        JButton btnLocal = createMenuButton("Graj lokalnie");
        btnLocal.addActionListener(e -> startGame("LOCAL", null, 0));
        card.add(btnLocal);

        card.add(Box.createRigidArea(new Dimension(0, 14)));

        JButton btnLoad = createMenuButton("Wczytaj grę");
        btnLoad.addActionListener(e -> {
            GameEngine loaded = loadEngineFromChooser();
            if (loaded != null) {
                launchGame(loaded, "LOCAL", null, 0);
            }
        });
        card.add(btnLoad);

        card.add(Box.createRigidArea(new Dimension(0, 14)));

        JButton btnHost = createMenuButton("Załóż serwer (Host)");
        btnHost.addActionListener(e -> {
            Integer port = askPort();
            if (port != null) {
                startGame("HOST", null, port);
            }
        });
        card.add(btnHost);

        card.add(Box.createRigidArea(new Dimension(0, 14)));

        JButton btnHostSaved = createMenuButton("Hostuj zapisaną grę");
        btnHostSaved.addActionListener(e -> {
            GameEngine loaded = loadEngineFromChooser();
            if (loaded == null) {
                return;
            }
            Integer port = askPort();
            if (port != null) {
                launchGame(loaded, "HOST", null, port);
            }
        });
        card.add(btnHostSaved);

        card.add(Box.createRigidArea(new Dimension(0, 14)));

        JButton btnJoin = createMenuButton("Dołącz do gry (Join)");
        btnJoin.addActionListener(e -> {
            String ip = JOptionPane.showInputDialog(frame, "Podaj adres IP serwera:", "127.0.0.1");
            if (ip != null && !ip.trim().isEmpty()) {
                String portStr = JOptionPane.showInputDialog(frame, "Podaj port serwera:", "9999");
                if (portStr != null && !portStr.trim().isEmpty()) {
                    try {
                        int port = Integer.parseInt(portStr.trim());
                        startGame("JOIN", ip, port);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(frame, "Nieprawidłowy port!", "Błąd", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        card.add(btnJoin);

        menuPanel.add(card);

        frame.setContentPane(menuPanel);
        frame.revalidate();
        frame.repaint();
    }

    /**
     * Buduje ostylowany przycisk menu z efektem najechania kursorem.
     *
     * @param text etykieta przycisku
     * @return ostylowany przycisk o stałej szerokości
     */
    private static JButton createMenuButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 17));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setPreferredSize(new Dimension(320, 50));
        btn.setMaximumSize(new Dimension(320, 50));
        btn.setFocusPainted(false);
        btn.setBackground(BTN);
        btn.setForeground(TEXT);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(BTN_HOVER);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(BTN);
            }
        });
        return btn;
    }

    /**
     * Prompts for a server port.
     *
     * @return the parsed port, or {@code null} if the user cancelled or typed an invalid value
     */
    private static Integer askPort() {
        String portStr = JOptionPane.showInputDialog(frame, "Podaj port dla serwera:", "9999");
        if (portStr == null || portStr.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(portStr.trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Nieprawidłowy port!", "Błąd", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    /**
     * Shows a file chooser and loads a saved {@link GameEngine} from the picked file.
     *
     * @return the loaded engine, or {@code null} if cancelled or loading failed
     */
    private static GameEngine loadEngineFromChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Wczytaj zapis gry");
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File file = chooser.getSelectedFile();
        try {
            GameEngine engine = new GameSaveManager(file.getPath()).loadGame(file.getPath());
            if (engine == null) {
                JOptionPane.showMessageDialog(frame, "Nie udało się wczytać gry.", "Błąd", JOptionPane.ERROR_MESSAGE);
            }
            return engine;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Błąd wczytywania: " + ex.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    /**
     * Builds a fresh default match (map, two players and their starting units).
     *
     * @return a ready-to-play engine for a brand-new game
     */
    private static GameEngine buildDefaultEngine() {
        GameEngine engine = new GameEngine();
        GameMap map = MapConfig.loadMap("/maps/level1.txt");
        engine.setMap(map);

        Player player1 = new Player("Gracz 1");
        Player player2 = new Player("Gracz 2");

        Knight p1Knight = new Knight("Artur", 0, 0);
        Archer p1Archer = new Archer("Robin", 0, 1);
        Mage p2Mage = new Mage("Merlin", 9, 9);
        Knight p2Knight = new Knight("Lancelot", 9, 8);

        player1.setUnits(new ArrayList<>(List.of(p1Knight, p1Archer)));
        player2.setUnits(new ArrayList<>(List.of(p2Mage, p2Knight)));

        engine.setPlayers(new ArrayList<>(List.of(player1, player2)));

        map.placeUnit(p1Knight, 0, 0);
        map.placeUnit(p1Archer, 0, 1);
        map.placeUnit(p2Mage, 9, 9);
        map.placeUnit(p2Knight, 9, 8);

        return engine;
    }

    /**
     * Starts a brand-new match in the given mode.
     */
    private static void startGame(String mode, String ip, int port) {
        launchGame(buildDefaultEngine(), mode, ip, port);
    }

    /**
     * Wires the renderer, networking and controller around the given engine
     * (either freshly built or loaded from disk) and switches to the board view.
     *
     * @param engine the engine to play (its players/units are used as-is)
     * @param mode {@code "LOCAL"}, {@code "HOST"} or {@code "JOIN"}
     * @param ip host address for {@code "JOIN"}, otherwise {@code null}
     * @param port network port for {@code "HOST"}/{@code "JOIN"}
     */
    private static void launchGame(GameEngine engine, String mode, String ip, int port) {
        final GameRenderer[] rendererProxy = new GameRenderer[1];
        GameRenderer dynamicRenderer = new GameRenderer() {
            @Override
            public void renderMap(GameMap m) {
                if (rendererProxy[0] != null) rendererProxy[0].renderMap(m);
            }
            @Override
            public void renderUnit(Unit u) {
                if (rendererProxy[0] != null) rendererProxy[0].renderUnit(u);
            }
            @Override
            public void renderHUD(List<Player> players, GameState state, UUID winnerId) {
                if (rendererProxy[0] != null) rendererProxy[0].renderHUD(players, state, winnerId);
            }
            @Override
            public void renderPlannedActions(List<Action> actions) {
                if (rendererProxy[0] != null) rendererProxy[0].renderPlannedActions(actions);
            }
        };

        NetworkManager network = null;
        try {
            if (mode.equals("JOIN")) {
                network = new ClientNetworkManager(ip, port);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Nie udało się połączyć z serwerem: " + ex.getMessage(), "Błąd Połączenia", JOptionPane.ERROR_MESSAGE);
            return;
        }

        GameController controller = new GameController(
                engine,
                dynamicRenderer,
                network,
                new GameSaveManager("save.dat"),
                new InputHandler()
        );

        if (mode.equals("HOST")) {
            try {
                ServerNetworkManager serverNet = new ServerNetworkManager(port, controller);
                java.lang.reflect.Field netField = GameController.class.getDeclaredField("network");
                netField.setAccessible(true);
                netField.set(controller, serverNet);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Błąd inicjalizacji serwera: " + ex.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        frame.getContentPane().removeAll();
        frame.setLayout(new BorderLayout());

        List<Player> players = engine.getPlayers();
        Player localPlayer = (mode.equals("JOIN") && players.size() > 1)
                ? players.get(1)
                : players.get(0);
        controller.configureLocalPlayer(localPlayer, mode.equals("LOCAL"));

        SwingRenderer swingRenderer = new SwingRenderer(controller, localPlayer);
        rendererProxy[0] = swingRenderer;

        JPanel hudPanel = GameHudPanel.createHudPanel(controller, localPlayer);

        frame.add(swingRenderer, BorderLayout.CENTER);
        frame.add(hudPanel, BorderLayout.EAST);

        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.revalidate();
        frame.repaint();

        try {
            if (mode.equals("LOCAL")) {
                controller.startLocalGame();
            } else {
                controller.startNetworkGame();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Błąd uruchamiania gry: " + ex.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
            showMainMenu();
        }
    }
}