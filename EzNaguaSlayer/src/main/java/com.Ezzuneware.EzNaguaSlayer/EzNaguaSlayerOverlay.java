package com.Ezzuneware.EzNaguaSlayer;


import com.example.EthanApiPlugin.Collections.TileObjects;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Optional;

public class EzNaguaSlayerOverlay extends OverlayPanel {

    private final Client client;
    private final EzNaguaSlayerPlugin plugin;

    @Inject
    private EzNaguaSlayerOverlay(Client client, EzNaguaSlayerPlugin plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.BOTTOM_LEFT);
        setPreferredSize(new Dimension(185, 180));

    }

    @Override
    public Dimension render(Graphics2D graphics) {


        panelComponent.getChildren().add(TitleComponent.builder()
                .text("EzNaguaSlayer")
                .color(Color.WHITE)
                .build());

        panelComponent.getChildren().add(TitleComponent.builder()
                .text(plugin.isStarted() ? "Running" : "Paused")
                .color(plugin.isStarted() ? Color.GREEN : Color.RED)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("State: ")
                .leftColor(Color.blue)
                .right(plugin.getState() != null ? plugin.getState().name() : "null")
                .rightColor(Color.WHITE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Prayer: ")
                .leftColor(new Color(125, 125, 255))
                .right(plugin.getCurrentPrayLvl() + " " + plugin.getPrayerBaseLvl())
                .rightColor(Color.WHITE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Idle ticks: ")
                .leftColor(Color.WHITE)
                .right(String.valueOf(plugin.getIdleTicks()))
                .rightColor(Color.WHITE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Timeout: ")
                .leftColor(Color.WHITE)
                .right(String.valueOf(plugin.getTimeout()))
                .rightColor(Color.WHITE)
                .build());

                panelComponent.getChildren().add(LineComponent.builder()
                .left("Aggro Reset: ")
                .leftColor(Color.WHITE)
                .right(String.valueOf(plugin.getAggroTimer()))
                .rightColor(Color.WHITE)
                .build());

        return super.render(graphics);
    }
}