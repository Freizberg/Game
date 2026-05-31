package Units;

import java.io.Serial;

import GameEngine.GameConfig;
import Map.Tile;

/**
 * A spell-casting unit powered by mana.
 *
 * <p>{@code mana} tracks the current available casting resource, while
 * {@code maxMana} defines the upper limit that can be held at one time.</p>
 *
 *   The action is invalid if current mana is below this threshold.</li>
 *   <li>At the end of every resolved round {@code GameEngine.resolveRound()} calls
 *      with the per-round regen amount (default: {@code 5}).</li>
 * </ul>
 *
 * @author Dzhyhar Volodymyr
 * @author Pacek Mateusz
 * @author Żurek Jan
 */
public class Mage extends Unit {

    @Serial
    private static final long serialVersionUID = 1L;

    private int mana;
    private final int maxMana;

    private static final int ATTACK_RANGE = 5;

    /**
     * Constructs a Mage with the given stats.
     * Mana is initialised to {@code maxMana}.
     *
     * @param name           display name
     * @param posX           starting column position
     * @param posY           starting row position
     */
    public Mage(String name, int posX, int posY) {
        super(name, GameConfig.getMageHp(), GameConfig.getMageSpeed(), GameConfig.getActionsPerTurn(), posX, posY);
        this.maxMana = GameConfig.getMageMaxMana();
        this.mana = maxMana;
        this.setAttackRange(GameConfig.getMageRange());
    }

    /**
     * Reduces the Mage's mana by {@code amount}, clamped to 0.
     * Called by {@code CombatResolver} after each successful spell cast.
     *
     * @param amount mana to consume (must be non-negative)
     */
    public void consumeMana(int amount) {
        if (amount < 0) return;
        mana = Math.max(0, mana - amount);
    }

    /**
     * Restores the Mage's mana by {@code amount}, clamped to {@link #maxMana}.
     * Called by {@code GameEngine.resolveRound()} during per-round regeneration.
     *
     * @param amount mana to restore (must be non-negative)
     */
    public void restoreMana(int amount) {
        if (amount < 0) return;
        mana = Math.min(maxMana, mana + amount);
    }

    /**
     * Returns whether this Mage has enough mana to cast a spell.
     *
     * @return {@code true} if {@code mana} is sufficient to cast a spell.
     */
    public boolean canCast() {
        return mana >= GameEngine.GameConfig.getSpellManaCost();
    }

    /**
     * Returns the current amount of mana this Mage has available for casting.
     *
     * @return current mana (between 0 and {@link #getMaxMana()} inclusive)
     */
    public int getMana() {
        return this.mana;
    }

    /**
     * Returns the maximum mana capacity of this Mage.
     * @return maximum mana points
     */
    public int getMaxMana() {
        return this.maxMana;
    }

    /**
     * Deducts spell mana cost from this Mage after casting a spell.
     * Should be called by {@code CombatResolver} when resolving an
     * {@code AttackAction} or {@code HealAction}.
     *
     * @throws IllegalStateException if current mana is below the spell cost
     */
    public void consumeMana() {
        int cost = GameEngine.GameConfig.getSpellManaCost();
        if (mana < cost) {
            throw new IllegalStateException("Not enough mana to cast a spell.");
        }
        mana -= cost;
    }
}
