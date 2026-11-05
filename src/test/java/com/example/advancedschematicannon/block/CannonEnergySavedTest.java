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
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FE another mod gives an idle cannon is saved with it (2026-11-06). NeoForge's EnergyStorage changes its count without
 * telling the block entity, and the cannon marks itself to be saved only while it places blocks or its slots change - so
 * FE pushed in through a cable, or pulled from AE2, reached the disk only when something else marked the cannon's chunk.
 * Measured on the real client before the fix (BelugaAOS run 20261106-010519-0845bc): an idle cannon charged to 100000
 * through a cable, and one through AE2, read 0 after the world was reopened; one whose chunk got another block kept
 * 100000.
 *
 * <p>This repo's unit-test classpath has no Minecraft (see CannonDropsContentsTest), so the storage cannot be loaded
 * here. What is held, read from the sources: the cannon's storage overrides receiveEnergy, calls the inherited one, and
 * calls the cannon's setChanged() exactly when a receive that was not simulated took FE, and no setChanged() override of
 * the block entity's drops the inherited one; the FE capability hands out that storage and the AE2 pull receives through
 * it; and every FE storage this module declares is one this test knows, so a new one is decided here rather than found on
 * a reload. Not held here: that setChanged() reaches the chunk and the disk - the real client shows that (run
 * 20261106-022422-85fe52, after the fix: a cannon charged through a cable and one through AE2 read 100000 after the
 * world was reopened).</p>
 */
class CannonEnergySavedTest {

    /** This module's own package directory under src/main/java: the marker the source root is found by. */
    private static final String OWN = "com/example/advancedschematicannon";
    private static final String BLOCK_ENTITY = OWN + "/block/EMCSchematicCannonBlockEntity.java";
    private static final String CAPABILITIES = OWN + "/ModCapabilities.java";
    private static final String WAND = OWN + "/item/WandEnergyStorage.java";

    /** Every FE storage this module declares ("file class"), and how what it receives is kept. */
    private static final Map<String, String> KNOWN = Map.of(
            BLOCK_ENTITY + " InternalEnergyStorage", "marks the cannon to be saved (this test)",
            WAND + " WandEnergyStorage", "an item's: written into the wand's data component, saved with the stack");

    private static final Pattern STORAGE = Pattern.compile(
            "((?:\\b(?:private|protected|public|static|final|abstract)\\s+)*)class InternalEnergyStorage extends EnergyStorage\\s*\\{");
    private static final Pattern RECEIVE = Pattern.compile("public int receiveEnergy\\(int (\\w+), boolean (\\w+)\\)\\s*\\{");
    /** A type whose supertypes name an FE storage: NeoForge's, or one of this module's (a name ending EnergyStorage). */
    private static final Pattern DECLARED = Pattern.compile(
            "\\b(?:class|record|enum|interface) (\\w+)\\b[^{;]*\\b(?:extends|implements)\\b[^{;]*\\b\\w*EnergyStorage\\b[^{;]*\\{");
    /** One of NeoForge's storages made in place, or an anonymous subclass of any FE storage. */
    private static final Pattern MADE = Pattern.compile("\\bnew\\s+(?:[\\w.]+\\.)?"
            + "(?:\\w*EnergyStorage\\s*\\([^;{]*\\)\\s*\\{|(?:IEnergyStorage|EnergyStorage|ComponentEnergyStorage)\\s*\\()");
    /** A setChanged() override: the storage's bare setChanged() call resolves to it. */
    private static final Pattern MARK_OVERRIDE = Pattern.compile("\\bvoid setChanged\\s*\\(\\s*\\)\\s*\\{");
    /** The block entity extends BlockEntity itself, so no class between them can override setChanged() unseen. */
    private static final Pattern DIRECT_BASE = Pattern.compile("\\bclass EMCSchematicCannonBlockEntity extends BlockEntity\\b");

    @Test
    @DisplayName("the cannon's FE storage marks the cannon to be saved when it receives FE")
    void theStorageMarksTheCannon() throws IOException {
        assertNull(storageProblem(read(BLOCK_ENTITY)));
    }

    @Test
    @DisplayName("the FE capability hands out that storage and the AE2 pull receives through it")
    void theCapabilityAndThePullGoThroughIt() throws IOException {
        assertNull(wiringProblem(read(BLOCK_ENTITY), read(CAPABILITIES)));
    }

