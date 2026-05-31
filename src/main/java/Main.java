import GameController.GameController;
import GameController.GameSaveManager;
import GameController.InputHandler;
import GameEngine.GameEngine;
import GameEngine.GameState;
import GameEngine.Player;
import GameRenderer.ConsoleRenderer;
import Map.GameMap;
import Map.MapConfig;
import Units.Archer;
import Units.Knight;
import Units.Mage;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class Main {
    public static void main(String[] args) {
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

        GameController controller = new GameController(
                engine,
                new ConsoleRenderer(),
                null,
                new GameSaveManager("save.dat"),
                new InputHandler()
        );

        controller.startLocalGame();

        Scanner scanner = new Scanner(System.in);
        System.out.println("\n*** GRA ROZPOCZĘTA ***");
        System.out.println("Instrukcja (kopiuj UUID jednostek z listy powyżej):");
        System.out.println("Ruch:   move <UUID_jednostki> <x> <y>");
        System.out.println("Atak:   attack <UUID_atakującego> <UUID_celu>");
        System.out.println("Koniec: endturn");
        System.out.println("Wyjście: exit\n");

        Player activePlayer = player1;

        while (engine.getState() == GameState.PLANNING || engine.getState() == GameState.RESOLVING) {

            System.out.println("\n====================================");
            System.out.println("TERAZ PLANUJE: " + activePlayer.getName());
            System.out.println("====================================");
            System.out.print("> ");

            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Wychodzenie z gry...");
                break;
            }

            if (input.equalsIgnoreCase("endturn")) {
                controller.onConsoleCommand("endturn", activePlayer.getUuid());

                if (activePlayer == player1) {
                    activePlayer = player2;
                } else {
                    activePlayer = player1;
                }

                continue;
            }

            controller.onConsoleCommand(input, activePlayer.getUuid());
        }
    }
}