package com.Ezzuneware.EzEdgeSmelter;


import net.runelite.client.config.*;

@ConfigGroup("EzEdgeSmelterConfig")
public interface EzEdgeSmelterConfig extends Config {
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
            keyName = "bar",
            name = "Bar",
            description = "Which bar you will use",
            position = 1
    )
    default Ore bar() {
        return Ore.SILVER;
    }

        @ConfigItem(
            keyName = "gem",
            name = "gem",
            description = "Which product you will use",
            position = 4
    )
    default Gem gem() {
        return Gem.NONE;
    }


        @ConfigItem(
            keyName = "product",
            name = "Bar",
            description = "Which product you will use",
            position = 5
    )
    default Product product() {
        return Product.BAR;
    }

    @ConfigItem(
            keyName = "item",
            name = "Item",
            description = "Which item you will make",
            position = 2
    )
    default SmeltingItem item() {
        return SmeltingItem.SILVER_BAR;
    }

    @ConfigItem(
            keyName = "tickDelay",
            name = "Tick Delay",
            description = "Slow down certain actions",
            position = 3
    )
    default int tickDelay() {
        return 0;
    }
}

