package com.example.advancedschematicannon.block;

import com.example.advancedschematicannon.block.CannonReplaceRule.Spot;
import com.example.advancedschematicannon.block.EMCSchematicCannonBlockEntity.ReplaceMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The filler mode does not use the schematic mode's replace mode ({@link CannonReplaceRule}, the user's decision of
 * 2026-11-11). Until then it did, while the strip hid the four buttons: with "don't replace" or "replace solid with
 * solid" chosen before switching, Erase and Remove skipped every block and finished - measured on the real client
 * (BelugaAOS run 20261111-052620-7b3916: 18 stone blocks left in all four cases, 18 cleared under "replace any").
 *
 * <p>This repo's unit-test classpath has no Minecraft (see CannonOwnershipTest): the first half calls the rule itself;
 * the second binds the block entity to it, read from the source - the tick hands the rule the kind of the job its
 * placements were loaded for, and only loading sets that kind. Not held here: the facts the tick derives from a
 * BlockState, and what the world shows after a job. The real client shows those (the run named above, and the one
 * after the fix).</p>
 */
class CannonReplaceRuleTest {

    private static final Spot AIR = new Spot(true, false, false);
    private static final Spot GRASS = new Spot(false, true, false);
    private static final Spot WATER = new Spot(false, true, true);
    private static final Spot STILL_WATER_LOGGED = new Spot(false, false, true);
    private static final Spot STONE = new Spot(false, false, false);
    private static final Map<String, Spot> SPOTS = Map.of(
            "air", AIR, "grass", GRASS, "water", WATER, "waterlogged", STILL_WATER_LOGGED, "stone", STONE);

    @Test
    @DisplayName("the filler sets its block whatever the replace mode and whatever stands there")
    void theFillerIgnoresTheReplaceMode() {
        for (ReplaceMode mode : ReplaceMode.values()) {
            for (Map.Entry<String, Spot> existing : SPOTS.entrySet()) {
                // Erase and Remove set air; Fill, Wall, Tower, Box and Circular wall set a block.
                assertTrue(CannonReplaceRule.mayPlace(mode, true, AIR, existing.getValue()),
                        "erase / remove over " + existing.getKey() + " in " + mode);
                assertTrue(CannonReplaceRule.mayPlace(mode, true, STONE, existing.getValue()),
                        "fill over " + existing.getKey() + " in " + mode);
            }
        }
    }

    @Test
    @DisplayName("the measured case: erase over stone with don't-replace or replace-solid now clears it")
    void theMeasuredCase() {
        assertTrue(CannonReplaceRule.mayPlace(ReplaceMode.DONT_REPLACE, true, AIR, STONE));
        assertTrue(CannonReplaceRule.mayPlace(ReplaceMode.REPLACE_SOLID, true, AIR, STONE));
    }

    /**
     * The schematic mode keeps the four modes as the tick's switch had them before 2026-11-11, row by row: mode,
     * target, existing, may place. "waterlogged" is a solid-shaped block holding a fluid - not solid, not replaceable.
     */
    private static final String[] SCHEMATIC = {
            "DONT_REPLACE air air -", "DONT_REPLACE air grass -", "DONT_REPLACE air water -",
            "DONT_REPLACE air waterlogged -", "DONT_REPLACE air stone -",
            "DONT_REPLACE stone air +", "DONT_REPLACE stone grass +", "DONT_REPLACE stone water +",
            "DONT_REPLACE stone waterlogged -", "DONT_REPLACE stone stone -",
            "DONT_REPLACE grass stone -", "DONT_REPLACE grass air +", "DONT_REPLACE waterlogged grass +",

            "REPLACE_SOLID air air -", "REPLACE_SOLID air grass -", "REPLACE_SOLID air water -",
            "REPLACE_SOLID air waterlogged -", "REPLACE_SOLID air stone -",
            "REPLACE_SOLID stone air +", "REPLACE_SOLID stone grass +", "REPLACE_SOLID stone water +",
            "REPLACE_SOLID stone waterlogged +", "REPLACE_SOLID stone stone +",
            "REPLACE_SOLID grass stone -", "REPLACE_SOLID waterlogged stone -", "REPLACE_SOLID grass air +",
            "REPLACE_SOLID grass waterlogged +", "REPLACE_SOLID water stone -",

            "REPLACE_ANY air air +", "REPLACE_ANY air stone +", "REPLACE_ANY stone stone +",
            "REPLACE_ANY grass waterlogged +",

            "REPLACE_EMPTY air air -", "REPLACE_EMPTY air grass +", "REPLACE_EMPTY air water +",
            "REPLACE_EMPTY air waterlogged +", "REPLACE_EMPTY air stone +",
            "REPLACE_EMPTY stone air +", "REPLACE_EMPTY stone grass +", "REPLACE_EMPTY stone water +",
            "REPLACE_EMPTY stone waterlogged +", "REPLACE_EMPTY stone stone -",
            "REPLACE_EMPTY grass air +", "REPLACE_EMPTY grass stone -", "REPLACE_EMPTY water grass +",
    };

