package GameController;

import Action.Action;
import Action.AttackAction;
import Action.HealAction;
import Action.MoveAction;
import Action.SkipTurnAction;
import Action.WaitAction;
import Map.Tile;
import Map.TileType;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Yevhenii Marienko
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class InputHandlerTest {

    private final InputHandler inputHandler = new InputHandler();

    @Test
    void onMoveUnit_should_create_MoveAction_when_input_is_valid() {
        UUID unitId = UUID.randomUUID();
        Tile destination = new Tile(2, 3, TileType.PLAIN);

        MoveAction action = inputHandler.onMoveUnit(unitId, destination);

        assertNotNull(action);
        assertInstanceOf(MoveAction.class, action);
        assertEquals(unitId, action.getUnitId());
    }

    @Test
    void onMoveUnit_should_return_null_when_unitId_is_null() {
        Tile destination = new Tile(2, 3, TileType.PLAIN);

        MoveAction action = inputHandler.onMoveUnit(null, destination);

        assertNull(action);
    }

    @Test
    void onMoveUnit_should_return_null_when_destination_is_null() {
        UUID unitId = UUID.randomUUID();

        MoveAction action = inputHandler.onMoveUnit(unitId, null);

        assertNull(action);
    }

    @Test
    void onAttack_should_create_AttackAction_when_input_is_valid() {
        UUID attackerId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        AttackAction action = inputHandler.onAttack(attackerId, targetId);

        assertNotNull(action);
        assertInstanceOf(AttackAction.class, action);
        assertEquals(attackerId, action.getUnitId());
    }

    @Test
    void onAttack_should_return_null_when_attackerId_is_null() {
        UUID targetId = UUID.randomUUID();

        AttackAction action = inputHandler.onAttack(null, targetId);

        assertNull(action);
    }

    @Test
    void onAttack_should_return_null_when_targetId_is_null() {
        UUID attackerId = UUID.randomUUID();

        AttackAction action = inputHandler.onAttack(attackerId, null);

        assertNull(action);
    }

    @Test
    void onHeal_should_create_HealAction_when_input_is_valid() {
        UUID casterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        HealAction action = inputHandler.onHeal(casterId, targetId);

        assertNotNull(action);
        assertInstanceOf(HealAction.class, action);
        assertEquals(casterId, action.getUnitId());
    }

    @Test
    void onHeal_should_return_null_when_casterId_is_null() {
        UUID targetId = UUID.randomUUID();

        HealAction action = inputHandler.onHeal(null, targetId);

        assertNull(action);
    }

    @Test
    void onHeal_should_return_null_when_targetId_is_null() {
        UUID casterId = UUID.randomUUID();

        HealAction action = inputHandler.onHeal(casterId, null);

        assertNull(action);
    }

    @Test
    void onWait_should_create_WaitAction_when_input_is_valid() {
        UUID unitId = UUID.randomUUID();

        WaitAction action = inputHandler.onWait(unitId);

        assertNotNull(action);
        assertInstanceOf(WaitAction.class, action);
        assertEquals(unitId, action.getUnitId());
    }

    @Test
    void onWait_should_return_null_when_unitId_is_null() {
        WaitAction action = inputHandler.onWait(null);

        assertNull(action);
    }

    @Test
    void onEndTurn_should_create_SkipTurnAction_when_playerId_is_valid() {
        UUID playerId = UUID.randomUUID();

        SkipTurnAction action = inputHandler.onEndTurn(playerId);

        assertNotNull(action);
        assertInstanceOf(SkipTurnAction.class, action);
    }

    @Test
    void onEndTurn_should_return_null_when_playerId_is_null() {
        SkipTurnAction action = inputHandler.onEndTurn(null);

        assertNull(action);
    }

    @Test
    void parseConsoleCommand_should_parse_move_command() {
        UUID unitId = UUID.randomUUID();

        Action action = inputHandler.parseConsoleCommand(
                "move " + unitId + " 4 5",
                UUID.randomUUID(),
                null
        );

        assertNotNull(action);
        assertInstanceOf(MoveAction.class, action);
        assertEquals(unitId, action.getUnitId());
    }

    @Test
    void parseConsoleCommand_should_parse_move_command_case_insensitively() {
        UUID unitId = UUID.randomUUID();

        Action action = inputHandler.parseConsoleCommand(
                "MOVE " + unitId + " 1 2",
                UUID.randomUUID(),
                null
        );

        assertNotNull(action);
        assertInstanceOf(MoveAction.class, action);
        assertEquals(unitId, action.getUnitId());
    }

    @Test
    void parseConsoleCommand_should_parse_attack_command() {
        UUID attackerId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Action action = inputHandler.parseConsoleCommand(
                "attack " + attackerId + " " + targetId,
                UUID.randomUUID(),
                null
        );

        assertNotNull(action);
        assertInstanceOf(AttackAction.class, action);
        assertEquals(attackerId, action.getUnitId());
    }

    @Test
    void parseConsoleCommand_should_parse_heal_command() {
        UUID casterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Action action = inputHandler.parseConsoleCommand(
                "heal " + casterId + " " + targetId,
                UUID.randomUUID(),
                null
        );

        assertNotNull(action);
        assertInstanceOf(HealAction.class, action);
        assertEquals(casterId, action.getUnitId());
    }

    @Test
    void parseConsoleCommand_should_parse_wait_command() {
        UUID unitId = UUID.randomUUID();

        Action action = inputHandler.parseConsoleCommand(
                "wait " + unitId,
                UUID.randomUUID(),
                null
        );

        assertNotNull(action);
        assertInstanceOf(WaitAction.class, action);
        assertEquals(unitId, action.getUnitId());
    }

    @Test
    void parseConsoleCommand_should_parse_endturn_command_using_currentPlayerId() {
        UUID currentPlayerId = UUID.randomUUID();

        Action action = inputHandler.parseConsoleCommand(
                "endturn",
                currentPlayerId,
                null
        );

        assertNotNull(action);
        assertInstanceOf(SkipTurnAction.class, action);
    }

    @Test
    void parseConsoleCommand_should_return_null_for_blank_command() {
        Action action = inputHandler.parseConsoleCommand("   ", UUID.randomUUID(), null);

        assertNull(action);
    }

    @Test
    void parseConsoleCommand_should_return_null_for_null_command() {
        Action action = inputHandler.parseConsoleCommand(null, UUID.randomUUID(), null);

        assertNull(action);
    }

    @Test
    void parseConsoleCommand_should_return_null_for_unknown_command() {
        Action action = inputHandler.parseConsoleCommand("dance", UUID.randomUUID(), null);

        assertNull(action);
    }

    @Test
    void parseConsoleCommand_should_return_null_when_move_has_wrong_number_of_arguments() {
        UUID unitId = UUID.randomUUID();

        Action action = inputHandler.parseConsoleCommand(
                "move " + unitId + " 4",
                UUID.randomUUID(),
                null
        );

        assertNull(action);
    }

    @Test
    void parseConsoleCommand_should_return_null_when_attack_has_wrong_number_of_arguments() {
        UUID attackerId = UUID.randomUUID();

        Action action = inputHandler.parseConsoleCommand(
                "attack " + attackerId,
                UUID.randomUUID(),
                null
        );

        assertNull(action);
    }

    @Test
    void parseConsoleCommand_should_return_null_when_heal_has_wrong_number_of_arguments() {
        UUID casterId = UUID.randomUUID();

        Action action = inputHandler.parseConsoleCommand(
                "heal " + casterId,
                UUID.randomUUID(),
                null
        );

        assertNull(action);
    }

    @Test
    void parseConsoleCommand_should_return_null_when_wait_has_wrong_number_of_arguments() {
        UUID unitId = UUID.randomUUID();

        Action action = inputHandler.parseConsoleCommand(
                "wait " + unitId + " extra",
                UUID.randomUUID(),
                null
        );

        assertNull(action);
    }

    @Test
    void parseConsoleCommand_should_return_null_when_endturn_has_null_currentPlayerId() {
        Action action = inputHandler.parseConsoleCommand("endturn", null, null);

        assertNull(action);
    }

    @Test
    void parseConsoleCommand_should_return_null_when_uuid_is_invalid() {
        Action action = inputHandler.parseConsoleCommand(
                "attack not-a-uuid also-not-a-uuid",
                UUID.randomUUID(),
                null
        );

        assertNull(action);
    }

    @Test
    void parseConsoleCommand_should_return_null_when_move_coordinates_are_not_numbers() {
        UUID unitId = UUID.randomUUID();

        Action action = inputHandler.parseConsoleCommand(
                "move " + unitId + " x y",
                UUID.randomUUID(),
                null
        );

        assertNull(action);
    }
}