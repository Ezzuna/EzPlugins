package com.Ezzuneware.EzHerbi;

import java.awt.Color;

import net.runelite.client.config.*;

@ConfigGroup("herbiafk")
public interface HerbiAfkConfig extends Config {
    @ConfigItem(
            position = 1,
            keyName = "showPathLine",
            name = "Show path lines",
            description = "Show trail path lines on the world."
    )
    default boolean showPathLine() {
        return true;
    }

    @Alpha
    @ConfigItem(
            position = 2,
            keyName = "lineColor",
            name = "Path line color",
            description = "Color of the trail path lines."
    )
    default Color getLineColor() {
        return Color.CYAN;
    }

    @ConfigItem(
            position = 3,
            keyName = "showMiniMapArrow",
            name = "Show arrow on the minimap",
            description = "Show an arrow on the minimap to the next search spot."
    )
    default boolean showMiniMapArrow() {
        return true;
    }

    @Alpha
    @ConfigItem(
            position = 4,
            keyName = "arrowColor",
            name = "Minimap arrow color",
            description = "Color of the arrow on the minimap."
    )
    default Color getArrowColor() {
        return Color.CYAN;
    }

    @ConfigItem(
            position = 5,
            keyName = "showMiniMaplines",
            name = "Show path lines on the minimap",
            description = "Show the trail path lines on the minimap."
    )
    default boolean showMiniMaplines() {
        return true;
    }

    @Alpha
    @ConfigItem(
            position = 6,
            keyName = "minimapPathColor",
            name = "Minimap path lines color",
            description = "Color of the trail path lines on the minimap."
    )
    default Color getMinimapPathColor() {
        return Color.CYAN;
    }

    @ConfigItem(
            position = 7,
            keyName = "highlightHerbiTile",
            name = "Highlight herbiboar tile",
            description = "Highlights herbiboar tile at the end of the trail."
    )
    default boolean highlightHerbiTile() {
        return false;
    }

    @ConfigItem(
            position = 8,
            keyName = "highlightHerbiHull",
            name = "Highlight herbiboar hull",
            description = "Highlights herbiboar hull at the end of the trail."
    )
    default boolean highlightHerbiHull() {
        return true;
    }

    @ConfigItem(
            position = 9,
            keyName = "highlightHerbiOutline",
            name = "Highlight herbiboar outline",
            description = "Highlights herbiboar outline at the end of the trail."
    )
    default boolean highlightHerbiOutline() {
        return false;
    }

    @Alpha
    @ConfigItem(
            position = 10,
            keyName = "herbiboarColor",
            name = "Herbiboar highlight",
            description = "Color of the herbiboar highlight."
    )
    default Color getHerbiboarColor() {
        return Color.CYAN;
    }

    @ConfigItem(
            position = 11,
            keyName = "pathRelativeToPlayer",
            name = "Path relative to player",
            description = "Make the trail path line relative to the player."
    )
    default boolean pathRelativeToPlayer() {
        return true;
    }

    @ConfigItem(
            position = 12,
            keyName = "dynamicMenuEntrySwap",
            name = "Dynamically swap trail menu entries",
            description = "Swap menu entries to only make the next correct object clickable."
    )
    default boolean dynamicMenuEntrySwap() {
        return true;
    }

    @ConfigItem(
            position = 13,
            keyName = "npcMenuEntrySwap",
            name = "Hide fossil island npcs menu entries",
            description = "Hide fungi, zygomite and crab interaction menus."
    )
    default boolean npcMenuEntrySwap() {
        return true;
    }

    @ConfigItem(
            position = 14,
            keyName = "noLootMode",
            name = "No loot mode",
            description = "Skip the step of looting herbi."
    )
    default boolean noLootMode() {
        return false;
    }

    @ConfigItem(
            position = 15,
            keyName = "botMode",
            name = "Bot Mode",
            description = "Will automatically start the next herbiboar after finishing the last."
    )
    default boolean botMode() {
        return false;
    }

    @ConfigSection(
            name = "Game Tick Configuration",
            description = "Configure how to handles game tick delays, 1 game tick equates to roughly 600ms",
            position = 1,
            closedByDefault = true
    )
    String delayTickConfig = "delayTickConfig";

    @Range(
            max = 10
    )
    @ConfigItem(
            keyName = "tickDelayMin",
            name = "Game Tick Min",
            description = "",
            position = 2,
            section = delayTickConfig
    )
    default int tickDelayMin() {
        return 1;
    }

    @Range(
            max = 10
    )
    @ConfigItem(
            keyName = "tickDelayMax",
            name = "Game Tick Max",
            description = "",
            position = 3,
            section = delayTickConfig
    )
    default int tickDelayMax() {
        return 3;
    }
}
