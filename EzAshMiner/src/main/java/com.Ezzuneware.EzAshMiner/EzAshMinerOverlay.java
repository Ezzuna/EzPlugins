package com.Ezzuneware.EzAshMiner;


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

public class EzAshMinerOverlay extends OverlayPanel {

    private final Client client;
    private final EzAshMinerPlugin plugin;

    @Inject
    private EzAshMinerOverlay(Client client, EzAshMinerPlugin plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);

    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.setPreferredSize(new Dimension(200, 320));
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Current State: ")
                .leftColor(Color.YELLOW)
                .right(String.valueOf(plugin.getState()))
                .rightColor(Color.WHITE)
                .build());
        return super.render(graphics);
    }
}