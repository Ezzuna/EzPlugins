package com.Ezzuneware.EzWintertodt;


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

public class EzWintertodtOverlay extends OverlayPanel {

    private final Client client;
    private final EzWintertodtPlugin plugin;

    @Inject
    private EzWintertodtOverlay(Client client, EzWintertodtPlugin plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.BOTTOM_LEFT);
        setPreferredSize(new Dimension(185, 180));

    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("EzWintertodt")
                .color(Color.WHITE)
                .build());

        panelComponent.getChildren().add(TitleComponent.builder()
                .text(plugin.isStartWT() ? "Running" : "Paused")
                .color(plugin.isStartWT() ? Color.GREEN : Color.RED)
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
                .left("WT Alive: ")
                .leftColor(Color.gray)
                .right(plugin.isGameRunning() ? "Yes" : "No")
                .rightColor(Color.LIGHT_GRAY)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Idle ticks: ")
                .leftColor(Color.WHITE)
                .right(String.valueOf(plugin.getIdleTicks()))
                .rightColor(Color.WHITE)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Food Left: ")
                .leftColor(Color.YELLOW)
                .right(String.valueOf(plugin.getFoodInInvent()))
                .rightColor(Color.GREEN)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Logs Burned: ")
                .leftColor(Color.RED)
                .right(String.valueOf(plugin.getLogsBurned()))
                .rightColor(Color.YELLOW)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Crates Earned: ")
                .leftColor(Color.LIGHT_GRAY)
                .right(String.valueOf(plugin.getCratesEarned()))
                .rightColor(Color.DARK_GRAY)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Crates Failed: ")
                .leftColor(Color.DARK_GRAY)
                .right(String.valueOf(plugin.getCratesFailed()))
                .rightColor(Color.LIGHT_GRAY)
                .build());


        return super.render(graphics);
    }
}