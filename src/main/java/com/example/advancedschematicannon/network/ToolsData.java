package com.example.advancedschematicannon.network;

import com.example.advancedschematicannon.AdvancedSchematicCannon;
import com.example.advancedschematicannon.ModRegistry;
import com.example.advancedschematicannon.item.AirPlacementWandItem;
import com.example.advancedschematicannon.item.ModDataComponents;
import com.example.advancedschematicannon.item.RangeBoardItem;
import com.manta.api.data.Host;
import com.manta.api.data.MantaData;
import com.manta.api.data.Schema;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.UUID;

/**
 * The held tools' input on manta:data (MANTA_7_CONCEPT C4): what RangeBoardEditPacket and WandDistancePacket
 * carried. Each player has one host that only they reach, opened at login; Manta closes it at logout. Its
 * actions set the held tool's component as the packets did. The schema is a state-only document (the tools
 * have no page).
 */
@EventBusSubscriber(modid = AdvancedSchematicCannon.MOD_ID)
public final class ToolsData {

    static final String DOCUMENT = "/assets/advancedschematicannon/manta/tools-state.json";
    static final String DOC = "tools";

    private static Schema schema;

    private ToolsData() {
    }

    public static synchronized Schema schema() {
        if (schema == null) {
            schema = MantaData.schemaResource(AdvancedSchematicCannon.class, DOCUMENT);
        }
        return schema;
    }

    public static String channel(UUID player) {
        return MantaData.channel(AdvancedSchematicCannon.MOD_ID, DOC, player);
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Host host = MantaData.host(player, AdvancedSchematicCannon.MOD_ID, DOC, schema());
        host.on("range-board-edit", (args, p) -> rangeBoardEdit(p, (Integer) args.get(0)));
        host.on("wand-distance", (args, p) -> wandDistance(p, (Integer) args.get(0)));
    }

    private static void rangeBoardEdit(ServerPlayer player, int mode) {
        ItemStack stack = RangeBoardItem.findHeldRangeBoard(player);
        if (stack.isEmpty()) {
            return;
        }
        if (mode == 0) {
            stack.remove(ModDataComponents.RANGE_EDIT_MODE.get());
        } else {
            stack.set(ModDataComponents.RANGE_EDIT_MODE.get(), mode);
        }
    }

    private static void wandDistance(ServerPlayer player, int distance) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModRegistry.AIR_PLACEMENT_WAND.get())) {
            stack = player.getOffhandItem();
            if (!stack.is(ModRegistry.AIR_PLACEMENT_WAND.get())) {
                return;
            }
        }
        // The schema bounds the param already; the clamp stays so the item's own rule is the last word.
        AirPlacementWandItem.setDistance(stack,
                Mth.clamp(distance, AirPlacementWandItem.MIN_DISTANCE, AirPlacementWandItem.MAX_DISTANCE));
    }
}
