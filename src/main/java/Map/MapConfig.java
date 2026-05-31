package Map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Static utility class for loading GameMap configurations from text files.
 *
 * <p>Expected file format (text-based grid):</p>
 * <ul>
 * <li>{@code .} -> PLAIN</li>
 * <li>{@code F} -> FOREST</li>
 * <li>{@code O} -> OBSTACLE</li>
 * <li>{@code W} -> WATER</li>
 * <li>{@code M} -> MOUNTAIN</li>
 * </ul>
 * * @author Jan Żurek
 */
public final class MapConfig {

    private MapConfig() {
        throw new UnsupportedOperationException("MapConfig is a utility class and cannot be instantiated.");
    }

    /**
     * Parses a text file from the resources folder and returns a fully loaded GameMap.
     *
     * @param resourcePath the path to the map file (e.g., "/maps/level1.txt")
     * @return a completely initialized GameMap
     * @throws IllegalArgumentException if the file is not found, empty, or has inconsistent dimensions
     * @throws RuntimeException if an I/O error occurs
     */
    public static GameMap loadMap(String resourcePath) {
        List<String> lines = new ArrayList<>();

        // 1. Read the file from resources
        try (InputStream is = MapConfig.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Map configuration file not found: " + resourcePath);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        lines.add(line.trim());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read map configuration file: " + resourcePath, e);
        }

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Map configuration file is empty: " + resourcePath);
        }

        int height = lines.size();
        int width = lines.get(0).length();

        for (String line : lines) {
            if (line.length() != width) {
                throw new IllegalArgumentException("Map file has inconsistent row widths. All rows must be exactly the same length.");
            }
        }

        GameMap map = new GameMap(width, height);

        for (int y = 0; y < height; y++) {
            String row = lines.get(y);
            for (int x = 0; x < width; x++) {
                char c = row.charAt(x);
                TileType type = switch (c) {
                    case '.' -> TileType.PLAIN;
                    case 'F' -> TileType.FOREST;
                    case 'O' -> TileType.OBSTACLE;
                    case 'W' -> TileType.WATER;
                    case 'M' -> TileType.MOUNTAIN;
                    default -> throw new IllegalArgumentException(
                            "Unknown terrain character '" + c + "' at coordinate (x:" + x + ", y:" + y + ")"
                    );
                };

                map.setTileType(x, y, type);
            }
        }

        return map;
    }
}