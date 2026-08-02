package com.example.advancedschematicannon.layout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ASC に raw control glyph を残さない (R4.23.1 / R4.23.4)。TSU / SAS の同名 gate の移植。
 *
 * <h2>なぜ consumer ごとに置くのか</h2>
 * manta の gate は manta の source しか走査しない。2026-07-26 に「control glyph 0 件」と
 * 報告しながら TSU の layout JSON に閉じる × が 51 箇所残っていた事故がこれ。
 * <b>gate が見ていない領域は「0 件」の根拠にならない</b>ので、ASC は ASC 自身を検査する。
 *
 * <h2>2 経路を両方見る</h2>
 * <ol>
 *   <li><b>layout JSON</b> の {@code "text"} — Java を grep しても出てこない。
 *       regex ではなく parse する (escape された {@code "\u00d7"} を 6 文字として
 *       読み落とした実害があるため)。{@code children} だけでなく<b>全 value</b> を歩くので
 *       {@code repeat} の {@code template} 配下も見える。</li>
 *   <li><b>Java の文字列リテラル</b> — 行ではなくリテラルを見る (変数へ代入してから描く形を
 *       取りこぼさないため)。<b>unicode escape も復号する</b> — 復号しない実装では
 *       {@code "\u2714"} が {@code u2714} に見えて素通りする (5 つめの盲点)。</li>
 * </ol>
 */
class ControlGlyphGateTest {

    /**
     * 用途が control の glyph。TSU / SAS の集合と揃えてある — 片方だけ広げると
     * 「この repo では見ていない」穴になる。
     *
     * <p>意図的に入れないもの: {@code ↑ ↓ ← →} (操作ヒント・行先の content typography) と
     * {@code ■ □ ● —} (形が汎用すぎて字面から用途を判定できず、誤検出で gate が無効化される)。
     */
    private static final Set<String> CONTROL_GLYPHS = Set.of(
            "×", "✕", "✖",       // close
            "☰", "≡",             // menu
            "⚙",                  // settings
            "🔍", "🔎",           // search
            "▼", "▲", "◀", "▶",   // dropdown / 並べ替え / 再生
            "⇅", "↕",             // swap
            "✎",                  // edit
            "✓", "✔",             // 確定
            "⏸", "⏹",             // 一時停止 / 停止
            "▾", "▴", "▸", "◂",   // 異体字 (同じ用途なら入れる)
            "📖"                  // wiki / help 導線
    );

    /**
     * <b>繰り延べ台帳</b> — icon 化すべきだが未対応の glyph。key = ファイル名、
     * value = その file で許す glyph。<b>空であることが完了条件</b>で、1 件でも足せば
     * {@link #deferralLedgerIsEmpty()} が赤くなる。「後でやる」が黙って積み上がらない。
     */
    private static final Map<String, Set<String>> DEFERRED = Map.of();

    // ===== 経路 1: layout JSON =====

