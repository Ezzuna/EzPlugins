package com.Ezzuneware.EzGarbageCollector;


import com.example.EthanApiPlugin.Collections.ETileItem;
import com.example.EthanApiPlugin.Collections.TileItems;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@PluginDescriptor(
        name = "EzGarbageCollector",
        description = "Picks up loot so you don't have to",
        enabledByDefault = false,
        tags = {"eco", "plugin"}
)
@Slf4j
public class EzGarbageCollectorPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzGarbageCollectorConfig config;
    @Inject
    private EzGarbageCollectorOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    @Getter
    private boolean started = false;
    public int timeout = 0;
    private List<Integer> highlightedItemsList = new CopyOnWriteArrayList<>();
    @Getter
    private State state = State.IDLE;
    private boolean acted = false;

    private LocalPoint targetPoint;
    private WorldPoint startTile;
    private int targetItemCount = 0;
    public int itemsCollectedCount = 0;


    @Getter
    public int idleTicks = 0;

    @Provides
    private EzGarbageCollectorConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzGarbageCollectorConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        keyManager.registerKeyListener(toggle);
        overlayManager.add(overlay);
        timeout = 0;
        idleTicks = 0;
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
        startTile = client.getLocalPlayer().getWorldLocation();
        acted = false;
        if (state == State.IDLE) {

            for (int targetId : highlightedItemsList) {
                if (!acted) {
                    TileItems.search().filter(t -> startTile.distanceTo(t.getLocation()) <= config.maxDist())
                            .withId(targetId).nearestToPlayer().ifPresent(target -> {
                                TileItemPackets.queueTileItemAction(target, false);
                                state = State.COLLECTING;
                                acted = true;
                            });
                }
            }

            idleTicks = 0;
            return;

        } else if (state == State.COLLECTING) {
            ++idleTicks;
        }

        if (idleTicks > 8) {
            state = State.IDLE;
        }

    }

    @Subscribe
    private void onItemSpawned(ItemSpawned event) {
        TileItem item = event.getItem();
        Tile tile = event.getTile();
        if (state == State.IDLE) {
            for (Integer itemId : highlightedItemsList) {
                if (item.getId() == itemId) {
                    state = State.COLLECTING;
                    targetPoint = tile.getLocalLocation();
                    MousePackets.queueClickPacket();
                    TileItemPackets.queueTileItemAction(new ETileItem(tile.getWorldLocation(), item), false);
                }
            }
        }

    }

    @Subscribe
    private void onItemDespawned(ItemDespawned event) {
        final TileItem item = event.getItem();
        final LocalPoint location = event.getTile().getLocalLocation();
        if (location == targetPoint) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Target item despawned.", null);
            targetPoint = null;
            state = State.IDLE;
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        final ItemContainer container = event.getItemContainer();
        if (container != client.getItemContainer(InventoryID.INVENTORY)) {
            return;
        }

        final Item[] inv = container.getItems();
        int currentInventTotal = 0;
        for (Item item : inv) {
            if (highlightedItemsList.contains(item.getId()))
                ++currentInventTotal;
        }
        if (state == State.COLLECTING && currentInventTotal > targetItemCount) {
            //We picked up the item
            state = State.IDLE;
            targetPoint = null;
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Target item collected.", null);
        }
    }

    private void Reset() {
        startTile = client.getLocalPlayer().getWorldLocation();
        highlightedItemsList = Stream.of(config.getHighlightItems().split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        state = State.IDLE;
        targetPoint = null;
        targetItemCount = 0;
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
        if (started)
            Reset();
    }
}