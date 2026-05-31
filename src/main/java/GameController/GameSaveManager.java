package GameController;

import GameEngine.GameEngine;

import java.io.*;

/**
 * Handles persistence of {@link GameEngine} state to disk.
 *
 * <p>Serializes the entire engine (and the object graph it owns) to a binary
 * file and deserializes it back on load, allowing a game session to be paused
 * and resumed at any point.</p>
 *
 * @author Dzhyhar Volodymyr
 * @author Yevhenii Marienko
 */
public class GameSaveManager {
    /** The default file-system path used when no explicit path is supplied. */
    private String savePath;

    public GameSaveManager() {
    }

    public GameSaveManager(String standardPath) {
        this.savePath = standardPath;
    }

    /**
     * Serializes the given {@link GameEngine} and writes it to the default path.
     *
     * @param engine the game engine whose state should be saved
     * @throws IllegalStateException if the default save path is not configured
     */
    public void saveGame(GameEngine engine) {
        if (savePath == null || savePath.isBlank()) {
            throw new IllegalStateException("Default save path is not configured.");
        }
        saveGame(engine, savePath);
    }

    /**
     * Serializes the given {@link GameEngine} and writes it to the specified path.
     *
     * @param engine the game engine whose state should be saved
     * @param path the file-system path to write the save file to
     */
    public void saveGame(GameEngine engine, String path) {
        if (engine == null) {
            throw new IllegalArgumentException("GameEngine cannot be null.");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Save path cannot be blank.");
        }

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path))) {
            out.writeObject(engine);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save game to: " + path, e);
        }
    }

    /**
     * Loads a {@link GameEngine} from the default save path.
     *
     * @return the restored {@link GameEngine}
     * @throws IllegalStateException if the default save path is not configured
     */
    public GameEngine loadGame() {
        if (savePath == null || savePath.isBlank()) {
            throw new IllegalStateException("Default save path is not configured.");
        }
        return loadGame(savePath);
    }

    /**
     * Loads a {@link GameEngine} from the specified save file.
     *
     * @param path the file-system path of the save file to load
     * @return the restored {@link GameEngine}
     */
    public GameEngine loadGame(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Load path cannot be blank.");
        }

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(path))) {
            return (GameEngine) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to load game from: " + path, e);
        }
    }

    /**
     * Updates the default save path used by parameterless save/load methods.
     *
     * @param savePath the new default save path
     */
    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    /**
     * Returns the currently configured default save path.
     *
     * @return default save path, or {@code null} if not configured
     */
    public String getSavePath() {
        return savePath;
    }
}