    @Test
    @DisplayName("**layout JSON の \"text\" に control glyph が無い** (閉じる × は icon ノードへ)")
    void layoutJsonHasNoControlGlyphText() throws IOException {
        Path dir = resolve("src/main/resources/assets/advancedschematicannon/layouts");
        assertTrue(dir != null, "layouts dir が見つからない");

        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                String base = p.getFileName().toString();
                Set<String> allowed = DEFERRED.getOrDefault(base, Set.of());
                collect(com.google.gson.JsonParser.parseString(
                        Files.readString(p, StandardCharsets.UTF_8)), base, allowed, violations);
            }
        }
        assertTrue(violations.isEmpty(),
                "layout の control glyph は tag=icon + icon/iconKey へ置換すること: " + violations);
    }

    /** parse 済みツリーの全 value を歩いて {@code text} の control glyph を集める。 */
    private static void collect(com.google.gson.JsonElement el, String base,
                                Set<String> allowed, List<String> out) {
        if (el.isJsonObject()) {
            com.google.gson.JsonObject o = el.getAsJsonObject();
            com.google.gson.JsonElement t = o.get("text");
            if (t != null && t.isJsonPrimitive() && t.getAsJsonPrimitive().isString()) {
                String v = t.getAsString().strip();
                if (!v.isEmpty() && !allowed.contains(v) && isAllControlGlyphs(v)) {
                    out.add(base + " : \"text\":\"" + v + "\"");
                }
            }
            for (var e : o.entrySet()) collect(e.getValue(), base, allowed, out);
        } else if (el.isJsonArray()) {
            for (com.google.gson.JsonElement c : el.getAsJsonArray()) {
                collect(c, base, allowed, out);
            }
        }
    }

    /**
     * R4.23.2: icon ノードに {@code text} を残さない。
     * 「icon なのか text なのか」が曖昧な形は、実際に調査を回り道させた実績がある。
     */
    @Test
    @DisplayName("**icon ノードに text が残っていない** (R4.23.2)")
    void iconNodesCarryNoText() throws IOException {
        Path dir = resolve("src/main/resources/assets/advancedschematicannon/layouts");
        assertTrue(dir != null, "layouts dir が見つからない");
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                collectIconWithText(com.google.gson.JsonParser.parseString(
                                Files.readString(p, StandardCharsets.UTF_8)),
                        p.getFileName().toString(), violations);
            }
        }
        assertTrue(violations.isEmpty(), "icon ノードの text は消すこと: " + violations);
    }

    private static void collectIconWithText(com.google.gson.JsonElement el, String base,
                                            List<String> out) {
        if (el.isJsonObject()) {
            var o = el.getAsJsonObject();
            var tag = o.get("tag");
            if (tag != null && tag.isJsonPrimitive() && "icon".equals(tag.getAsString())
                    && o.has("text")) {
                out.add(base + " : " + o.get("classes"));
            }
            for (var e : o.entrySet()) collectIconWithText(e.getValue(), base, out);
        } else if (el.isJsonArray()) {
            for (var c : el.getAsJsonArray()) collectIconWithText(c, base, out);
        }
    }

    // ===== 経路 2: Java 文字列リテラル =====

    @Test
    @DisplayName("**Java の文字列リテラルに control glyph が無い** (変数代入経由も捕捉する)")
    void javaLiteralsHaveNoControlGlyph() throws IOException {
        Path root = resolve("src/main/java");
        assertTrue(root != null, "main source が見つからない");

        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String src = Files.readString(p, StandardCharsets.UTF_8);
                String base = p.getFileName().toString();
                Set<String> allowed = DEFERRED.getOrDefault(base, Set.of());
                for (String lit : stringLiterals(src)) {
                    String t = lit.strip();
                    if (t.isEmpty() || allowed.contains(t)) continue;
                    if (isAllControlGlyphs(t)) {
                        violations.add(base + " : \"" + t + "\"");
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(),
                "control glyph は Icons.draw / icon ノードへ置換すること: " + violations);
    }

    @Test
    @DisplayName("**繰り延べ台帳が空** = 未 icon 化の control glyph 0")
    void deferralLedgerIsEmpty() {
        assertTrue(DEFERRED.isEmpty(),
                "未 icon 化の control glyph が残っている: " + DEFERRED.keySet());
    }

    // ===== 検出器が赤に振れることの証明 =====

    @Test
    @DisplayName("検出器: glyph 単独は control、文中埋込 (乗算・寸法) は typography として無視")
    void detectorSemantics() {
        assertTrue(isAllControlGlyphs("×"));
        assertTrue(isAllControlGlyphs(" ✕ "));
        assertTrue(isAllControlGlyphs("🔍"));
        assertTrue(isAllControlGlyphs("▼"), "dropdown も control");
        assertTrue(isAllControlGlyphs("⇅"), "swap も control");
        assertTrue(isAllControlGlyphs("✎"), "edit も control");
        assertTrue(isAllControlGlyphs("× 閉じる"), "ラベル付き close");
        assertTrue(isAllControlGlyphs("✓ 保存"), "ラベル付き確定");
        assertTrue(!isAllControlGlyphs("×100"));       // 倍率
        assertTrue(!isAllControlGlyphs("16×14"));      // 寸法
        assertTrue(!isAllControlGlyphs("↑↓"));         // 操作ヒント
        assertTrue(!isAllControlGlyphs("Search"));

        // 字句スキャナ: コメント / URL の // を誤検出しない
        assertTrue(stringLiterals("String s = \"×\"; // 16×14").equals(List.of("×")));
        assertTrue(stringLiterals("String u = \"http://a/b\";").equals(List.of("http://a/b")));
        assertTrue(stringLiterals("/* × */ String s = \"ok\";").equals(List.of("ok")));

        // unicode escape も復号する。この 3 行を落とすと
        // 「escape で書けば gate をすり抜けられる」状態に戻る (5 つめの盲点)。
        assertTrue(stringLiterals("String s = \"\\u2714\";").equals(List.of("✔")),
                "\\u2714 は ✔ 1 文字として読む");
        assertTrue(stringLiterals("String s = \"\\u2261 目次\";").equals(List.of("≡ 目次")),
                "escape + 通常文字の混在");
        assertTrue(stringLiterals("String s = \"\\uuu2716\";").equals(List.of("✖")),
                "JLS は \\uuu2716 のような u の連続も認める");
        assertTrue(stringLiterals("String s = \"\\u2714OK\";").equals(List.of("✔OK")));
        assertTrue(stringLiterals("String s = \"a\\nb\";").equals(List.of("anb")));
    }

    /**
     * control としての使用か。glyph 単独 ({@code "×"}) と 先頭 glyph + ラベル
     * ({@code "× 閉じる"}) の 2 形態を認める。typography との切り分けは先頭直後の 1 文字 —
     * 数字が続けば倍率・寸法 ({@code "×100"})、glyph が先頭でなければ ({@code "16×14"}) 対象外。
     */
    private static boolean isAllControlGlyphs(String s) {
        String t = s.strip();
        if (t.isEmpty()) return false;
        if (t.codePoints().allMatch(
                cp -> CONTROL_GLYPHS.contains(new String(Character.toChars(cp))))) {
            return true;
        }
        int first = t.codePointAt(0);
        if (!CONTROL_GLYPHS.contains(new String(Character.toChars(first)))) return false;
        String rest = t.substring(Character.charCount(first));
        if (rest.isBlank()) return false;
        return !Character.isDigit(rest.charAt(0));
    }

    /** Java ソースから文字列リテラルの中身だけを取り出す (comment / char literal は除外)。 */
    private static List<String> stringLiterals(String src) {
        List<String> out = new ArrayList<>();
        final int CODE = 0, STR = 1, CHR = 2, LINE = 3, BLOCK = 4;
        int state = CODE;
        StringBuilder cur = null;
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            char next = i + 1 < src.length() ? src.charAt(i + 1) : '\0';
            switch (state) {
                case CODE -> {
                    if (c == '"') { state = STR; cur = new StringBuilder(); }
                    else if (c == '\'') state = CHR;
                    else if (c == '/' && next == '/') { state = LINE; i++; }
                    else if (c == '/' && next == '*') { state = BLOCK; i++; }
                }
                case STR -> {
                    if (c == '\\') {
                        int end = unicodeEscapeEnd(src, i);
                        if (end > 0) {
                            if (cur != null) {
                                cur.append((char) Integer.parseInt(src.substring(end - 4, end), 16));
                            }
                            i = end - 1;
                        } else {
                            if (cur != null) cur.append(next);
                            i++;
                        }
                    } else if (c == '"') {
                        out.add(cur.toString());
                        cur = null;
                        state = CODE;
                    } else if (cur != null) {
                        cur.append(c);
                    }
                }
                case CHR -> {
                    if (c == '\\') i++;
                    else if (c == '\'') state = CODE;
                }
                case LINE -> { if (c == '\n') state = CODE; }
                case BLOCK -> { if (c == '*' && next == '/') { state = CODE; i++; } }
                default -> { }
            }
        }
        return out;
    }

    /**
     * {@code src[i]} が {@code \}{@code uXXXX} の開始なら 4 桁 hex 直後の index、
     * そうでなければ -1。JLS は {@code u} の連続も認めるのでまとめて読み飛ばす。
     */
    private static int unicodeEscapeEnd(String src, int i) {
        int u = i + 1;
        while (u < src.length() && src.charAt(u) == 'u') u++;
        if (u == i + 1 || u + 4 > src.length()) return -1;
        for (int k = u; k < u + 4; k++) {
            if (Character.digit(src.charAt(k), 16) < 0) return -1;
        }
        return u + 4;
    }

    /** cwd は実行方法で変わるので祖先を遡って探す。見つからなければ null (= test を落とす)。 */
    private static Path resolve(String relative) {
        for (Path base = Paths.get("").toAbsolutePath(); base != null; base = base.getParent()) {
            Path c = base.resolve(relative);
            if (Files.isDirectory(c)) return c;
        }
        return null;
    }
}
