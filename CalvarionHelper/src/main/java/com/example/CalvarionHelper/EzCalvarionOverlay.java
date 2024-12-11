package com.example.CalvarionHelper;


import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class EzCalvarionOverlay extends OverlayPanel {

    private final Client client;
    private final CalvarionHelper plugin;

    @Inject
    private EzCalvarionOverlay(Client client, CalvarionHelper plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.BOTTOM_LEFT);
        setPreferredSize(new Dimension(185, 180));

    }

    @Override
    public Dimension render(Graphics2D graphics) {


        panelComponent.getChildren().add(TitleComponent.builder()
                .text("EzCalverion")
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
                .left("Timeout: ")
                .leftColor(Color.WHITE)
                .right(String.valueOf(plugin.getTimeout()))
                .rightColor(Color.WHITE)
                .build());




        return super.render(graphics);
    }
}