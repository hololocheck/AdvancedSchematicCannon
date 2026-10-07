package com.example.advancedschematicannon.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.manta.api.wiki.WikiLiveScreens;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The cannon's wiki (改善1, 2026-10-07: every Manta UI screen operated in the wiki): its playback scripts pass Manta's own
 * check ({@link WikiLiveScreens#checkPlayback} - never a copy of its rules here), and every screen picture its pages show
 * is drawn live.
 *
 * <p>The row is registered with the id and states {@link AscWikiLive} registers ({@link AscWikiRows}); its factory and
 * state applier are stand-ins that are never called here - this mod's unit-test classpath has no Minecraft, so the screen
 * class cannot be loaded (AscScreenCatalogueTest, measured 2026-09-27), and the check reads only the row's id and states.
 *
 * <p>No part is listed as never pressed: the stand-in's play, pause, stop, settings and owner toggle act on its own
 * cannon in no level, and its slots are a throwaway menu's (EMCSchematicCannonScreenV2.wikiCreate) - nothing it does is a
 * packet, a file, a native dialog or the mod's process-wide state. Not seen here: whether each step finds its part on the
 * real screen and state - the real client's run of each script ({@code /manta debug live}: played, delivered n, no stop).
 */
class AscWikiPlaybackScriptsTest {

    private static final String MOD = "advancedschematicannon";
    private static final String ASSETS = "src/main/resources/assets/advancedschematicannon";
    private static final Pattern PICTURE = Pattern.compile("bws:advancedschematicannon:[^)\\s]+");

    @Test
    void everyPlaybackScriptPassesMantasCheck() {
        registerTheRow();
        Path assets = assets();
        WikiLiveScreens.PlaybackCheck check = WikiLiveScreens.checkPlayback(assets, MOD, Set.of());
        assertTrue(check.scripts() >= AscWikiRows.states().length,
                check.scripts() + " playback script(s) read under " + assets.toAbsolutePath() + " - one per state expected");
        assertEquals(List.of(), check.problems(), check.scripts() + " script(s) judged");
    }

    @Test
    void everyScreenPictureTheCannonPagesShowIsDrawnLive() throws IOException {
        registerTheRow();
        Path pages = assets().resolve("wiki");
        int judged = 0;
        List<String> notLive = new ArrayList<>();
        List<Path> files;
        try (Stream<Path> walk = Files.walk(pages)) {
            files = walk.filter(p -> p.getFileName().toString().endsWith(".md")).sorted().toList();
        }
        for (Path page : files) {
            List<String> lines = Files.readAllLines(page, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                Matcher m = PICTURE.matcher(lines.get(i));
                while (m.find()) {
                    String url = m.group();
                    if (!url.contains("wiki/screens/")) continue;
                    judged++;
                    if (!WikiLiveScreens.resolves(url)) {
                        notLive.add(pages.relativize(page).toString().replace('\\', '/') + ":" + (i + 1) + " " + url);
                    }
                }
            }
        }
        assertTrue(judged >= 2 * AscWikiRows.states().length,
                judged + " screen picture(s) read under " + pages.toAbsolutePath() + " - every state in both languages expected");
        assertEquals(List.of(), notLive, "the cannon's pages show screen pictures no live screen draws (" + judged + " judged)");
    }

    /**
     * The row as the client registers it: AscWikiRows' id and states, with a factory and an applier never called here.
     * Built by reflection - the constructor's parameters name Minecraft's Screen, which this classpath cannot compile
     * against (measured 2026-10-07: {@code compileTestJava} failed on the class file of Screen).
     */
    private static void registerTheRow() {
        Supplier<Object> factory = () -> null;
        BiConsumer<Object, String> apply = (screen, state) -> { };
        Object row;
        try {
            row = WikiLiveScreens.Row.class
                    .getConstructor(String.class, Supplier.class, BiConsumer.class, boolean.class, String[].class)
                    .newInstance(AscWikiRows.ID, factory, apply, false, AscWikiRows.states());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("WikiLiveScreens.Row(id, factory, apply, applyBeforeInit, states...) not found", e);
        }
        WikiLiveScreens.register(MOD, untyped(List.of(row)));
    }

    @SuppressWarnings("unchecked")
    private static <T> T untyped(Object o) {
        return (T) o;
    }

    /** The assets, found upwards from the working directory. */
    private static Path assets() {
        for (Path base = Paths.get("").toAbsolutePath(); base != null; base = base.getParent()) {
            if (Files.isDirectory(base.resolve(ASSETS))) return base.resolve(ASSETS);
        }
        return Paths.get(ASSETS);
    }
}
