package com.Ezzuneware.EzBlastFurnace.overlays;

import com.Ezzuneware.EzBlastFurnace.EasyBlastFurnaceConfig;
import com.Ezzuneware.EzBlastFurnace.EasyBlastFurnacePlugin;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.Ezzuneware.EzBlastFurnace.state.BlastFurnaceState;
import net.runelite.api.ItemID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.components.TextComponent;

import java.awt.*;

@Singleton
public class CoalBagOverlay extends WidgetItemOverlay
{
    @Inject
    private EasyBlastFurnacePlugin plugin;

    @Inject
    private EasyBlastFurnaceConfig config;

    @Inject
    private BlastFurnaceState state;

    CoalBagOverlay()
    {
        showOnInventory();
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
    {
        if (!plugin.isEnabled()) return;
        if (!(itemId == ItemID.COAL_BAG ||
            itemId == ItemID.COAL_BAG_12019 ||
            itemId == ItemID.COAL_BAG_25627 ||
            itemId == ItemID.OPEN_COAL_BAG))
            return;
        if (!config.showCoalBagOverlay()) return;

        Color color = config.coalBagOverlayColor();

        Rectangle bounds = widgetItem.getCanvasBounds();
        TextComponent textComponent = new TextComponent();

        textComponent.setPosition(new Point(bounds.x - 1, bounds.y + 8));
        textComponent.setColor(color);
        textComponent.setText(Integer.toString(state.getCoalBag().getCoal()));

        textComponent.render(graphics);
    }
}
