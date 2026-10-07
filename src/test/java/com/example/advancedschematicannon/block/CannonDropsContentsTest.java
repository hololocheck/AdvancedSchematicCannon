package com.example.advancedschematicannon.block;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A broken cannon spills what it holds, as the user chose on 2026-10-07. Until then neither cannon block had an
 * {@code onRemove}, so all 67 slots vanished with the block - measured on the real client the same day (BelugaAOS run
 * 20261007-200133-8033cf: 75 items in slots 0, 1, 2, 3 and 66, broken four ways, none on the ground).
 *
 * <p>This repo's unit-test classpath has no Minecraft (see AscScreenCatalogueTest), so the blocks and the block entity
 * cannot be loaded here. What is held, read from the sources: every block whose {@code newBlockEntity} makes a cannon
 * block entity drops its contents in {@code onRemove}, only when the block is replaced by another one, and before
 * {@code super.onRemove} takes the block entity away; {@code dropContents} and {@code clearContent} walk every slot from
 * 0 and empty it; and the block entity is {@code Clearable}, which keeps {@code /clone ... move} from doubling the
 * contents. Not held here: that the items reach the ground, that commands empty a Clearable first, or that a job placing
 * over its own cannon takes nothing twice - vanilla and the real client show those. The scan reads the loops' headers,
 * so a filter inside a loop that skips slots is not seen either.
 */
class CannonDropsContentsTest {

    /** This module's own package directory under src/main/java: the marker the source root is found by. */
    private static final String OWN = "com/example/advancedschematicannon";
    private static final String BLOCKS = OWN + "/block/";
    private static final String BLOCK_ENTITY = BLOCKS + "EMCSchematicCannonBlockEntity.java";
    private static final List<String> KNOWN_BLOCKS = List.of(
            BLOCKS + "EMCSchematicCannonBlock.java", BLOCKS + "EnhancedSchematicCannonBlock.java");

    private static final String EVERY_SLOT = "for (int i = 0; i < itemHandler.getSlots(); i++)";
    private static final String EMPTY_SLOT = "itemHandler.setStackInSlot(i, ItemStack.EMPTY)";
    private static final Pattern DROP_WHEN_REPLACED = Pattern.compile(
            "if \\(!\\w+\\.is\\(\\w+\\.getBlock\\(\\)\\)[^{;]*\\) \\{ \\w+\\.dropContents\\(\\); \\}");
    private static final Pattern CLEARABLE = Pattern.compile(
            "class EMCSchematicCannonBlockEntity extends BlockEntity implements [^{]*\\bClearable\\b");

