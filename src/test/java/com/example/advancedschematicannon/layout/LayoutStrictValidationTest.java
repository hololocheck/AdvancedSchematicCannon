package com.example.advancedschematicannon.layout;

import belugalab.mcss3.ir.compiler.A11yPolicy;
import belugalab.mcss3.ir.compiler.LayoutValidator;
import belugalab.mcss3.ir.compiler.ScreenPolicy;
import belugalab.mcss3.ir.compiler.ValidationContext;
import com.example.advancedschematicannon.gui.AscBindings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ASC の全 layout JSON を Manta の strict validator に通す。TSU の同名 test の移植で、
 * <b>同じ {@code LayoutValidator} class</b> を呼ぶので in-game の {@code /manta validate layout}
 * と実装が分岐しない。
 *
 * <p>検出するのは runtime が<b>黙って無視する</b>種類の欠陥 — 綴り違いの property、型違い、
 * tag に効かない property、必須欠落。どれも「動かないが error も出ない」ので、この test が
 * 無いと発見手段が目視しか無い。
 */
class LayoutStrictValidationTest {

    private static final String LAYOUT_DIR =
            "src/main/resources/assets/advancedschematicannon/layouts";

    /**
     * 設計 viewport = dialog の実寸。
     *
     * <p><b>この宣言では R4.25.4 (OUTSIDE_VIEWPORT) は空振りする</b>。同 check は
     * {@code LayoutValidator} が <b>root の矩形だけ</b>を viewport と比較する実装で、子要素は
     * 見ないため、viewport = root サイズにすると必ず通る。ではなぜ小さい値を宣言しないかと
     * いうと、{@code JsonLayoutScreen} は dialog を実サーフェスへ auto-scale する
     * ({@code computeAutoScale}) ので「368x400 が 320x240 に収まらない」は実害の無い恒久赤に
     * なるだけだから。
     *
     * <p>ScreenPolicy を渡す価値は残りの 3 つ — MISSING_SCREEN_TITLE / 増減 icon の対 /
     * 動的テキストの classes — にあり、そちらは実際に走っている。
     * 「子が dialog からはみ出していない」という本来の不変条件は
     * {@link #everyNodeStaysInsideTheDialog()} が別途担保する。
     */
    private static final int VIEWPORT_W = 368;
    private static final int VIEWPORT_H = 400;

