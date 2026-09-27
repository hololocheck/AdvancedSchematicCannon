package com.example.advancedschematicannon.gui;

import com.example.advancedschematicannon.ModRegistry;
import com.example.advancedschematicannon.block.EMCSchematicCannonBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * This mod's screens and the states they can be put in, for BelugaAOS's UI sweep — nothing in this mod reads it.
 *
 * <p>BelugaAOS reads {@link #table()} by reflection (its {@code Catalogue}, accessors id / factory / apply / states) and
 * SHOWS each screen on a world of its own run, then reads it: text fit, the engine's hit map, the close. The showing,
 * and the containment it needs, live there and never here: the factory builds the cannon's menu with containerId 0
 * over a throwaway block entity, and a server accepts containerId 0 against the player's own inventory. Until
 * 2026-09-27 the sweep's catalogue was TSU's alone, so no ASC screen had been driven by the machine (BelugaAOS
 * improvement log 8f1675).
 */
final class AscScreenCatalogue {

    private AscScreenCatalogue() {}

    /** One screen: its id, how to build it, how to put it in a state, and its states (TSU's catalogue shape). */
    record Entry(String id, Supplier<Screen> factory, BiConsumer<Screen, String> apply, String... states) {}

    /** The catalogue, in the order a driver walks it. */
    static List<Entry> table() {
        return List.of(
            new Entry("emc-schematic-cannon", AscScreenCatalogue::cannon,
                    (s, st) -> ((EMCSchematicCannonScreenV2) s).wikiApplyState(st), "main", "options")
        );
    }

    /**
     * The cannon's screen over a block entity that exists only on this client, where the player stands — the shape of
     * {@link EMCSchematicCannonMenu#fromNetwork}'s fallback before the real one arrives, including its block when
     * ProjectE is absent. Null without a player or a level (the driver refuses a null rather than showing it).
     */
    private static Screen cannon() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return null;
        }
        Block block = ModRegistry.EMC_CANNON_BLOCK != null
                ? ModRegistry.EMC_CANNON_BLOCK.get()
                : ModRegistry.ENHANCED_CANNON_BLOCK.get();
        EMCSchematicCannonBlockEntity be =
                new EMCSchematicCannonBlockEntity(mc.player.blockPosition(), block.defaultBlockState());
        be.setLevel(mc.level);
        EMCSchematicCannonMenu menu = new EMCSchematicCannonMenu(0, mc.player.getInventory(), be);
        return new EMCSchematicCannonScreenV2(menu, mc.player.getInventory(), be.getDisplayName());
    }
}
