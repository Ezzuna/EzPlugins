package com.Ezzuneware.EzSpindel;


import net.runelite.client.config.*;

@ConfigGroup("EzSpindelConfig")
public interface EzSpindelConfig extends Config {
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
            name = "Spindel Settings",
            description = "",
            position = 2,
            closedByDefault = false
    )
    String spindelSection = "spindelSection";

    @ConfigItem(
            keyName = "rangedItem",
            name = "Ranged weapon name",
            description = "Your ranged weapon name for killing babies. Supports wildcards",
            position = 1,
            section = spindelSection
    )
    default String rangedItem() {
        return "*dart";
    }

    @ConfigItem(
            keyName = "meleeItem",
            name = "Melee weapon name",
            description = "Your melee weapon name. Supports wildcards",
            position = 2,
            section = spindelSection
    )
    default String meleeItem() {
        return "Zombie axe";
    }

}