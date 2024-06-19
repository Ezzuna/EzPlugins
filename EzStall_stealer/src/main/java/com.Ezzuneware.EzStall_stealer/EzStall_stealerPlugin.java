package com.Ezzuneware.EzStall_stealer;


import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectSpawned;
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

import java.util.Arrays;
import java.util.Set;

import static com.example.EthanApiPlugin.EthanApiPlugin.canPathToTile;
import static net.runelite.api.ItemID.*;

@PluginDescriptor(
        name = "EzStall_stealer",
        description = "Steals from stalls, stalls from steals.",
        enabledByDefault = false,
        tags = {"eco", "plugin"}
)
@Slf4j
public class EzStall_stealerPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzStall_stealerConfig config;
    @Inject
    private EzStall_stealerOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    @Getter
    private boolean started = false;

    public int timeout = 0;
    private final Set<Integer> STALL_IDS = Set.of(11730, 11731);

    @Provides
    private EzStall_stealerConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzStall_stealerConfig.class);
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
    private void onGameObjectSpawned(GameObjectSpawned event) {
        if (!EthanApiPlugin.loggedIn() || !started) {
            return;
        }


        if (Inventory.full()) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Stall spawned but we're full dog", null);
            return;
        }
        final GameObject gameObject = event.getGameObject();

        if (STALL_IDS.contains(gameObject.getId())) {
            if (Inventory.full()) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Stall spawned but we're full dog", null);
                return;
            }
            WorldPoint loc = gameObject.getWorldLocation();
            EthanApiPlugin.PathResult result;
            result = canPathToTile(loc);
            if (result.isReachable() && result.getDistance() <= 3) {
                TileObjectInteraction.interact(gameObject, "Steal-from");
            }
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (!EthanApiPlugin.loggedIn() || !started) {
            return;
        }
        final ItemContainer container = event.getItemContainer();


        int[] array = Arrays.stream(config.itemIds().split(",")).mapToInt(Integer::parseInt).toArray();
        if (container != client.getItemContainer(InventoryID.INVENTORY)) {
            return;
        }
        final Item[] inv = container.getItems();
        for (Item item : inv) {
            if (Arrays.stream(array).anyMatch(x -> x == item.getId())) {
                InventoryInteraction.useItem(item.getId(), "Drop");
            }
        }
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