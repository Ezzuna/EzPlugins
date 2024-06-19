package com.Ezzuneware.EzGemMineBot;


import com.example.EthanApiPlugin.Collections.Equipment;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.BreakHandler.ReflectBreakHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
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
import java.util.Optional;
import java.util.Set;

@PluginDescriptor(
        name = "EzGemMineBot",
        description = "Please have a pickaxe equiped. Only works inside the gem mine, do the diary (medium). Will add support for gem bag when I have one to test. ",
        enabledByDefault = false,
        tags = {"eco", "plugin"}
)
@Slf4j
public class EzGemMineBotPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private ReflectBreakHandler breakHandler;
    @Inject
    private EzGemMineBotConfig config;
    @Inject
    private EzGemMineBotOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    @Getter
    private State state = State.idle;
    private final Set<Integer> REGION_IDS = Set.of(11410);
    @Getter
    private boolean started = false;
    @Getter
    public int timeout = 0;
    int rock = 0;
    private Instant timer;

    @Provides
    private EzGemMineBotConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzGemMineBotConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        breakHandler.registerPlugin(this);
        keyManager.registerKeyListener(toggle);
        overlayManager.add(overlay);
        timeout = 0;
    }

    @Override
    protected void shutDown() throws Exception {
        breakHandler.unregisterPlugin(this);
        keyManager.unregisterKeyListener(toggle);
        overlayManager.remove(overlay);
        timeout = 0;
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

    @Subscribe
    private void onGameTick(GameTick event) {
        if (timeout > 0) {
            timeout--;
            return;
        }
        if (!EthanApiPlugin.loggedIn() || !started || !REGION_IDS.contains(client.getLocalPlayer().getWorldLocation().getRegionID())) {
            return;
        }

        if (breakHandler.shouldBreak(this)) {
            breakHandler.startBreak(this);
            return;
        }

        if (depositInventory()) {
            return;
        }

        Player player = client.getLocalPlayer();
        int animId = player.getAnimation();


        timeout = timeout == 0 ? 2 : timeout - 1;
        if (timeout != 2) return;
        if (client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) == 1000) {
            if (!Equipment.search().matchesWildCardNoCase("*Dragon pickaxe*").empty() || !Equipment.search().matchesWildCardNoCase("*infernal pickaxe*").empty()) {
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetActionPacket(1, 38862885, -1, -1);
            }
        }

        Optional<Widget> guam = Inventory.search().withId(ItemID.GUAM_LEAF).first();
        Optional<Widget> tar = Inventory.search().withId(ItemID.SWAMP_TAR).first();
        Optional<Widget> pestle = Inventory.search().withId(ItemID.PESTLE_AND_MORTAR).first();
        if (guam.isEmpty() || tar.isEmpty() || pestle.isEmpty()) {
            EthanApiPlugin.stopPlugin(this);
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "please make sure you have guam leaf and swamp tar" +
                    " and a pestle and mortar before starting", null);
            return;
        }

        if (Inventory.search().withId(ItemID.GUAM_LEAF).onlyUnnoted().result().size() > 1) {
            EthanApiPlugin.stopPlugin(this);
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "plugin not able to work with more than one " +
                    "cleaned guam in inventory", null);
            return;
        }
        if (tar.get().getItemQuantity() < 15) {
            EthanApiPlugin.stopPlugin(this);
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "plugin not able to work with less than 15 swamp tar" +
                    " in inventory", null);
            return;
        }


        if (Inventory.full()) {
            state = State.ferrying;
            TileObjectInteraction.interactNearest("Bank Deposit Chest", "Deposit");
            return;
        }
        if (animId == 6746) {
            state = State.mining;

        } else if (animId == -1) {
            state = State.idle;

        }

        if (state == State.idle || state == State.mining) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetOnWidget(guam.get(), tar.get());
            rock = rock == 3 ? 0 : rock + 1;
            state = State.mining;
            TileObjectInteraction.interactNearest("Gem rocks", "Mine");
            return;
        }


    }

    public boolean depositInventory() {

        if (state == State.timeout)
            return false;

        if (Inventory.getEmptySlots() >= 24 && state == State.banking) {


            state = State.mining;
            TileObjectInteraction.interactNearest("Gem rocks", "Mine");
            return false;

        } else if (isBankOpen()) {
            state = State.banking;
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetActionPacket(1, ComponentID.BANK_DEPOSIT_INVENTORY, -1, -1);
            return true;
        } else if (Widgets.search().withId(12582916).first().isPresent()) {
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
}