    @Test
    @DisplayName("every block that makes a cannon block entity drops its contents when it is replaced")
    void everyCannonBlockDropsItsContents() throws IOException {
        Path root = mainSources();
        List<String> cannons = new ArrayList<>();
        List<String> wrong = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                String src = Files.readString(f, StandardCharsets.UTF_8);
                if (!makesACannon(src)) continue;
                String name = root.relativize(f).toString().replace('\\', '/');
                cannons.add(name);
                String problem = blockProblem(src);
                if (problem != null) wrong.add(name + ": " + problem);
            }
        }
        // The scan reads something: both cannon blocks this module has today are among those it found.
        assertTrue(cannons.containsAll(KNOWN_BLOCKS), "cannon blocks found: " + cannons);
        assertEquals(List.of(), wrong);
    }

    @Test
    @DisplayName("the block entity empties every slot, drops what it empties, and is Clearable")
    void theBlockEntityEmptiesEverySlot() throws IOException {
        assertNull(blockEntityProblem(Files.readString(mainSources().resolve(BLOCK_ENTITY), StandardCharsets.UTF_8)));
    }

    /** One way to break the rule, applied to the real source of {@code file}, inside {@code method} when one is named. */
    private record Mutant(String what, String file, String method, String needle, String replacement) {
    }

    @Test
    @DisplayName("the scan itself: each way of breaking the real sources is found, one mutant per part of the rule")
    void eachMutantOfTheRealSourcesIsFound() throws IOException {
        Path root = mainSources();
        String emc = KNOWN_BLOCKS.get(0);
        String enhanced = KNOWN_BLOCKS.get(1);
        List<Mutant> mutants = List.of(
                new Mutant("no onRemove", emc, null, "void onRemove(", "void onRemoveGone("),
                new Mutant("no onRemove", enhanced, null, "void onRemove(", "void onRemoveGone("),
                new Mutant("drops on a facing change too", emc, "void onRemove(",
                        "!state.is(newState.getBlock()) && ", ""),
                new Mutant("drops on a facing change too", enhanced, "void onRemove(",
                        "!state.is(newState.getBlock()) && ", ""),
                new Mutant("drops after super.onRemove took the block entity", emc, "void onRemove(",
                        "cannon.dropContents();\n        }\n        super.onRemove(state, level, pos, newState, movedByPiston);",
                        "}\n        super.onRemove(state, level, pos, newState, movedByPiston);\n        cannon.dropContents();"),
                new Mutant("drops after super.onRemove took the block entity", enhanced, "void onRemove(",
                        "cannon.dropContents();\n        }\n        super.onRemove(state, level, pos, newState, movedByPiston);",
                        "}\n        super.onRemove(state, level, pos, newState, movedByPiston);\n        cannon.dropContents();"),
                new Mutant("drops the filler slots only", BLOCK_ENTITY, "void dropContents()",
                        "int i = 0;", "int i = SLOT_FILLER_START;"),
                new Mutant("drops a copy and leaves the slot full", BLOCK_ENTITY, "void dropContents()",
                        EMPTY_SLOT + ";\n            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);",
                        "Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());"),
                new Mutant("empties without dropping", BLOCK_ENTITY, "void dropContents()",
                        "Containers.dropItemStack(", "java.util.Objects.requireNonNull("),
                new Mutant("clears the filler slots only", BLOCK_ENTITY, "void clearContent()",
                        "int i = 0;", "int i = SLOT_FILLER_START;"),
                new Mutant("clears nothing", BLOCK_ENTITY, "void clearContent()", EMPTY_SLOT, "itemHandler.getSlots()"),
                new Mutant("not Clearable", BLOCK_ENTITY, null, "implements MenuProvider, Clearable",
                        "implements MenuProvider"));
        for (Mutant m : mutants) {
            String src = Files.readString(root.resolve(m.file()), StandardCharsets.UTF_8).replace("\r\n", "\n");
            String mutated = mutate(src, m);
            assertNotEquals(src, mutated, "mutant did not apply: " + m);
            String problem = m.file().equals(BLOCK_ENTITY) ? blockEntityProblem(mutated) : blockProblem(mutated);
            assertNotNull(problem, "mutant not found: " + m);
        }
    }

    /** {@code src} with {@code m.needle()} replaced once - inside {@code m.method()}'s body when it names one. */
    private static String mutate(String src, Mutant m) {
        int from = 0;
        int to = src.length();
        if (m.method() != null) {
            from = src.indexOf(m.method());
            if (from < 0) return src;
            to = closingBrace(src, src.indexOf('{', from));
        }
        int at = src.indexOf(m.needle(), from);
        if (at < 0 || at + m.needle().length() > to) return src;
        return src.substring(0, at) + m.replacement() + src.substring(at + m.needle().length());
    }

    /** Whether {@code src} declares a {@code newBlockEntity} that makes the cannon block entity. */
    static boolean makesACannon(String src) {
        String body = body(src, "newBlockEntity(");
        return body != null && body.contains("new EMCSchematicCannonBlockEntity(");
    }

    /** What is wrong with a cannon block's onRemove, or null when it drops the contents as it should. */
    static String blockProblem(String src) {
        String body = body(src, "void onRemove(");
        if (body == null) return "no onRemove";
        String code = body.replaceAll("\\s+", " ");
        int drop = code.indexOf(".dropContents();");
        int sup = code.indexOf("super.onRemove(");
        if (drop < 0) return "onRemove does not call dropContents";
        if (sup < 0) return "onRemove does not call super.onRemove";
        if (drop > sup) return "dropContents after super.onRemove, which has taken the block entity away";
        if (!DROP_WHEN_REPLACED.matcher(code).find()) {
            return "dropContents is not under if (!state.is(newState.getBlock()) ...) - a facing change would spill it";
        }
        return null;
    }

    /** What is wrong with the block entity, or null when both methods empty every slot and it is Clearable. */
    static String blockEntityProblem(String src) {
        String code = stripComments(src).replaceAll("\\s+", " ");
        if (!CLEARABLE.matcher(code).find()) return "not Clearable: /clone ... move would double the contents";
        String drop = body(src, "void dropContents()");
        String clear = body(src, "void clearContent()");
        if (drop == null) return "no dropContents()";
        if (clear == null) return "no clearContent()";
        drop = drop.replaceAll("\\s+", " ");
        clear = clear.replaceAll("\\s+", " ");
        if (!drop.contains(EVERY_SLOT)) return "dropContents does not walk every slot from 0";
        if (!drop.contains(EMPTY_SLOT)) return "dropContents does not empty the slots it drops";
        if (!drop.contains("Containers.dropItemStack(")) return "dropContents drops nothing";
        if (!clear.contains(EVERY_SLOT)) return "clearContent does not walk every slot from 0";
        if (!clear.contains(EMPTY_SLOT)) return "clearContent does not empty the slots";
        return null;
    }

    /** src/main/java, found upwards from the working directory (a test may run from a subdirectory). */
    private static Path mainSources() {
        for (Path base = Paths.get("").toAbsolutePath(); base != null; base = base.getParent()) {
            Path root = base.resolve("src/main/java");
            if (Files.isDirectory(root.resolve(OWN))) return root;
        }
        throw new AssertionError("src/main/java/" + OWN + " not found from " + Paths.get("").toAbsolutePath());
    }

    private static String stripComments(String src) {
        return src.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");
    }

    /** The body of the first method whose declaration contains {@code header}, comments removed; null if none. */
    static String body(String src, String header) {
        String code = stripComments(src);
        int at = code.indexOf(header);
        if (at < 0) return null;
        int open = code.indexOf('{', at);
        int semi = code.indexOf(';', at);
        if (open < 0 || (semi >= 0 && semi < open)) return null; // a declaration without a body
        return code.substring(open + 1, closingBrace(code, open));
    }

    /** The index of the brace that closes the one at {@code open}. */
    private static int closingBrace(String code, int open) {
        int depth = 0;
        for (int i = open; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return i;
        }
        return code.length();
    }
}
