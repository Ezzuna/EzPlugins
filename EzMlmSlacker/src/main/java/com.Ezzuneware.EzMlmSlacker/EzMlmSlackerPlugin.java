package com.Ezzuneware.EzMlmSlacker;


import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.PacketUtils.WidgetID;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.ComponentID;
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

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import static net.runelite.api.ItemID.*;

@PluginDescriptor(
        name = "EzMlmSlacker",
        description = "Makes MLM a lot easier. After you click a vein this will continue mining fresh veins until you are full invent. Makes it actually AFK. MLM is a pretty big bot hotspot, so trying extra hard to avoid bans here.",
        enabledByDefault = false,
        tags = {"eco", "plugin"}
)
@Slf4j
public class EzMlmSlackerPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzMlmSlackerConfig config;
    @Inject
    private EzMlmSlackerOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    @Getter(AccessLevel.PACKAGE)
    private int curSackSize;
    @Getter
    private boolean started = false;
    public int timeout = 0;
    @Getter
    private int idleTicks = 0;
    @Getter
    private State state = State.timeout;
    private Instant timer;

    @Provides
    private EzMlmSlackerConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzMlmSlackerConfig.class);
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

    private void CalcSackValues() {
        curSackSize = client.getVarbitValue(Varbits.SACK_NUMBER);
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

        if(depositInventory()){
            return;
        }
        Player player = client.getLocalPlayer();
        int animId = player.getAnimation();



        if (animId == 6752) {
            //we're mining
            idleTicks = 0;
            state = State.mining;
            return;
        } else if (animId == -1 && (state == State.mining || state == State.idle)) {
            idleTicks++;
            state = State.idle;
        }

        if (state == State.ferrying) {
            return;
        }

        if (!Inventory.full() && state != State.full && state != State.returning) {
            if (idleTicks > 7) {
                Random r = new Random();
                int low = 0;
                int high = 4;
                int result = r.nextInt(high - low) + low;
                if (result >= 2) {
                    try {
                        TileObjectInteraction.interactNearest("Ore vein", "Mine");
                        idleTicks = 0;
                    } catch (Exception ex) {
                        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Unable to locate vein. Disabling plugin.", null);
                        state = State.timeout;
                        started = false;
                    }
                }


            }
        } else {
            state = State.full;
        }


    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        final ItemContainer container = event.getItemContainer();

        if (container != client.getItemContainer(InventoryID.INVENTORY)) {
            return;
        }

        if (!started)
            return;

        final Item[] inv = container.getItems();
        int currentInventTotal = 0;
        for (Item item : inv) {
            switch (item.getId()) {
                case COAL:
                    ++currentInventTotal;
                case GOLD_ORE:
                    ++currentInventTotal;
                case MITHRIL_ORE:
                    ++currentInventTotal;
                case ADAMANTITE_ORE:
                    ++currentInventTotal;
                case IRON_ORE:
                    ++currentInventTotal;
                case RUNITE_ORE:
                    ++currentInventTotal;
                case GOLDEN_NUGGET:
                    ++currentInventTotal;
            }
        }
        if (currentInventTotal > 0 && state != State.ferrying && state != State.banking) {
            //we just emptied sack
            TileObjectInteraction.interact("Bank deposit box", "Deposit");
            state = State.ferrying;
        }
    }

    public boolean depositInventory() {

        if(state == State.timeout)
            return false;

        if(Inventory.getEmptySlots() == 28 && state == State.banking){
            CalcSackValues();
            if(curSackSize > 0){
                TileObjectInteraction.interact("Sack", "Search");
                state = State.emptying;
            }
            else{
                state = State.timeout;
            }
        }

        else if (isBankOpen()) {
            state = State.banking;
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetActionPacket(1, ComponentID.BANK_DEPOSIT_INVENTORY, -1, -1);
            return true;
        } else if ( Widgets.search().withId(12582916).first().isPresent()) {
            state = State.banking;
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetActionPacket(1, 12582916, -1, -1);
            return true;
        }
        return false;
    }

    private boolean isBankOpen() {
        Widget bank = client.getWidget(WidgetInfo.BANK_CONTAINER);
        return bank != null && !bank.isHidden();
    }

    private final HotkeyListener toggle = new HotkeyListener(() -> config.toggle()) {
        @Override
        public void hotkeyPressed() {
            toggle();
        }
    };

    public void toggle() {
        if (!EthanApiPlugin.loggedIn()) {
            return;
        }
        started = !started;
        if (started) {
            timer = Instant.now();
        }
    }

    public String getElapsedTime() {
        if (!started) {
            return "00:00:00";
        }
        Duration duration = Duration.between(timer, Instant.now());
        long durationInMillis = duration.toMillis();
        long second = (durationInMillis / 1000) % 60;
        long minute = (durationInMillis / (1000 * 60)) % 60;
        long hour = (durationInMillis / (1000 * 60 * 60)) % 24;

        return String.format("%02d:%02d:%02d", hour, minute, second);
    }
}