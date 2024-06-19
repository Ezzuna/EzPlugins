package com.Ezzuneware.EzSplasher;


import com.example.EthanApiPlugin.Collections.TileObjects;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Optional;

public class EzSplasherOverlay extends OverlayPanel {

    private final Client client;
    private final EzSplasherPlugin plugin;

    @Inject
    private EzSplasherOverlay(Client client, EzSplasherPlugin plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);

    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.setPreferredSize(new Dimension(200, 320));
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Time since splash: ")
                .leftColor(Color.YELLOW)
                .right(String.valueOf(plugin.GetNotCastingTimer()))
                .rightColor(Color.WHITE)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Current time: ")
                .leftColor(Color.YELLOW)
                .right(String.valueOf(plugin.GetTimer()))
                .rightColor(Color.WHITE)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Time to reclick: ")
                .leftColor(Color.YELLOW)
                .right(String.valueOf(plugin.GetTimeoutTimer()))
                .rightColor(Color.WHITE)
                .build());


        return super.render(graphics);
    }
}