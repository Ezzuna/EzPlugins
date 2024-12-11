package com.Ezzuneware.EzSpindel;


import com.example.EthanApiPlugin.Collections.TileObjects;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Optional;

public class EzSpindelOverlay extends OverlayPanel {

    private final Client client;
    private final EzSpindelPlugin plugin;

    @Inject
    private EzSpindelOverlay(Client client, EzSpindelPlugin plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.BOTTOM_LEFT);
        setPreferredSize(new Dimension(185, 180));

    }

    @Override
    public Dimension render(Graphics2D graphics) {
        return super.render(graphics);
    }
}