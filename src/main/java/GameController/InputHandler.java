package GameController;

import Action.Action;
import Action.AttackAction;
import Action.HealAction;
import Action.MoveAction;
import Action.SkipTurnAction;
import Action.WaitAction;
import GameEngine.GameEngine;
import Map.Tile;

import java.util.Locale;
import java.util.UUID;

/**
 * Translates raw backend input signals into concrete {@link Action} objects
 * that can be queued by the {@link GameController}.
 *
 * <p>This class does not implement any actual user interface. Instead, it
 * exposes entry points that a console adapter, network listener, REST layer,
 * or future graphical UI may call when a player requests an interaction.</p>
 *
 * <p>Its responsibility is limited to turning raw input data into action
 * objects. Final gameplay validation remains the responsibility of the
 * {@link GameController}, {@link GameEngine}, and the engine resolvers.</p>
 *
 * @author Dzhyhar Volodymyr
 * @author Yevhenii Marienko
 */
public class InputHandler {

    /**
     * Creates a {@link MoveAction} instructing the given unit to move
     * to the specified destination tile.
     *
     * <p>This method is intended as a backend entry point for UI events such as
     * selecting a unit and clicking a destination tile.</p>
     *
     * @param unitId the UUID of the unit to move
     * @param dest the destination tile selected by the caller
     * @return a new {@link MoveAction}, or {@code null} if the input is incomplete
     */
    public MoveAction onMoveUnit(UUID unitId, Tile dest) {
        if (unitId == null || dest == null) {
            return null;
        }

        return new MoveAction(unitId, dest.getX(), dest.getY());
    }

    /**
     * Creates an {@link AttackAction} instructing one unit to attack another.
     *
     * <p>This method is intended as a backend entry point for UI events such as
     * selecting an attacker and then selecting a target unit.</p>
     *
     * @param attackerId the UUID of the attacking unit
     * @param targetId the UUID of the targeted unit
     * @return a new {@link AttackAction}, or {@code null} if the input is incomplete
     */
    public AttackAction onAttack(UUID attackerId, UUID targetId) {
        if (attackerId == null || targetId == null) {
            return null;
        }

        return new AttackAction(attackerId, targetId);
    }

    /**
     * Creates a {@link HealAction} instructing a Mage to heal a friendly unit.
     *
     * <p>This method is intended as a backend entry point for UI events such as
     * selecting a Mage and then selecting a friendly target unit.</p>
     *
     * @param casterId the UUID of the Mage casting the heal
     * @param targetId the UUID of the friendly target unit
     * @return a new {@link HealAction}, or {@code null} if the input is incomplete
     */
    public HealAction onHeal(UUID casterId, UUID targetId) {
        if (casterId == null || targetId == null) {
            return null;
        }

        return new HealAction(casterId, targetId);
    }

    /**
     * Creates a {@link WaitAction} instructing the given unit to remain in place.
     *
     * <p>This method is intended as a backend entry point for UI or console
     * commands that explicitly consume a unit action without movement or attack.</p>
     *
     * @param unitId the UUID of the unit that should wait
     * @return a new {@link WaitAction}, or {@code null} if the input is incomplete
     */
    public WaitAction onWait(UUID unitId) {
        if (unitId == null) {
            return null;
        }

        return new WaitAction(unitId);
    }

    /**
     * Creates a {@link SkipTurnAction} signalling that the player has finished
     * issuing commands for the current planning phase.
     *
     * @param playerId the UUID of the player ending their turn
     * @return a new {@link SkipTurnAction}, or {@code null} if the input is incomplete
     */
    public SkipTurnAction onEndTurn(UUID playerId) {
        if (playerId == null) {
            return null;
        }

        return new SkipTurnAction(playerId);
    }

    /**
     * Parses a raw console command into an {@link Action}.
     *
     * <p>This method is a backend-friendly console entry point. It does not read
     * from standard input directly; instead, another layer passes the raw text
     * command here. The returned action can then be forwarded to
     * {@link GameController#handleInput(Action)}.</p>
     *
     * <p>Supported commands:</p>
     * <ul>
     *     <li>{@code move <unitId> <x> <y>}</li>
     *     <li>{@code attack <attackerId> <targetId>}</li>
     *     <li>{@code heal <casterId> <targetId>}</li>
     *     <li>{@code wait <unitId>}</li>
     *     <li>{@code endturn}</li>
     * </ul>
     *
     * <p>The {@code currentPlayerId} is used for player-level commands such as
     * {@code endturn}. The engine parameter is accepted to keep this method
     * compatible with future lightweight pre-validation or command expansion.</p>
     *
     * @param commandLine raw command text supplied by a console adapter
     * @param currentPlayerId the UUID of the player issuing the command
     * @param engine the current engine instance, available for future command support
     * @return the parsed {@link Action}, or {@code null} if the command is invalid
     */
    public Action parseConsoleCommand(String commandLine, UUID currentPlayerId, GameEngine engine) {
        if (commandLine == null || commandLine.isBlank()) {
            return null;
        }

        String[] tokens = commandLine.trim().split("\\s+");
        if (tokens.length == 0) {
            return null;
        }

        String command = tokens[0].toLowerCase(Locale.ROOT);

        try {
            return switch (command) {
                case "move" -> {
                    if (tokens.length != 4) {
                        yield null;
                    }

                    UUID unitId = UUID.fromString(tokens[1]);
                    int x = Integer.parseInt(tokens[2]);
                    int y = Integer.parseInt(tokens[3]);

                    yield new MoveAction(unitId, x, y);
                }
                case "attack" -> {
                    if (tokens.length != 3) {
                        yield null;
                    }

                    UUID attackerId = UUID.fromString(tokens[1]);
                    UUID targetId = UUID.fromString(tokens[2]);

                    yield new AttackAction(attackerId, targetId);
                }
                case "heal" -> {
                    if (tokens.length != 3) {
                        yield null;
                    }

                    UUID casterId = UUID.fromString(tokens[1]);
                    UUID targetId = UUID.fromString(tokens[2]);

                    yield new HealAction(casterId, targetId);
                }
                case "wait" -> {
                    if (tokens.length != 2) {
                        yield null;
                    }

                    UUID unitId = UUID.fromString(tokens[1]);
                    yield new WaitAction(unitId);
                }
                case "endturn" -> {
                    if (currentPlayerId == null) {
                        yield null;
                    }

                    yield new SkipTurnAction(currentPlayerId);
                }
                default -> null;
            };
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}