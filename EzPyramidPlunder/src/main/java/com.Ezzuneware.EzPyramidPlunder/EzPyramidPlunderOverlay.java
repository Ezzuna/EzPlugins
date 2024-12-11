package com.Ezzuneware.EzPyramidPlunder;


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

public class EzPyramidPlunderOverlay extends OverlayPanel {

    private final Client client;
    private final EzPyramidPlunderPlugin plugin;

    @Inject
    private EzPyramidPlunderOverlay(Client client, EzPyramidPlunderPlugin plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.BOTTOM_LEFT);
        setPreferredSize(new Dimension(185, 180));

    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("EzPyramidPlunder")
                .color(Color.WHITE)
                .build());

        panelComponent.getChildren().add(TitleComponent.builder()
                .text(plugin.isStarted() ? "Running" : "Paused")
                .color(plugin.isStarted() ? Color.GREEN : Color.RED)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Region ID: ")
                .leftColor(Color.blue)
                .right(String.valueOf(plugin.currentRegion))
                .rightColor(Color.WHITE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("State: ")
                .leftColor(Color.blue)
                .right(plugin.getState() != null ? plugin.getState().name() : "null")
                .rightColor(Color.WHITE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Timeout: ")
                .leftColor(new Color(255, 0, 255))
                .right(String.valueOf(plugin.getTimeout()))
                .rightColor(Color.WHITE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Floor: ")
                .leftColor(new Color(255, 0, 255))
                .right(String.valueOf(plugin.getCurrentFloor()))
                .rightColor(Color.WHITE)
                .build());


        return super.render(graphics);
    }
}