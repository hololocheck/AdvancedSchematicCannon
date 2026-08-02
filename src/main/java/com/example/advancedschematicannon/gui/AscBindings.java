package com.example.advancedschematicannon.gui;

import java.util.List;

/**
 * {@code emc-schematic-cannon.json} が参照する binding key の全集合。
 *
 * <p>{@link EMCSchematicCannonScreenV2} から切り出してあるのは、この一覧を読む gate
 * ({@code LayoutStrictValidationTest}) が <b>Minecraft を載せない素の JUnit classpath</b> で
 * 動くため。Screen 本体は {@code JsonLayoutScreen} を継承していて、参照するだけで
 * Minecraft の class ロードが要る。
 *
 * <p>この一覧と layout JSON の突き合わせが R4.6.1 クラスのバグ —
 * 「JSON 側に textKey はあるが handler が知らない key」で <b>永久に既定値のまま</b>になる —
 * をビルド時に赤にする。
 */
public final class AscBindings {

    private AscBindings() {}

    /**
     * engine 側が解決する key。handler は実装しない。
     *
     * <p>{@code LayoutValidator} は {@code animationKey} / {@code transitionKey} /
     * {@code canvasKey} も同じ catalog で照合するため、handler が返す key だけを並べると
     * これらが UNRESOLVED_BINDING になる。由来が違うので節を分けてある。
     */
    private static final List<String> ENGINE_KEYS = List.of(
            "dialog-open",          // animationKey: 入場アニメ (R4.3.1)
            // option strip の展開アニメ。JsonLayoutScreen.getDynamicAnimation が
            // "-popup-open" 接尾辞を Animation.popIn(220ms) に解決するので handler 実装は不要。
            "asc-opt-popup-open",
            "toggle-bg",            // transitionKey: track 色補間 (R5.2.1)
            "toggle-knob");         // transitionKey: knob 位置補間 (R5.2.1)

    /** {@code drawCanvas} が受け取る canvasKey。 */
    private static final List<String> CANVAS_KEYS = List.of("block-list", "owner-face");

    public static final List<String> KEYS = concat(ENGINE_KEYS, CANVAS_KEYS, List.of(
            "asc-title", "asc-materials-label", "asc-status", "asc-status-color",
            "asc-progress-text", "asc-remaining", "asc-missing", "asc-progress-fill",
            "asc-fe-fill", "asc-fe-label", "asc-emc-label",
            "asc-storage-val", "asc-storage-icon", "asc-speed-val",
            "asc-block-list-visible", "asc-block-scrollbar-visible",
            "asc-block-thumb-y", "asc-block-thumb-h", "block-list-scroll",
            "asc-fuel-visible",
            "asc-schematic-mode", "asc-filler-mode",
            "asc-mode-icon", "asc-mode-label", "asc-mode-val", "asc-mode-summary",
            "asc-mode-opt-icon", "asc-owner-border", "asc-owner-face-empty",
            "asc-play-color", "asc-pause-color", "asc-stop-color",
            "asc-dont-replace-color", "asc-replace-solid-color",
            "asc-replace-any-color", "asc-replace-empty-color",
            "asc-filler-fill-color", "asc-filler-erase-color", "asc-filler-remove-color",
            "asc-filler-wall-color", "asc-filler-tower-color", "asc-filler-box-color",
            "asc-filler-circle-wall-color",
            "asc-skip-missing-toggle-bg", "asc-skip-missing-knob-bg", "asc-skip-missing-knob-x",
            "asc-protect-be-toggle-bg", "asc-protect-be-knob-bg", "asc-protect-be-knob-x",
            "asc-reuse-toggle-bg", "asc-reuse-knob-bg", "asc-reuse-knob-x",
            "asc-preview-toggle-bg", "asc-preview-knob-bg", "asc-preview-knob-x",
            "asc-emc-toggle-bg", "asc-emc-knob-bg", "asc-emc-knob-x",
            "hint-toggle-bg", "hint-knob-bg", "hint-knob-x"));

    @SafeVarargs
    private static List<String> concat(List<String>... parts) {
        var out = new java.util.ArrayList<String>();
        for (List<String> p : parts) out.addAll(p);
        return List.copyOf(out);
    }
}