    @Test
    @DisplayName("every FE storage this module declares is one this test knows")
    void everyStorageIsKnown() throws IOException {
        assertEquals(List.of(), storageListProblems(sources()));
    }

    /** One way to break the rule, applied to the real source of {@code file}, inside {@code method} when one is named. */
    private record Mutant(String what, String file, String method, String needle, String replacement) {
    }

    @Test
    @DisplayName("the scan itself: each way of breaking the real sources is found, one mutant per part of the rule")
    void eachMutantOfTheRealSourcesIsFound() throws IOException {
        String receive = "public int receiveEnergy(int toReceive, boolean simulate)";
        String pull = "private void pullEnergyFromAE2()";
        String field = "private final InternalEnergyStorage energyStorage = new InternalEnergyStorage(MAX_ENERGY);";
        String getter = "public EnergyStorage getEnergyStorage() { return energyStorage; }";
        List<Mutant> storage = List.of(
                new Mutant("no receive override", BLOCK_ENTITY, null, receive, receive.replace("receiveEnergy", "receiveGone")),
                new Mutant("marks nothing", BLOCK_ENTITY, receive, "setChanged();", ""),
                new Mutant("marks a simulated receive too", BLOCK_ENTITY, receive, "got > 0 && !simulate", "got > 0"),
                new Mutant("marks a receive that took nothing", BLOCK_ENTITY, receive, "got > 0 && !simulate", "!simulate"),
                new Mutant("marks only simulated receives", BLOCK_ENTITY, receive, "!simulate", "simulate"),
                new Mutant("stores nothing", BLOCK_ENTITY, receive, "super.receiveEnergy(toReceive, simulate)", "toReceive"),
                new Mutant("answers that it took nothing", BLOCK_ENTITY, receive, "return got;", "return 0;"),
                new Mutant("a static storage", BLOCK_ENTITY, null, "private final class InternalEnergyStorage",
                        "private static final class InternalEnergyStorage"),
                new Mutant("a setChanged() of its own", BLOCK_ENTITY, null, "super(capacity, capacity, 0, 0);\n        }\n",
                        "super(capacity, capacity, 0, 0);\n        }\n        void setChanged() {\n        }\n"),
                new Mutant("the block entity's setChanged() marks nothing", BLOCK_ENTITY, null, getter,
                        "@Override\n    public void setChanged() {\n        syncToClient();\n    }\n    " + getter),
                new Mutant("the block entity extends a class between", BLOCK_ENTITY, null,
                        "class EMCSchematicCannonBlockEntity extends BlockEntity",
                        "class EMCSchematicCannonBlockEntity extends CannonBase"),
                new Mutant("the storage before 2026-11-06", BLOCK_ENTITY, null,
                        "private final class InternalEnergyStorage extends EnergyStorage {\n"
                                + "        InternalEnergyStorage(int capacity) {\n"
                                + "            super(capacity, capacity, 0, 0);\n"
                                + "        }\n"
                                + "        @Override\n"
                                + "        " + receive + " {\n"
                                + "            int got = super.receiveEnergy(toReceive, simulate);\n"
                                + "            if (got > 0 && !simulate) {\n"
                                + "                setChanged();\n"
                                + "            }\n"
                                + "            return got;\n"
                                + "        }\n",
                        "private static final class InternalEnergyStorage extends EnergyStorage {\n"
                                + "        InternalEnergyStorage(int capacity) {\n"
                                + "            super(capacity, capacity, 0, 0);\n"
                                + "        }\n"));
        List<Mutant> wiring = List.of(
                new Mutant("the field is another storage", BLOCK_ENTITY, null, field,
                        field.replace("(MAX_ENERGY);", "(MAX_ENERGY) { };")),
                new Mutant("the getter hands out another storage", BLOCK_ENTITY, null, getter,
                        getter.replace("return energyStorage;", "return new EnergyStorage(MAX_ENERGY);")),
                new Mutant("the capability hands out another storage", CAPABILITIES, null,
                        "(be, direction) -> be.getEnergyStorage()",
                        "(be, direction) -> new net.neoforged.neoforge.energy.EnergyStorage(1)"),
                new Mutant("the AE2 pull writes around the storage", BLOCK_ENTITY, pull,
                        "energyStorage.receiveEnergy(received, false);",
                        "energyStorage.setStoredInternal(energyStorage.getEnergyStored() + received);"),
                new Mutant("the AE2 pull also writes around the storage", BLOCK_ENTITY, pull,
                        "energyStorage.receiveEnergy(received, false);\n            }",
                        "energyStorage.receiveEnergy(received, false);\n            } else {\n"
                                + "                energyStorage.setStoredInternal(MAX_ENERGY);\n            }"));
        List<Mutant> list = List.of(
                new Mutant("a new storage class", WAND, null, "public class WandEnergyStorage",
                        "class SpareStorage extends EnergyStorage {\n    SpareStorage() { super(1); }\n}\n\n"
                                + "public class WandEnergyStorage"),
                new Mutant("a new storage record", WAND, null, "public class WandEnergyStorage",
                        "record SpareRecord(int stored) implements IEnergyStorage {\n}\n\npublic class WandEnergyStorage"),
                new Mutant("a storage made in place", BLOCK_ENTITY, null, field,
                        "private final EnergyStorage spare = new EnergyStorage(1);\n    " + field),
                new Mutant("a subclass of the cannon's storage", BLOCK_ENTITY, null, field,
                        "private final class SpareStorage extends InternalEnergyStorage {\n"
                                + "        SpareStorage() { super(1); }\n    }\n    " + field),
                new Mutant("an anonymous storage in a second field", BLOCK_ENTITY, null, field,
                        "private final InternalEnergyStorage spare = new InternalEnergyStorage(1) { };\n    " + field),
                new Mutant("a known storage the scan no longer sees", WAND, null, "implements IEnergyStorage",
                        "implements java.util.function.IntSupplier"));
        Map<String, String> sources = sources();
        for (Mutant m : storage) {
            assertNotNull(storageProblem(mutate(sources, m).get(m.file())), "mutant not found: " + m.what());
        }
        for (Mutant m : wiring) {
            Map<String, String> mutated = mutate(sources, m);
            assertNotNull(wiringProblem(mutated.get(BLOCK_ENTITY), mutated.get(CAPABILITIES)),
                    "mutant not found: " + m.what());
        }
        for (Mutant m : list) {
            assertNotEquals(List.of(), storageListProblems(mutate(sources, m)), "mutant not found: " + m.what());
        }
        Mutant callsSuper = new Mutant("the block entity's setChanged() calls super's", BLOCK_ENTITY, null, getter,
                "@Override\n    public void setChanged() {\n        super.setChanged();\n        syncToClient();\n    }\n    "
                        + getter);
        assertNull(storageProblem(mutate(sources, callsSuper).get(BLOCK_ENTITY)),
                "an override that calls super.setChanged() still reaches the chunk");
    }

