package com.example.advancedschematicannon.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every class the cannon screen registers an F1 hint for is carried by a node of the cannon's layouts. The engine
 * shows a hint for the class of the node under the pointer, so a class no node carries is a hint nobody ever sees:
 * {@code asc-mode-schematic} and {@code asc-mode-filler} were registered that way (found reading on 2026-11-07,
 * removed 2026-11-11 - the mode button carries its own hint).
 *
 * <p>Read from the sources: the screen class needs Minecraft to load, which this repo's unit-test classpath has none
 * of (see CannonOwnershipTest). Every node is walked, children and repeat templates alike. Not held here: that the
 * node is hovered on screen, and the hint's text.</p>
 */
class AscHintClassesTest {

    private static final Path SCREEN = Path.of(
            "src/main/java/com/example/advancedschematicannon/gui/EMCSchematicCannonScreenV2.java");
    private static final Path LAYOUTS = Path.of("src/main/resources/assets/advancedschematicannon/layouts");
    private static final Pattern REGISTRATION = Pattern.compile("HINTS\\.put\\(\\s*\"([^\"]+)\"");

    static Set<String> registered(String source) {
        Set<String> classes = new LinkedHashSet<>();
        Matcher m = REGISTRATION.matcher(source);
        while (m.find()) classes.add(m.group(1));
        return classes;
    }

    static void collect(JsonElement node, Set<String> into) {
        if (node == null) return;
        if (node.isJsonArray()) {
            for (JsonElement e : node.getAsJsonArray()) collect(e, into);
            return;
        }
        if (!node.isJsonObject()) return;
        JsonObject o = node.getAsJsonObject();
        if (o.has("classes") && o.get("classes").isJsonArray()) {
            for (JsonElement c : o.getAsJsonArray("classes")) into.add(c.getAsString());
        }
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            if (e.getValue().isJsonObject() || e.getValue().isJsonArray()) collect(e.getValue(), into);
        }
    }

    @Test
    @DisplayName("every hinted class is carried by a node of the cannon's layouts")
    void everyHintedClassIsOnSomeNode() throws IOException {
        Set<String> hinted = registered(Files.readString(SCREEN, StandardCharsets.UTF_8));
        // An empty read is not an answer: the screen registers over twenty hints.
        assertTrue(hinted.size() >= 20, "read " + hinted.size() + " registrations from " + SCREEN);

        Set<String> carried = new TreeSet<>();
        List<Path> files = new ArrayList<>();
        try (Stream<Path> s = Files.list(LAYOUTS)) {
            s.filter(p -> p.toString().endsWith(".json")).forEach(files::add);
        }
        assertEquals(2, files.size(), "the cannon's two layouts (main page and option strip): " + files);
        for (Path f : files) {
            collect(JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)), carried);
        }

        List<String> missing = new ArrayList<>();
        for (String c : hinted) if (!carried.contains(c)) missing.add(c);
        assertEquals(List.of(), missing, "hinted classes no layout node carries");
    }

    @Test
    @DisplayName("the walk reaches a repeat's template, and the read finds a registration")
    void theReadersSeeWhatTheyClaim() {
        Set<String> into = new TreeSet<>();
        JsonArray children = new JsonArray();
        JsonObject repeat = JsonParser.parseString(
                "{\"tag\":\"repeat\",\"template\":{\"tag\":\"div\",\"classes\":[\"row-x\"]}}").getAsJsonObject();
        children.add(repeat);
        JsonObject root = new JsonObject();
        root.add("children", children);
        collect(root, into);
        assertEquals(Set.of("row-x"), into);
        assertEquals(Set.of("a-b"), registered("HINTS.put(\"a-b\", \"k\");"));
    }
}
