package com.example.advancedschematicannon.client;

import com.example.advancedschematicannon.AdvancedSchematicCannon;
import com.example.advancedschematicannon.ModRegistry;
import com.example.advancedschematicannon.item.ModDataComponents;
import belugalab.tsu.api.SmoothFollow;
import com.example.advancedschematicannon.item.RangeBoardItem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 範囲指定ボードのシアン枠をワールドに描画する。
 * Pos1のみ設定済みの場合、Pos2はプレイヤーの視線先に動的に追従する（なめらかな補間付き）。
 */
@EventBusSubscriber(modid = AdvancedSchematicCannon.MOD_ID, value = Dist.CLIENT)
public class RangeRenderer {

    private static final double MAX_LOOK_DISTANCE = 64.0;

    /**
     * 視線追従のスムージング。自前の {@code smoothX/Y/Z + updateSmooth()} を
     * {@link SmoothFollow} に置き換えた — 同 class は TSU の
     * StationRangeRenderer / TrainPresetRangeRenderer にあった<b>同一コードの重複を集約</b>
     * したもので、ASC のこれは 3 つめの複製だった。挙動は完全互換
     * (LERP_SPEED 12 / dt clamp 0.1s / 未初期化時は即スナップ)。
     */
    private static final SmoothFollow SMOOTH = new SmoothFollow();

    /** プレビュー表示中の概略図砲の位置を登録（BlockEntityRendererから呼ばれる） */
    private static final java.util.Map<BlockPos, long[]> previewRanges = new java.util.concurrent.ConcurrentHashMap<>();

    public static void updatePreviewRange(BlockPos cannonPos, BlockPos p1, BlockPos p2) {
        previewRanges.put(cannonPos.immutable(), new long[]{
                p1.asLong(), p2.asLong()
        });
    }

    public static void removePreviewRange(BlockPos cannonPos) {
        previewRanges.remove(cannonPos.immutable());
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        ItemStack mainHand = mc.player.getMainHandItem();
        ItemStack offHand = mc.player.getOffhandItem();

        if (mainHand.is(ModRegistry.RANGE_BOARD_ITEM.get())) {
            renderRangeBoardOutline(mainHand, event.getPoseStack(), camera, bufferSource);
        }
        if (offHand.is(ModRegistry.RANGE_BOARD_ITEM.get())) {
            renderRangeBoardOutline(offHand, event.getPoseStack(), camera, bufferSource);
        }

        // プレビュー表示: 概略図砲のpreviewVisibleがONの場合、範囲枠を描画
        renderCannonPreviewFrames(event.getPoseStack(), camera, bufferSource);

        bufferSource.endBatch();
    }

