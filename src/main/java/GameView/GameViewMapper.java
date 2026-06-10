package GameView;

import Action.Action;
import Action.AttackAction;
import Action.HealAction;
import Action.MoveAction;
import Action.WaitAction;
import GameController.GameController;
import GameEngine.Player;
import Map.GameMap;
import Map.Tile;
import Units.Mage;
import Units.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Assembles an immutable {@link GameView} snapshot from the mutable domain
 * objects exposed by the {@link GameController}.
 *
 * <p>This class is the only place that depends on both the domain layer
 * (engine, map, players, units, actions) and the presentation DTO. It keeps
 * {@link GameView} free of domain dependencies so the view stays a pure,
 * read-only snapshot suitable for rendering and network transfer.</p>
 *
 * @author Miłosz Koziejowski
 */
public final class GameViewMapper {

    private GameViewMapper() {

    }

    /**
     * Builds a graphical snapshot from the current {@link GameController}.
     *
     * @param controller source controller
     * @return graphical snapshot prepared for rendering
     */
    public static GameView fromController(GameController controller) {
        UUID selectedUnitId = controller.getSelectedUnitId();
        List<Player> domainPlayers = controller.getEngine().getPlayers();

        return new GameView(
                controller.getCurrentPhase(),
                controller.getEngine().getCurrentRound(),
                controller.getEngine().getWinnerId(),
                controller.isAutosaveEnabled(),
                mapTiles(controller.getEngine().getMap()),
                mapUnits(domainPlayers, selectedUnitId),
                mapPlayers(domainPlayers),
                mapActions(controller.getPlannedActionsView(), controller.getEngine().getMap()),
                selectedUnitId
        );
    }

    /**
     * Maps the game map into a flat list of tile views.
     *
     * @param map source game map
     * @return tile views prepared for rendering
     */
    private static List<GameView.TileView> mapTiles(GameMap map) {
        List<GameView.TileView> result = new ArrayList<>();
        if (map == null) {
            return result;
        }
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile t = map.getTiles()[x][y];
                // highlighted: trzeba dorobić jak będzie okno jakieś
                result.add(new GameView.TileView(t.getX(), t.getY(), t.getType(), t.isOccupied(), false));
            }
        }
        return result;
    }

    /**
     * Maps every player's units into unit views, marking the selected one.
     *
     * @param players domain players owning the units
     * @param selectedUnitId id of the currently selected unit, or {@code null}
     * @return unit views prepared for rendering
     */
    private static List<GameView.UnitView> mapUnits(List<Player> players, UUID selectedUnitId) {
        List<GameView.UnitView> result = new ArrayList<>();
        if (players == null) {
            return result;
        }
        for (Player p : players) {
            if (p.getUnits() == null) {
                continue;
            }
            for (Unit u : p.getUnits()) {
                boolean selected = u.getId().equals(selectedUnitId);

                int mana = -1;
                int maxMana = -1;
                boolean canCast = false;
                if (u instanceof Mage mage) {
                    mana = mage.getMana();
                    maxMana = mage.getMaxMana();
                    canCast = mage.canCast();
                }

                result.add(new GameView.UnitView(
                        u.getId(),
                        u.getName(),
                        p.getUuid(),
                        u.getClass().getSimpleName(),
                        u.getHp(),
                        u.getMaxHp(),
                        u.getPosX(),
                        u.getPosY(),
                        selected,
                        u.getSpeed(),
                        u.getAttackRange(),
                        u.getArmor(),
                        mana,
                        maxMana,
                        u.getActionsPerTurn(),
                        canCast));
            }
        }
        return result;
    }

    /**
     * Maps domain players into HUD player summaries.
     *
     * @param players domain players
     * @return player views prepared for HUD display
     */
    private static List<GameView.PlayerView> mapPlayers(List<Player> players) {
        List<GameView.PlayerView> result = new ArrayList<>();
        if (players == null) {
            return result;
        }
        for (Player p : players) {
            int count = (p.getUnits() == null) ? 0 : p.getUnits().size();
            result.add(new GameView.PlayerView(p.getUuid(), p.getName(), p.isTurnEnded(), count));
        }
        return result;
    }

    /**
     * Maps queued actions into planned action views.
     *
     * @param actions queued actions
     * @param map current game map
     * @return planned action views prepared for side panels
     */
    private static List<GameView.PlannedActionView> mapActions(List<Action> actions, GameMap map) {
        List<GameView.PlannedActionView> result = new ArrayList<>();
        if (actions == null) {
            return result;
        }
        for (Action a : actions) {
            result.add(new GameView.PlannedActionView(a.type(), a.getUnitId(), describe(a, map)));
        }
        return result;
    }
    /**
     * Builds a human-readable, presentation-friendly description for a queued action.
     *
     * @param a the queued action
     * @param map current map, used to resolve target units by id
     * @return display text shown in the planned-actions list
     */
    private static String describe(Action a, GameMap map) {
        if (a instanceof MoveAction move) {
            return "Ruch na (" + move.getDestX() + ", " + move.getDestY() + ")";
        }
        if (a instanceof AttackAction atk) {
            return "Atak na " + targetLabel(map, atk.getTargetId());
        }
        if (a instanceof HealAction heal) {
            return "Leczenie: " + targetLabel(map, heal.getTargetId());
        }
        if (a instanceof WaitAction) {
            return "Czekaj";
        }
        return a.type();
    }

    /**
     * Resolves a target unit's name and position for display, or a placeholder
     * if the unit can no longer be found on the map.
     *
     * @param map current game map
     * @param targetId id of the target unit
     * @return label like {@code "Rycerz (3, 4)"}, or {@code "(?)"} if not found
     */
    private static String targetLabel(GameMap map, UUID targetId) {
        Unit target = (map != null) ? map.findUnit(targetId) : null;
        if (target != null) {
            return target.getName() + " (" + target.getPosX() + ", " + target.getPosY() + ")";
        }
        return "(?)";
    }
}
