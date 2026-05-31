package GameEngine;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Menedżer konfiguracji wczytujący balans gry z pliku config.properties.
 * Zapewnia wartości domyślne na wypadek braku pliku.
 *
 * @author Cybulski Mikołaj
 */
public class GameConfig {
    private static final Properties props = new Properties();

    static {
        try (InputStream input = GameConfig.class.getResourceAsStream("/config.properties")) {
            if (input != null) {
                props.load(input);
            } else {
                System.err.println("Ostrzeżenie: Nie znaleziono pliku config.properties w resources. Używa balansu domyślnego.");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static int getInt(String key, int defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static int getActionsPerTurn() { return getInt("actions_per_turn", 2); }

    public static int getKnightHp() { return getInt("knight_hp", 50); }
    public static int getKnightRange() { return getInt("knight_range", 1); }
    public static int getKnightArmor() { return getInt("knight_armor", 2); }
    public static int getKnightSpeed() { return getInt("knight_speed", 6); }
    public static int getKnightBaseAttack() { return getInt("knight_base_attack", 10); }

    public static int getArcherHp() { return getInt("archer_hp", 30); }
    public static int getArcherRange() { return getInt("archer_range", 4); }
    public static int getArcherSpeed() { return getInt("archer_speed", 4); }
    public static int getArcherBaseAttack() { return getInt("archer_base_attack", 7); }

    public static int getMageHp() { return getInt("mage_hp", 20); }
    public static int getMageRange() { return getInt("mage_range", 5); }
    public static int getMageSpeed() { return getInt("mage_speed", 3); }
    public static int getMageBaseAttack() { return getInt("mage_base_attack", 20); }
    public static int getMageMaxMana() { return getInt("max_mana", 50); }
    public static int getSpellManaCost() { return getInt("spell_mana_cost", 10); }
    public static int getManaRegenPerRound() { return getInt("mana_regen_per_round", 5); }
    public static int getHealValue() { return getInt("heal_value", 10); }

    public static int getForestCoverReduction() { return getInt("forest_cover_reduction", 1); }
}