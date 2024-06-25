package com.Ezzuneware.EzPitfallHunter;


import net.runelite.client.config.*;

@ConfigGroup("EzPitfallHunterConfig")
public interface EzPitfallHunterConfig extends Config {
    @ConfigItem(
            keyName = "Toggle",
            name = "Toggle",
            description = "",
            position = 0
    )
    default Keybind toggle() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
            keyName = "foodName",
            name = "Food Name",
            description = "",
            position = 1
    )
    default String foodList() {
        return "";
    }

    @Range(
            max = 98
    )
    @ConfigItem(
            keyName = "eatAt",
            name = "Eat threshold",
            description = "Hp value to eat at",
            position = 2
    )
    default int eatAt() {
        return 20;
    }

    @ConfigSection(
            name = "Game Tick Configuration",
            description = "Configure how to handles game tick delays, 1 game tick equates to roughly 600ms",
            position = 2,
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
}