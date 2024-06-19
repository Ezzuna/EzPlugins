package com.Ezzuneware.EzNaguaSlayer;


import net.runelite.client.config.*;

@ConfigGroup("EzNaguaSlayerConfig")
public interface EzNaguaSlayerConfig extends Config {
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
            name = "Looting Configuration",
            description = "Configure how to handle looting",
            position = 2,
            closedByDefault = false
    )
    String lootingConfig = "lootingConfig";

    @ConfigItem(
            keyName = "lootEnabled",
            name = "Loot?",
            description = "Loots items",
            position = 1,
            section = lootingConfig
    )
    default boolean lootEnabled() {
        return true;
    }
}