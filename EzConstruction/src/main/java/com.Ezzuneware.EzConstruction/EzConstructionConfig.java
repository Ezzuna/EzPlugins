package com.Ezzuneware.EzConstruction;


import com.Ezzuneware.EzPlankMaker.PlankType;
import net.runelite.client.config.*;

@ConfigGroup("EzConstructionConfig")
public interface EzConstructionConfig extends Config {
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
            name = "Construction settings",
            description = "",
            position = 2,
            closedByDefault = false
    )
    String conConfigSection = "conConfigSection";

    @ConfigItem(
            keyName = "buildSpot",
            name = "Building item type",
            description = "Not that clever. Only choose something you have a build spot for.",
            position = 2,
            section = conConfigSection
    )
    default ConstructionItem BuildSpot() {
        return ConstructionItem.LARDER;
    }

    @ConfigItem(
            keyName = "optionValue",
            name = "Construction Menu option to choose",
            description = "What is the option number of what we're building e.g. wooden larder = 1, oak larder = 2, etc.",
            position = 2,
            section = conConfigSection
    )
    default int optionValue() {
        return 1;
    }

    @ConfigItem(
            keyName = "planksPerItem",
            name = "Number of planks per item",
            description = "Number of planks expected to use per item. Will restock with butler once we have fewer than this number.",
            position = 3,
            section = conConfigSection
    )
    default int planksPerItem() {
        return 3;
    }

    @ConfigItem(
            keyName = "plankType",
            name = "Plank Type",
            description = "Type of plank to check for",
            position = 4,
            section = conConfigSection
    )
    default PlankType plankType() {
        return PlankType.OAK_PLANK;
    }

    @ConfigItem(
            keyName = "plankCountForButlering",
            name = "Plank Count before we go butlering",
            description = "Number of planks before we start looking for butler.",
            position = 5,
            section = conConfigSection
    )
    default int plankCountForButlering() {
        return 16;
    }


}