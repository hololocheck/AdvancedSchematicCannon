package com.example.advancedschematicannon.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.manta.screen.ResizeGrip;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MANTA_7 U-G1 (user decision 2026-09-28, option (a)): the options strip opens from a button and goes away on the next
 * press - a transient menu, not a window - so the resize grip (BelugaExperience R4.22.2) must not show on it. Manta
 * refuses the grip by the overlay root's classes ({@code ResizeGrip.allowedOnOverlay}); this asks that same method with
 * the layout as it ships. Measured on the real client before the class was added (2026-09-28, manta 2.75.0-dev6): the
 * strip's corner drew the grip, 132 pixels of its colour.
 */
class OptionsStripGripTest {

    private static final Path LAYOUT =
            Path.of("src/main/resources/assets/advancedschematicannon/layouts/emc-schematic-cannon-options.json");

    private static JsonObject root() throws IOException {
        return JsonParser.parseString(Files.readString(LAYOUT, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void theOptionsStripGetsNoGrip() throws IOException {
        assertFalse(ResizeGrip.allowedOnOverlay(root()),
                "the options strip is a transient menu: its root must carry a class Manta reads as one");
    }

    @Test
    void itIsTheMenuClassThatTakesTheGripAway() throws IOException {
        JsonObject root = root();
        JsonArray without = new JsonArray();
        for (JsonElement c : root.getAsJsonArray("classes")) {
            if (!"asc-opt-dropdown-menu".equals(c.getAsString())) {
                without.add(c);
            }
        }
        root.add("classes", without);
        assertTrue(ResizeGrip.allowedOnOverlay(root),
                "without asc-opt-dropdown-menu the strip would read as a window and get the grip");
    }
}
