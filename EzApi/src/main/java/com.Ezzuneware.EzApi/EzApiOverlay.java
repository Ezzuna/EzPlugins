package com.Ezzuneware.EzApi;


import net.runelite.api.*;
import net.runelite.client.ui.overlay.*;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;

public class EzApiOverlay extends OverlayPanel {

    private final Client client;
    private final EzApi plugin;

    @Inject
    private EzApiOverlay(Client client, EzApi plugin) {
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