    private static void renderRangeBoardOutline(ItemStack stack, PoseStack poseStack, Vec3 camera,
                                                 MultiBufferSource bufferSource) {
        if (stack.isEmpty()) return;

        BlockPos pos1 = stack.get(ModDataComponents.RANGE_POS1.get());
        BlockPos pos2 = stack.get(ModDataComponents.RANGE_POS2.get());
        int editMode = RangeBoardItem.getEditMode(stack);

        // 編集モード: 選択中の点を視線先に追従させる
        if (editMode == 1) {
            // Pos1を編集中: Pos1を視線先に追従
            BlockPos lookTarget = getLookTargetPos();
            if (lookTarget == null) {
                // 視線先にブロックがない場合、Pos2のみ確定なら何も表示しない
                if (pos2 != null) return;
                return;
            }
            if (pos2 != null) {
                // Pos2が確定済み: 視線先(Pos1)～Pos2の範囲をなめらかに表示
                BlockPos smoothPos = SMOOTH.update(lookTarget);
                renderBox(poseStack, camera, bufferSource, smoothPos, pos2, 0.0f, 1.0f, 1.0f, 0.3f);
            } else {
                // Pos2も未設定: 1マスボックス
                SMOOTH.reset();
                renderBox(poseStack, camera, bufferSource, lookTarget, lookTarget, 0.0f, 1.0f, 1.0f, 0.4f);
            }
            return;
        }
        if (editMode == 2) {
            // Pos2を編集中: Pos2を視線先に追従
            BlockPos lookTarget = getLookTargetPos();
            if (lookTarget == null) {
                if (pos1 != null) return;
                return;
            }
            if (pos1 != null) {
                // Pos1が確定済み: Pos1～視線先(Pos2)の範囲をなめらかに表示
                BlockPos smoothPos = SMOOTH.update(lookTarget);
                renderBox(poseStack, camera, bufferSource, pos1, smoothPos, 0.0f, 1.0f, 1.0f, 0.3f);
            } else {
                // Pos1も未設定: 1マスボックス
                SMOOTH.reset();
                renderBox(poseStack, camera, bufferSource, lookTarget, lookTarget, 0.0f, 1.0f, 1.0f, 0.4f);
            }
            return;
        }

        // --- 通常モード (editMode == 0) ---

        // 両方未設定: 視線先に1マスのシアンボックスを表示
        if (pos1 == null && pos2 == null) {
            SMOOTH.reset();
            BlockPos lookTarget = getLookTargetPos();
            if (lookTarget == null) return;
            renderBox(poseStack, camera, bufferSource, lookTarget, lookTarget, 0.0f, 1.0f, 1.0f, 0.4f);
            return;
        }

        // Pos1のみ設定済み: Pos2を視線先になめらかに追従
        if (pos1 != null && pos2 == null) {
            BlockPos lookTarget = getLookTargetPos();
            if (lookTarget == null) return;
            BlockPos smoothPos = SMOOTH.update(lookTarget);
            renderBox(poseStack, camera, bufferSource, pos1, smoothPos, 0.0f, 1.0f, 1.0f, 0.3f);
            return;
        }

        if (pos1 == null) return;

        // 両方設定済みの場合は補間リセット
        SMOOTH.reset();
        renderBox(poseStack, camera, bufferSource, pos1, pos2, 0.0f, 1.0f, 1.0f, 0.5f);
    }

    /**
     * 遠距離レイキャストでプレイヤーの視線先のブロック位置を取得する。
     * mc.hitResultはバニラの到達距離のみなので、level.clip()で64ブロック先まで検出。
     * ブロックが見つからない場合（空を見ている等）はnullを返す。
     */
    private static BlockPos getLookTargetPos() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;

        Vec3 eye = mc.player.getEyePosition(1.0f);
        Vec3 look = mc.player.getLookAngle();
        Vec3 end = eye.add(look.scale(MAX_LOOK_DISTANCE));
        BlockHitResult hitResult = mc.level.clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            return hitResult.getBlockPos();
        }
        return null;
    }

    /**
     * 概略図砲のプレビュー表示: previewVisibleがONの場合、範囲枠を描画。
     * BlockEntityRendererから登録されたプレビュー範囲を使用。
     */
    private static void renderCannonPreviewFrames(PoseStack poseStack, Vec3 camera,
                                                    MultiBufferSource bufferSource) {
        for (var entry : previewRanges.entrySet()) {
            long[] range = entry.getValue();
            BlockPos p1 = BlockPos.of(range[0]);
            BlockPos p2 = BlockPos.of(range[1]);
            renderBox(poseStack, camera, bufferSource, p1, p2, 0.0f, 1.0f, 1.0f, 0.5f);
        }
    }

    private static void renderBox(PoseStack poseStack, Vec3 camera, MultiBufferSource bufferSource,
                                   BlockPos p1, BlockPos p2, float r, float g, float b, float a) {
        double minX = Math.min(p1.getX(), p2.getX());
        double minY = Math.min(p1.getY(), p2.getY());
        double minZ = Math.min(p1.getZ(), p2.getZ());
        double maxX = Math.max(p1.getX(), p2.getX()) + 1;
        double maxY = Math.max(p1.getY(), p2.getY()) + 1;
        double maxZ = Math.max(p1.getZ(), p2.getZ()) + 1;

        AABB rangeAabb = new AABB(
                minX - camera.x, minY - camera.y, minZ - camera.z,
                maxX - camera.x, maxY - camera.y, maxZ - camera.z);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, consumer, rangeAabb, r, g, b, a);
    }
}
