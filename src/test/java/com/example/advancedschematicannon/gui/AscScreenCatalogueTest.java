package com.example.advancedschematicannon.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The screen table BelugaAOS's UI sweep reads by reflection ({@code belugalab.aos.manta.Catalogue}): it finds
 * {@code com.example.advancedschematicannon.gui.AscScreenCatalogue} by name, calls its static {@code table()}, and reads each
 * row through the accessors {@code id}, {@code factory}, {@code apply} and {@code states}. Nothing in this mod calls the
 * table, and the driver treats a provider it cannot read as one with no catalogue - the sweep simply has fewer rows, which
 * the C2 gate cannot notice because it counts against the same catalogue - so the names are held here, as the STRINGS the
 * driver uses: an IDE rename rewrites a call, not a string (second reading, 2026-09-27).
 *
 * <p>This mod's unit-test classpath has no Minecraft: initialising the catalogue class fails on
 * {@code net.minecraft.world.level.Level}, and so does listing its methods (the factory and the state lambda carry
 * Minecraft types) - both measured 2026-09-27. So neither the table nor the name {@code table} is held here: BelugaAOS's
 * {@code ui catalogue} reports this provider as {@code no table} when the method is gone (spec v24), and its UI sweep does
 * not pass while a provider has lost rows. The rows and the screens they build are exercised there, on the real client.
 */
class AscScreenCatalogueTest {

    @Test
    void theClassIsWhereTheDriverLooksForIt() {
        assertEquals("com.example.advancedschematicannon.gui.AscScreenCatalogue", AscScreenCatalogue.class.getName());
    }

    @Test
    void everyRowHasTheAccessorsTheDriverReads() throws ReflectiveOperationException {
        for (String name : List.of("id", "factory", "apply", "states")) {
            Method m = AscScreenCatalogue.Entry.class.getDeclaredMethod(name);
            assertTrue(m.getParameterCount() == 0, name + " is an accessor");
        }
        assertEquals(String[].class, AscScreenCatalogue.Entry.class.getDeclaredMethod("states").getReturnType());
    }
}