    /** What is wrong with the cannon's FE storage, or null when what it receives marks the cannon to be saved. */
    static String storageProblem(String src) {
        String code = stripComments(src);
        Matcher declared = STORAGE.matcher(code);
        if (!declared.find()) return "no InternalEnergyStorage extends EnergyStorage";
        if (declared.group(1).contains("static")) {
            return "InternalEnergyStorage is static: the setChanged() it would call is not the cannon's";
        }
        int open = declared.end() - 1;
        String storage = code.substring(open + 1, closingBrace(code, open));
        if (Pattern.compile("\\bvoid setChanged\\s*\\(").matcher(storage).find()) {
            return "InternalEnergyStorage declares a setChanged() of its own, which marks nothing";
        }
        Matcher receive = RECEIVE.matcher(storage);
        if (!receive.find()) return "no receiveEnergy override: EnergyStorage's own receive marks nothing";
        int bodyOpen = receive.end() - 1;
        String body = storage.substring(bodyOpen + 1, closingBrace(storage, bodyOpen)).replaceAll("\\s+", " ").trim();
        String amount = Pattern.quote(receive.group(1));
        String simulate = Pattern.quote(receive.group(2));
        Pattern marks = Pattern.compile("int (\\w+) = super\\.receiveEnergy\\(" + amount + ", " + simulate + "\\); "
                + "if \\((?:\\1 > 0 && !" + simulate + "|!" + simulate + " && \\1 > 0)\\) \\{ setChanged\\(\\); \\} "
                + "return \\1;");
        if (!marks.matcher(body).matches()) {
            return "receiveEnergy does not call setChanged() exactly when a receive that was not simulated took FE: "
                    + body;
        }
        if (!DIRECT_BASE.matcher(code).find()) {
            return "the block entity does not extend BlockEntity itself: a class between could drop setChanged()";
        }
        Matcher override = MARK_OVERRIDE.matcher(code);
        while (override.find()) {
            int at = override.end() - 1;
            if (!code.substring(at + 1, closingBrace(code, at)).contains("super.setChanged();")) {
                return "the block entity overrides setChanged() without super.setChanged(): the storage's mark reaches nothing";
            }
        }
        return null;
    }

