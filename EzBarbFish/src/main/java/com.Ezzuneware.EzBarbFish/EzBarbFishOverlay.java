package com.Ezzuneware.EzBarbFish;


import com.example.EthanApiPlugin.Collections.TileObjects;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Optional;

public class EzBarbFishOverlay extends OverlayPanel {

    private final Client client;
    private final EzBarbFishPlugin plugin;

    @Inject
    private EzBarbFishOverlay(Client client, EzBarbFishPlugin plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);

    }

    @Override
    public Dimension render(Graphics2D graphics) {
        return super.render(graphics);
    }
}