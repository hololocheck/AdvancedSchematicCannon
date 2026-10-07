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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Who owns a cannon, as the user chose it on 2026-10-07 ({@link CannonOwnership}). Until then opening an idle cannon,
 * or starting a job on it, made that player its owner, so anyone could take over a private cannon by opening it.
 *
 * <p>This repo's unit-test classpath has no Minecraft (see AscScreenCatalogueTest), so the block entity, the blocks and
 * CannonData cannot be loaded here: the first half holds the decisions themselves, the second binds the callers to
 * them - every write of the owner in src/main/java goes through CannonOwnership, read from the sources, so the
 * takeover cannot come back as a direct assignment. Not held here: that the callers hand over the right facts (the
 * placer, the op level, the public flag), and what a player sees - the refusal, the screen closing, the face. Only the
 * real client shows those.
 */
class CannonOwnershipTest {

    private static final UUID PLACER = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID OP = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    @Test
    @DisplayName("placing records the placer; a placement by no player leaves the owner as it was")
    void placing() {
        assertEquals(PLACER, CannonOwnership.placed(null, PLACER));
        assertNull(CannonOwnership.placed(null, null), "a fake player's cannon stays unowned");
        assertEquals(PLACER, CannonOwnership.placed(OTHER, PLACER), "the placer wins over an owner the item carried");
        assertEquals(OTHER, CannonOwnership.placed(OTHER, null));
    }

    @Test
    @DisplayName("opening or starting never changes an owner; a cannon nobody owns goes to its first user")
    void using() {
        assertEquals(PLACER, CannonOwnership.used(PLACER, OTHER), "the takeover this fixes");
        assertEquals(PLACER, CannonOwnership.used(PLACER, OP), "an op's start leaves the owner too");
        assertEquals(PLACER, CannonOwnership.used(PLACER, PLACER));
        assertEquals(OTHER, CannonOwnership.used(null, OTHER));
    }

    @Test
    @DisplayName("the owner, ops, and anyone while nobody owns it act as the owner")
    void actingAsOwner() {
        assertTrue(CannonOwnership.actsAsOwner(PLACER, PLACER, false));
        assertFalse(CannonOwnership.actsAsOwner(PLACER, OTHER, false));
        assertTrue(CannonOwnership.actsAsOwner(PLACER, OP, true));
        assertTrue(CannonOwnership.actsAsOwner(null, OTHER, false));
    }

    @Test
    @DisplayName("public: anyone opens it and changes the settings; private: the owner and ops only")
    void openingAndSettings() {
        assertTrue(CannonOwnership.mayUse(PLACER, OTHER, false, true));
        assertFalse(CannonOwnership.mayUse(PLACER, OTHER, false, false));
        assertTrue(CannonOwnership.mayUse(PLACER, PLACER, false, false));
        assertTrue(CannonOwnership.mayUse(PLACER, OP, true, false));
        assertTrue(CannonOwnership.mayUse(null, OTHER, false, false), "nobody owns it: whoever opens it claims it");
    }

    @Test
    @DisplayName("the sequence that took a cannon over: placed, opened by another, started, made private")
    void theTakeoverSequence() {
        UUID owner = CannonOwnership.placed(null, PLACER);
        boolean publicAccess = true;

        owner = CannonOwnership.used(owner, OTHER);
        assertEquals(PLACER, owner, "another player opened it while idle");
        assertTrue(CannonOwnership.mayUse(owner, OTHER, false, publicAccess), "they may change a public one's settings");
        assertFalse(CannonOwnership.actsAsOwner(owner, OTHER, false), "they may not start it or take it private");

        publicAccess = false;
        assertFalse(CannonOwnership.mayUse(owner, OTHER, false, publicAccess), "the owner made it private: shut out");
        assertTrue(CannonOwnership.mayUse(owner, PLACER, false, publicAccess));
        assertTrue(CannonOwnership.mayUse(owner, OP, true, publicAccess));

        owner = CannonOwnership.used(owner, OP);
        assertEquals(PLACER, owner, "an op opened and started it: the job still runs on the owner");
    }

    // ===== every write of the owner goes through CannonOwnership =====

    /** This module's own package directory under src/main/java: the marker the source root is found by. */
    private static final String OWN = "com/example/advancedschematicannon";
    private static final String BLOCK_ENTITY = OWN + "/block/EMCSchematicCannonBlockEntity.java";

    /**
     * The methods whose writes of the owner are not decisions: reading the save, and the wiki's stand-in - a block
     * entity in no level that the wiki screen builds on the client (EMCSchematicCannonScreenV2.wikiCreate).
     */
    private static final List<String> EXEMPT = List.of("void loadAdditional(", "void wikiDemo(");

    /**
     * A write of the field through whatever qualifier - none, {@code this.}, another cannon, an inner class's
     * {@code EMCSchematicCannonBlockEntity.this.}, a cast or a call - and not {@code ==}. Matching the qualifier
     * instead missed {@code X.this.} (first second reading) and then a cast or a call (second), 2026-10-07.
     */
    private static final Pattern WRITE = Pattern.compile("(?<!\\w)ownerUUID\\s*=(?!=)");

    /** src/main/java, found upwards from the working directory (a test may run from a subdirectory). */
    private static Path mainSources() {
        for (Path base = Paths.get("").toAbsolutePath(); base != null; base = base.getParent()) {
            Path root = base.resolve("src/main/java");
            if (Files.isDirectory(root.resolve(OWN))) return root;
        }
        throw new AssertionError("src/main/java/" + OWN + " not found from " + Paths.get("").toAbsolutePath());
    }

