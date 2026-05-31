package GameEngine;

import Action.Action;
import Action.AttackAction;
import Action.HealAction;
import Map.GameMap;
import Map.TileType;
import Units.Archer;
import Units.Knight;
import Units.Mage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import GameEngine.GameConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CombatResolverTest {
    private GameEngine engine;
    private GameMap map;
    private Player player1;
    private Player player2;
    private Knight knight;
    private Archer archer;
    private Mage mage;

    @BeforeEach
    void setUp() {
        engine = new GameEngine();
        // Standard 10x10 map for testing
        map = new GameMap(10, 10);
        engine.setMap(map);

        player1 = new Player("Player 1");
        player2 = new Player("Player 2");

        // Initializing units: Name, maxHp, speed, armor, x, y, maxMana
        knight = new Knight("Knight", 1, 1);
        archer = new Archer("Archer",2, 1);
        mage = new Mage("Mage", 1, 2);

        player1.setUnits(new ArrayList<>(Arrays.asList(knight, mage)));
        player2.setUnits(new ArrayList<>(Arrays.asList(archer)));
        engine.setPlayers(new ArrayList<>(Arrays.asList(player1, player2)));

        map.placeUnit(knight, 1, 1);
        map.placeUnit(archer, 2, 1);
        map.placeUnit(mage, 1, 2);
    }

    @Test
    void testSimultaneousCombatResolution() {
        // Set HP low enough that both would die if damage is applied instantly
        knight.applyDamage(49); // 1 HP left
        archer.applyDamage(29); // 1 HP left

        // Create cross-attack actions
        List<Action> actions = new ArrayList<>();
        actions.add(new AttackAction(knight.getId(), archer.getId()));
        actions.add(new AttackAction(archer.getId(), knight.getId()));

        CombatResolver resolver = new CombatResolver(actions);
        resolver.resolveAll(engine);

        // Under simultaneous rules, both units should manage to deal damage before dying
        assertTrue(knight.getHp() <= 0, "Knight should be dead");
        assertTrue(archer.getHp() <= 0, "Archer should be dead");
    }

    @Test
    void testForestCoverReduction() {
        // Force a tile to be a forest[cite: 1]
        map.getTile(2, 1).setType(TileType.FOREST);

        List<Action> actions = new ArrayList<>();
        actions.add(new AttackAction(knight.getId(), archer.getId()));

        CombatResolver resolver = new CombatResolver(actions);

        // Base Knight attack (10) - Target Armor (2) - Forest Reduction (1) = 7[cite: 1]
        int expectedDamage = GameConfig.getKnightBaseAttack() - archer.getArmor() - GameConfig.getForestCoverReduction();
        int hpBefore = archer.getHp();

        resolver.resolveAll(engine);

        assertEquals(hpBefore - expectedDamage, archer.getHp(), "Damage should be reduced by forest cover");
    }

    @Test
    void testFriendlyFireFiltering() {
        List<Action> actions = new ArrayList<>();
        actions.add(new AttackAction(mage.getId(), knight.getId()));

        CombatResolver resolver = new CombatResolver(actions);
        int hpBefore = knight.getHp();

        resolver.resolveAll(engine);

        assertEquals(hpBefore, knight.getHp(), "Friendly fire should be filtered out and deal no damage");
    }

    @Test
    void testMageHealing() {
        knight.applyDamage(10);
        int hpAfterDamage = knight.getHp();

        List<Action> actions = new ArrayList<>();
        actions.add(new HealAction(mage.getId(), knight.getId()));

        CombatResolver resolver = new CombatResolver(actions);
        resolver.resolveAll(engine);

        assertTrue(knight.getHp() > hpAfterDamage, "Knight should have been healed by the Mage");
        assertTrue(mage.getMana() < 50, "Mage should have consumed mana for healing");
    }

    @Test
    void testRangeValidation() {
        map.placeUnit(archer, 9, 9);

        List<Action> actions = new ArrayList<>();
        actions.add(new AttackAction(knight.getId(), archer.getId()));

        CombatResolver resolver = new CombatResolver(actions);
        int hpBefore = archer.getHp();

        resolver.resolveAll(engine);

        assertEquals(hpBefore, archer.getHp(), "Attack should fail because target is out of range");
    }

    @Test
    void testFocusFireDamageStacking() {
        Knight target = new Knight("Target",3, 3);
        map.placeUnit(target, 3, 3);
        map.placeUnit(knight, 3, 4);

        List<Action> actions = new ArrayList<>();
        actions.add(new AttackAction(knight.getId(), target.getId()));
        actions.add(new AttackAction(archer.getId(), target.getId()));
        actions.add(new AttackAction(mage.getId(), target.getId()));

        CombatResolver resolver = new CombatResolver(actions);
        resolver.resolveAll(engine);

        assertTrue(target.getHp() < 80);
    }
}