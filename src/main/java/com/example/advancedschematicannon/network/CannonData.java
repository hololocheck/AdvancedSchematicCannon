package com.example.advancedschematicannon.network;

import com.example.advancedschematicannon.AdvancedSchematicCannon;
import com.example.advancedschematicannon.block.EMCSchematicCannonBlockEntity;
import com.manta.api.data.Host;
import com.manta.api.data.MantaData;
import com.manta.api.data.Schema;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The cannon's actions on manta:data (MANTA_7_CONCEPT C4): what CannonActionPacket and CannonSettingsPacket
 * carried, declared once in the cannon layout's {@code state} member. The screen sends them through a
 * {@code Mirror}; the block entity's {@link Host} admits a player within 8 blocks of this cannon who may use it (the
 * packets' distance check, now the host's {@code canView}) and applies them with the owner rules of
 * {@link com.example.advancedschematicannon.block.CannonOwnership}.
 */
public final class CannonData {

    /** The layout that declares the actions, read from the jar on both sides (the same bytes, the same hash). */
    static final String LAYOUT = "/assets/advancedschematicannon/layouts/emc-schematic-cannon.json";

    private static Schema schema;

    private CannonData() {
    }

    public static synchronized Schema schema() {
        if (schema == null) {
            schema = MantaData.schemaResource(AdvancedSchematicCannon.class, LAYOUT);
        }
        return schema;
    }

    public static String channel(Level level, BlockPos pos) {
        return MantaData.channel(AdvancedSchematicCannon.MOD_ID, "cannon", level.dimension(), pos);
    }

    /**
     * The cannon's host, on the server thread. Closed by the block entity with itself. A private cannon's state and
     * actions reach its owner and ops only, as its screen does: the host asks again on every action and every
     * fan-out, so whoever had it open when it went private drops off.
     */
    public static Host open(EMCSchematicCannonBlockEntity cannon, ServerLevel level) {
        BlockPos pos = cannon.getBlockPos();
        Host host = MantaData.host(level.getServer(), channel(level, pos), schema(),
                player -> player.level() == level
                        && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0
                        && level.getBlockEntity(pos) == cannon
                        && cannon.mayUse(player));
        host.on("cannon-action", (args, player) -> action(cannon, player, (String) args.get(0)));
        host.on("cannon-settings", (args, player) -> settings(cannon, player, args));
        return host;
    }

    private static void action(EMCSchematicCannonBlockEntity cannon, ServerPlayer player, String action) {
        // Starting, pausing and stopping are the owner's and the ops', public or not: the job runs on the owner's EMC.
        if (!cannon.actsAsOwner(player)) {
            cannon.refuse(player, "message.advancedschematicannon.cannon_owner_only");
            return;
        }
        switch (action) {
            case "START" -> cannon.startPlacement(player);
            case "PAUSE" -> cannon.pausePlacement();
            case "RESUME" -> cannon.resumePlacement();
            case "STOP" -> cannon.stopPlacement();
            default -> { }
        }
    }

    private static void settings(EMCSchematicCannonBlockEntity cannon, ServerPlayer player, List<Object> args) {
        // Public access lets anyone change the settings; private leaves them to the owner and ops.
        if (!cannon.mayUse(player)) {
            return;
        }
        boolean owner = cannon.actsAsOwner(player);
        int replaceMode = (Integer) args.get(0);
        int storageMode = (Integer) args.get(1);
        boolean skipMissing = (Boolean) args.get(2);
        boolean skipTileEntities = (Boolean) args.get(3);
        boolean useEmc = (Boolean) args.get(4);
        int blocksPerTick = (Integer) args.get(5);
        boolean reuseSchematic = (Boolean) args.get(6);
        boolean fillerMode = (Boolean) args.get(7);
        boolean previewVisible = (Boolean) args.get(8);
        boolean publicAccess = (Boolean) args.get(9);
        int fillerModule = (Integer) args.get(10);
        // **Only the owner and ops change the public flag itself** - applied like the other settings, a third party
        // could take a public cannon private and shut out everyone its owner had let in.
        if (owner) {
            cannon.setPublicAccess(publicAccess);
        }
        EMCSchematicCannonBlockEntity.ReplaceMode[] modes = EMCSchematicCannonBlockEntity.ReplaceMode.values();
        if (replaceMode < modes.length) {
            cannon.setReplaceMode(modes[replaceMode]);
        }
        EMCSchematicCannonBlockEntity.StorageMode[] storages = EMCSchematicCannonBlockEntity.StorageMode.values();
        if (storageMode < storages.length) {
            cannon.setStorageMode(storages[storageMode]);
        }
        cannon.setSkipMissing(skipMissing);
        cannon.setSkipTileEntities(skipTileEntities);
        cannon.setUseEmc(useEmc);
        cannon.setBlocksPerTick(blocksPerTick);
        cannon.setReuseSchematic(reuseSchematic);
        cannon.setFillerMode(fillerMode);
        cannon.setPreviewVisible(previewVisible);
        EMCSchematicCannonBlockEntity.FillerModule[] modules = EMCSchematicCannonBlockEntity.FillerModule.values();
        if (fillerModule < modules.length) {
            cannon.setFillerModule(modules[fillerModule]);
        }
    }
}
