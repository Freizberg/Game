package testutil;

import GameController.GameStateUpdate;
import GameEngine.GameEngine;
import GameEngine.GameState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Small reusable fixtures used by networking tests.
 *
 * <p>For the current client networking tests we do not need a fully populated
 * engine with real players, units and map. The tests only need:</p>
 * <ul>
 *     <li>a non-null engine in PLANNING state,</li>
 *     <li>a serializable {@link GameStateUpdate} instance.</li>
 * </ul>
 *
 * <p>If future tests need real gameplay state, this fixture can be extended
 * to build a complete engine graph using actual domain constructors.</p>
 */
public final class TestFixtures {

    private TestFixtures() {
    }

    /**
     * Creates a minimal engine instance in the PLANNING phase.
     *
     * <p>This is enough for networking tests that only need an engine object
     * to exist and expose a current round/state.</p>
     *
     * @return minimal engine in PLANNING state
     */
    public static GameEngine newPlanningEngine() {
        GameEngine engine = new GameEngine();
        engine.setState(GameState.PLANNING);
        engine.setCurrentRound(1);
        return engine;
    }

    /**
     * Creates a small sample state update suitable for serialization tests.
     *
     * <p>The returned update does not depend on real players or units existing
     * in the engine. It is only meant to verify that update transport from
     * server to client works correctly.</p>
     *
     * @param engine source engine used only for basic metadata such as round number
     * @return sample serialized game-state update
     * @throws IllegalArgumentException if the engine is null
     */
    public static GameStateUpdate sampleUpdate(GameEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("Engine cannot be null.");
        }

        UUID sampleUnitId = UUID.randomUUID();

        Map<UUID, int[]> positions = new HashMap<>();
        positions.put(sampleUnitId, new int[]{1, 1});

        Map<UUID, Integer> hps = new HashMap<>();
        hps.put(sampleUnitId, 100);

        return new GameStateUpdate(
                positions,
                hps,
                new ArrayList<>(),
                null,
                false,
                engine.getCurrentRound() > 0 ? engine.getCurrentRound() : 1
        );
    }
}