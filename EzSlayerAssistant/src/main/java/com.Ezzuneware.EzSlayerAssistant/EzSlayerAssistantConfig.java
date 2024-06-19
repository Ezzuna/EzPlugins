package com.Ezzuneware.EzSlayerAssistant;


import net.runelite.client.config.*;

@ConfigGroup("EzSlayerAssistantConfig")
public interface EzSlayerAssistantConfig extends Config {
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
            name = "Slayer Settings",
            description = "",
            position = 5,
            closedByDefault = false
    )
    String slayerSection = "slayerSection";

    @ConfigItem(
            keyName = "npcName",
            name = "npcName",
            description = "Supports wildcards *",
            position = 0,
            section = slayerSection
    )
    default String npcName() {
        return "";
    }

    @ConfigItem(
            keyName = "pickupLoot",
            name = "Pick up loot.",
            description = "",
            position = 1,
            section = slayerSection
    )
    default Boolean pickupLoot() {
        return true;
    }

    @ConfigItem(
            keyName = "lootableItems",
            name = "Lootable items",
            description = "Values are a comma seperated list of ids or names, including wildcards, e.g 513,rune " + "pickaxe,dragon*",
            position = 2,
            section = slayerSection
    )
    default String lootableItems() {
        return "";
    }

    @ConfigItem(
            keyName = "alchLoot",
            name = "Alch loot.",
            description = "",
            position = 3,
            section = slayerSection
    )
    default Boolean alchLoot() {
        return true;
    }

    @ConfigItem(
            keyName = "alchableItems",
            name = "Alchable items",
            description = "Will attempt to alch these items if found and runes are present. Values are a comma seperated list of ids or names, including wildcards, e.g 513,rune " + "pickaxe,dragon*",
            position = 4,
            section = slayerSection
    )
    default String alchableItems() {
        return "";
    }

    @ConfigSection(
            name = "Wildy Rules",
            description = "Sections on how to handle in the wilderness (Only applies while in wildy).",
            position = 6,
            closedByDefault = true
    )
    String wildySection = "wildySection";

    @ConfigItem(
            keyName = "attemptToLogOnPlayer",
            name = "Attempt to log on player",
            description = "If enabled, will attempt to log out if a player is spotted within player range and matching criteria. Highest priority.",
            position = 1,
            section = wildySection
    )
    default boolean attemptToLogOnPlayer() {
        return true;
    }

    @ConfigItem(
            keyName = "attemptToTeleOnPlayer",
            name = "Attempt to tele on player",
            description = "If enabled, will attempt to tele if a player is spotted within player range and matching criteria. NOTE: This ignores the chunks around Ferox Enclave to prevent teleing when leaving.",
            position = 2,
            section = wildySection
    )
    default boolean attemptToTeleOnPlayer() {
        return true;
    }

    @ConfigItem(
            keyName = "scaryOnly",
            name = "Only log/tele on scary players",
            description = "Attempts to only log/tele on players that are scary to us. More risky.",
            position = 3,
            section = wildySection
    )
    default boolean scaryOnly() {
        return true;
    }

    @ConfigItem(
            keyName = "teleItemChosen",
            name = "Item to tele with",
            description = "If equipable, will check if equipped and also will scan inventory to rub + 1t tele.",
            position = 4,
            section = wildySection
    )
    default teleItem teleItemChosen() {
        return teleItem.RING_OF_DUELING;
    }

    @ConfigItem(
            keyName = "weaponFilter",
            name = "dont tele from enemies with these weapons",
            description = "Values are a comma seperated list of ids or names, including wildcards, e.g 513,rune " + "pickaxe,dragon*",
            position = 5,
            section = wildySection
    )
    default String weaponFilter() {
        return "";
    }

    @ConfigItem(
            keyName = "scaryItems",
            name = "Scary Items to check for",
            description = "Items to look for when checking for scary items. If one of these is found we'll tele even if they're not skulled. These items we will tele/log on on sight.Values are a comma seperated list of ids or names, including wildcards, e.g 513,rune " + "pickaxe,dragon*",
            position = 5,
            section = wildySection
    )
    default String scaryItems() {
        return "Voidwaker,Trident of the swamp*,Dragon claws,*Granite maul*,*ballista,*dark bow*,Ahrim's robe*,Ahrim's staff*";
    }

    @ConfigItem(
            keyName = "pkerMaxDistance",
            name = "Max Distance to Player",
            description = "Adds a distance check when checking for players. If set to -1 will consider any player visible.",
            position = 6,
            section = wildySection
    )
    default Integer pkerMaxDistance() {
        return -1;
    }


}