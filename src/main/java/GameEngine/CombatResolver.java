package GameEngine;

import Action.Action;
import Action.AttackAction;
import Action.HealAction;
import Map.GameMap;
import Map.Tile;
import Units.*;


import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves {@link AttackAction} and {@link HealAction} instances during the
 * RESOLVING phase of a turn.
 *
 * <h3>Simultaneous resolution</h3>
 * <p>All attacks in a single resolution batch are evaluated at the same time:
 * damage values are calculated for every attack before any unit's HP is changed.
 * If two units deliver lethal attacks to each other in the same round, both die.
 * Dead units are removed from the map by {@link GameEngine} only after this class
 * has finished processing all combat actions.</p>
 *
 * <h3>Damage formula</h3>
 * <ul>
 *   <li>Knight : {@code max(0, BASE_ATTACK - target.getArmor() - coverReduction)}</li>
 *   <li>Archer  : {@code max(0, BASE_ATTACK - coverReduction)}</li>
 *   <li>Mage    : {@code max(0, mage.getSpellPower() - coverReduction)}</li>
 * </ul>
 * {@code BASE_ATTACK} is a constant defined per unit class (see inner constants below).
 * {@code coverReduction} is {@value #FOREST_COVER_REDUCTION} when the defending unit
 * stands on a {@link Map.TileType#FOREST} tile, 0 otherwise.
 *
 * <h3>Friendly fire</h3>
 * <p>The engine does not allow attacks targeting units belonging to the same player.
 * Such actions are filtered out in {@link #resolveAll(GameEngine)} before
 * any damage is calculated.</p>
 *
 * @author Dzhyhar Volodymyr
 * @author Żurek Jan
 */
public class CombatResolver implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private List<Action> actions = null;

    /**
     * Flat damage reduction granted to a defender standing on a FOREST tile.
     */


    public CombatResolver(List<Action> actions) {
        this.actions = actions;
    }

    /**
     * Resolves a batch of {@link AttackAction} objects with simultaneous semantics.
     *
     * <p>Damage is calculated for every action in the list before being applied.
     * This ensures that a unit killed by one attack can still deal its own damage.</p>
     *
     * @param e       the authoritative game engine
     */
    public void resolveAll(GameEngine e) {
        Map<Unit, Integer> hpDeltas = new HashMap<>();
        GameMap map = e.getMap();

        for (Action action : actions) {
            if (action instanceof AttackAction attack) {
                Unit attacker = map.findUnit(attack.getAttackerId());
                Unit target = map.findUnit(attack.getTargetId());

                if (attacker != null && target != null && isRangeValid(attacker, target, map)) {
                    if (!isFriendlyFire(attacker, target, e)) {
                        int dmg = calcDamage(attacker, target, map);
                        hpDeltas.merge(target, -dmg, Integer::sum);
                    }
                }
            }
            else if (action instanceof HealAction heal) {
                Unit caster = map.findUnit(heal.getCasterId());
                Unit target = map.findUnit(heal.getTargetId());

                if (caster instanceof Mage mage && target != null && isRangeValid(mage, target, map)) {
                    mage.consumeMana();
                    int amount = GameConfig.getHealValue();
                    hpDeltas.merge(target, amount, Integer::sum);
                }
            }
        }

        hpDeltas.forEach((unit, delta) -> {
            if (delta > 0) {
                unit.heal(delta);
            } else if (delta < 0) {
                unit.applyDamage(Math.abs(delta));
            }
        });
    }

    /**
     * Calculates the raw damage the attacker deals to the target, taking into
     * account unit-specific stats (spell power, armor) and terrain cover.
     *
     * @param attacker the unit performing the attack
     * @param target   the unit receiving the attack
     * @param map      the current game map (used to read the target's tile for cover)
     * @return the calculated damage value (non-negative)
     */
    public int calcDamage(Unit attacker, Unit target, GameMap map) {
        int coverReduction = getCoverReduction(target, map);
        int raw;

        if (attacker instanceof Knight) {
            raw = GameConfig.getKnightBaseAttack() - target.getArmor() - coverReduction;
        } else if (attacker instanceof Archer) {
            raw = GameConfig.getArcherBaseAttack() - target.getArmor() - coverReduction;
        } else if (attacker instanceof Mage mage) {
            raw = GameConfig.getMageBaseAttack() - coverReduction;
        } else {
            // Fallback for unknown unit types
            raw = 0;
        }

        return Math.max(0, raw);
    }

    /**
     * Overload kept for backward compatibility with existing call sites that do
     * not pass a map. Cover reduction is not applied when the map is unavailable.
     *
     * @param attacker the attacking unit
     * @param target   the defending unit
     * @return damage without terrain modifier
     */
    public int calcDamage(Unit attacker, Unit target) {
        return calcDamage(attacker, target, null);
    }

    /**
     * Applies the given amount of damage to the target unit.
     *
     * @param target the unit to damage
     * @param dmg    the amount of damage to apply (non-negative)
     */
    public void applyDamage(Unit target, int dmg) {
        target.applyDamage(dmg);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Returns the cover damage reduction for the defending unit based on the
     * tile it currently occupies.
     *
     * @param target the defending unit
     * @param map    the game map, or {@code null} if unavailable
     * @return {GameConfig.getForestCoverReduction()} if target is on a FOREST tile, else 0
     */
    private int getCoverReduction(Unit target, GameMap map) {
        if (map == null) return 0;
        Tile tile = map.getTile(target.getPosX(), target.getPosY());
        if (tile == null) return 0;
        return tile.providesCover() ? GameConfig.getForestCoverReduction() : 0;
    }

    private boolean isRangeValid(Unit source, Unit target, GameMap map) {
        if (source == null || target == null || map == null) {
            return false;
        }

        int distance = map.getDistance(source.getPosX(), source.getPosY(), target.getPosX(), target.getPosY());
        int range = source.getAttackRange();

        if (distance > range) {
            return false;
        }

        return range <= 1 || map.hasLineOfSight(source.getPosX(), source.getPosY(), target.getPosX(), target.getPosY());
    }

    private boolean isFriendlyFire(Unit attacker, Unit target, GameEngine e) {
        for (Player p : e.getPlayers()) {
            if (p.isPlayersUnit(attacker.getId())) {
                return p.isPlayersUnit(target.getId());
            }
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Getters/setters
    // -----------------------------------------------------------------------

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }
}
