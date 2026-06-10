package GameRenderer;

import GameController.GameController;
import GameController.InputMode;
import GameEngine.GameState;
import GameEngine.Player;
import GameView.GameView;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Side panel (HUD) shown to the right of the game map.
 *
 * <p>Displays the current round and a details card for the unit the local
 * player has selected. For the player's own units the card also lists their
 * planned actions and exposes the per-unit action buttons (move, attack,
 * heal for mages, wait). The player-level "end turn" button is at the
 * bottom of the panel, outside the unit card.</p>
 */
public class GameHudPanel {
    /** Panel background colour. */
    private static final Color BG       = new Color(28, 26, 23);
    /** Background colour of a unit/info card. */
    private static final Color CARD     = new Color(40, 36, 30);
    /** Accent colour used for card borders, titles and section header. */
    private static final Color ACCENT   = new Color(217, 160, 74);
    /** Primary text colour. */
    private static final Color TEXT     = new Color(232, 224, 211);
    /** Muted text colour for secondary/empty-state labels. */
    private static final Color SUBTLE   = new Color(150, 142, 126);
    /** Background colour of regular action buttons. */
    private static final Color BTN      = new Color(58, 51, 42);
    /** Background colour of the "end turn" button. */
    private static final Color BTN_END  = new Color(176, 84, 68);

