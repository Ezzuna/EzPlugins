package com.Ezzuneware.EzCannonballSmelter;

import com.Ezzuneware.EzApi.EzApi;
import com.example.EthanApiPlugin.Collections.*;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.BankInteraction;
import com.example.InteractionApi.BankInventoryInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.PacketUtils.WidgetID;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
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
import java.util.Optional;

@PluginDescriptor(
        name = "<html><font color=\"#00c27e\">[Ez]</font>EzCannonballSmelter</html>",
        description = "Supports double mould. Tested in Edgeville.",
        enabledByDefault = false,
        tags = {"eco", "plugin", "ez"}
)
@Slf4j
public class EzCannonballSmelterPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzCannonballSmelterConfig config;
    @Inject
    private EzCannonballSmelterOverlay overlay;
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
    private int steelbar_count = 0;
    private int idleTicks = 0;

    @Provides
    private EzCannonballSmelterConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzCannonballSmelterConfig.class);
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

    private void CheckInventory() {
        steelbar_count = Inventory.getItemAmount(2353);

        Optional<Widget> mould = Inventory.search().matchesWildCardNoCase("*mmo mould").first();
        if(mould.isEmpty()){
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "NO MOULD FOUND IN INVENTORY. STOPPING.", null);
            started = false;
        }
    }

    private State DetermineState() {
        if(IsSmeltMenuOpen())
            return State.smelting;

        int animId = client.getLocalPlayer().getAnimation();
        if (animId == 899 || animId == 827) {
            return State.busy;
        }

        if (Inventory.getItemAmount(2353) == 0) {
            return State.resupplying;
        } else if(state == State.resupplying){
            //if we were last resupplying and now have our bars we want to skip the delay
            return State.smelting;
        }
        else if (idleTicks > 4) {
            return State.smelting;
        }

        return State.idle;

    }

    private boolean IsSmeltMenuOpen(){
         Optional<Widget> smeltScreen = Widgets.search().withId(17694733).first();
         return smeltScreen.isPresent();
    }

    private void DoSmelt() {
        if(Bank.isOpen()){
            client.runScript(29);
            return;
        }

        if (IsSmeltMenuOpen()) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueResumePause(17694734, steelbar_count);
            setTimeout();
            return;
        }

        TileObjectInteraction.interactNearest("Furnace", "Smelt");
        setTimeout();
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
                if (!Text.removeTags(item.getName()).contains("mmo mould")) {
                    MousePackets.queueClickPacket();
                    BankInventoryInteraction.useItem(item, "Deposit-All");
                    return;
                }

            }
            BankInteraction.withdrawX(Bank.search().withId(2353).first().get(), 27);
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


    @Subscribe
    private void onGameTick(GameTick event) {

        if (!EthanApiPlugin.loggedIn() || !started) {
            return;
        }

        if (timeout > 0) {
            timeout--;
            return;
        }
        CheckInventory();
        state = DetermineState();

        switch (state) {
            case idle:
                ++idleTicks;
                break;
            case smelting:
                DoSmelt();
                idleTicks = 0;
                break;
            case resupplying:
                Restock();
                break;
            case busy:
                idleTicks = 0;
                break;
        }


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