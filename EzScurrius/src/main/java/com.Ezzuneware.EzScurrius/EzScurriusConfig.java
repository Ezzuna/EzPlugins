package com.Ezzuneware.EzScurrius;


import net.runelite.client.config.*;

@ConfigGroup("EzScurriusConfig")
public interface EzScurriusConfig extends Config {
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
            name = "Combat",
            description = "",
            position = 2,
            closedByDefault = true
    )
    String combatSection = "combatSection";

    @ConfigItem(
            keyName = "eatAt",
            name = "Eat threshold",
            description = "",
            position = 1,
            section = combatSection
    )
    default int eatAt() {
        return 30;
    }

    @ConfigItem(
            keyName = "Food",
            name = "Food name",
            description = "",
            position = 1,
            section = combatSection
    )
    default String Food() {
        return "";
    }

    @ConfigItem(
            keyName = "lootableItems",
            name = "Lootable items",
            description = "Values are a comma seperated list of ids or names, including wildcards, e.g 513,rune " + "pickaxe,dragon*",
            position = 2,
            section = combatSection
    )
    default String lootableItems() {
        return "";
    }

    @ConfigItem(
            keyName = "alchLoot",
            name = "Alch loot.",
            description = "",
            position = 3,
            section = combatSection
    )
    default Boolean alchLoot() {
        return true;
    }

    @ConfigItem(
            keyName = "dodgeRocks",
            name = "Dodge rocks.",
            description = "Use at your own peril if using melee. Designed to be used with Range/mage.",
            position = 4,
            section = combatSection
    )
    default Boolean dodgeRocks() {
        return true;
    }

    @ConfigItem(
            keyName = "onetapBabies",
            name = "One tap babies.",
            description = "Best used with range/mage.",
            position = 5,
            section = combatSection
    )
    default Boolean onetapBabies() {
        return true;
    }


    @ConfigItem(
            keyName = "alchableItems",
            name = "Alchable items",
            description = "Will attempt to alch these items if found and runes are present. Values are a comma seperated list of ids or names, including wildcards, e.g 513,rune " + "pickaxe,dragon*",
            position = 6,
            section = combatSection
    )
    default String alchableItems() {
        return "";
    }
}