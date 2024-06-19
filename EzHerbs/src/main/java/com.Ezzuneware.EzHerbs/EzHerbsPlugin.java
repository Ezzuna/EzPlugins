package com.Ezzuneware.EzHerbs;


import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import java.util.Set;

@PluginDescriptor(
        name = "EzHerbs",
        description = "Auto-flowering herbs.",
        enabledByDefault = false,
        tags = {"eco", "plugin", "ez"}
)
@Slf4j
public class EzHerbsPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzHerbsConfig config;
    @Inject
    private EzHerbsOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    private boolean started = false;
    public int timeout = 0;
    private final Set<Integer> REGION_IDS = Set.of(6462, 6461);

    //Empty -> *birdhouse
    //chisel 1755
    //hammer2347
    //Teak logs 6333

    //Build -> Space 30568(bhouse)
    //Use Chisel -> *Logs
    //Use *seed -> *birdhouse (empty)

    //Use -> Mushtree 30924
    //WIDGET 39845888 parentid. Path: S 161.15 RESIZABLE_VIEW...S 161.16, N 608.0, Value: D 608.0[0]
    //HouseOnHill [1]
    //VerdantValley [5]
    //MushroomMeadow [11]
    //Travel is the menu option


    @Provides
    private EzHerbsConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzHerbsConfig.class);
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