    @Test
    @DisplayName("the schematic mode keeps each replace mode's answers")
    void theSchematicModeKeepsItsModes() {
        List<String> wrong = new ArrayList<>();
        for (String row : SCHEMATIC) {
            String[] f = row.split(" ");
            boolean expected = f[3].equals("+");
            boolean actual = CannonReplaceRule.mayPlace(ReplaceMode.valueOf(f[0]), false, SPOTS.get(f[1]), SPOTS.get(f[2]));
            if (actual != expected) wrong.add(row + " (got " + (actual ? "+" : "-") + ")");
        }
        assertEquals(List.of(), wrong);
    }

    /**
     * The one call the tick makes: the cannon's replaceMode and the kind of the job its placements were loaded for
     * (fillerJob) - not fillerMode, the switch, which a player can turn while a job runs or is paused (second reading,
     * 2026-11-11: a schematic job turned to filler mid-way would have overwritten everything, and an Erase turned back
     * to schematic would have skipped the rest - the measured defect).
     */
    private static final Pattern CALL = Pattern.compile(
            "CannonReplaceRule\\.mayPlace\\(\\s*replaceMode\\s*,\\s*fillerJob\\s*,\\s*spot\\(targetState\\)\\s*,\\s*spot\\(existingState\\)\\s*\\)");

    private static String blockEntity() throws IOException {
        return Files.readString(Path.of(
                "src/main/java/com/example/advancedschematicannon/block/EMCSchematicCannonBlockEntity.java"),
                StandardCharsets.UTF_8);
    }

    /** The body of {@code methodHead} in {@code source}, from its head to the next line that starts a member at depth 1. */
    private static String body(String source, String methodHead) {
        int at = source.indexOf(methodHead);
        assertTrue(at >= 0, "no " + methodHead);
        int depth = 0;
        for (int i = source.indexOf('{', at); i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            if (c == '}' && --depth == 0) return source.substring(at, i + 1);
        }
        throw new AssertionError("unbalanced " + methodHead);
    }

    @Test
    @DisplayName("the filler generator loads a filler job, the schematic parser a schematic job, and only loading sets it")
    void theJobKindIsFixedWhenLoaded() throws IOException {
        String source = blockEntity();
        assertTrue(body(source, "private boolean generateFillerPlacements(").contains("loadPlacements(placements, true)"),
                "the filler generator loads its placements as a filler job");
        assertTrue(body(source, "private boolean parseAndLoadSchematic(").contains("loadPlacements(placements, false)"),
                "the schematic parser loads its placements as a schematic job");
        assertTrue(body(source, "public void loadPlacements(").contains("this.fillerJob = fillerJob;"));
        // Saved with the placements, or a paused job reloaded after a mode switch comes back as the other kind.
        assertTrue(body(source, "protected void saveAdditional(").contains("tag.putBoolean(\"FillerJob\", fillerJob);"),
                "the job's kind is saved with its placements");
        // A removal collects what it clears for the job's kind, not for the mode the switch shows now.
        assertTrue(source.contains("if (fillerJob && fillerModule == FillerModule.REMOVE && targetState.isAir())"),
                "the removal's collection follows the job's kind");
        // Anywhere else an assignment would let a mode switch reach a running job again; the NBT read restores it.
        Matcher assign = Pattern.compile("\\bfillerJob\\s*=[^=]").matcher(source);
        List<String> sites = new ArrayList<>();
        while (assign.find()) {
            String line = source.substring(source.lastIndexOf('\n', assign.start()) + 1,
                    source.indexOf('\n', assign.start())).trim();
            sites.add(line);
        }
        assertEquals(List.of("private boolean fillerJob = false;", "this.fillerJob = fillerJob;",
                "fillerJob = tag.contains(\"FillerJob\") ? tag.getBoolean(\"FillerJob\") : fillerMode;"), sites);
    }

    @Test
    @DisplayName("the tick asks the rule with the job's kind, and decides the replace mode nowhere else")
    void theTickAsksTheRule() throws IOException {
        String source = blockEntity();
        Matcher call = CALL.matcher(source);
        int calls = 0;
        while (call.find()) calls++;
        assertEquals(1, calls, "the tick's call to CannonReplaceRule.mayPlace with replaceMode and fillerJob");
        int calledAnywhere = source.split("CannonReplaceRule\\.mayPlace\\(", -1).length - 1;
        assertEquals(1, calledAnywhere, "no second call of the rule with other flags");
        for (ReplaceMode mode : ReplaceMode.values()) {
            // Outside the enum's declaration and the NBT read of old names, a mode named in the block entity is a
            // second decision the filler would not pass through.
            String uses = "case " + mode.name() + " ->";
            assertEquals(-1, source.indexOf(uses), "the block entity decides " + mode + " itself");
        }
    }
}
