package com.Ezzuneware.EzEdgeSmelter.tasks;

import com.Ezzuneware.EzEdgeSmelter.EzEdgeSmelterConfig;
import com.Ezzuneware.EzEdgeSmelter.EzEdgeSmelterPlugin;
import com.Ezzuneware.EzEdgeSmelter.Gem;
import com.Ezzuneware.EzEdgeSmelter.Product;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.PacketUtils.WidgetInfoExtended;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.piggyplugins.PiggyUtils.strategy.AbstractTask;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.widgets.Widget;

import java.util.List;
import java.util.Optional;

import static com.example.PacketUtils.PacketReflection.client;

@Slf4j
public class DoSmithing extends AbstractTask<EzEdgeSmelterPlugin, EzEdgeSmelterConfig> {
//    @Inject
//    protected Client client;
//    @Inject
//    protected ClientThread clientThread;

    public DoSmithing(EzEdgeSmelterPlugin plugin, EzEdgeSmelterConfig config) {
        super(plugin, config);
    }

    @Override
    public boolean validate() {
        if (plugin.isSmithing)
            return false;
        if (config.gem() != Gem.NONE || config.product() != Product.BAR)
            return false;
        Widget smithingInterface = plugin.getClient().getWidget(17694733);
        return smithingInterface != null && plugin.hasEnoughOres();

        //17694737
    }

    @Override
    public void execute() {
        log.info("Smeltingz: " + config.bar().getProduct());
        MousePackets.queueClickPacket();
        //interactWidget not implemented yet
        Optional<Widget> continue1Opt = Widgets.search().withId(17694737).hiddenState(false).first();
        if (client.getWidget(17694737) != null) {
            List<Widget> unusedOre = Inventory.search().filter(item -> item.getName().contains(config.bar().getName())).result();

            WidgetPackets.queueResumePause(17694737, unusedOre.size());
            //WidgetPackets.queueResumePause(17694737,4);
            plugin.isSmithing = true;
            plugin.timeout = 110;
        }

    }
}

//17694733
//17694721
//17694720

//17694734
//17694737