    /** What is wrong between the storage, the FE capability and the AE2 pull, or null when both go through it. */
    static String wiringProblem(String blockEntity, String capabilities) {
        String code = stripComments(blockEntity).replaceAll("\\s+", " ");
        if (!code.contains("private final InternalEnergyStorage energyStorage = new InternalEnergyStorage(MAX_ENERGY);")) {
            return "the cannon's energyStorage is not a plain InternalEnergyStorage";
        }
        if (!code.contains("public EnergyStorage getEnergyStorage() { return energyStorage; }")) {
            return "getEnergyStorage() does not hand out energyStorage";
        }
        String registered = stripComments(capabilities).replaceAll("\\s+", " ");
        if (!registered.contains(
                "Capabilities.EnergyStorage.BLOCK, ModRegistry.EMC_CANNON_BE.get(), (be, direction) -> be.getEnergyStorage() )")) {
            return "the cannon's FE capability does not hand out getEnergyStorage()";
        }
        String pull = body(blockEntity, "private void pullEnergyFromAE2()");
        if (pull == null) return "no pullEnergyFromAE2()";
        pull = pull.replaceAll("\\s+", " ");
        if (!pull.contains("energyStorage.receiveEnergy(received, false);")) {
            return "the AE2 pull does not receive through energyStorage.receiveEnergy";
        }
        // The only other write the block entity can make: EnergyStorage's own field is protected, out of the pull's reach.
        if (pull.contains("setStoredInternal(")) {
            return "the AE2 pull writes the stored FE around receiveEnergy";
        }
        return null;
    }

    /** The FE storages {@code sources} declares or makes that this test does not know, and the known ones not found. */
    static List<String> storageListProblems(Map<String, String> sources) {
        TreeSet<String> found = new TreeSet<>();
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, String> e : sources.entrySet()) {
            String code = stripComments(e.getValue());
            Matcher declared = DECLARED.matcher(code);
            while (declared.find()) found.add(e.getKey() + " " + declared.group(1));
            Matcher made = MADE.matcher(code);
            while (made.find()) {
                problems.add(e.getKey() + ": a storage made in place (" + made.group().trim()
                        + "...) - decide how what it receives is saved, and declare it");
            }
        }
        for (String name : found) {
            if (!KNOWN.containsKey(name)) {
                problems.add(name + ": an FE storage this test does not know - decide how what another mod gives it is"
                        + " saved, then add it to KNOWN");
            }
        }
        for (String name : KNOWN.keySet()) {
            if (!found.contains(name)) problems.add(name + ": a known FE storage the scan did not find");
        }
        return problems;
    }

    /** {@code sources} with {@code m.needle()} replaced once in {@code m.file()} - inside {@code m.method()} if named. */
    private static Map<String, String> mutate(Map<String, String> sources, Mutant m) {
        String src = sources.get(m.file());
        int from = 0;
        int to = src.length();
        if (m.method() != null) {
            from = src.indexOf(m.method());
            assertNotEquals(-1, from, "mutant's method not found: " + m.what());
            to = closingBrace(src, src.indexOf('{', from));
        }
        int at = src.indexOf(m.needle(), from);
        assertNotEquals(-1, at, "mutant did not apply: " + m.what());
        assertTrue(at + m.needle().length() <= to, "mutant did not apply inside its method: " + m.what());
        Map<String, String> out = new TreeMap<>(sources);
        out.put(m.file(), src.substring(0, at) + m.replacement() + src.substring(at + m.needle().length()));
        return out;
    }

    /** Every .java file under src/main/java, by its path from there, line ends as LF. */
    private static Map<String, String> sources() throws IOException {
        Path root = mainSources();
        Map<String, String> out = new TreeMap<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                out.put(root.relativize(f).toString().replace('\\', '/'),
                        Files.readString(f, StandardCharsets.UTF_8).replace("\r\n", "\n"));
            }
        }
        return out;
    }

    private static String read(String file) throws IOException {
        return Files.readString(mainSources().resolve(file), StandardCharsets.UTF_8).replace("\r\n", "\n");
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
