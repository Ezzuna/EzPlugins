package com.Ezzuneware.EzMiningGuildIronMiner;

import com.Ezzuneware.EzApi.EzApi;
import com.Ezzuneware.EzApi.Utility.EzWorldManagement;
import com.example.EthanApiPlugin.Collections.*;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.BankInventoryInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.PacketUtils.WidgetID;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.RandomUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@PluginDescriptor(
        name = "<html><font color=\"#00c27e\">[Ez]</font>EzMiningGuildIronMiner</html>",
        description = "Mines Iron, deposits iron. Meant for grinding mining gloves. Only usuable in the 60 mining section of mining guild. Hops away from others.",
        enabledByDefault = false,
        tags = {"eco", "plugin", "ez"}
)
@Slf4j
public class EzMiningGuildIronMinerPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzMiningGuildIronMinerConfig config;
    @Inject
    private EzMiningGuildIronMinerOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    @Getter
    private boolean started = false;
    @Getter
    public int timeout = 0;
    @Getter
    private State state = State.idle;
    private net.runelite.api.World quickHopTargetWorld;
    private int displaySwitcherAttempts = 0;
    private WorldPoint miningTile = new WorldPoint(3021, 9721, 0);

    @Provides
    private EzMiningGuildIronMinerConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzMiningGuildIronMinerConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        keyManager.registerKeyListener(toggle);
        overlayManager.add(overlay);
        timeout = 0;
    }

    @Override
    protected void shutDown() throws Exception {
        keyManager.unregisterKeyListener(toggle);
        overlayManager.remove(overlay);
        timeout = 0;
    }


    @Subscribe
    private void onGameTick(GameTick event) {
        if (timeout > 0) {
            timeout--;
            return;
        }
        if (!EthanApiPlugin.loggedIn() || !started) {
            return;
        }

        state = DetermineState();
        if (state == State.mining) {
            DoMining();
            return;
        }
        if (state == State.banking) {
            Restock();
        }
        if (state == State.traveling) {
            DoTraveling();
            setTimeout();
            return;
        }

        if (state == State.hopping) {
            DoHopping();
            setTimeout();
            return;
        }

    }


    private void Restock() {
        if (Widgets.search().withId(WidgetID.SHOP_INVENTORY_GROUP_ID).first().isPresent()) {
            client.runScript(29);
            setTimeout();
            return;
        }

        if (Bank.isOpen()) {
            List<Widget> bankInv = BankInventory.search().result();
            for (Widget item : bankInv) {
                if (!Text.removeTags(item.getName()).contains("pickaxe")) {
                    MousePackets.queueClickPacket();
                    BankInventoryInteraction.useItem(item, "Deposit-All");
                }

            }
            setTimeout();
        } else {
            Optional<TileObject> bankBooth = TileObjects.search().filter(tileObject -> {
                ObjectComposition objectComposition = TileObjectQuery.getObjectComposition(tileObject);
                return getName().toLowerCase().contains("bank") ||
                        Arrays.stream(objectComposition.getActions()).anyMatch(action -> action != null && action.toLowerCase().contains("bank"));
            }).nearestToPlayer();

            if (bankBooth.isPresent()) {

                MousePackets.queueClickPacket();
                TileObjectInteraction.interact(bankBooth.get(), "Bank");
            }

            TileObjects.search().withName("Bank chest").nearestToPlayer().ifPresent(tileObject -> {
                MousePackets.queueClickPacket();
                TileObjectInteraction.interact(tileObject, "Use");
            });

            setTimeout();
        }
    }

    private void DoHopping() {
        EzWorldManagement.hop();
        return;
    }

    private void DoTraveling() {
        if (Bank.isOpen())
            client.runScript(29);
        MousePackets.queueClickPacket();
        MovementPackets.queueMovement(miningTile);
        setTimeout();
    }

    private void DoMining() {
        if (client.getLocalPlayer().isInteracting() || client.getLocalPlayer().getAnimation() == 624)
            return;
        Optional<TileObject> ironOre = TileObjects.search().withId(11364).withinDistance(2).nearestToPlayer();
        if(ironOre.isEmpty())
            ironOre = TileObjects.search().withId(11365).withinDistance(2).nearestToPlayer();
        if (ironOre.isPresent()) {
            TileObjectInteraction.interact(ironOre.get(), "Mine");
            setTimeout();
            return;
        }
    }

    private State DetermineState() {
        if (Inventory.full()) {
            return State.banking;
        }
        if (client.getLocalPlayer().getWorldLocation().distanceTo(miningTile) > 0) {
            return State.traveling;
        }

        for (Player player : client.getPlayers()) {
            if (player.equals(client.getLocalPlayer())) {
                continue;
            }
            if (player.getWorldLocation().distanceTo(miningTile) <= 1) {
                return State.hopping;
            }
        }


        return State.mining;
    }

    private final HotkeyListener toggle = new HotkeyListener(() -> config.toggle()) {
        @Override
        public void hotkeyPressed() {
            toggle();
        }
    };

    private void setTimeout() {
        timeout = RandomUtils.nextInt(config.tickDelayMin(), config.tickDelayMax());
    }

    public void toggle() {
        if (!EthanApiPlugin.loggedIn()) {
            return;
        }
        started = !started;
    }
}