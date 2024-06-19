package com.Ezzuneware.EzBankingUtility;


import net.runelite.client.config.*;

@ConfigGroup("EzBankingUtilityConfig")
public interface EzBankingUtilityConfig extends Config {
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
            position = 0,
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
            name = "Generic Items",
            description = "Items used in lots and lots of things.",
            position =2,
            closedByDefault = true
    )
    String GenericItemsSection = "GenericItemsSection";

    @ConfigItem(
            keyName = "DoHerbSack",
            name = "Empty Herb Sack",
            description = "",
            position = 1,
            section = GenericItemsSection
    )
    default boolean DoHerbSack() {
        return true;
    }




    @ConfigSection(
            name = "Hunter Items",
            description = "Items used in hunter.",
            position = 3,
            closedByDefault = true
    )
    String hunterItemsSection = "hunterItemsSection";

    @ConfigItem(
            keyName = "DoFurPouch",
            name = "Empty Fur Pouch",
            description = "Works on all pouches.",
            position = 0,
            section = hunterItemsSection
    )
    default boolean DoFurPouch() {
        return true;
    }

    @ConfigItem(
            keyName = "DoMeatSack",
            name = "Empty Meat Sack",
            description = "Works on all sacks. (⸝⸝⸝• ω •⸝⸝⸝) ♡",
            position = 1,
            section = hunterItemsSection
    )
    default boolean DoMeatSack() {
        return true;
    }
}