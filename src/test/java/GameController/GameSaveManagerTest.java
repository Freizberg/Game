package GameController;

import GameEngine.GameEngine;
import GameEngine.GameState;
import GameEngine.Player;
import Map.GameMap;
import Map.TileType;
import Units.Unit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Yevhenii Marienko
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GameSaveManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void saveGame_with_default_path_should_create_file() {
        Path saveFile = tempDir.resolve("game.dat");
        GameSaveManager manager = new GameSaveManager(saveFile.toString());
        GameEngine engine = TestGameFactory.createEngine();

        manager.saveGame(engine);

        assertTrue(Files.exists(saveFile));
        assertTrue(Files.isRegularFile(saveFile));
    }

    @Test
    void saveGame_with_explicit_path_should_create_file() {
        Path saveFile = tempDir.resolve("manual-save.dat");
        GameSaveManager manager = new GameSaveManager();
        GameEngine engine = TestGameFactory.createEngine();

        manager.saveGame(engine, saveFile.toString());

        assertTrue(Files.exists(saveFile));
        assertTrue(Files.isRegularFile(saveFile));
    }

    @Test
    void loadGame_with_default_path_should_restore_engine() {
        Path saveFile = tempDir.resolve("default-save.dat");
        GameSaveManager manager = new GameSaveManager(saveFile.toString());
        GameEngine original = TestGameFactory.createEngine();

        manager.saveGame(original);
        GameEngine loaded = manager.loadGame();

        assertNotNull(loaded);
        assertNotSame(original, loaded);
        assertEquals(original.getState(), loaded.getState());
        assertEquals(original.getCurrentRound(), loaded.getCurrentRound());
        assertEquals(original.getPlayers().size(), loaded.getPlayers().size());
        assertEquals(original.getPlayers().get(0).getUnits().size(),
                loaded.getPlayers().get(0).getUnits().size());
    }

    @Test
    void loadGame_with_explicit_path_should_restore_engine() {
        Path saveFile = tempDir.resolve("explicit-save.dat");
        GameSaveManager manager = new GameSaveManager();
        GameEngine original = TestGameFactory.createEngine();

        manager.saveGame(original, saveFile.toString());
        GameEngine loaded = manager.loadGame(saveFile.toString());

        assertNotNull(loaded);
        assertNotSame(original, loaded);
        assertEquals(original.getState(), loaded.getState());
        assertEquals(original.getCurrentRound(), loaded.getCurrentRound());
    }

    @Test
    void saveGame_without_default_path_should_throw_exception() {
        GameSaveManager manager = new GameSaveManager();
        GameEngine engine = TestGameFactory.createEngine();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> manager.saveGame(engine));

        assertEquals("Default save path is not configured.", exception.getMessage());
    }

    @Test
    void loadGame_without_default_path_should_throw_exception() {
        GameSaveManager manager = new GameSaveManager();

        IllegalStateException exception = assertThrows(IllegalStateException.class, manager::loadGame);

        assertEquals("Default save path is not configured.", exception.getMessage());
    }

    @Test
    void saveGame_with_null_engine_should_throw_exception() {
        GameSaveManager manager = new GameSaveManager();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> manager.saveGame(null, tempDir.resolve("x.dat").toString()));

        assertEquals("GameEngine cannot be null.", exception.getMessage());
    }

    @Test
    void saveGame_with_blank_path_should_throw_exception() {
        GameSaveManager manager = new GameSaveManager();
        GameEngine engine = TestGameFactory.createEngine();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> manager.saveGame(engine, " "));

        assertEquals("Save path cannot be blank.", exception.getMessage());
    }

    @Test
    void loadGame_with_blank_path_should_throw_exception() {
        GameSaveManager manager = new GameSaveManager();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> manager.loadGame(" "));

        assertEquals("Load path cannot be blank.", exception.getMessage());
    }

    @Test
    void loadGame_when_file_does_not_exist_should_throw_runtime_exception_wrapping_file_not_found() {
        GameSaveManager manager = new GameSaveManager();
        Path missing = tempDir.resolve("missing-save.dat");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> manager.loadGame(missing.toString()));

        assertEquals("Failed to load game from: " + missing, exception.getMessage());
        assertNotNull(exception.getCause());
        assertInstanceOf(FileNotFoundException.class, exception.getCause());
    }

    @Test
    void setSavePath_should_update_default_path() {
        GameSaveManager manager = new GameSaveManager();
        Path saveFile = tempDir.resolve("configured-save.dat");

        manager.setSavePath(saveFile.toString());

        assertEquals(saveFile.toString(), manager.getSavePath());
    }

    @Test
    void setSavePath_with_blank_value_should_store_blank_value() {
        GameSaveManager manager = new GameSaveManager();

        manager.setSavePath("");

        assertEquals("", manager.getSavePath());
    }

    @Test
    void saveGame_to_missing_parent_directory_should_throw_runtime_exception() {
        Path saveFile = tempDir.resolve("nested").resolve("deep").resolve("save.dat");
        GameSaveManager manager = new GameSaveManager();
        GameEngine engine = TestGameFactory.createEngine();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> manager.saveGame(engine, saveFile.toString()));

        assertEquals("Failed to save game to: " + saveFile, exception.getMessage());
        assertNotNull(exception.getCause());
        assertInstanceOf(FileNotFoundException.class, exception.getCause());
    }

    private static final class TestUnit extends Unit implements Serializable {
        private TestUnit(String name, int posX, int posY) {
            super(name, 10, 3, 1, posX, posY);
            setAttackRange(1);
        }
    }

    private static final class TestGameFactory {

        private static GameEngine createEngine() {
            GameEngine engine = new GameEngine();
            GameMap map = new GameMap(5, 5);

            Player playerOne = new Player("Player One");
            Player playerTwo = new Player("Player Two");

            TestUnit p1Unit = new TestUnit("P1-A", 0, 0);
            TestUnit p2Unit = new TestUnit("P2-A", 4, 4);

            playerOne.setUnits(new ArrayList<>(List.of(p1Unit)));
            playerTwo.setUnits(new ArrayList<>(List.of(p2Unit)));

            map.placeUnit(p1Unit, 0, 0);
            map.placeUnit(p2Unit, 4, 4);
            map.setTileType(2, 2, TileType.FOREST);

            engine.setMap(map);
            engine.setPlayers(new ArrayList<>(List.of(playerOne, playerTwo)));
            engine.setState(GameState.PLANNING);
            engine.setCurrentRound(3);

            return engine;
        }
    }
}