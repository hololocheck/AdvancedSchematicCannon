package com.example.advancedschematicannon.gui;

/**
 * The cannon's wiki row as {@link AscWikiLive} registers it - its id and states - in a class that names no Minecraft
 * type, so this mod's unit tests, whose classpath has no Minecraft, judge the scripts and pictures against the same row.
 * Each state has a page: "main" the overview, "options" the schematic mode's replace modes, "filler" the filler mode,
 * "storage" the storage selector.
 */
final class AscWikiRows {

    static final String ID = "emc-schematic-cannon";

    private AscWikiRows() {}

    static String[] states() {
        return new String[] {"main", "options", "filler", "storage"};
    }
}
