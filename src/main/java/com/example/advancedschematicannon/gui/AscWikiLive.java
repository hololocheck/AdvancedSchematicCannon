package com.example.advancedschematicannon.gui;

import com.example.advancedschematicannon.AdvancedSchematicCannon;
import com.manta.api.wiki.WikiLiveScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * The cannon's screen lent to Manta's wiki (改善1, 2026-10-07: every Manta UI screen operated in the wiki): drawn live
 * where the cannon's pages show its picture, and played by the scripts in {@code wiki/playback} - on the wiki's stand-in
 * ({@link EMCSchematicCannonScreenV2#wikiCreate}), inside Manta's playback containment. Once, at client setup.
 */
@OnlyIn(Dist.CLIENT)
public final class AscWikiLive {

    private AscWikiLive() {}

    public static void register() {
        WikiLiveScreens.register(AdvancedSchematicCannon.MOD_ID, List.of(new WikiLiveScreens.Row(AscWikiRows.ID,
                EMCSchematicCannonScreenV2::wikiCreate,
                (s, st) -> ((EMCSchematicCannonScreenV2) s).wikiApplyState(st), false, AscWikiRows.states())));
    }
}
