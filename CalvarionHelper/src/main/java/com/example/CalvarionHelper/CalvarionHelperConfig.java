package com.example.CalvarionHelper;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("Calvarion")
public interface CalvarionHelperConfig extends Config {

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
            keyName = "lightning",
            name = "lightning colour",
            description = ""
    )
    default Color lightning() {
        return Color.RED;
    }

    @ConfigItem(
            keyName = "swing",
            name = "swing colour",
            description = ""
    )
    default Color swing() {
        return Color.BLUE;
    }

    @ConfigItem(
            keyName = "lightningFill",
            name = "lightning fill colour",
            description = ""
    )
    @Alpha
    default Color lightningFill() {
        return new Color(0, 0, 0, 50);
    }

    @Alpha
    @ConfigItem(
            keyName = "swingFill",
            name = "swing fill colour",
            description = ""
    )
    default Color swingFill() {
        return new Color(0, 0, 0, 50);
    }


}
