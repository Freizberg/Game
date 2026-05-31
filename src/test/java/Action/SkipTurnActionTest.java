package Action;

import Action.SkipTurnAction;
import Map.GameMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SkipTurnActionTest {

    @Test
    @DisplayName("getUnitId returns null (player-level action)")
    public void getUnitIdIsNull() {
        assertNull(new SkipTurnAction(UUID.randomUUID()).getUnitId());
    }

    @Test
    @DisplayName("getPlayerId returns the correct UUID")
    public void getPlayerIdCorrect() {
        UUID pid = UUID.randomUUID();
        assertEquals(pid, new SkipTurnAction(pid).getPlayerId());
    }

    @Test
    @DisplayName("isValid always returns true")
    public void isValidAlwaysTrue() {
        SkipTurnAction s = new SkipTurnAction(UUID.randomUUID());
        assertTrue(s.isValid(new GameMap(3, 3)));
        assertTrue(s.isValid(new GameMap(1, 1)));
    }
}