package com.Ezzuneware.EzStall_stealer;


import com.example.EthanApiPlugin.Collections.TileObjects;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Optional;

public class EzStall_stealerOverlay extends OverlayPanel {

    private final Client client;
    private final EzStall_stealerPlugin plugin;

    @Inject
    private EzStall_stealerOverlay(Client client, EzStall_stealerPlugin plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.BOTTOM_LEFT);
        setPreferredSize(new Dimension(180, 170));

    }

    @Override
    public Dimension render(Graphics2D graphics) {

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("EzStallStealer")
                .color(Color.WHITE)
                .build());

        panelComponent.getChildren().add(TitleComponent.builder()
                .text(plugin.isStarted() ? "Running" : "Paused")
                .color(plugin.isStarted() ? Color.GREEN : Color.RED)
                .build());

        return super.render(graphics);
    }
}