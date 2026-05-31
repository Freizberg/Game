package Units;

import java.io.Serial;
import GameEngine.GameConfig;

/**
 * A heavily armoured melee unit.
 *
 * @author Dzhyhar Volodymyr
 * @author Żurek Jan
 */
public class Knight extends Unit {

    @Serial
    private static final long serialVersionUID = 1L;
    private int armor;


    /**
     * Constructs a Knight with the given base stats and armour value.
     *
     * @param name           display name of the unit
     * @param posX           starting column position on the map
     * @param posY           starting row position on the map
     */
    // Usuń: private static final int ATTACK_RANGE = 1;

    public Knight(String name, int posX, int posY) {
        super(name, GameConfig.getKnightHp(), GameConfig.getKnightSpeed(), GameConfig.getActionsPerTurn(), posX, posY);
        this.armor = GameConfig.getKnightArmor();
        this.setAttackRange(GameConfig.getKnightRange());
    }

    /**
     * Returns the armour value of this Knight.
     * Armour reduces incoming damage during combat resolution.
     *
     * @return flat damage reduction value
     */
    @Override
    public int getArmor() {
        return armor;
    }
}
