package GameRenderer;

import Action.Action;
import GameEngine.GameState;
import GameEngine.Player;
import Map.GameMap;
import Map.Tile;
import Units.Unit;

import java.util.List;
import java.util.UUID;

/**
 * {@link GameRenderer} implementation that outputs the game state as plain text
 * to the standard output stream.
 *
 * <p>Intended for debugging, headless servers, or text-only environments where
 * a graphical renderer is unavailable.</p>
 *
 * @author Dzhyhar Volodymyr
 */
public class ConsoleRenderer implements GameRenderer {

    @Override
    public void renderMap(GameMap m) {
        System.out.println("\n============= MAPA GRY =============");
        for (int y = 0; y < m.getHeight(); y++) {
            for (int x = 0; x < m.getWidth(); x++) {
                Tile t = m.getTile(x, y);
                if (t.isOccupied()) {
                    // Wyświetla pierwszą literę nazwy jednostki (np. A dla Archer/Artur)
                    String name = t.getUnit().getName();
                    System.out.print("\033[1;31m" + name.substring(0, 1).toUpperCase() + "\033[0m ");
                } else {
                    switch (t.getType()) {
                        case PLAIN -> System.out.print(". ");
                        case OBSTACLE -> System.out.print("O ");
                        case FOREST -> System.out.print("F ");
                        case WATER -> System.out.print("W ");
                        case MOUNTAIN -> System.out.print("M ");
                    }
                }
            }
            System.out.println();
        }
        System.out.println("====================================");
    }

    @Override
    public void renderUnit(Unit u) {
        // Zostawiamy puste, jednostki wypisujemy zbiorczo w renderHUD
    }

    @Override
    public void renderHUD(List<Player> players, GameState state, UUID winnerId) {
        System.out.println("\nSTAN GRY: " + state);
        for (Player player : players) {
            String status = player.isTurnEnded() ? "[GOTOWY/POMIJA]" : "[PLANUJE]";
            System.out.println("Gracz: " + player.getName() + " " + status);
            for (Unit u : player.getUnits()) {
                System.out.printf("  -> %s (AP: %d/%d, HP: %d) | UUID: %s%n",
                        u.getName(), u.getRemainingActions(), u.getActionsPerTurn(), u.getHp(), u.getId());
            }
        }

        if (state == GameState.FINISHED) {
            System.out.println("\n🏆 KONIEC GRY! ZWYCIĘZCA: " + winnerId);
        } else if (state == GameState.DRAW) {
            System.out.println("\n💀 KONIEC GRY! REMIS (Wzajemne wyniszczenie)");
        }
    }

    @Override
    public void renderPlannedActions(List<Action> actions) {
        System.out.println("Liczba wszystkich zakolejkowanych akcji w tej rundzie: " + actions.size());
        System.out.println("------------------------------------");
    }
}
