package com.Ezzuneware.EzAshMiner;


import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

@PluginDescriptor(
        name = "EzAshMiner",
        description = "Mines Volcanic Ash",
        enabledByDefault = false,
        tags = {"eco", "plugin"}
)
@Slf4j
public class EzAshMinerPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzAshMinerConfig config;
    @Inject
    private EzAshMinerOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    private boolean started = false;
    public int timeout = 0;
    private int moving_timeout = 0;
    private int moving_timeout_cutoff = 12;
    private GameObject targetObject = null;



    enum MINER_STATE {
        IDLE,
        MINING,
        MOVING,
        FULLINVENT
    }

    public MINER_STATE state;
    public MINER_STATE getState() {return state;}

    @Provides
    private EzAshMinerConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzAshMinerConfig.class);
    }


    @Override
    protected void startUp() throws Exception {
        keyManager.registerKeyListener(toggle);
        overlayManager.add(overlay);
        timeout = 0;
        state = MINER_STATE.IDLE;

        started = true;
    }

    @Override
    protected void shutDown() throws Exception {
        keyManager.unregisterKeyListener(toggle);
        overlayManager.remove(overlay);
        timeout = 0;
    }

    private void ClickAshPile() {

    }

    private void HandleAshing() {

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
        final Player local = client.getLocalPlayer();
        final Actor interact = local.getInteracting();
        int animId = -1;
        int targetId = -1;
        animId = local.getAnimation();
        ++moving_timeout;
        if (Inventory.full()) {
            state = MINER_STATE.FULLINVENT;
            return;
        } else if (animId == -1 && interact == null && state != MINER_STATE.MOVING)
            state = MINER_STATE.IDLE;
        else if (animId == 627)
            state = MINER_STATE.MINING;

        if (state == MINER_STATE.MOVING) {

            if (moving_timeout > moving_timeout_cutoff) {
                state = MINER_STATE.IDLE;
                moving_timeout = 0;
            }
        }

        if (!Inventory.full() && state == MINER_STATE.IDLE) {
            TileObjects.search().withId(30985).nearestToPlayer().ifPresent(tileObject -> {
                MousePackets.queueClickPacket();
                if (TileObjectInteraction.interact(tileObject, "Mine")) {
                    state = MINER_STATE.MOVING;
                    moving_timeout = 0;
                } else {
                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "No Ash Found. Implement world hopper.", null);
                }

            });
        }


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
    }


}