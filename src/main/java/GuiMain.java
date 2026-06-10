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
 * @author Filip Glaser
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
                        // Klient nie wybiera liczby graczy ani jednostek — pełny stan
                        // (z armiami ustalonymi przez hosta) przyjdzie od serwera.
                        // Budujemy tylko tymczasowy 2-osobowy silnik jako placeholder.
                        List<String[]> placeholderChoices = List.of(
                                new String[]{"Rycerz", "Łucznik"},
                                new String[]{"Rycerz", "Łucznik"}
                        );
                        launchGame(buildDefaultEngine(placeholderChoices), "JOIN", ip, port);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(frame, "Nieprawidłowy port!", "Błąd", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        card.add(btnJoin);

        card.add(Box.createRigidArea(new Dimension(0, 14)));
        JButton btnRules = createMenuButton("Zasady Gry");
        btnRules.addActionListener(e -> showRulesDialog());
        card.add(btnRules);

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
     * Prompts for the number of players in a brand-new match (2–4).
     *
     * @return the chosen player count, or {@code null} if the user cancelled
     */
    private static Integer askPlayerCount() {
        Integer[] opts = {2, 3, 4};
        return (Integer) JOptionPane.showInputDialog(
                frame, "Na ilu graczy?", "Nowa gra",
                JOptionPane.QUESTION_MESSAGE, null, opts, 2);
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
     * Buduje i w pełni konfiguruje instancję silnika gry. Przed alokacją wybranych
     * jednostek czyści planszę z domyślnych obiektów zainicjalizowanych przez parser mapy,
     * eliminując błąd nadpisywania struktur danych.
     *
     * @param allChoices lista zawierająca tablice z wybranymi typami jednostek dla obu graczy
     * @return w pełni zainicjalizowany obiekt GameEngine z poprawnym układem armii na mapie
     */
    private static GameEngine buildDefaultEngine(List<String[]> allChoices) {
        GameEngine engine = new GameEngine();
        GameMap map = MapConfig.loadMap("/maps/level1.txt");
        engine.setMap(map);

        // CZYSZCZENIE MAPY: Usuwamy automatycznie wygenerowane postacie z pliku level1.txt
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                if (map.getTile(x, y) != null) {
                    map.getTile(x, y).setUnit(null);
                }
            }
        }

        // Pozycje startowe
        int[][][] spawns = {
                { {0, 0}, {0, 1} },   // Gracz 1
                { {9, 9}, {9, 8} },   // Gracz 2
                { {0, 9}, {0, 8} },   // Gracz 3
                { {9, 0}, {9, 1} },   // Gracz 4
        };

        List<Player> players = new ArrayList<>();
        for (int i = 0; i < allChoices.size(); i++) {
            Player p = new Player("Gracz " + (i + 1));
            String[] choices = allChoices.get(i);

            int x1 = spawns[i][0][0], y1 = spawns[i][0][1];
            int x2 = spawns[i][1][0], y2 = spawns[i][1][1];

            Unit u1 = createUnit(choices[0], "Bohater " + (i + 1) + "_1", x1, y1);
            Unit u2 = createUnit(choices[1], "Bohater " + (i + 1) + "_2", x2, y2);

            p.setUnits(new ArrayList<>(List.of(u1, u2)));
            players.add(p);

            map.placeUnit(u1, x1, y1);
            map.placeUnit(u2, x2, y2);
        }
        engine.setPlayers(players);

        return engine;
    }

    /**
     * Mapuje tekstowy wybór z poziomu komponentu JComboBox na konkretną klasę
     * domenową reprezentującą typ postaci w grze.
     *
     * @param type nazwa typu jednostki pobrana z interfejsu ("Mag", "Łucznik", "Rycerz")
     * @param name unikalne imię przypisywane tworzonej jednostce
     * @param x współrzędna startowa X
     * @param y współrzędna startowa Y
     * @return instancja klasy pochodnej typu {@link Unit}
     */
    private static Unit createUnit(String type, String name, int x, int y) {
        if ("Mag".equals(type)) return new Mage(name, x, y);
        if ("Łucznik".equals(type)) return new Archer(name, x, y);
        return new Knight(name, x, y);
    }

    /**
     * Inicjuje proces uruchomienia rozgrywki. Przeprowadza sekwencyjny wybór
     * armii startowej najpierw dla Gracza 1, a następnie dla Gracza 2 za pomocą
     * dedykowanych okien dialogowych.
     *
     * @param mode tryb uruchomienia gry ("LOCAL", "HOST", "JOIN")
     * @param ip adres IP serwera (używany wyłącznie przy dołączaniu jako klient)
     * @param port port nasłuchu dla modułu sieciowego
     */
    private static void startGame(String mode, String ip, int port) {
        Integer playerCount = askPlayerCount();
        if (playerCount == null) {
            return;
        }

        List<String[]> allChoices = new ArrayList<>();
        String[] options = {"Rycerz", "Łucznik", "Mag"};

        for (int i = 0; i < playerCount; i++) {
            JComboBox<String> unit1 = new JComboBox<>(options);
            JComboBox<String> unit2 = new JComboBox<>(options);
            unit2.setSelectedIndex(1); // Domyślnie druga jednostka to Łucznik

            JPanel panel = new JPanel(new GridLayout(0, 1));
            panel.add(new JLabel("GRACZ " + (i + 1) + " - Wybierz pierwszą jednostkę:"));
            panel.add(unit1);
            panel.add(new JLabel("GRACZ " + (i + 1) + " - Wybierz drugą jednostkę:"));
            panel.add(unit2);

            int result = JOptionPane.showConfirmDialog(frame, panel,
                    "Skonfiguruj armię - Gracz " + (i + 1),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return;
            }
            allChoices.add(new String[]{(String) unit1.getSelectedItem(), (String) unit2.getSelectedItem()});
        }

        launchGame(buildDefaultEngine(allChoices), mode, ip, port);
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
    /**
     * Wyświetla graficzne okno dialogowe zawierające skrócone zasady gry
     * i instrukcje dla początkujących graczy.
     */
    private static void showRulesDialog() {
        String rules = "ZASADY GRY:\n\n" +
                "1. Faza Planowania: Wszyscy gracze w tajemnicy planują swoje akcje (ruch, atak, czekanie).\n" +
                "2. Punkty Akcji: Każda jednostka ma ograniczoną liczbę akcji na rundę.\n" +
                "3. Faza Rozwiązywania: Po kliknięciu 'Zakończ turę' przez wszystkich, gra wykonuje akcje.\n" +
                "4. Teren: \n" +
                "   - Las redukuje otrzymywane obrażenia.\n" +
                "   - Woda i Góry blokują poruszanie się.\n" +
                "5. Zwycięstwo: Wygrywa ten, kto wyeliminuje wszystkie jednostki przeciwnika.";

        JOptionPane.showMessageDialog(frame, rules, "Zasady Gry", JOptionPane.INFORMATION_MESSAGE);
    }
}