    @Test
    @DisplayName("**ASC の全 layout が strict validator で issue 0**")
    void allLayoutsAreClean() {
        List<Path> files = layoutFiles();
        assertFalse(files.isEmpty(), "layout が 1 件も見つからないなら test 自体が壊れている");

        var icons = iconCatalog();
        List<String> problems = new ArrayList<>();
        List<String> unverified = new ArrayList<>();
        List<String> screened = new ArrayList<>();
        List<String> nonDialog = new ArrayList<>();
        for (Path p : files) {
            String name = p.getFileName().toString();
            JsonObject layout;
            try {
                layout = JsonParser.parseString(
                        Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (RuntimeException e) {
                problems.add(name + ": JSON parse error: " + e);
                continue;
            }
            var ctx = ValidationContext.coreStrict("advancedschematicannon:layouts/" + name)
                    // R4.24.1 (hit target >= 24px) and R4.25.x (title / viewport / paired
                    // stepper icons / classes on dynamic text) only run when these policies
                    // are supplied. Without them validateAll returns UNVERIFIABLE_A11Y and
                    // UNVERIFIABLE_SCREEN -- green, but having looked at nothing.
                    .withA11y(A11yPolicy.belugaExperience())
                    // handler が知らない binding key を赤にする。textKey はあるが handler が
                    // 対応していない要素は実行時に既定値のまま出続ける (R4.6.1 の症状) ので、
                    // ここで JSON と Java の突き合わせを機械化する。
                    .withBindings(new java.util.HashSet<>(AscBindings.KEYS));
            if (icons != null) ctx = ctx.withIcons(icons);
            var i18n = i18nKeys();
            if (i18n != null) ctx = ctx.withI18n(i18n);
            // ScreenPolicy (題名 / viewport / 増減 icon の対) は **画面** の規約なので、
            // root が dialog のものにだけ掛ける。option strip は dropdown 相当の
            // 「枠だけの一列」で、R4.17.1 も dropdown を別扱いにしている。
            // 外した事実は下の出力に残す — 黙って検査対象から落とさない。
            if (isDialogRoot(layout)) {
                ctx = ctx.withScreen(ScreenPolicy.belugaExperience(VIEWPORT_W, VIEWPORT_H));
                screened.add(name);
            } else {
                nonDialog.add(name);
            }
            // strict の入口は validateAll ただ一つ。validate だけを呼ぶと binding 解決と
            // 「未検査軸 (UNVERIFIABLE_*)」を見落とす。
            for (LayoutValidator.Issue i
                    : LayoutValidator.defects(LayoutValidator.validateAll(layout, ctx))) {
                problems.add(i.format());
            }
            for (LayoutValidator.Issue i : LayoutValidator.validateAll(layout, ctx)) {
                if (LayoutValidator.isUnverifiable(i)) unverified.add(name + ": " + i.code());
            }
        }
        assertTrue(problems.isEmpty(),
                "strict validation issues:\n  " + String.join("\n  ", problems));
        assertFalse(screened.isEmpty(), "ScreenPolicy が 1 枚も掛かっていない = 効いていない");
        System.out.println("[ASC] validateAll: layouts=" + files.size()
                + " screenPolicy=" + screened + " nonDialog(screenPolicy skipped)=" + nonDialog
                + " defects=0 unverifiable=" + unverified.size()
                + (unverified.isEmpty() ? "" : " -> " + String.join(", ", unverified)));
    }

    @Test
    @DisplayName("**全ノードが dialog の内側に収まる** (OUTSIDE_VIEWPORT が見ない軸)")
    void everyNodeStaysInsideTheDialog() {
        for (Path p : layoutFiles()) {
            JsonObject root;
            try {
                root = JsonParser.parseString(
                        Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            int w = root.get("w").getAsInt();
            int h = root.get("h").getAsInt();
            List<String> outside = new ArrayList<>();
            collectOutside(root, w, h, outside);
            assertTrue(outside.isEmpty(),
                    p.getFileName() + " の外へ出ているノード:\n  " + String.join("\n  ", outside));
        }
    }

    @Test
    @DisplayName("検出器として働くことの確認 — dialog の外に置いたノードは red になる")
    void insideDialogCheckActuallyDetects() {
        JsonObject root = JsonParser.parseString(
                "{\"tag\":\"div\",\"x\":0,\"y\":0,\"w\":100,\"h\":100,\"children\":["
                        + "{\"tag\":\"div\",\"classes\":[\"escapee\"],"
                        + "\"x\":90,\"y\":10,\"w\":30,\"h\":10}]}").getAsJsonObject();
        List<String> outside = new ArrayList<>();
        collectOutside(root, 100, 100, outside);
        assertFalse(outside.isEmpty(), "はみ出しを検出できないなら検出器として死んでいる");
    }

    /** root の classes に "dialog" を含むか (= 画面 / ダイアログとして扱うか)。 */
    private static boolean isDialogRoot(JsonObject root) {
        if (!root.has("classes") || !root.get("classes").isJsonArray()) return false;
        for (var e : root.getAsJsonArray("classes")) {
            if (e.isJsonPrimitive() && "dialog".equals(e.getAsString())) return true;
        }
        return false;
    }

    private static void collectOutside(JsonObject n, int w, int h, List<String> out) {
        if (n.has("x") && n.has("y") && n.has("w") && n.has("h")) {
            int x = n.get("x").getAsInt(), y = n.get("y").getAsInt();
            int nw = n.get("w").getAsInt(), nh = n.get("h").getAsInt();
            if (x < 0 || y < 0 || x + nw > w || y + nh > h) {
                out.add((n.has("classes") ? n.get("classes").toString() : n.toString())
                        + " -> (" + x + "," + y + "," + nw + "," + nh + ")");
            }
        }
        if (n.has("children")) {
            for (var c : n.getAsJsonArray("children")) {
                if (c.isJsonObject()) collectOutside(c.getAsJsonObject(), w, h, out);
            }
        }
    }

    @Test
    @DisplayName("検出器として働くことの確認 — 壊した JSON はちゃんと red になる")
    void validatorActuallyDetects() {
        // これが無いと、validator が常に空を返す実装に退化しても上の test は green のまま。
        JsonObject broken = JsonParser.parseString(
                "{\"tag\":\"div\",\"x\":0,\"y\":0,\"w\":10,\"h\":10,\"dynamicText\":\"x\"}")
                .getAsJsonObject();
        assertFalse(LayoutValidator.defects(
                        LayoutValidator.validateAll(broken, ValidationContext.coreStrict("t")))
                        .isEmpty(),
                "validator が空を返すなら検出器として死んでいる");
    }

    /**
     * layout dir を探す。cwd は実行方法で変わる (ModDevGradle の unit test は
     * {@code build/minecraft-junit}) ので決め打ちの相対パスは使えない。見つからなければ
     * green にせず fail させる — 「0 件だから合格」を防ぐ。
     */
    private static List<Path> layoutFiles() {
        Path dir = null;
        for (Path base = Paths.get("").toAbsolutePath(); base != null; base = base.getParent()) {
            Path c = base.resolve(LAYOUT_DIR);
            if (Files.isDirectory(c)) { dir = c; break; }
        }
        assertTrue(dir != null, "layout dir が見つからない (探索起点: "
                + Paths.get("").toAbsolutePath() + ", 相対: " + LAYOUT_DIR + ")");
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @return en_us.json の全キー。取得できなければ null (= 未提供、検査しない)。
     *
     * <p>en_us を正とするのは、翻訳漏れではなく<b>キーそのものの不在</b>を見たいから。
     * ja_jp との差分は {@link #langFilesDeclareTheSameKeys()} が別に見る。
     */
    private static java.util.Set<String> i18nKeys() {
        Path p = resolveFromAnyAncestor(
                "src/main/resources/assets/advancedschematicannon/lang/en_us.json");
        if (p == null) return null;
        try {
            JsonObject o = JsonParser.parseString(
                    Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
            return o.keySet();
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    @Test
    @DisplayName("**en_us と ja_jp が同じキー集合を持つ** (片方だけ足すと実機で生キーが出る)")
    void langFilesDeclareTheSameKeys() {
        Path en = resolveFromAnyAncestor(
                "src/main/resources/assets/advancedschematicannon/lang/en_us.json");
        Path ja = resolveFromAnyAncestor(
                "src/main/resources/assets/advancedschematicannon/lang/ja_jp.json");
        assertTrue(en != null && ja != null, "lang ファイルが見つからない");
        try {
            var enKeys = JsonParser.parseString(Files.readString(en, StandardCharsets.UTF_8))
                    .getAsJsonObject().keySet();
            var jaKeys = JsonParser.parseString(Files.readString(ja, StandardCharsets.UTF_8))
                    .getAsJsonObject().keySet();
            var onlyEn = new java.util.TreeSet<>(enKeys);
            onlyEn.removeAll(jaKeys);
            var onlyJa = new java.util.TreeSet<>(jaKeys);
            onlyJa.removeAll(enKeys);
            assertTrue(onlyEn.isEmpty() && onlyJa.isEmpty(),
                    "en のみ: " + onlyEn + " / ja のみ: " + onlyJa);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path resolveFromAnyAncestor(String relative) {
        for (Path base = Paths.get("").toAbsolutePath(); base != null; base = base.getParent()) {
            Path c = base.resolve(relative);
            if (Files.isRegularFile(c)) return c;
        }
        return null;
    }

    /** @return icon ID 集合。取得できなければ null (= 未提供、検査しない)。 */
    private static java.util.Set<String> iconCatalog() {
        try (var in = LayoutStrictValidationTest.class
                .getResourceAsStream("/assets/manta/icons/icons.json")) {
            if (in == null) return null;
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            var out = new java.util.HashSet<String>();
            if (root.has("icons") && root.get("icons").isJsonArray()) {
                for (var e : root.getAsJsonArray("icons")) {
                    if (e.isJsonObject() && e.getAsJsonObject().has("id")) {
                        out.add(e.getAsJsonObject().get("id").getAsString());
                    } else if (e.isJsonPrimitive()) {
                        out.add(e.getAsString());
                    }
                }
            }
            return out.isEmpty() ? null : out;
        } catch (IOException | RuntimeException e) {
            return null;   // catalog 無しは「未解決」ではない
        }
    }
}
