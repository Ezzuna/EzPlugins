package com.Ezzuneware.EzBarbFish;


import net.runelite.client.config.*;

@ConfigGroup("EzBarbFishConfig")
public interface EzBarbFishConfig extends Config {
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

    @ConfigSection(
            name = "Running Config",
            description = "Core Settings",
            position = 2,
            closedByDefault = true
    )
    String barbFishConfig = "barbFishConfig";

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
            max = 28
    )
    @ConfigItem(
            keyName = "minFishGoods",
            name = "Minimum Fish Parts",
            description = "Min fish parts before we stop using herb and start eating.",
            position = 1,
            section = barbFishConfig
    )
    default int minFishGoods() {
        return 3;
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

    @ConfigItem(
            keyName = "ThreeTicking",
            name = "Three Ticking",
            description = "Attempts to three tick",
            position = 5,
            section = barbFishConfig
    )
    default boolean ThreeTicking() {
        return true;
    }
}