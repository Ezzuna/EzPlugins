package com.Ezzuneware.EzEdgeSmelter.tasks;

import com.Ezzuneware.EzEdgeSmelter.EzEdgeSmelterConfig;
import com.Ezzuneware.EzEdgeSmelter.EzEdgeSmelterPlugin;
import com.Ezzuneware.EzEdgeSmelter.Product;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.piggyplugins.PiggyUtils.strategy.AbstractTask;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.TileObject;
import net.runelite.api.widgets.Widget;

import java.util.Optional;

@Slf4j
public class OpenFurnace extends AbstractTask<EzEdgeSmelterPlugin, EzEdgeSmelterConfig> {
    //    @Inject
//    protected Client client;
//    @Inject
//    protected ClientThread clientThread;
    private Optional<TileObject> furnace;

    public OpenFurnace(EzEdgeSmelterPlugin plugin, EzEdgeSmelterConfig config) {
        super(plugin, config);
    }

    @Override
    public boolean validate() {
        if (plugin.isSmithing)
            return false;
        furnace = TileObjects.search().withName("Furnace").nearestToPlayer();
        if (plugin.config.product() == Product.BAR) {
            Widget smeltingInterface = plugin.getClient().getWidget(17694733);
            return furnace.isPresent() && smeltingInterface == null && plugin.hasEnoughOres();
        } else {
            Widget smeltingInterface = plugin.getClient().getWidget(393216);      //change this
            return furnace.isPresent() && smeltingInterface == null && plugin.hasEnoughGems() && plugin.hasEnoughBars() && plugin.hasCorrectMould();
        }


    }

    @Override
    public void execute() {
        log.info("interacting with furnace");
        boolean action = interactObject(furnace.get(), "Smelt");
//        boolean action = TileObjectInteraction.interact(anvil.get(), "Smith");
        if (!action)
            log.info("failed furnace interaction");
        plugin.timeout = config.tickDelay() == 0 ? 1 : config.tickDelay();//must be at least 1
    }
}