    /** Font for the round title at the top of the panel. */
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    /** Font for card titles and section headers. */
    private static final Font FONT_CARD  = new Font("Segoe UI", Font.BOLD, 14);
    /** Font for stat rows and planned-action labels. */
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 14);
    /** Font for action buttons. */
    private static final Font FONT_BTN   = new Font("Segoe UI", Font.BOLD, 13);

    /** Fixed pixel width of the side panel. */
    private static final int PANEL_WIDTH = 300;

    /** Controller used to read game state and dispatch player actions. */
    GameController gameController;

    /** The local player this panel belongs to. */
    Player player;

    /**
     * Creates a HUD panel bound to the given controller and local player.
     *
     * @param gameController controller used to read state and dispatch actions
     * @param player the local player viewing this panel
     */
    public GameHudPanel(GameController gameController, Player player) {
        this.player = player;
        this.gameController = gameController;
    }

    /**
     * Builds the side panel component.
     *
     * @param gameController controller used to read state and dispatch actions
     * @param player the local player viewing this panel, or {@code null}
     * @return the assembled, self-updating HUD panel
     */
    public static JPanel createHudPanel(GameController gameController, Player player) {
        JPanel hud = new JPanel();
        hud.setLayout(new BoxLayout(hud, BoxLayout.Y_AXIS));
        hud.setBackground(BG);
        hud.setPreferredSize(new Dimension(PANEL_WIDTH, 0));
        hud.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, ACCENT),
                BorderFactory.createEmptyBorder(20, 16, 16, 16)));

        JLabel round = new JLabel("Runda 0");
        round.setForeground(TEXT);
        round.setFont(FONT_TITLE);
        round.setAlignmentX(Component.LEFT_ALIGNMENT);

        //Informacja, kto aktualnie planuje(gra lokalna).
        JLabel turnInfo = new JLabel(" ");
        turnInfo.setForeground(ACCENT);
        turnInfo.setFont(FONT_CARD);
        turnInfo.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Panel z listą graczy.
        JPanel playersBox = new JPanel();
        playersBox.setLayout(new BoxLayout(playersBox, BoxLayout.Y_AXIS));
        playersBox.setBackground(BG);
        playersBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Panel na szczegóły zaznaczonej jednostki.
        JPanel detail = new JPanel();
        detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
        detail.setBackground(BG);
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Przycisk Zapisz grę.
        JButton save = styledButton("Zapisz grę", BTN);
        save.setVisible(!gameController.isNetworkClient());
        save.addActionListener(e -> doSave(gameController, save));

        // Przycisk Koniec tury.
        JButton endTurn = styledButton("Koniec tury", BTN_END);
        endTurn.addActionListener(e -> {
            // Twarda prewalidacja planu tury na podstawie nowej metody z kontrolera
            if (!gameController.areAllUnitsConfiguredForTurn()) {
                JOptionPane.showMessageDialog(
                        null,
                        "Nie możesz zakończyć tury! Musisz wybrać przynajmniej jedną akcję (ruch, atak, leczenie lub czekanie) dla KAŻDEJ swojej jednostki.",
                        "Niekompletne rozkazy",
                        JOptionPane.WARNING_MESSAGE
                );
                return; // Przerywamy akcję przycisku, kontroler nie dostanie żądania zakończenia tury
            }

            // NAPRAWA HOT-SEAT: Dynamiczne pobieranie aktualnie planującego gracza
            Player activePlayer = gameController.getActivePlayer();
            if (activePlayer != null) {
                gameController.requestPlayerEndTurn(activePlayer.getUuid());
            }
        });

        hud.add(round);
        hud.add(Box.createVerticalStrut(4));
        hud.add(turnInfo);
        hud.add(Box.createVerticalStrut(14));
        hud.add(playersBox);
        hud.add(Box.createVerticalStrut(14));
        hud.add(detail);
        hud.add(Box.createVerticalGlue());
        hud.add(save);
        hud.add(Box.createVerticalStrut(8));
        hud.add(endTurn);

        final String[] lastSignature = {null};
        final String[] lastPlayersSig = {null};

        //Timer dzięki któremu dane jednostek się zmieniają
        Timer timer = new Timer(150, e -> {
            GameView view = safeView(gameController);
            Player activePlayer = gameController.getActivePlayer();
            UUID localId = (activePlayer != null) ? activePlayer.getUuid() : null;

            round.setText("Runda " + (view != null ? view.getCurrentRound() : 0));
            turnInfo.setText(turnInfoText(view, activePlayer));
            endTurn.setEnabled(canEndTurn(view, localId));
            save.setEnabled(view != null && view.getState() != GameState.RESOLVING);

            String playersSig = playersSignature(view, localId);
            if (!playersSig.equals(lastPlayersSig[0])) {
                lastPlayersSig[0] = playersSig;
                playersBox.removeAll();
                playersBox.add(buildPlayersCard(view, localId));
                playersBox.revalidate();
                playersBox.repaint();
            }

            GameView.UnitView sel = selectedUnit(view);
            String signature = signatureOf(view, sel, localId);
            if (signature.equals(lastSignature[0])) {
                return;
            }
            lastSignature[0] = signature;

            detail.removeAll();
            if (sel != null) {
                detail.add(buildUnitCard(gameController, view, sel, localId));
            } else {
                JLabel empty = new JLabel("Brak zaznaczonej jednostki");
                empty.setForeground(SUBTLE);
                empty.setFont(FONT_LABEL);
                empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                detail.add(empty);
            }
            detail.revalidate();
            detail.repaint();
        });
        timer.start();

        return hud;
    }

    /**
     * Opens a save window and writes the current game to the chosen file.
     *
     * @param gc the controller performing the save
     * @param parent the component the dialogs are anchored to
     */
    private static void doSave(GameController gc, Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Zapisz grę");
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            gc.requestSaveGame(chooser.getSelectedFile().getPath());
            JOptionPane.showMessageDialog(parent, "Gra zapisana.", "Zapis", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                    "Nie udało się zapisać: " + ex.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Builds the "Players" card listing each player.
     *
     * @param view current game view snapshot, possibly {@code null}
     * @param activeId id of the player currently planning, or {@code null}
     * @return the assembled players card
     */
    private static JPanel buildPlayersCard(GameView view, UUID activeId) {
        JPanel card = makeCard("Gracze");
        if (view == null || view.getPlayers() == null || view.getPlayers().isEmpty()) {
            card.add(actionLabel("• brak danych", SUBTLE));
            return card;
        }

        List<GameView.PlayerView> players = view.getPlayers();
        boolean planning = view.getState() == GameState.PLANNING;

        List<UUID> orderedIds = new ArrayList<>();
        for (GameView.PlayerView pv : players) {
            if (pv != null) {
                orderedIds.add(pv.getPlayerId());
            }
        }

        for (int i = 0; i < players.size(); i++) {
            GameView.PlayerView p = players.get(i);
            if (p == null) {
                continue;
            }
            boolean isActive = planning && p.getPlayerId().equals(activeId);

            JPanel rowPanel = new JPanel();
            rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
            rowPanel.setBackground(CARD);
            rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            rowPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

            JPanel swatch = new JPanel();
            Color c = SwingRenderer.colorFor(orderedIds, activeId, p.getPlayerId());
            swatch.setBackground(c);
            swatch.setMaximumSize(new Dimension(14, 14));
            swatch.setMinimumSize(new Dimension(14, 14));
            swatch.setPreferredSize(new Dimension(14, 14));
            swatch.setAlignmentY(Component.CENTER_ALIGNMENT);

            String status;
            if (p.getUnitsCount() <= 0) {
                status = "pokonany";
            } else if (!planning) {
                status = "—";
            } else if (p.isTurnEnded()) {
                status = "gotowy";
            } else if (isActive) {
                status = "planuje…";
            } else {
                status = "czeka";
            }

            JLabel label = new JLabel(p.getName() + "  (" + p.getUnitsCount() + " jedn.) – " + status);
            label.setForeground(isActive ? ACCENT : TEXT);
            label.setFont(FONT_LABEL);
            label.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

            rowPanel.add(swatch);
            rowPanel.add(label);
            card.add(rowPanel);
        }
        return card;
    }

    /**
     * Builds the short "whose turn it is" line shown under the round counter.
     *
     * @param view current game view snapshot, possibly {@code null}
     * @param active the player currently planning, or {@code null}
     * @return a human-readable status line
     */
    private static String turnInfoText(GameView view, Player active) {
        if (view == null) {
            return " ";
        }
        switch (view.getState()) {
            case PLANNING:
                return (active != null) ? "Planuje: " + active.getName() : "Faza planowania";
            case RESOLVING:
                return "Rozstrzyganie rundy…";
            case FINISHED:
                return "Gra zakończona";
            case DRAW:
                return "Remis";
            default:
                return " ";
        }
    }

    /**
     * Builds a signature that changes whenever the players card needs to be
     * rebuilt (player list, unit counts, ready flags, phase or active player).
     *
     * @param view current game view snapshot, possibly {@code null}
     * @param activeId id of the active player, or {@code null}
     * @return a signature string for the players card
     */
    private static String playersSignature(GameView view, UUID activeId) {
        if (view == null || view.getPlayers() == null) {
            return "none";
        }
        StringBuilder sb = new StringBuilder("state:").append(view.getState())
                .append("|active:").append(activeId);
        for (GameView.PlayerView p : view.getPlayers()) {
            if (p == null) {
                continue;
            }
            sb.append('|').append(p.getName())
              .append(':').append(p.getUnitsCount())
              .append(':').append(p.isTurnEnded());
        }
        return sb.toString();
    }

    /**
     * Builds the details card for a single unit.
     *
     * @param gc controller used to resolve owner names and dispatch actions
     * @param view current game view snapshot
     * @param u the unit to display
     * @param localId id of the local player, or {@code null}
     * @return the assembled unit card
     */
    private static JPanel buildUnitCard(GameController gc, GameView view,
                                        GameView.UnitView u, UUID localId) {
        boolean own = localId != null && localId.equals(u.getOwnerId());

        JPanel card = makeCard(u.getName());

        card.add(row("Typ", u.getUnitType()));
        card.add(row("Właściciel", ownerName(gc, u.getOwnerId())));
        card.add(row("HP", u.getHp() + " / " + u.getMaxHp()));
        card.add(row("Pozycja", "(" + u.getX() + ", " + u.getY() + ")"));
        card.add(row("Prędkość", String.valueOf(u.getSpeed())));
        card.add(row("Zasięg ataku", String.valueOf(u.getAttackRange())));

        if (u.getArmor() > 0) {
            card.add(row("Pancerz", String.valueOf(u.getArmor())));
        }
        if (u.usesMana()) {
            card.add(row("Mana", u.getMana() + " / " + u.getMaxMana()));
        }

        if (own) {
            card.add(Box.createVerticalStrut(8));
            JLabel header = new JLabel("Zaplanowane akcje:");
            header.setForeground(ACCENT);
            header.setFont(FONT_CARD);
            header.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(header);

            List<String> actions = plannedActionsFor(view, u.getUnitId());
            if (actions.isEmpty()) {
                card.add(actionLabel("• brak", SUBTLE));
            } else {
                for (String a : actions) {
                    card.add(actionLabel("• " + a, TEXT));
                }
            }

            card.add(Box.createVerticalStrut(10));
            card.add(buildActionButtons(gc, view, u));
        }

        return card;
    }

    /**
     * Builds the per-unit action buttons (Move, Attack, Heal for mages, Wait).
     *
     * @param gc controller used to set input mode and dispatch the wait action
     * @param view current game view snapshot
     * @param u the unit the buttons act on
     * @return a panel containing the wired action buttons
     */
    private static JPanel buildActionButtons(GameController gc, GameView view, GameView.UnitView u) {
        boolean planning = view != null && view.getState() == GameState.PLANNING;
        boolean alive = u.getHp() > 0;

        int queued = 0;
        boolean hasFinalizing = false;
        if (view != null && view.getPlannedActions() != null) {
            for (GameView.PlannedActionView a : view.getPlannedActions()) {
                if (a == null || !u.getUnitId().equals(a.getUnitId())) {
                    continue;
                }
                queued++;
                if ("Attack".equals(a.getActionType()) || "Heal".equals(a.getActionType())) {
                    hasFinalizing = true;
                }
            }
        }
        boolean freeSlot = queued < u.getActionsPerTurn();
        boolean canAct = planning && alive && freeSlot && !hasFinalizing;

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(CARD);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        UUID unitId = u.getUnitId();

        panel.add(actionButton("Ruch", canAct, () -> gc.setInputMode(InputMode.MOVE)));
        panel.add(Box.createVerticalStrut(6));
        panel.add(actionButton("Atak", canAct, () -> gc.setInputMode(InputMode.ATTACK)));


        if (u.usesMana()) {
            panel.add(Box.createVerticalStrut(6));
            panel.add(actionButton("Leczenie", canAct && u.canCast(),
                    () -> gc.setInputMode(InputMode.HEAL)));
        }

        panel.add(Box.createVerticalStrut(6));
        panel.add(actionButton("Czekaj", canAct, () -> {
            gc.requestUnitWait(unitId);
            gc.setInputMode(InputMode.NONE);
        }));

        return panel;
    }

    /**
     * Creates an empty card panel with the accent-coloured titled border and
     * standard padding used across the HUD.
     *
     * @param title the title shown on the card border
     * @return an empty, styled card ready to receive rows
     */
    private static JPanel makeCard(String title) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT, 1, true), title);
        tb.setTitleColor(ACCENT);
        tb.setTitleFont(FONT_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                tb, BorderFactory.createEmptyBorder(8, 12, 12, 12)));
        return card;
    }

    /**
     * Builds a single "label: value" row.
     *
     * @param label the field name
     * @param value the field value
     * @return a styled label component
     */
    private static JComponent row(String label, String value) {
        JLabel l = new JLabel(label + ": " + value);
        l.setForeground(TEXT);
        l.setFont(FONT_LABEL);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        return l;
    }

    /**
     * Builds a single planned-action line.
     *
     * @param text the line text
     * @param color the text colour
     * @return a styled label component for the line
     */
    private static JComponent actionLabel(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(FONT_LABEL);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 0));
        return l;
    }

    /**
     * Builds a full-width action button.
     *
     * @param text the button label
     * @param enabled whether the button should be clickable
     * @param onClick action to run when the button is pressed
     * @return the configured button
     */
    private static JButton actionButton(String text, boolean enabled, Runnable onClick) {
        JButton b = styledButton(text, BTN);
        b.setEnabled(enabled);
        b.addActionListener(e -> onClick.run());
        return b;
    }

    /**
     * Builds a flat, full-width button using the HUD's visual style.
     *
     * @param text the button label
     * @param bg the button background colour
     * @return the styled button
     */
    private static JButton styledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(FONT_BTN);
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        return b;
    }

    /**
     * Returns the current game view.
     *
     * @param gc the game controller
     * @return the current view snapshot, or {@code null} if unavailable
     */
    private static GameView safeView(GameController gc) {
        try {
            return gc.getGameView();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Finds the unit currently marked as selected in the view.
     *
     * @param view current game view snapshot, possibly {@code null}
     * @return the selected unit, or {@code null} if none is selected
     */
    private static GameView.UnitView selectedUnit(GameView view) {
        if (view == null || view.getUnits() == null) {
            return null;
        }
        for (GameView.UnitView u : view.getUnits()) {
            if (u != null && u.isSelected()) {
                return u;
            }
        }
        return null;
    }

    /**
     * Resolves a player's display name from its id.
     *
     * @param gc the game controller
     * @param ownerId id of the owning player
     * @return the player's name, or {@code "?"} if it cannot be resolved
     */
    private static String ownerName(GameController gc, UUID ownerId) {
        try {
            Player owner = gc.getEngine().getPlayer(ownerId);
            if (owner != null && owner.getName() != null) {
                return owner.getName();
            }
        } catch (Exception ignored) {

        }
        return "?";
    }

    /**
     * Collects the display descriptions of all actions planned for a given unit.
     *
     * @param view current game view snapshot, possibly {@code null}
     * @param unitId id of the unit whose actions are wanted
     * @return descriptions of the unit's planned actions, in queue order (possibly empty)
     */
    private static List<String> plannedActionsFor(GameView view, UUID unitId) {
        List<String> result = new ArrayList<>();
        if (view == null || view.getPlannedActions() == null) {
            return result;
        }
        for (GameView.PlannedActionView a : view.getPlannedActions()) {
            if (a != null && unitId.equals(a.getUnitId())) {
                String desc = a.getDescription();
                result.add((desc != null && !desc.isBlank()) ? desc : a.getActionType());
            }
        }
        return result;
    }

    /**
     * Determines whether the local player may still end their turn this round.
     *
     * @param view current game view snapshot, possibly {@code null}
     * @param localId id of the local player, or {@code null}
     * @return {@code true} during the planning phase while the player has not yet ended their turn
     */
    private static boolean canEndTurn(GameView view, UUID localId) {
        if (view == null || localId == null || view.getState() != GameState.PLANNING) {
            return false;
        }
        if (view.getPlayers() != null) {
            for (GameView.PlayerView p : view.getPlayers()) {
                if (p != null && localId.equals(p.getPlayerId())) {
                    return !p.isTurnEnded();
                }
            }
        }
        return true;
    }

    /**
     * Builds a string that changes whenever anything displayed for the selected
     * unit (or anything affecting the buttons) changes, so the panel only
     * rebuilds when the value actually differs from the previous frame.
     *
     * @param view current game view snapshot, possibly {@code null}
     * @param u the selected unit, or {@code null} if none is selected
     * @param localId id of the local player, or {@code null}
     * @return a signature string summarising the displayed state
     */
    private static String signatureOf(GameView view, GameView.UnitView u, UUID localId) {
        GameState state = (view != null) ? view.getState() : null;
        if (u == null) {
            return "none@" + state;
        }
        boolean own = localId != null && localId.equals(u.getOwnerId());
        StringBuilder sb = new StringBuilder()
                .append(u.getUnitId()).append('|')
                .append(u.getHp()).append('/').append(u.getMaxHp()).append('|')
                .append(u.getX()).append(',').append(u.getY()).append('|')
                .append(u.getArmor()).append('|')
                .append(u.getMana()).append('/').append(u.getMaxMana()).append('|')
                .append(u.canCast()).append('|')
                .append("state:").append(state);
        if (own) {
            sb.append("|actions:").append(plannedActionsFor(view, u.getUnitId()));
        }
        return sb.toString();
    }
}
