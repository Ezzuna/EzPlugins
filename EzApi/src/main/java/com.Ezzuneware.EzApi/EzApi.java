package com.Ezzuneware.EzApi;


import com.Ezzuneware.EzApi.Combat.EzInventory;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import java.util.Arrays;
import java.util.List;

@PluginDescriptor(
        name = "EzApi",
        description = "Ezzune Api",
        enabledByDefault = false,
        tags = {"eco", "plugin"}
)
@Slf4j
public class EzApi extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzApiConfig config;
    @Inject
    private EzApiOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    @Inject
    public EzInventory ezInventory;
    private boolean started = false;
    public int timeout = 0;

    @Provides
    private EzApiConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzApiConfig.class);
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
