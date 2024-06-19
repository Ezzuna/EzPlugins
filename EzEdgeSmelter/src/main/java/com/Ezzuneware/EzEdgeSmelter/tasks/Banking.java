package com.Ezzuneware.EzEdgeSmelter.tasks;

import com.Ezzuneware.EzEdgeSmelter.EzEdgeSmelterConfig;
import com.Ezzuneware.EzEdgeSmelter.EzEdgeSmelterPlugin;
import com.Ezzuneware.EzEdgeSmelter.Gem;
import com.Ezzuneware.EzEdgeSmelter.Product;
import com.example.EthanApiPlugin.Collections.Bank;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.BankInventoryInteraction;
import com.example.InteractionApi.InventoryInteraction;
import com.example.PacketUtils.WidgetInfoExtended;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.piggyplugins.PiggyUtils.strategy.AbstractTask;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.widgets.Widget;

@Slf4j
public class Banking extends AbstractTask<EzEdgeSmelterPlugin, EzEdgeSmelterConfig> {
//    @Inject
//    protected Client client;
//    @Inject
//    protected ClientThread clientThread;

    public Banking(EzEdgeSmelterPlugin plugin, EzEdgeSmelterConfig config) {
        super(plugin, config);
    }

    @Override
    public boolean validate() {
        return Bank.isOpen() && (!plugin.hasEnoughOres() && plugin.config.product() == Product.BAR
                || (plugin.config.gem() != Gem.NONE && (!plugin.hasEnoughGems() || !plugin.hasEnoughBars() || !plugin.hasCorrectMould())));
    }

    @Override
    public void execute() {
        log.info("Do Banking");
        bankHandler();
    }

    private void bankHandler() {

        if (plugin.config.product() == Product.BAR) {
            Widget depositInventory = plugin.getClient().getWidget(WidgetInfoExtended.BANK_DEPOSIT_INVENTORY.getPackedId());
            if (depositInventory != null) {
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetAction(depositInventory, "Deposit inventory");
            }


            Bank.search().withName(config.bar().getName()).first().ifPresentOrElse(bar -> {
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetAction(bar, "Withdraw-All");
            }, () -> {
                EthanApiPlugin.sendClientMessage("No ores left");
                EthanApiPlugin.stopPlugin(plugin);
            });
            plugin.timeout = config.tickDelay();
        } else {
            if (plugin.hasAnyProduct()) {
//                Integer idToFind = -1;
//                switch (config.gem()) {
//                    case JADE:
//                        idToFind = config.product().getJadeId();
//                        break;
//                    case OPAL:
//                        idToFind = config.product().getOpalId();
//                        break;
//                    case RedTopaz:
//                        idToFind = config.product().getTopazId();
//                        break;
//                }
                String product = "";
                if (config.gem() == Gem.RedTopaz) {
                    product = "Topaz " + config.product().getName();
                } else{
                    product = config.gem().getName() + " " + config.product().getName();
                }

                BankInventoryInteraction.useItem(product, "Deposit-All");
                plugin.timeout = config.tickDelay();
            }

            if (!plugin.hasCorrectMould()) {
                Bank.search().withId(config.product().getMouldId()).first().ifPresentOrElse(bar -> {
                    MousePackets.queueClickPacket();
                    WidgetPackets.queueWidgetAction(bar, "Withdraw-1");
                }, () -> {
                    EthanApiPlugin.sendClientMessage("No mould left ?");
                    EthanApiPlugin.stopPlugin(plugin);
                });
                plugin.timeout = config.tickDelay();

            }

            if (!plugin.hasEnoughGems()) {
                Bank.search().withName(config.gem().getName()).first().ifPresentOrElse(bar -> {
                    MousePackets.queueClickPacket();
                    WidgetPackets.queueWidgetAction(bar, "Withdraw-13");
                }, () -> {
                    EthanApiPlugin.sendClientMessage("No gems left.");
                    EthanApiPlugin.stopPlugin(plugin);
                });
                plugin.timeout = config.tickDelay();

            }

            if (!plugin.hasEnoughBars()) {
                Bank.search().withName(config.gem().getBarCompanion()).first().ifPresentOrElse(bar -> {
                    MousePackets.queueClickPacket();
                    WidgetPackets.queueWidgetAction(bar, "Withdraw-13");
                }, () -> {
                    EthanApiPlugin.sendClientMessage("No bars left.");
                    EthanApiPlugin.stopPlugin(plugin);
                });
                plugin.timeout = config.tickDelay();

            }


        }

    }
}
