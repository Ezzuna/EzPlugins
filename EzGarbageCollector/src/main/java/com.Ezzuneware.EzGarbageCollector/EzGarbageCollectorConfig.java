package com.Ezzuneware.EzGarbageCollector;


import net.runelite.client.config.*;

@ConfigGroup("EzGarbageCollectorConfig")
public interface EzGarbageCollectorConfig extends Config {
    @ConfigItem(
            keyName = "Toggle",
            name = "Toggle",
            description = "",
            position = 0
    )
    default Keybind toggle() {
        return Keybind.NOT_SET;
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

    @ConfigItem(
            keyName = "tickDelayEnabled",
            name = "Tick delay",
            description = "enables some tick delays",
            position = 4,
            section = delayTickConfig
    )
    default boolean tickDelay() {
        return true;
    }

    @ConfigSection(
            name = "Item Lists",
            description = "The highlighted and hidden item lists",
            position = 0
    )
    String itemLists = "itemLists";

    @ConfigItem(
            keyName = "highlightedItems",
            name = "Highlighted Items",
            description = "Configures specifically highlighted ground items. Format: (item), (item)",
            position = 0,
            section = itemLists
    )
    default String getHighlightItems() {
        return "";
    }

        @ConfigItem(
            keyName = "maxArea",
            name = "Max dist",
            description = "Max distance from start tile to pick shit up",
            position = 2
    )
    default int maxDist() {
        return 10;
    }

}