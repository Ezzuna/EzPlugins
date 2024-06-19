package com.Ezzuneware.EzStarReminer;


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

public class EzStarReminerOverlay extends OverlayPanel {

    private final Client client;
    private final EzStarReminerPlugin plugin;

    @Inject
    private EzStarReminerOverlay(Client client, EzStarReminerPlugin plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.BOTTOM_LEFT);
        setPreferredSize(new Dimension(185, 180));

    }

    @Override
    public Dimension render(Graphics2D graphics) {

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("EzStarReminer")
                .color(Color.WHITE)
                .build());

        panelComponent.getChildren().add(TitleComponent.builder()
                .text(plugin.isStarted() ? "Running" : "Paused")
                .color(plugin.isStarted() ? Color.GREEN : Color.RED)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Elapsed Time: ")
                .leftColor(Color.white)
                .right(plugin.getElapsedTime())
                .rightColor(Color.WHITE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("State: ")
                .leftColor(Color.blue)
                .right(plugin.getState() != null ? plugin.getState().name() : "null")
                .rightColor(Color.WHITE)
                .build());


        panelComponent.getChildren().add(LineComponent.builder()
                .left("Idle ticks: ")
                .leftColor(Color.WHITE)
                .right(String.valueOf(plugin.getIdleTicks()))
                .rightColor(Color.WHITE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Stardust: ")
                .leftColor(Color.YELLOW)
                .right(String.valueOf(plugin.getDustEarned()))
                .rightColor(Color.YELLOW)
                .build());

        return super.render(graphics);
    }
}