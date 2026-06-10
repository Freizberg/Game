package GameController;

/**
 * UI input mode used by the graphical layer to interpret the next map click.
 *
 * <p>Action buttons in the unit panel switch the controller into one of these
 * modes; the mouse handler then reads the active mode to decide whether the
 * next click selects a move tile, an attack target, or a heal target.</p>
 */
public enum InputMode {
    /** Default mode: clicks select units or issue a direct move. */
    NONE,
    /** Next map click chooses a destination tile for the selected unit. */
    MOVE,
    /** Next map click chooses an enemy unit to attack. */
    ATTACK,
    /** Next map click chooses a friendly unit to heal. */
    HEAL
}
