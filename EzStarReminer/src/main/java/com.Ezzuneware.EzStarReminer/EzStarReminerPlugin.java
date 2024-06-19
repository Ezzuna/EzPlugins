package com.Ezzuneware.EzStarReminer;


import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.query.ItemQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
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
import java.util.Optional;

@PluginDescriptor(
        name = "EzStarReminer",
        description = "Allows you to fully afk a star from start to finish.",
        enabledByDefault = false,
        tags = {"eco", "plugin"}
)
@Slf4j
public class EzStarReminerPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzStarReminerConfig config;
    @Inject
    private EzStarReminerOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    @Getter
    private boolean started = false;
    public int timeout = 0;
    @Getter
    private State state = State.timeout;
    @Getter
    private int idleTicks = 0;
    @Getter
    private int dustEarned = 0;
    private int dustCount = 0;
    private boolean inventoryTotalsSetup = false;
    private Instant timer;

    @Provides
    private EzStarReminerConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzStarReminerConfig.class);
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

        if(!inventoryTotalsSetup)
            setUpInventTotals();

        Player player = client.getLocalPlayer();
        int animId = player.getAnimation();

        if (animId == 6746) {
            //we're mining
            idleTicks = 0;
            state = State.mining;
            return;
        } else if (animId == -1) {
            idleTicks++;
            state = State.idle;
        }

        if (idleTicks > 3) {
            try {
                TileObjectInteraction.interact("Crashed Star", "Mine");
                idleTicks = 0;
            } catch (Exception ex) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Unable to locate star. Disabling plugin.", null);
                state = State.timeout;
                started = false;
            }

        }

    }

    private void ResetValues() {
        timer = Instant.now();

        idleTicks = 0;
        inventoryTotalsSetup = false;
    }

    private void setUpInventTotals() {
        Optional<Widget> dust = Inventory.search().withId(ItemID.STARDUST).first();
        if(dust.get().getItemQuantity() > 0){
            dustCount = dust.get().getItemQuantity();
        }
        else dustCount = 0;
        inventoryTotalsSetup = true;


    }


    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {

        if (!started)
            return;
        final ItemContainer container = event.getItemContainer();

        if (container != client.getItemContainer(InventoryID.INVENTORY)) {
            return;
        }


        Optional<Widget> dust = Inventory.search().withId(ItemID.STARDUST).first();
        if(dust.get().getItemQuantity() > 0){
            dustEarned = dust.get().getItemQuantity() - dustCount;
        }
        else dustEarned = 0;



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
            ResetValues();
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