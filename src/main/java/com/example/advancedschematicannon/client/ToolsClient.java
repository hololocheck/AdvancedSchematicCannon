package com.example.advancedschematicannon.client;

import com.example.advancedschematicannon.AdvancedSchematicCannon;
import com.example.advancedschematicannon.network.ToolsData;
import com.manta.api.data.Mirror;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * The client end of the player's tools host (see {@link ToolsData}): open while the client is in a world,
 * so the HUD and the wand renderer can send their input as actions.
 */
@EventBusSubscriber(modid = AdvancedSchematicCannon.MOD_ID, value = Dist.CLIENT)
public final class ToolsClient {

    private static Mirror mirror;

    private ToolsClient() {
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (mirror != null) {
            mirror.close();
        }
        mirror = Mirror.open(ToolsData.channel(event.getPlayer().getUUID()), ToolsData.schema());
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        if (mirror != null) {
            mirror.close();
            mirror = null;
        }
    }

    /** A tool action, when the client is in a world; before that there is nobody to send it to. */
    static void send(String action, Object... args) {
        if (mirror != null) {
            mirror.send(action, args);
        }
    }
}
