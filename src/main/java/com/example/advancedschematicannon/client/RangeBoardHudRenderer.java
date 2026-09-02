package com.example.advancedschematicannon.client;

import com.manta.api.render.Icons;
import com.manta.api.hud.HudAnimState;
import com.manta.api.hud.HudChrome;
import com.manta.api.hud.HudConstants;
import com.manta.api.hud.HudText;
import com.manta.api.hud.ScrollCooldown;
import com.example.advancedschematicannon.AdvancedSchematicCannon;
import com.example.advancedschematicannon.item.ModDataComponents;
import com.example.advancedschematicannon.item.RangeBoardItem;
import com.example.advancedschematicannon.network.RangeBoardEditPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 範囲指定ボードの HUD (MantaUI)。
 *
 * <p>Mode 0: 通常範囲指定 — 2 点クリックで範囲設定、Shift+右クリックで全解除。
 * <p>Mode 1: 編集モード — Shift+スクロールで点を選択、Shift+右クリックで選択した点のみ解除。
 *
 * <h2>旧実装からの変更</h2>
 * <ul>
 *   <li>PNG パネル + PNG モード icon 2 枚 → {@link HudChrome} の二層角丸 +
 *       registry icon ({@link Icons}) (R2.4.1 / R4.23.1)。</li>
 *   <li>自前の {@code altProgress} 補間 → {@link HudAnimState} (R2.3.3)。
 *       入退場比 300/250ms は {@link HudConstants} 由来 (R2.2.1 / R2.2.2)。</li>
 *   <li>bar 幅 192 → {@link HudConstants#BADGE_W} = 280 (R2.2.1)。<b>これに伴い説明文の
 *       マーキー送りを廃止し {@link HudText#ellipsize} に変えた</b> — 幅が 1.46 倍になって
 *       ほぼ収まるようになったため。溢れる場合は末尾省略になる。</li>
 *   <li>{@code hideGui} (F1) 判定を追加 (R2.3.1)。<b>旧実装には無く、F1 で HUD を消しても
 *       このバーだけ残っていた。</b></li>
 *   <li>ホイールに {@link ScrollCooldown} を挿入 (R3.4.1)。旧実装は cooldown 無しで、
 *       1 ノッチで複数回 fire しうる形だった。</li>
 * </ul>
 */
@EventBusSubscriber(modid = AdvancedSchematicCannon.MOD_ID, value = Dist.CLIENT)
public class RangeBoardHudRenderer {

    private static final int BAR_W = HudConstants.BADGE_W;
    private static final int BAR_H = 32;
    private static final int ICON = 10;
    private static final int ICON_LIFT = 4;
    private static final int MODE_COUNT = 2;
    /** 1 行ラベル用の box 高さ。drawCenteredLabel は {@code y + (h - 9) / 2} で縦中央に置く。 */
    private static final int LABEL_H = 9;

    /** 二層角丸の外枠色 = tool mode のアクセント (R2.5.1: 色は switch で分岐させる)。 */
    private static final int BG_ARGB = 0xE01A1A2E;

    private static final String[] MODE_ICONS = {
            "manta:square-dashed",   // 通常範囲指定
            "manta:pencil",          // 編集
    };
    private static final String[] MODE_KEYS = {
            "message.advancedschematicannon.mode_normal",
            "message.advancedschematicannon.mode_edit",
    };
    private static final String[] DESC_KEYS = {
            "message.advancedschematicannon.mode_normal_desc",
            "message.advancedschematicannon.mode_edit_desc",
    };

    // -------- client-side state --------
    public static int currentMode = 0;
    /** 編集モード時の編集対象: 0=なし, 1=Pos1, 2=Pos2 */
    public static int editTarget = 0;

    private static final HudAnimState BAR_ANIM = HudAnimState.defaults();
    private static final HudAnimState ALT_ANIM = HudAnimState.defaults();
    private static final ScrollCooldown SCROLL = new ScrollCooldown();

    // -------- rendering --------

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // R2.3.1 / R2.3.2: F1 中と screen が開いているときは描かない。
        if (mc.options != null && mc.options.hideGui) return;
        if (mc.screen != null) return;

        ItemStack stack = RangeBoardItem.findHeldRangeBoard(mc.player);
        boolean held = !stack.isEmpty();

        // R2.3.3: 条件分岐で skip せず毎フレーム update する (state machine が壊れる)。
        BAR_ANIM.update(held);
        ALT_ANIM.update(held && Screen.hasAltDown());

        if (!BAR_ANIM.shouldRender()) return;   // R2.3.4

        GuiGraphics gui = event.getGuiGraphics();
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        int barX = (sw - BAR_W) / 2;
        int altVisibleY = sh - 22 - 4 - BAR_H;
        int barY = altVisibleY - 4 - BAR_H;

        float barFade = BAR_ANIM.fade();

        // -- alt panel (下からスライドイン) --
        if (ALT_ANIM.shouldRender()) {
            float p = ALT_ANIM.isVisibleNow() ? ALT_ANIM.entryEased() : 1f - ALT_ANIM.exitEased();
            int hiddenY = sh + BAR_H;
            int panelY = Math.round(hiddenY + (altVisibleY - hiddenY) * p);
            float altFade = ALT_ANIM.fade();

            HudChrome.drawRoundedRect(gui, barX, panelY, BAR_W, BAR_H,
                    HudChrome.fadeAlpha(BG_ARGB, altFade),
                    HudChrome.fadeAlpha(accentArgb(), altFade));

            String desc = Component.translatable(DESC_KEYS[currentMode]).getString();
            HudChrome.drawCenteredLabel(gui, font,
                    HudText.ellipsize(font, desc, BAR_W - 12),
                    barX, panelY, BAR_W, BAR_H,
                    HudChrome.fadeAlpha(0xFFCCCCCC, altFade));
        }

        // -- main bar --
        HudChrome.drawRoundedRect(gui, barX, barY, BAR_W, BAR_H,
                HudChrome.fadeAlpha(BG_ARGB, barFade),
                HudChrome.fadeAlpha(accentArgb(), barFade));

        // モード icon: 全部出し、選択中だけ持ち上げる。
        int section = BAR_W / MODE_COUNT;
        for (int i = 0; i < MODE_COUNT; i++) {
            int ix = barX + section * i + (section - ICON) / 2;
            int iy = barY + 11 - (i == currentMode ? ICON_LIFT : 0);
            int tint = i == currentMode ? accentArgb() : 0xFF888888;
            Icons.draw(gui, MODE_ICONS[i], ix, iy, ICON, HudChrome.fadeAlpha(tint, barFade));
        }

        // 選択中モード名 (選択 icon の下)
        int nameY = barY + BAR_H - font.lineHeight - 2;
        int nameCx = barX + section * currentMode;
        HudChrome.drawCenteredLabel(gui, font,
                Component.translatable(MODE_KEYS[currentMode]).getString(),
                nameCx, nameY, section, LABEL_H, HudChrome.fadeAlpha(0xFFFFFFFF, barFade));

        // アイテム名 (バーの上)
        int aboveY = barY - font.lineHeight - 2;
        if (held) {
            HudChrome.drawCenteredLabel(gui, font, stack.getHoverName().getString(),
                    barX, aboveY, BAR_W, LABEL_H, HudChrome.fadeAlpha(0xFFFFFFFF, barFade));
        }

        // 編集モード: 編集対象の表示
        if (currentMode == 1 && editTarget > 0) {
            String editInfo = Component.translatable(
                    editTarget == 1 ? "message.advancedschematicannon.edit_pos1"
                            : "message.advancedschematicannon.edit_pos2").getString();
            HudChrome.drawCenteredLabel(gui, font, editInfo,
                    barX, aboveY - font.lineHeight - 2, BAR_W, LABEL_H,
                    HudChrome.fadeAlpha(0xFFFFD54F, barFade));
        }
    }

    /** R2.5.1: アクセント色は mode から switch で決める (色をハードコード分散させない)。 */
    private static int accentArgb() {
        return switch (currentMode) {
            case 1 -> 0xFF66BB6A;   // 編集 = SELECTION 系 green
            default -> 0xFF4FC3F7;  // 範囲選択 = cyan
        };
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.SELECTED_ITEM_NAME)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!RangeBoardItem.findHeldRangeBoard(mc.player).isEmpty()) {
            event.setCanceled(true);
        }
    }

    // -------- scroll input --------

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        ItemStack stack = RangeBoardItem.findHeldRangeBoard(mc.player);
        if (stack.isEmpty()) return;

        boolean altHeld = Screen.hasAltDown();
        boolean shiftHeld = Screen.hasShiftDown();
        double delta = event.getScrollDeltaY();
        if (delta == 0) return;
        if (!altHeld && !(shiftHeld && currentMode == 1)) return;

        // R3.4.1 / R3.4.2: cooldown 内のホイールも消費して、バニラのホットバー切替を出さない。
        if (!SCROLL.tryAccept()) {
            event.setCanceled(true);
            return;
        }

        int dir = delta > 0 ? -1 : 1;
        // R3.3.1: alt > ctrl > shift。alt (= tool mode 循環) が最優先。
        if (altHeld) {
            currentMode = ((currentMode + dir) % MODE_COUNT + MODE_COUNT) % MODE_COUNT;
            if (currentMode != 1) {
                editTarget = 0;
                syncEditMode(stack, 0);
            }
        } else {
            editTarget = ((editTarget + dir) % 3 + 3) % 3;
            syncEditMode(stack, editTarget);
        }
        event.setCanceled(true);   // R3.5
    }

    // -------- left-click handler --------

    @SubscribeEvent
    public static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (!event.isAttack()) return;
        if (!Screen.hasShiftDown()) return;

        ItemStack stack = RangeBoardItem.findHeldRangeBoard(mc.player);
        if (stack.isEmpty()) return;

        if (currentMode == 1 && editTarget > 0) {
            editTarget = 0;
            syncEditMode(stack, 0);
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    private static void syncEditMode(ItemStack stack, int mode) {
        if (mode == 0) {
            stack.remove(ModDataComponents.RANGE_EDIT_MODE.get());
        } else {
            stack.set(ModDataComponents.RANGE_EDIT_MODE.get(), mode);
        }
        PacketDistributor.sendToServer(new RangeBoardEditPacket(mode));
    }
}
