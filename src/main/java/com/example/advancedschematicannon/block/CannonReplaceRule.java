package com.example.advancedschematicannon.block;

import com.example.advancedschematicannon.block.EMCSchematicCannonBlockEntity.ReplaceMode;

/**
 * Whether the cannon may set a placement's block where another block stands: the decision alone, on flags, so a test
 * holds it without Minecraft - this repo's unit-test classpath has none (see CannonOwnership). The block entity's tick
 * supplies the facts of the two block states and skips the placement when the answer is no.
 *
 * <p>The replace mode is the schematic mode's setting: its four buttons show only in the schematic mode's strip. The
 * filler mode does not use it and always sets its block, whatever stands there (the user's decision of 2026-11-11).
 * Until then the hidden mode applied to the filler too, and with "don't replace" or "replace solid with solid" Erase
 * and Remove skipped every block and still finished - measured on the real client (BelugaAOS run
 * 20261111-052620-7b3916): 18 stone blocks left in the range in all four cases, none set, the job complete. The two
 * switches (skip missing, protect block entities) are not part of this rule: they still apply in both modes.</p>
 */
public final class CannonReplaceRule {

    private CannonReplaceRule() {
    }

    /**
     * A block state's facts the rule reads: air, replaceable (grass, snow layers - {@code canBeReplaced()}), and holding
     * a fluid.
     */
    public record Spot(boolean air, boolean replaceable, boolean fluid) {
        /** Solid as the cannon counts it: not air, not replaceable, no fluid. */
        public boolean solid() {
            return !air && !replaceable && !fluid;
        }
    }

    /**
     * May the cannon set {@code target} where {@code existing} stands. {@code fillerJob}: a filler module generated the
     * job (else a schematic) - the job's kind, fixed when its placements were loaded, never the mode switch, which can
     * be turned while the job runs or is paused.
     */
    public static boolean mayPlace(ReplaceMode mode, boolean fillerJob, Spot target, Spot existing) {
        if (fillerJob) return true;
        return switch (mode) {
            // Air in the schematic is never set: no existing block is broken.
            case DONT_REPLACE -> !target.air() && (existing.air() || existing.replaceable());
            case REPLACE_SOLID -> !target.air()
                    && (existing.air() || existing.replaceable() || existing.fluid()
                        || (existing.solid() && target.solid()));
            case REPLACE_ANY -> true;
            // Air in the schematic removes what stands there; a block goes only into an empty spot.
            case REPLACE_EMPTY -> target.air()
                    ? !existing.air()
                    : existing.air() || existing.replaceable() || existing.fluid();
        };
    }
}
