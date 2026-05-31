package Units;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

import Map.GameMap;
import Map.Tile;

/**
 * Abstract base class for all units in the game.
 * Holds core combat and positioning state shared by every unit type.
 *
 * @author Dzhyhar Volodymyr
 * @author Żurek Jan
 * @author Pacek Mateusz
 */
public abstract class Unit implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String name;
    private int hp;
    private final int maxHp;
    private final int speed;
    private final int actionsPerTurn;
    private int usedActions;
    private int posX;
    private int posY;
    private int attackRange;

    /**
     * Constructs a unit with the given base stats.
     *
     * @param name           display name of the unit
     * @param maxHp          maximum (and starting) hit points
     * @param speed          number of tiles the unit can move per action
     * @param actionsPerTurn total actions available each turn
     * @param posX           starting column position on the map
     * @param posY           starting row position on the map
     */
    protected Unit(String name, int maxHp, int speed, int actionsPerTurn, int posX, int posY) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Unit name cannot be blank.");
        }
        if (maxHp <= 0) {
            throw new IllegalArgumentException("Max HP must be positive.");
        }
        if (speed < 0) {
            throw new IllegalArgumentException("Speed cannot be negative.");
        }
        if (actionsPerTurn <= 0) {
            throw new IllegalArgumentException("Actions per turn must be positive.");
        }

        this.id = UUID.randomUUID();
        this.name = name;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.speed = speed;
        this.actionsPerTurn = actionsPerTurn;
        this.usedActions = 0;
        this.posX = posX;
        this.posY = posY;
        this.attackRange = 0;
    }

    /**
     * Moves this unit to the given destination tile.
     * Implementations should update the unit's position and handle tile state.
     *
     * @param dest the tile to move to
     */
    public void applyMove(Tile dest, GameMap map){
        int x = dest.getX();
        int y = dest.getY();
        map.placeUnit(this,x,y);
    }

    /**
     * Backward-compatible move helper used by older unit tests operating on standalone tiles.
     *
     * @param dest destination tile
     */
    public void applyMove(Tile dest) {
        if (dest == null) {
            return;
        }

        dest.setUnit(this);
        if (dest.getX() >= 0 && dest.getY() >= 0) {
            setPos(dest.getX(), dest.getY());
        }
    }

    /**
     * Reduces this unit's HP by the given amount, with a minimum of 0.
     *
     * @param dmg the amount of damage to apply
     */
    public void applyDamage(int dmg) {
        if (dmg < 0) {
            return;
        }
        this.hp = Math.max(0, this.hp - dmg);
    }

    public void heal(int hp) {
        if (hp < 0) {
            throw new IllegalArgumentException("Healing cannot be negative.");
        }
        this.hp = Math.min(maxHp, this.hp + hp);
    }

    /**
     * Returns whether this unit is still alive.
     *
     * @return {@code true} if the unit has at least 1 HP
     */
    public boolean isAlive() {
        return this.hp > 0;
    }

    /**
     * Returns the number of actions this unit can still take this turn.
     *
     * @return remaining action count
     */
    public int getRemainingActions() {
        return actionsPerTurn - usedActions;
    }

    /**
     * Consumes one action for this turn.
     * Should be called each time the unit performs an action.
     */
    public void useAction() {
        usedActions = Math.min(usedActions + 1, actionsPerTurn);
    }

    /**
     * Resets used actions at the start of a new turn.
     */
    public void resetActions() {
        usedActions = 0;
    }

    /**
     * Sets the unit's position on the map.
     *
     * @param posX the column index
     * @param posY the row index
     */
    public void setPos(int posX, int posY) {
        this.posX = posX;
        this.posY = posY;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getSpeed() {
        return speed;
    }

    public int getActionsPerTurn() {
        return actionsPerTurn;
    }

    public int getUsedActions()    { return usedActions; }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    public int getArmor() {return 0;}

    public void setHp(int newHP) {
        if (newHP < 0) {
            throw new IllegalArgumentException("HP cannot be negative.");
        }
        this.hp = newHP;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }

    public int getAttackRange() {return this.attackRange;}

    public void setAttackRange(int attackRange) {
        if (attackRange < 0){
            throw new IllegalArgumentException("Attack range cannot be negative.");
        }
        this.attackRange=attackRange;
    }
}
