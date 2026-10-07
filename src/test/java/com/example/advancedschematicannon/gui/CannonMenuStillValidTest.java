package com.example.advancedschematicannon.gui;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A cannon's screen closes once the cannon is gone, as the user chose on 2026-10-07. Until then the menu's
 * {@code stillValid} asked for a level, the distance and the owner rule only, and a removed block entity keeps its level:
 * a broken cannon's screen stayed open over its emptied handler, and what went in afterwards vanished - measured on the
 * real client the same day (BelugaAOS run 20261007-212633-356aa5: the screen still open 40 ticks after
 * {@code setblock ... air destroy}, 16 emeralds shift-clicked into it, none on the player or the ground once it closed).
 *
 * <p>This repo's unit-test classpath has no Minecraft (see AscScreenCatalogueTest), so the menu cannot be built here.
 * What is held, read from the source: {@code stillValid} is one return of a conjunction that asks for the level first
 * (the wiki's stand-in has none), the distance, whether the level's block entity at the cannon's position is this very
 * one - after the distance, so a player who walked off never has the chunk looked up - and the owner rule; and that menu
 * is the only {@code stillValid} in this module, so a new one is held to the same question before it can pass. Not held
 * here: that the server closes the screen on false and drops clicks on it (vanilla does, and the real client showed
 * it), or that only the server asks.
 */
class CannonMenuStillValidTest {

    /** This module's own package directory under src/main/java: the marker the source root is found by. */
    private static final String OWN = "com/example/advancedschematicannon";
    private static final String MENU = OWN + "/gui/EMCSchematicCannonMenu.java";

    private static final String LEVEL_PRESENT = "blockEntity.getLevel() != null";
    private static final String DISTANCE = "player.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5, "
            + "blockEntity.getBlockPos().getY() + 0.5, blockEntity.getBlockPos().getZ() + 0.5) <= 64.0";
    private static final Pattern DISTANCE_CLAUSE = Pattern.compile("player\\.distanceToSqr\\(.*\\) <= 64\\.0");
    private static final String SAME_BLOCK_ENTITY =
            "blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) == blockEntity";
    private static final String OWNER_RULE = "blockEntity.mayUse(player)";

    private static final String NOT_ONE_RETURN = "stillValid is not one return statement";
    private static final String OR = "an || at the top level";
    private static final String LEVEL_NOT_FIRST = "the level is not asked first";
    private static final String NOT_SAME = "does not ask whether the level's block entity there is this one";
    private static final String NO_DISTANCE = "does not ask the distance";
    private static final String LOOKED_UP_FIRST = "looks the block entity up before the distance";
    private static final String NO_OWNER = "does not ask the owner rule";

    @Test
    @DisplayName("stillValid asks for the level, the distance, this very block entity at its position, and the owner")
    void stillValidAsksWhetherTheCannonStillStands() throws IOException {
        assertNull(problem(stillValidBody(read(MENU))));
    }

    @Test
    @DisplayName("the menu is this module's only stillValid, so a new one cannot pass without this question")
    void theMenuIsTheOnlyStillValid() throws IOException {
        Path root = mainSources();
        List<String> declaring = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                // Once per declaration, so a second stillValid in the menu itself - above the override, where
                // stillValidBody would read it instead - is a second entry too.
                String code = stripComments(Files.readString(f, StandardCharsets.UTF_8));
                for (int at = code.indexOf("boolean stillValid("); at >= 0;
                        at = code.indexOf("boolean stillValid(", at + 1)) {
                    declaring.add(root.relativize(f).toString().replace('\\', '/'));
                }
            }
        }
        assertEquals(List.of(MENU), declaring,
                "a new stillValid: ask whether its block entity is still the level's one there, and hold it here");
    }

    /** One way to break the rule, applied once to the real body of stillValid, and the problem it must be found as. */
    private record Mutant(String what, String needle, String replacement, String foundAs) {
    }

    @Test
    @DisplayName("the scan itself: each way of breaking the real stillValid is found as itself, one mutant per part")
    void eachMutantOfTheRealSourceIsFound() throws IOException {
        String body = stillValidBody(read(MENU));
        List<Mutant> mutants = List.of(
                new Mutant("does not ask whether the cannon still stands", " && " + SAME_BLOCK_ENTITY, "", NOT_SAME),
                new Mutant("asks the opposite", SAME_BLOCK_ENTITY, SAME_BLOCK_ENTITY.replace(" == ", " != "), NOT_SAME),
                new Mutant("any block entity there will do", SAME_BLOCK_ENTITY,
                        SAME_BLOCK_ENTITY.replace("== blockEntity", "!= null"), NOT_SAME),
                new Mutant("joined by ||", " && " + SAME_BLOCK_ENTITY, " || " + SAME_BLOCK_ENTITY, OR),
                new Mutant("looks the block entity up before the distance", DISTANCE + " && " + SAME_BLOCK_ENTITY,
                        SAME_BLOCK_ENTITY + " && " + DISTANCE, LOOKED_UP_FIRST),
                new Mutant("no owner rule", " && " + OWNER_RULE, "", NO_OWNER),
                new Mutant("the level is not asked first", LEVEL_PRESENT + " && ", "", LEVEL_NOT_FIRST),
                new Mutant("no distance", DISTANCE + " && ", "", NO_DISTANCE),
                new Mutant("decided before the return", "return ", "if (" + OWNER_RULE + ") return true; return ",
                        NOT_ONE_RETURN));
        for (Mutant m : mutants) {
            int at = body.indexOf(m.needle());
            String mutated = at < 0 ? body
                    : body.substring(0, at) + m.replacement() + body.substring(at + m.needle().length());
            assertNotEquals(body, mutated, "mutant did not apply: " + m);
            String found = problem(mutated);
            assertTrue(found != null && found.startsWith(m.foundAs()), "mutant " + m + " found as: " + found);
        }
    }

    /** What is wrong with stillValid's body (comments removed, whitespace collapsed), or null when it holds the rule. */
    static String problem(String body) {
        String code = body.trim();
        if (!code.matches("return [^;]+;")) return NOT_ONE_RETURN;
        List<String> conjuncts = split(code.substring("return ".length(), code.length() - 1), " && ");
        for (String c : conjuncts) {
            if (split(c, " || ").size() > 1) return OR + ": '" + c + "' - the whole is no conjunction";
        }
        if (conjuncts.indexOf(LEVEL_PRESENT) != 0) return LEVEL_NOT_FIRST + " - the wiki's stand-in has none";
        int distance = -1;
        for (int i = 0; i < conjuncts.size(); i++) {
            if (DISTANCE_CLAUSE.matcher(conjuncts.get(i)).matches()) distance = i;
        }
        int same = conjuncts.indexOf(SAME_BLOCK_ENTITY);
        if (same < 0) return NOT_SAME + " - a broken cannon's screen stays open";
        if (distance < 0) return NO_DISTANCE;
        if (same < distance) return LOOKED_UP_FIRST + " - a player far off has the chunk looked up";
        if (!conjuncts.contains(OWNER_RULE)) return NO_OWNER + " - a cannon switched private stays open on others";
        return null;
    }

    /** {@code expr} split at {@code op} where no parenthesis is open. */
    static List<String> split(String expr, String op) {
        List<String> out = new ArrayList<>();
        int depth = 0;
        int from = 0;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (depth == 0 && expr.startsWith(op, i)) {
                out.add(expr.substring(from, i).trim());
                from = i + op.length();
                i = from - 1;
            }
        }
        out.add(expr.substring(from).trim());
        return out;
    }

    /** The body of {@code stillValid(Player)} in {@code src}, comments removed and whitespace collapsed. */
    static String stillValidBody(String src) {
        String code = stripComments(src);
        int at = code.indexOf("boolean stillValid(Player");
        assertNotEquals(-1, at, "no stillValid(Player) in " + MENU);
        int open = code.indexOf('{', at);
        int depth = 0;
        int i = open;
        for (; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) break;
        }
        return code.substring(open + 1, i).replaceAll("\\s+", " ");
    }

    private static String read(String file) throws IOException {
        return Files.readString(mainSources().resolve(file), StandardCharsets.UTF_8);
    }

    private static String stripComments(String src) {
        return src.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");
    }

    /** src/main/java, found upwards from the working directory (a test may run from a subdirectory). */
    private static Path mainSources() {
        for (Path base = Paths.get("").toAbsolutePath(); base != null; base = base.getParent()) {
            Path root = base.resolve("src/main/java");
            if (Files.isDirectory(root.resolve(OWN))) return root;
        }
        throw new AssertionError("src/main/java/" + OWN + " not found from " + Paths.get("").toAbsolutePath());
    }
}
