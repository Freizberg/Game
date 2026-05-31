package Map;

/**
 * Terrain types that determine tile behaviour.
 *
 * <ul>
 *   <li>{@link #PLAIN}    – open ground; no movement or combat modifiers.</li>
 *   <li>{@link #FOREST}   – passable; defending unit receives –1 flat damage reduction (cover).</li>
 *   <li>{@link #OBSTACLE} – impassable; blocks movement and line-of-sight.</li>
 *   <li>{@link #WATER}    – impassable for all unit types in the base ruleset.</li>
 *   <li>{@link #MOUNTAIN} – impassable; blocks movement and line-of-sight.
 *                           Visually distinct from {@code OBSTACLE} for rendering purposes.</li>
 * </ul>
 *
 * @author Dzhyhar Volodymyr
 */
public enum TileType {
    PLAIN,
    FOREST,
    OBSTACLE,
    WATER,
    MOUNTAIN
}
