package com.Ezzuneware.EzWintertodt;


import net.runelite.client.config.*;

@ConfigGroup("EzWintertodtConfig")
public interface EzWintertodtConfig extends Config {
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
            name = "Wintertodt Settings",
            description = "Set up WT",
            position = 1

    )
    String wtConfig = "wtConfig";

    @ConfigItem(
            keyName = "foodName",
            name = "Food Name",
            description = "Name of food to eat (i.e Cake)",
            position = 2,
            section = wtConfig
    )
    default String foodName() {
        return "";
    }

    @ConfigItem(
            keyName = "foodCount",
            name = "Food Count",
            description = "Number of food to withdraw when restocking.",
            position = 3,
            section = wtConfig
    )
    default int foodCount() {
        return 5;
    }

    @ConfigItem(
            keyName = "eatAt",
            name = "Eat Threshold",
            description = "Hp to eat at",
            position = 4,
            section = wtConfig
    )
    default int eatAt() {
        return 6;
    }

    @ConfigItem(
            keyName = "crateCount",
            name = "Crate Count",
            description = "Ideal number of crates to earn before banking.",
            position = 5,
            section = wtConfig
    )
    default int crateCount() {
        return 6;
    }

    @ConfigItem(
            keyName = "keepGoing",
            name = "Keep Running",
            description = "Keep running after out of food in bank.",
            position = 6,
            section = wtConfig
    )
    default boolean keepGoing() {
        return false;
    }

    @ConfigItem(
            keyName = "hopIdle",
            name = "Hop Gap",
            description = "Hop the gap between rounds (66 agility).",
            position = 8,
            section = wtConfig
    )
    default boolean hopIdle() {
        return false;
    }

    @ConfigItem(
            keyName = "doFletch",
            name = "Why Fletch?",
            description = "Should we fletch?",
            position = 7,
            section = wtConfig
    )
    default boolean doFletch() {
        return true;
    }

        @ConfigItem(
            keyName = "skipLookForTinderBox",
            name = "Skip Tinderbox Check?",
            description = "If selected, wont look for a tinderbox when checking having required items (useful for using a Bruhma torch).",
            position = 8,
            section = wtConfig
    )
    default boolean skipLookForTinderBox() {
        return false;
    }
}