package com.example.advancedschematicannon.gui;

import com.manta.api.controller.NumberWheelInput;
import com.manta.api.controller.OverlayController;
import com.manta.api.controller.PixelScrollViewport;
import com.manta.api.controller.TileGrid;
import com.manta.api.controller.ToggleSwitchController;
import com.manta.api.hud.HintRegistry;
import com.manta.api.hud.HintToggleHelper;
import com.manta.api.screen.JsonLayoutScreen;
import com.example.advancedschematicannon.AdvancedSchematicCannon;
import com.example.advancedschematicannon.block.EMCSchematicCannonBlockEntity;
import com.example.advancedschematicannon.block.EMCSchematicCannonBlockEntity.FillerModule;
import com.example.advancedschematicannon.block.EMCSchematicCannonBlockEntity.ReplaceMode;
import com.example.advancedschematicannon.block.EMCSchematicCannonBlockEntity.StorageMode;
import com.example.advancedschematicannon.integration.ProjectEBridge;
import com.example.advancedschematicannon.network.CannonActionPacket;
import com.example.advancedschematicannon.network.CannonSettingsPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 概略図砲の管理画面 (MantaUI)。旧 {@code EMCSchematicCannonScreen}
 * ({@code AbstractContainerScreen} + gen2 テクスチャ + 自前 widget 8 クラス) の置き換え。
 *
 * <h2>旧実装からの対応</h2>
 * <ul>
 *   <li>設定 / 速度 / 情報の張り出しタブ 3 枚 → 1 枚の dialog に集約。情報は overlay。</li>
 *   <li>PNG ボタン 14 種 → registry icon (R4.23.1)。</li>
 *   <li>{@code [ON]/[OFF]} 相当のトグル 4 種 → {@link ToggleSwitchController} (R4.14.0)。</li>
 *   <li>ストレージモードのクリック循環 → 値 div の hover + wheel (R4.13.0.8)。</li>
 *   <li>速度スライダー → wheel 入力 (R4.13.0 / {@link NumberWheelInput})。</li>
 *   <li>ブロック一覧の自前スクロール → {@link PixelScrollViewport} + {@link TileGrid}
 *       (R4.19.1 / R4.21.1)。</li>
 * </ul>
 *
 * <h2>サーバー同期</h2>
 * 旧実装と同じ二段構え (R4.9.1): 操作したら即 payload を送り、同時にローカル値も更新する。
 * {@link #syncCooldown} の間はサーバー値でローカルを上書きしない — 短くすると
 * 「動かしたつもりが戻る」が再発する (旧実装のコメントに記録されていた実害)。
 */
@OnlyIn(Dist.CLIENT)
public class EMCSchematicCannonScreenV2 extends JsonLayoutScreen<EMCSchematicCannonMenu> {

    private static final String LAYOUT = "layouts/emc-schematic-cannon.json";

    /** progress bar の内側幅 (track w=238 - border 1*2)。 */
    private static final int PROGRESS_INNER_W = 236;
    /** FE bar の内側幅 (track w=104 - border 1*2)。 */
    private static final int FE_INNER_W = 102;

    private static final int GRID_COLS = 4;
    /** 2026-08-02: 13 → 16。インベントリを上げて空いたクリアランスぶん増設した。 */
    private static final int GRID_ROWS = 16;
    private static final int CELL = 18;
    /** scrollbar track の内側高さ (track h=288 - border 1*2)。 */
    private static final int SCROLL_TRACK_INNER_H = 286;

    /** モードボタンの layout 座標 (option strip をその真下へ出すため)。 */
    private static final int MODE_BTN_X = 100;
    private static final int MODE_BTN_Y = 180;
    private static final int MODE_BTN_H = 24;

    // ===== ローカル状態 (サーバーへ送る値の source of truth) =====
    private ReplaceMode replaceMode;
    private boolean skipMissing;
    private boolean protectBlockEntities;
    private boolean useEmc;
    private boolean reuseSchematic;
    private boolean previewVisible;
    private StorageMode storageMode;
    private boolean fillerMode;
    private FillerModule fillerModule;
    private boolean publicAccess;

    private int syncCooldown = 0;

    // ===== controllers =====
    private final ToggleSwitchController skipMissingToggle;
    private final ToggleSwitchController protectBeToggle;
    private final ToggleSwitchController reuseToggle;
    private final ToggleSwitchController previewToggle;
    private final ToggleSwitchController emcToggle;
    private final NumberWheelInput speedInput;
    private final PixelScrollViewport blockScroll;
    private final OverlayController optionStrip = new OverlayController();

    /** 直近フレームの一覧描画範囲。tooltip の hit-test を描画と同じ幾何で行うため。 */
    private TileGrid blockGrid;
    private int blockGridX, blockGridY;

    public EMCSchematicCannonScreenV2(EMCSchematicCannonMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);

        this.replaceMode = menu.getReplaceMode();
        this.skipMissing = menu.isSkipMissing();
        this.protectBlockEntities = menu.isSkipTileEntities();
        this.useEmc = menu.isUseEmc();
        this.reuseSchematic = menu.isReuseSchematic();
        this.previewVisible = menu.isPreviewVisible();
        this.storageMode = menu.getStorageMode();
        this.fillerMode = menu.isFillerMode();
        this.fillerModule = menu.getFillerModule();
        this.publicAccess = menu.isPublicAccess();

        this.skipMissingToggle = new ToggleSwitchController(
                "asc-skip-missing-toggle-track", "asc-skip-missing-toggle-knob",
                () -> skipMissing, v -> { skipMissing = v; sendAllSettings(); })
                .aliasClasses("asc-skip-missing-toggle");
        this.protectBeToggle = new ToggleSwitchController(
                "asc-protect-be-toggle-track", "asc-protect-be-toggle-knob",
                () -> protectBlockEntities, v -> { protectBlockEntities = v; sendAllSettings(); })
                .aliasClasses("asc-protect-be-toggle");
        this.reuseToggle = new ToggleSwitchController(
                "asc-reuse-toggle-track", "asc-reuse-toggle-knob",
                () -> reuseSchematic, v -> { reuseSchematic = v; sendAllSettings(); })
                .aliasClasses("asc-reuse-toggle");
        this.previewToggle = new ToggleSwitchController(
                "asc-preview-toggle-track", "asc-preview-toggle-knob",
                () -> previewVisible, v -> {
            previewVisible = v;
            // クライアント側 BE へ即時反映 (描画遅延を防ぐ) — 旧実装と同じ。
            EMCSchematicCannonBlockEntity be = menu.getBlockEntity();
            if (be != null) be.setPreviewVisible(v);
            sendAllSettings();
        }).aliasClasses("asc-preview-toggle");
        this.emcToggle = new ToggleSwitchController(
                "asc-emc-toggle-track", "asc-emc-toggle-knob",
                () -> useEmc, v -> { useEmc = v; sendAllSettings(); })
                .aliasClasses("asc-emc-toggle")
                .enabledWhen(menu::supportsEmc);

        this.speedInput = new NumberWheelInput(
                Math.max(1, menu.getBlocksPerTick()), 1, 256, 1, "%.0f")
                .onChange(v -> sendAllSettings());

        this.blockScroll = new PixelScrollViewport(this::blockListContentHeight,
                GRID_ROWS * CELL);

        registerHints();
    }

    @Override
    protected String layoutJson() {
        return loadModResourceJson(AdvancedSchematicCannon.MOD_ID, LAYOUT);
    }

    /** 📖 で開く wiki ページ (R4.17.3)。assets/advancedschematicannon/wiki/ に実体がある。 */
    @Override
    protected String wikiPageId() { return "cannon/overview"; }

    // ================================================================= hints

    /**
     * R4.18.1: F1 ヒントは class 単位で登録するだけでよい (hover 検出は engine 側)。
     * 旧実装の per-widget tooltip はここへ集約した。
     */
    private void registerHints() {
        for (Map.Entry<String, String> e : HINTS.entrySet()) {
            HintRegistry.register(e.getKey(), e.getValue(), null);
        }
    }

    private static final Map<String, String> HINTS = new LinkedHashMap<>();

    static {
        HINTS.put("asc-play", "gui.advancedschematicannon.start");
        HINTS.put("asc-pause", "gui.advancedschematicannon.pause");
        HINTS.put("asc-stop", "gui.advancedschematicannon.stop");
        HINTS.put("asc-wiki-btn", "gui.advancedschematicannon.information");
        HINTS.put("asc-close", "gui.advancedschematicannon.close");
        HINTS.put("hint-toggle-label", "gui.advancedschematicannon.hint");
        HINTS.put("asc-mode-btn", "gui.advancedschematicannon.mode.hint");
        HINTS.put("asc-owner-face-box", "gui.advancedschematicannon.owner");
        HINTS.put("asc-owner-face-canvas", "gui.advancedschematicannon.owner");
        HINTS.put("asc-mode-schematic", "gui.advancedschematicannon.mode.schematic");
        HINTS.put("asc-mode-filler", "gui.advancedschematicannon.mode.filler");
        HINTS.put("asc-dont-replace", "gui.advancedschematicannon.settings.dont_replace");
        HINTS.put("asc-replace-solid", "gui.advancedschematicannon.settings.replace_solid");
        HINTS.put("asc-replace-any", "gui.advancedschematicannon.settings.replace_any");
        HINTS.put("asc-replace-empty", "gui.advancedschematicannon.settings.replace_empty");
        HINTS.put("asc-filler-fill", "gui.advancedschematicannon.filler.fill");
        HINTS.put("asc-filler-erase", "gui.advancedschematicannon.filler.erase");
        HINTS.put("asc-filler-remove", "gui.advancedschematicannon.filler.remove");
        HINTS.put("asc-filler-wall", "gui.advancedschematicannon.filler.wall");
        HINTS.put("asc-filler-tower", "gui.advancedschematicannon.filler.tower");
        HINTS.put("asc-filler-box", "gui.advancedschematicannon.filler.box");
        HINTS.put("asc-filler-circle-wall", "gui.advancedschematicannon.filler.circle_wall");
        HINTS.put("asc-skip-missing-toggle", "gui.advancedschematicannon.settings.skip_missing");
        HINTS.put("asc-protect-be-toggle", "gui.advancedschematicannon.settings.protect_be");
        HINTS.put("asc-reuse-toggle", "gui.advancedschematicannon.reuse.hint");
        HINTS.put("asc-preview-toggle", "gui.advancedschematicannon.visibility.hint");
        HINTS.put("asc-emc-toggle", "gui.advancedschematicannon.emc_toggle");
        HINTS.put("asc-speed-val", "gui.advancedschematicannon.speed.hint");
        HINTS.put("asc-storage-val", "gui.advancedschematicannon.storage.hint");
    }

    // ================================================================= dynamic text

    @Override
    public String getDynamicText(String[] classes, String defaultText) {
        for (String c : classes) {
            switch (c) {
                case "asc-title":
                    return this.title.getString();
                case "asc-materials-label":
                    return Component.translatable(fillerMode && !isRemovalMode()
                            ? "gui.advancedschematicannon.filler.item_slot"
                            : "gui.advancedschematicannon.block_list").getString();
                case "asc-status":
                    return Component.translatable(statusKey()).getString();
                case "asc-progress-text": {
                    int total = menu.getTotalBlocks();
                    if (total <= 0) return "";
                    return String.format("%d/%d (%.0f%%)",
                            menu.getPlacedBlocks(), total, menu.getProgress() * 100);
                }
                case "asc-remaining": {
                    int remaining = menu.getTotalBlocks() - menu.getPlacedBlocks();
                    if (remaining <= 0) return "";
                    return Component.translatable(
                            "gui.advancedschematicannon.remaining", remaining).getString();
                }
                case "asc-missing": {
                    String missing = missingBlockName();
                    if (missing == null) return "";
                    return Component.translatable("gui.advancedschematicannon.missing")
                            .getString() + " " + missing;
                }
                case "asc-fe-label":
                    return formatNumber(menu.getEnergy()) + " / "
                            + formatNumber(menu.getMaxEnergy()) + " FE";
                case "asc-emc-label":
                    return menu.supportsEmc() ? "EMC: " + formatNumber(menu.getPlayerEmc()) : "";
                case "asc-speed-val":
                    return String.valueOf(blocksPerTick());
                case "asc-storage-val":
                    return Component.translatable(storageKey()).getString();
                case "asc-mode-btn-label":
                    // ここは「どちらのモードか」だけを出す。細目 (置換モード / モジュール) は
                    // ステータス枠の asc-mode-summary 側に出す。
                    return Component.translatable(fillerMode
                            ? "gui.advancedschematicannon.mode.filler"
                            : "gui.advancedschematicannon.mode.schematic").getString();
                case "asc-mode-summary":
                    // モード名だけ。細目はモードボタン右側の icon が示す。
                    return Component.translatable(fillerMode
                            ? "gui.advancedschematicannon.mode.filler"
                            : "gui.advancedschematicannon.mode.schematic").getString();
                // iconKey も getDynamicText 経由で解決される (SvgRenderNode)
                case "asc-mode-btn-glyph":
                    return fillerMode ? "manta:layout-grid" : "manta:file-text";
                case "asc-mode-btn-opt":
                    // strip から選んだ細目の icon。strip 側の icon と同じものを返す。
                    return fillerMode ? fillerModuleIcon() : replaceModeIcon();
                // iconKey は SvgRenderNode が getDynamicText(classes, "") で解決する
                case "asc-storage-icon":
                    return switch (storageMode) {
                        case AE_AND_CHEST -> "manta:layers";
                        case AE_ONLY -> "manta:database";
                        case CHEST_ONLY -> "manta:archive";
                    };
                default:
                    break;
            }
        }
        return null;
    }

    private String statusKey() {
        return switch (menu.getCannonState()) {
            case IDLE -> "gui.advancedschematicannon.status.idle";
            case RUNNING -> "gui.advancedschematicannon.status.running";
            case PAUSED -> "gui.advancedschematicannon.status.paused";
            case FINISHED -> "gui.advancedschematicannon.status.finished";
            case ERROR -> "gui.advancedschematicannon.status.error";
        };
    }

    private String replaceModeIcon() {
        return switch (replaceMode) {
            case DONT_REPLACE -> "manta:ban";
            case REPLACE_SOLID -> "manta:replace";
            case REPLACE_ANY -> "manta:replace-all";
            case REPLACE_EMPTY -> "manta:square-dashed";
        };
    }

    private String fillerModuleIcon() {
        return switch (fillerModule) {
            case FILL -> "manta:paint-bucket";
            case ERASE -> "manta:eraser";
            case REMOVE -> "manta:pickaxe";
            case WALL -> "manta:brick-wall";
            case TOWER -> "manta:tower-control";
            case BOX -> "manta:box";
            case CIRCLE_WALL -> "manta:circle-dashed";
        };
    }

    private String replaceModeKey() {
        return switch (replaceMode) {
            case DONT_REPLACE -> "gui.advancedschematicannon.settings.dont_replace";
            case REPLACE_SOLID -> "gui.advancedschematicannon.settings.replace_solid";
            case REPLACE_ANY -> "gui.advancedschematicannon.settings.replace_any";
            case REPLACE_EMPTY -> "gui.advancedschematicannon.settings.replace_empty";
        };
    }

    private String fillerModuleKey() {
        return switch (fillerModule) {
            case FILL -> "gui.advancedschematicannon.filler.fill";
            case ERASE -> "gui.advancedschematicannon.filler.erase";
            case REMOVE -> "gui.advancedschematicannon.filler.remove";
            case WALL -> "gui.advancedschematicannon.filler.wall";
            case TOWER -> "gui.advancedschematicannon.filler.tower";
            case BOX -> "gui.advancedschematicannon.filler.box";
            case CIRCLE_WALL -> "gui.advancedschematicannon.filler.circle_wall";
        };
    }

    /** 撤去モードでは「搬入先」の意味になるので別キーを使う (旧実装の分岐を維持)。 */
    private String storageKey() {
        if (isRemovalMode()) {
            return storageMode == StorageMode.AE_ONLY
                    ? "gui.advancedschematicannon.storage.insert_ae"
                    : "gui.advancedschematicannon.storage.insert_chest";
        }
        return switch (storageMode) {
            case AE_AND_CHEST -> "gui.advancedschematicannon.storage.ae_chest";
            case AE_ONLY -> "gui.advancedschematicannon.storage.ae_only";
            case CHEST_ONLY -> "gui.advancedschematicannon.storage.chest_only";
        };
    }

    private String missingBlockName() {
        EMCSchematicCannonBlockEntity be = menu.getBlockEntity();
        if (be == null) return null;
        String missing = be.getMissingBlockName();
        if (missing == null || missing.isEmpty()) return null;
        ItemStack stack = itemFromRegistryName(missing);
        if (!stack.isEmpty()) return stack.getHoverName().getString();
        return missing.contains(":") ? missing.substring(missing.indexOf(':') + 1) : missing;
    }

    // ================================================================= dynamic numbers

    @Override
    public Integer getDynamicNumber(String[] classes, String key, int defaultValue) {
        switch (key) {
            case "asc-progress-fill":
                return Math.round(PROGRESS_INNER_W * clamp01(menu.getProgress()));
            case "asc-fe-fill": {
                int max = menu.getMaxEnergy();
                if (max <= 0) return 0;
                return Math.round(FE_INNER_W * clamp01((float) menu.getEnergy() / max));
            }
            case "asc-block-thumb-h":
                return blockThumbH();
            case "asc-block-thumb-y":
                return blockScroll.thumbY(defaultValue, SCROLL_TRACK_INNER_H, blockThumbH());
            case "asc-skip-missing-knob-x":
                return skipMissingToggle.knobX(defaultValue);
            case "asc-protect-be-knob-x":
                return protectBeToggle.knobX(defaultValue);
            case "asc-reuse-knob-x":
                return reuseToggle.knobX(defaultValue);
            case "asc-preview-knob-x":
                return previewToggle.knobX(defaultValue);
            case "asc-emc-knob-x":
                return emcToggle.knobX(defaultValue);
            default:
                return HintToggleHelper.resolveNumber(key, defaultValue);
        }
    }

    private int blockThumbH() {
        int content = blockListContentHeight();
        if (content <= 0) return SCROLL_TRACK_INNER_H;
        int h = SCROLL_TRACK_INNER_H * (GRID_ROWS * CELL) / content;
        return Math.max(12, Math.min(SCROLL_TRACK_INNER_H, h));
    }

    // ================================================================= dynamic colors

    private static final int ON = 0xFF66BB6A;      // 選択中 (§7 palette: 確定 / ON)
    private static final int OFF = 0xFF888888;     // 非選択
    private static final int DISABLED = 0xFF555555;

    @Override
    public Integer getDynamicColor(String[] classes, String key, int defaultArgb) {
        switch (key) {
            case "asc-owner-border":
                // 緑 = 公開 / 赤 = 非公開 (TSU と同じ意味)。
                return publicAccess ? 0xFF66BB6A : 0xFFEF5350;
            case "asc-status-color":
                return switch (menu.getCannonState()) {
                    case IDLE -> 0xFFAAAAAA;
                    case RUNNING -> 0xFF66BB6A;
                    case PAUSED -> 0xFFFFD54F;
                    case FINISHED -> 0xFF66BB6A;
                    case ERROR -> 0xFFEF5350;
                };
            case "asc-mode-schematic-color": return sel(!fillerMode);
            case "asc-mode-filler-color":    return sel(fillerMode);

            case "asc-play-color":  return canPlay() ? ON : DISABLED;
            case "asc-pause-color": return canPause() ? 0xFFFFD54F : DISABLED;
            case "asc-stop-color":  return canStop() ? 0xFFEF5350 : DISABLED;

            case "asc-dont-replace-color":  return sel(replaceMode == ReplaceMode.DONT_REPLACE);
            case "asc-replace-solid-color": return sel(replaceMode == ReplaceMode.REPLACE_SOLID);
            case "asc-replace-any-color":   return sel(replaceMode == ReplaceMode.REPLACE_ANY);
            case "asc-replace-empty-color": return sel(replaceMode == ReplaceMode.REPLACE_EMPTY);

            case "asc-filler-fill-color":        return sel(fillerModule == FillerModule.FILL);
            case "asc-filler-erase-color":       return sel(fillerModule == FillerModule.ERASE);
            case "asc-filler-remove-color":      return sel(fillerModule == FillerModule.REMOVE);
            case "asc-filler-wall-color":        return sel(fillerModule == FillerModule.WALL);
            case "asc-filler-tower-color":       return sel(fillerModule == FillerModule.TOWER);
            case "asc-filler-box-color":         return sel(fillerModule == FillerModule.BOX);
            case "asc-filler-circle-wall-color": return sel(fillerModule == FillerModule.CIRCLE_WALL);

            case "asc-skip-missing-toggle-bg": return skipMissingToggle.trackBg();
            case "asc-skip-missing-knob-bg":   return skipMissingToggle.knobBg();
            case "asc-protect-be-toggle-bg":   return protectBeToggle.trackBg();
            case "asc-protect-be-knob-bg":     return protectBeToggle.knobBg();
            case "asc-reuse-toggle-bg":        return reuseToggle.trackBg();
            case "asc-reuse-knob-bg":          return reuseToggle.knobBg();
            case "asc-preview-toggle-bg":      return previewToggle.trackBg();
            case "asc-preview-knob-bg":        return previewToggle.knobBg();
            case "asc-emc-toggle-bg":          return emcToggle.trackBg();
            case "asc-emc-knob-bg":            return emcToggle.knobBg();
            default:
                return HintToggleHelper.resolveColor(key);
        }
    }

    private static int sel(boolean active) { return active ? ON : OFF; }

    // ================================================================= dynamic bools

    @Override
    public Boolean getDynamicBool(String[] classes, String key, boolean defaultValue) {
        return switch (key) {
            case "asc-schematic-mode" -> !fillerMode;
            case "asc-filler-mode" -> fillerMode;
            case "asc-fuel-visible" -> menu.supportsEmc();
            // 所有者が居ないときだけ空状態の icon を出す。顔が描かれるときは重ねない。
            case "asc-owner-face-empty" -> ownerUuid() == null;
            // 撤去モードは「範囲内にあるブロック」を一覧表示するので概略図モードと同じ扱い。
            //
            // 材料スロットの枠自体は両モードで出したままにする (2026-08-02 のユーザー指示):
            // 概略図モードでは同じ枠の上にこの canvas が要求素材を描く。枠が消えないので
            // モードを跨いでもグリッドの見え方が変わらない。
            case "asc-block-list-visible" -> !fillerMode || isRemovalMode();
            case "asc-block-scrollbar-visible" -> (!fillerMode || isRemovalMode())
                    && blockScroll.needsScrollbar();
            default -> null;
        };
    }

    // ================================================================= clicks

    @Override
    public void onElementClick(String[] classes, int mouseX, int mouseY) {
        if (HintToggleHelper.handleClick(classes)) return;
        if (skipMissingToggle.handleClick(classes)) return;
        if (protectBeToggle.handleClick(classes)) return;
        if (reuseToggle.handleClick(classes)) return;
        if (previewToggle.handleClick(classes)) return;
        if (emcToggle.handleClick(classes)) return;

        for (String c : classes) {
            switch (c) {
                case "mc-popup-close": onClose(); return;
                case "asc-mode-btn": optionStrip.toggle(); return;

                // 実クリックは **innermost の要素** に来る (TSU の owner-face も同じ理由で
                // box / canvas の両方を受けている)。外枠の class だけ見ていると
                // 顔の上を押しても何も起きない — 実機で「切り替えられない」として出た。
                case "asc-owner-face-box":
                case "asc-owner-face-fallback":
                case "asc-owner-face-canvas":
                    // 公開設定を変えられるのは所有者だけ (server 側も同じ判定をする)。
                    // 所有者でないときも click は消費する — 背後へ落とさない (R6.0.1)。
                    if (isOwnerClient()) {
                        publicAccess = !publicAccess;
                        sendAllSettings();
                    }
                    return;

                case "asc-play": onPlayPressed(); return;
                case "asc-pause": onPausePressed(); return;
                case "asc-stop": onStopPressed(); return;
                default: break;
            }
        }
    }

    /**
     * overlay 上のクリック。基底は overlay を先に見て、true を返せば main へ落とさない。
     *
     * <p>閉じるボタンに {@code mc-popup-close} を<b>使わない</b> — あれは基底が拾って
     * {@code onClose()} を呼ぶ = <b>画面ごと閉じる</b>ので、sub UI の × としては誤り。
     */
    @Override
    protected boolean handleOverlayClick(String[] classes, int mouseX, int mouseY, int button) {
        for (String c : classes) {
            switch (c) {

                case "asc-dont-replace": setReplaceMode(ReplaceMode.DONT_REPLACE); return true;
                case "asc-replace-solid": setReplaceMode(ReplaceMode.REPLACE_SOLID); return true;
                case "asc-replace-any": setReplaceMode(ReplaceMode.REPLACE_ANY); return true;
                case "asc-replace-empty": setReplaceMode(ReplaceMode.REPLACE_EMPTY); return true;

                case "asc-filler-fill": setFillerModule(FillerModule.FILL); return true;
                case "asc-filler-erase": setFillerModule(FillerModule.ERASE); return true;
                case "asc-filler-remove": setFillerModule(FillerModule.REMOVE); return true;
                case "asc-filler-wall": setFillerModule(FillerModule.WALL); return true;
                case "asc-filler-tower": setFillerModule(FillerModule.TOWER); return true;
                case "asc-filler-box": setFillerModule(FillerModule.BOX); return true;
                case "asc-filler-circle-wall":
                    setFillerModule(FillerModule.CIRCLE_WALL); return true;
                default: break;
            }
        }
        return false;
    }

    private void setFillerMode(boolean filler) {
        if (fillerMode == filler) return;
        fillerMode = filler;
        // 開いたままだと「切り替えた瞬間に中身が別モードの細目へ差し替わる」ので畳む。
        optionStrip.setOpen(false);
        sendAllSettings();
    }

    private void setReplaceMode(ReplaceMode m) {
        replaceMode = m;
        optionStrip.setOpen(false);
        sendAllSettings();
    }

    private void setFillerModule(FillerModule m) {
        fillerModule = m;
        optionStrip.setOpen(false);
        sendAllSettings();
    }

    // ================================================================= wheel

    @Override
    public boolean onElementWheel(String[] classes, String key,
                                   int mouseX, int mouseY, double scrollY) {
        switch (key) {
            case "asc-mode-val":
                // R4.13.0.8: 固定選択肢の循環はホイール。モード本体はここで切り替わり、
                // クリックは「今のモードの細目」を出す側 (option strip) に割り当ててある。
                setFillerMode(!fillerMode);
                return true;
            case "asc-speed-val":
                return speedInput.handleWheel(scrollY);
            case "asc-storage-val": {
                if (!ae2Available()) return true;   // consume: click-through 防止 (R6.0.1 と同趣旨)
                StorageMode[] all = isRemovalMode()
                        ? new StorageMode[]{StorageMode.AE_ONLY, StorageMode.CHEST_ONLY}
                        : StorageMode.values();
                int idx = 0;
                for (int i = 0; i < all.length; i++) if (all[i] == storageMode) idx = i;
                int dir = scrollY > 0 ? 1 : -1;
                storageMode = all[((idx + dir) % all.length + all.length) % all.length];
                sendAllSettings();
                return true;
            }
            case "block-list-scroll":
                blockScroll.scroll((int) (-scrollY * CELL));
                return true;
            default:
                return false;
        }
    }

    /**
     * option strip はモードボタンの真下に出す (「クリックすると設定アイコンが下に出現する」)。
     *
     * <p>screen 座標へは {@code dialogLocalToScreen*} で変換する — この基底は dialog を
     * viewport 中心 pivot で auto-scale するので、layout 座標をそのまま画面座標として
     * 使うと縮尺 != 1 でずれる (overlay_coord_gate が禁じている手計算)。
     * 情報 overlay は既定の中央配置のままにするので null を返す。
     */
    @Override
    protected int[] overlayDefaultPosition(int overlayW, int overlayH) {
        if (!optionStrip.isOpen()) return null;
        return new int[]{
                dialogLocalToScreenX(MODE_BTN_X),
                dialogLocalToScreenY(MODE_BTN_Y + MODE_BTN_H + 2)};
    }

    // ================================================================= canvas

    @Override
    public void drawCanvas(GuiGraphics g, String[] classes, String key,
                            int x, int y, int w, int h, int mouseX, int mouseY) {
        if ("owner-face".equals(key)) {
            // UUID は BE の getUpdateTag 経由でクライアントにも来ている (§5.1 対応済み)。
            if (ownerUuid() != null) {
                com.manta.api.hud.OwnerFacePainter.draw(g, x, y, w, h, ownerUuid());
            }
            return;
        }
        if (!"block-list".equals(key)) return;

        blockGridX = x;
        blockGridY = y;
        blockGrid = new TileGrid(x, y, GRID_COLS, CELL, CELL, CELL, CELL);

        List<Map.Entry<String, Integer>> entries = blockEntries();
        if (entries.isEmpty()) return;

        boolean inRemoval = isRemovalMode();
        int offset = blockScroll.offset();

        // **enableScissor は使わない。** GuiGraphics.enableScissor は pose を考慮しない
        // (Manta 自身が ScissorNode / GuiDrawSurface で「1.21 quirk」として明記している) ので、
        // ここへ layout 座標を渡すと dialog の scale+translate ぶんズレた矩形で切られ、
        // **一覧も下の枠もまとめて消える** (実機で発生: データはあるので tooltip だけ出た)。
        //
        // 行の cull だけで足りる: スクロール量は常に CELL 単位で、offset も
        // [0, contentHeight - viewportHeight] (どちらも CELL の倍数) に clamp されるため、
        // 半端な高さの行は原理的に出ない。
        for (int i = 0; i < entries.size(); i++) {
            int tileY = blockGrid.tileY(i, offset);
            // R4.21.2: render 側の cull を click 側にも同じ述語で適用する (下の hitTest 参照)。
            if (tileY + CELL <= y || tileY >= y + h) continue;
            int tileX = blockGrid.tileX(i);

            Map.Entry<String, Integer> entry = entries.get(i);
            ItemStack stack = itemFromRegistryName(entry.getKey());
            if (!stack.isEmpty()) {
                g.renderItem(stack, tileX, tileY);
                if (!inRemoval) {
                    String count = formatCount(entry.getValue());
                    int tw = font.width(count);
                    g.pose().pushPose();
                    g.pose().translate(0, 0, 200);
                    g.drawString(font, count, tileX + 16 - tw, tileY + 8, 0xFFFFFFFF, true);
                    g.pose().popPose();
                }
            } else {
                String shortName = entry.getKey().contains(":")
                        ? entry.getKey().substring(entry.getKey().indexOf(':') + 1)
                        : entry.getKey();
                if (shortName.length() > 4) shortName = shortName.substring(0, 3) + "~";
                g.drawString(font, shortName, tileX, tileY + 1, 0xFFAAAAAA, true);
            }
        }
    }

    /** 一覧の tooltip。dialog の上に出すので afterDialogRender (= screen 座標) で描く。 */
    @Override
    protected void afterDialogRender(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (blockGrid == null) return;
        if (fillerMode && !isRemovalMode()) return;

        List<Map.Entry<String, Integer>> entries = blockEntries();
        if (entries.isEmpty()) return;

        double lmx = toLocalMx(mouseX);
        double lmy = toLocalMy(mouseY);
        int idx = blockGrid.hitTest(lmx, lmy, blockScroll.offset(), entries.size());
        if (idx < 0) return;
        // R4.21.2: hitTest は viewport 下端を bound しないので、描画と同じ cull を掛ける。
        int tileY = blockGrid.tileY(idx, blockScroll.offset());
        if (tileY < blockGridY || tileY + CELL > blockGridY + GRID_ROWS * CELL) return;

        Map.Entry<String, Integer> entry = entries.get(idx);
        ItemStack stack = itemFromRegistryName(entry.getKey());
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(stack.isEmpty()
                ? Component.literal(entry.getKey()).withStyle(net.minecraft.ChatFormatting.YELLOW)
                : stack.getHoverName());
        tooltip.add(Component.literal(entry.getValue() + "x")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        if (!stack.isEmpty() && useEmc && ProjectEBridge.hasEmcValue(stack)) {
            tooltip.add(Component.translatable(isRemovalMode()
                            ? "gui.advancedschematicannon.emc_converted"
                            : "gui.advancedschematicannon.emc_placed")
                    .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        }
        g.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
    }

    // ================================================================= overlay (info)

    @Override
    protected String overlayJson() {
        if (optionStrip.isOpen()) {
            return loadModResourceJson(AdvancedSchematicCannon.MOD_ID, OPTIONS_LAYOUT);
        }
        return null;
    }

    @Override
    protected boolean closeOpenOverlay() {
        return optionStrip.close();
    }

    private static final String OPTIONS_LAYOUT = "layouts/emc-schematic-cannon-options.json";

    // ================================================================= transport

    private boolean canPlay() {
        var s = menu.getCannonState();
        return s == EMCSchematicCannonBlockEntity.State.IDLE
                || s == EMCSchematicCannonBlockEntity.State.FINISHED
                || s == EMCSchematicCannonBlockEntity.State.ERROR
                || s == EMCSchematicCannonBlockEntity.State.PAUSED;
    }

    private boolean canPause() {
        return menu.getCannonState() == EMCSchematicCannonBlockEntity.State.RUNNING;
    }

    private boolean canStop() {
        var s = menu.getCannonState();
        return s == EMCSchematicCannonBlockEntity.State.RUNNING
                || s == EMCSchematicCannonBlockEntity.State.PAUSED;
    }

    private void onPlayPressed() {
        if (!canPlay()) return;
        EMCSchematicCannonBlockEntity be = menu.getBlockEntity();
        if (be == null) return;
        CannonActionPacket.Action action = switch (menu.getCannonState()) {
            case IDLE, FINISHED, ERROR -> CannonActionPacket.Action.START;
            case PAUSED -> CannonActionPacket.Action.RESUME;
            default -> null;
        };
        if (action != null) {
            PacketDistributor.sendToServer(new CannonActionPacket(be.getBlockPos(), action));
        }
    }

    private void onPausePressed() {
        if (!canPause()) return;
        EMCSchematicCannonBlockEntity be = menu.getBlockEntity();
        if (be == null) return;
        PacketDistributor.sendToServer(
                new CannonActionPacket(be.getBlockPos(), CannonActionPacket.Action.PAUSE));
    }

    private void onStopPressed() {
        if (!canStop()) return;
        EMCSchematicCannonBlockEntity be = menu.getBlockEntity();
        if (be == null) return;
        PacketDistributor.sendToServer(
                new CannonActionPacket(be.getBlockPos(), CannonActionPacket.Action.STOP));
    }

    // ================================================================= server sync

    private void sendAllSettings() {
        // 30 tick (1.5s): サーバー往復 + 反映時間を見込む。短すぎるとサーバー反映前に
        // containerTick が旧値で上書きし「動かしたつもりが戻る」UX バグが起きる (旧実装の実害)。
        syncCooldown = 30;
        EMCSchematicCannonBlockEntity be = menu.getBlockEntity();
        if (be == null) return;
        PacketDistributor.sendToServer(new CannonSettingsPacket(
                be.getBlockPos(),
                CannonSettingsPacket.packModes(replaceMode.ordinal(), storageMode.ordinal()),
                skipMissing,
                protectBlockEntities,
                useEmc,
                CannonSettingsPacket.packSpeedAndFlags(blocksPerTick(), reuseSchematic),
                CannonSettingsPacket.packFillerModeAndModule(
                        fillerMode, fillerModule.ordinal(), previewVisible, publicAccess)));
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if (syncCooldown > 0) {
            syncCooldown--;
            return;
        }
        replaceMode = menu.getReplaceMode();
        skipMissing = menu.isSkipMissing();
        protectBlockEntities = menu.isSkipTileEntities();
        useEmc = menu.isUseEmc();
        reuseSchematic = menu.isReuseSchematic();
        previewVisible = menu.isPreviewVisible();
        fillerMode = menu.isFillerMode();
        fillerModule = menu.getFillerModule();
        publicAccess = menu.isPublicAccess();
        storageMode = ae2Available() ? menu.getStorageMode() : StorageMode.CHEST_ONLY;
        if (menu.getBlocksPerTick() > 0) {
            speedInput.setValue(menu.getBlocksPerTick());
        }
        // 撤去モードでは AE+チェストの同時指定が無い (搬入先は 1 つ) — 旧実装と同じ正規化。
        if (isRemovalMode() && storageMode == StorageMode.AE_AND_CHEST) {
            storageMode = ae2Available() ? StorageMode.AE_ONLY : StorageMode.CHEST_ONLY;
            sendAllSettings();
        }
        blockScroll.clamp();
    }

    // ================================================================= misc

    /**
     * Ctrl + 左クリックで EMC 燃料スロットへ搬入する経路 (旧実装から維持)。
     * button=2 の QUICK_MOVE として送り、Menu 側が燃料スロットへルーティングする。
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (menu.supportsEmc() && button == 0 && hasControlDown() && !hasShiftDown()
                && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 2,
                    net.minecraft.world.inventory.ClickType.QUICK_MOVE);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * JEI に避けさせる矩形。
     *
     * <p><b>leftPos/imageWidth ではなく視覚矩形を返す</b> — この基底は dialog を viewport 中心
     * pivot で auto-scale するため、scale != 1 のとき論理値 (leftPos, imageWidth) は
     * 画面上の見た目とずれる。旧実装は張り出しタブのために例外領域を返していたが、
     * MantaUI では dialog 1 枚なのでその矩形そのものを返す。
     */
    public List<Rect2i> getExclusionAreas() {
        float scale = dialogScale();
        int vw = Math.round(this.imageWidth * scale);
        int vh = Math.round(this.imageHeight * scale);
        int vx = Math.round(this.width / 2f - vw / 2f);
        int vy = Math.round(this.height / 2f - vh / 2f);
        return List.of(new Rect2i(vx, vy, vw, vh));
    }

    private java.util.UUID ownerUuid() {
        EMCSchematicCannonBlockEntity be = menu.getBlockEntity();
        return be == null ? null : be.getOwnerUUID();
    }

    /** クライアント側の所有者判定。owner 未設定の砲は誰でも所有者扱い (server と同じ)。 */
    private boolean isOwnerClient() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return false;
        return ownerUuid() == null || ownerUuid().equals(mc.player.getUUID());
    }

    private boolean isRemovalMode() {
        return fillerMode && fillerModule == FillerModule.REMOVE;
    }

    private boolean ae2Available() {
        EMCSchematicCannonBlockEntity be = menu.getBlockEntity();
        return be != null && be.isAe2Available();
    }

    private int blocksPerTick() {
        return Math.max(1, Math.min(256, Math.round(speedInput.value())));
    }

    private List<Map.Entry<String, Integer>> blockEntries() {
        EMCSchematicCannonBlockEntity be = menu.getBlockEntity();
        if (be == null) return List.of();
        LinkedHashMap<String, Integer> summary = be.getBlockSummary();
        return summary.isEmpty() ? List.of() : new ArrayList<>(summary.entrySet());
    }

    private int blockListContentHeight() {
        int count = blockEntries().size();
        if (count == 0) return 0;
        return ((count + GRID_COLS - 1) / GRID_COLS) * CELL;
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static ItemStack itemFromRegistryName(String registryName) {
        try {
            ResourceLocation rl = ResourceLocation.parse(registryName);
            var blockOpt = BuiltInRegistries.BLOCK.getOptional(rl);
            if (blockOpt.isPresent()) {
                ItemStack stack = new ItemStack(blockOpt.get().asItem());
                if (!stack.isEmpty() && stack.getItem() != Items.AIR) return stack;
            }
            if (BuiltInRegistries.ITEM.containsKey(rl)) {
                var item = BuiltInRegistries.ITEM.get(rl);
                if (item != Items.AIR) return new ItemStack(item);
            }
        } catch (Exception ignored) {
        }
        return ItemStack.EMPTY;
    }

    private static String formatCount(int count) {
        if (count >= 1000) return String.format("%.1fK", count / 1000.0);
        return String.valueOf(count);
    }

    private static String formatNumber(long num) {
        if (num >= 1_000_000_000) return String.format("%.1fB", num / 1_000_000_000.0);
        if (num >= 1_000_000) return String.format("%.1fM", num / 1_000_000.0);
        if (num >= 1_000) return String.format("%.1fK", num / 1_000.0);
        return String.valueOf(num);
    }
}