    @Test
    @DisplayName("every write of ownerUUID in src/main/java goes through CannonOwnership")
    void everyOwnerWriteGoesThroughTheRule() throws IOException {
        Path root = mainSources();
        List<String> direct = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                for (String w : directWrites(Files.readString(f, StandardCharsets.UTF_8))) {
                    direct.add(root.relativize(f) + ": " + w);
                }
            }
        }
        assertEquals(List.of(), direct, "a write of the owner that is not CannonOwnership's decision");

        // The scan reads something: the three decisions this module makes today (placed, opened, started) are there,
        // and each exempt method still exists - a renamed one would otherwise widen nothing silently, it would just
        // stop matching and its writes would be judged like any other.
        String be = stripComments(Files.readString(root.resolve(BLOCK_ENTITY), StandardCharsets.UTF_8));
        Matcher decided = Pattern.compile("(?<![\\w.])(?:this\\.)?ownerUUID\\s*=\\s*CannonOwnership\\.").matcher(be);
        int decisions = 0;
        while (decided.find()) decisions++;
        assertEquals(3, decisions, "ownerUUID = CannonOwnership.* writes in the block entity");
        for (String header : EXEMPT) {
            assertTrue(be.contains(header), "exempt method still in the block entity: " + header);
        }
    }

    @Test
    @DisplayName("the scan itself: a direct write is found, a decision, a comparison, a declaration and the exempt methods are not")
    void scanSeesDirectWrites() {
        assertEquals(List.of("this.ownerUUID = serverPlayer.getUUID()"),
                directWrites("class A { void createMenu() { this.ownerUUID = serverPlayer.getUUID(); } }"),
                "the takeover as it was");
        assertEquals(List.of("ownerUUID = id"), directWrites("class A { void f(UUID id) { ownerUUID = id; } }"));
        assertEquals(List.of(), directWrites("class A { void f() { ownerUUID = CannonOwnership.used(ownerUUID, p); } }"));
        assertEquals(List.of(), directWrites("class A { void f() { if (ownerUUID == null) return; } }"));
        assertEquals(List.of(), directWrites("class A { private UUID ownerUUID = null; }"), "the declaration");
        assertEquals(List.of("cannon.ownerUUID = id"), directWrites("class A { void f() { cannon.ownerUUID = id; } }"),
                "another cannon's field is an owner too");
        assertEquals(List.of("A.this.ownerUUID = p.getUUID()"), directWrites(
                "class A { Object h = new Object() { void f() { A.this.ownerUUID = p.getUUID(); } }; }"),
                "a write from an inner class (second reading, 2026-10-07)");
        assertEquals(List.of("((A) be).ownerUUID = p.getUUID()", "cannon().ownerUUID = id"), directWrites(
                "class A { void f() { ((A) be).ownerUUID = p.getUUID(); cannon().ownerUUID = id; } }"),
                "a write through a cast or a call");
        assertEquals(List.of(), directWrites("class A { void f() { // ownerUUID = id;\n /* ownerUUID = id; */ } }"));
        assertEquals(List.of(), directWrites(
                "class A { void loadAdditional(CompoundTag tag) { if (tag.hasUUID(\"Owner\")) ownerUUID = tag.getUUID(\"Owner\"); } }"));
        assertEquals(List.of("ownerUUID = id"), directWrites(
                "class A { void wikiDemo(UUID owner) { ownerUUID = owner; } void f(UUID id) { ownerUUID = id; } }"),
                "a write after an exempt method is still judged");
    }

    /** Every write of ownerUUID in {@code src}, outside the exempt methods, whose value is not a CannonOwnership call. */
    static List<String> directWrites(String src) {
        String code = stripComments(src);
        for (String header : EXEMPT) code = blankBody(code, header);
        List<String> out = new ArrayList<>();
        Matcher m = WRITE.matcher(code);
        while (m.find()) {
            // What stands before the field in its statement: a qualifier ("this.", "((A) be)."), nothing, or a type.
            int start = 1 + Math.max(code.lastIndexOf(';', m.start()),
                    Math.max(code.lastIndexOf('{', m.start()), code.lastIndexOf('}', m.start())));
            String before = code.substring(start, m.start()).strip();
            if (before.endsWith("UUID")) continue; // a declaration
            String value = code.substring(m.end(), code.indexOf(';', m.end())).trim();
            if (!value.startsWith("CannonOwnership.")) out.add(before + "ownerUUID = " + value);
        }
        return out;
    }

    private static String stripComments(String src) {
        return src.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");
    }

    /** {@code code} with the body of every method declared as {@code header} blanked out, braces matched. */
    private static String blankBody(String code, String header) {
        StringBuilder sb = new StringBuilder(code);
        int from = 0;
        while (true) {
            int at = sb.indexOf(header, from);
            if (at < 0) return sb.toString();
            int open = sb.indexOf("{", at);
            if (open < 0) return sb.toString();
            int depth = 0;
            int i = open;
            for (; i < sb.length(); i++) {
                char c = sb.charAt(i);
                if (c == '{') depth++;
                else if (c == '}' && --depth == 0) break;
            }
            for (int j = open + 1; j < i; j++) sb.setCharAt(j, ' ');
            from = i;
        }
    }
}
