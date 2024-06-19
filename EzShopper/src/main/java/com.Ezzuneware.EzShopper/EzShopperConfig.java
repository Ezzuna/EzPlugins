package com.Ezzuneware.EzShopper;


import net.runelite.client.config.*;

@ConfigGroup("EzShopperConfig")
public interface EzShopperConfig extends Config {
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
            name = "EzShopper Config",
            description = "Shopping settings. Handles both Buy and Sell",
            position = 2,
            closedByDefault = true
    )
    String ezShopperConfig = "ezShopperConfig";

    @ConfigItem(
            keyName = "buySell",
            name = "Buying or selling?",
            description = "He he he, welcome stranger.",
            position = 0,
            section = ezShopperConfig
    )
    default buysell buySell() {
        return buysell.buying;
    }

    @ConfigItem(
            keyName = "NPCName",
            name = "NPC Name",
            description = "The name of the NPC we're going to be interacting with.",
            position = 1,
            section = ezShopperConfig
    )
    default String NPCName() {
        return "Warwick Davis";
    }

    @ConfigItem(
            keyName = "itemToExchange",
            name = "Item to Exchange",
            description = "The name of the item you wish to buy/sell.",
            position = 2,
            section = ezShopperConfig
    )
    default String itemToExchange() {
        return "Lucky charms";
    }

        @ConfigItem(
            keyName = "quantityToTradeTill",
            name = "Stock Quantity to trade till",
            description = "We will buy/sell until the stock is at x amount. If you wanted to sell 20 per world, or buy 20 per world, you'd enter 20. Genius.",
            position = 3,
            section = ezShopperConfig
    )
    default int quantityToTradeTill() {
        return 1;
    }

}