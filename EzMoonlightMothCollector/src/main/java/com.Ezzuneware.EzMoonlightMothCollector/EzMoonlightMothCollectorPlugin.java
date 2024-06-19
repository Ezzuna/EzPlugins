package com.Ezzuneware.EzMoonlightMothCollector;


import com.example.EthanApiPlugin.Collections.*;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.BankInteraction;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.API.BankUtil;
import com.piggyplugins.PiggyUtils.API.InventoryUtil;
import com.piggyplugins.PiggyUtils.API.PlayerUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
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
import org.apache.commons.lang3.RandomUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@PluginDescriptor(
        name = "EzMoonlightMothCollector",
        description = "",
        enabledByDefault = false,
        tags = {"eco", "plugin"}
)
@Slf4j
public class EzMoonlightMothCollectorPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzMoonlightMothCollectorConfig config;
    @Inject
    private EzMoonlightMothCollectorOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private PlayerUtil playerUtil;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    @Getter
    private boolean started = false;

    private String butterflyName = "Moonlight moth";
    private final WorldPoint opTileSpot = new WorldPoint(1572, 9445, 0);
    @Getter
    public int timeout = 0;
    @Getter
    public int idleTicks = 0;
    @Getter
    private State state = State.idle;
    private int castleWarsChunkId = 9776;

    @Provides
    private EzMoonlightMothCollectorConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzMoonlightMothCollectorConfig.class);
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

        if(Equipment.search().matchesWildCardNoCase("Dueling ring*").first().isEmpty() && Inventory.search().matchesWildCardNoCase("Dueling ring*").first().isPresent()){
            Widget ring = Inventory.search().matchesWildCardNoCase("Dueling ring*").first().get();
            InventoryInteraction.useItem(ring, "Wear");
            return;
        }

        if (state == State.kitbanking) {
            Optional<Widget> emptyJar = Inventory.search().withName("Butterfly jar").first();
            if (emptyJar.isEmpty())
                handleKitBanking();
            else
                client.runScript(29);
        }

        if (state == State.banking) {
            restockItems();
            return;
        }
        if (state == State.travelling) {
            handleTravelling();
            return;
        }

        if (state == State.toBank) {
            if (client.getLocalPlayer().getWorldLocation().getRegionID() == 6291) {
                TeleportToBank();
                return;
            }
        }

        if (state == State.idle) {
            doButterfly();
            setTimeout();
            return;
        }

    }

    @Subscribe
    private void onChatMessage(ChatMessage event) {
        String message = event.getMessage();

        if (message.contains("quetzal whistle has 0 charges")){
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "","OUT OF CHARGES. GO GET MEAT.", null);
            started = false;
        }
    }

    private void handleKitBanking() {
        Optional<Widget> kitScreen = Widgets.search().withParentId(57081866).first();
        if (kitScreen.isPresent()) {

            Widget test = Widgets.search().withItemId(10012).first().get();
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(test, "Withdraw-All");
            setTimeout();
            return;
        } else {
            InventoryInteraction.useItem("Huntsman's kit", "View");
            setTimeout();
            return;
        }
    }

    private void handleTravelling() {
        if (client.getLocalPlayer().getWorldLocation().getRegionID() == castleWarsChunkId) //castle wars
        {
            InventoryInteraction.useItem(29273, "Signal");
            setTimeout();
            return;
        } else if (client.getLocalPlayer().getWorldLocation().getRegionID() == 6191) {
            TileObjectInteraction.interactNearest(51641, "Climb-down");
            setTimeout();
            return;
        }
    }

    private void restockItems() {
        if (Bank.isOpen()) {

            Optional<Widget> jar = BankInventory.search().matchesWildCardNoCase("*moth*").first();
            if (jar.isPresent()) {
                BankInteraction.useItem(jar.get(), "Deposit-All");
                return;
            }

            Optional<EquipmentItemWidget> ring = Equipment.search().matchesWildCardNoCase("Ring of dueling*").first();
            if (ring.isEmpty()) {
                Optional<Widget> ringInvent = BankInventory.search().matchesWildCardNoCase("Ring of dueling*").first();
                Optional<Widget> bankFood = BankUtil.nameContainsNoCase("Ring of dueling").first();
                if (bankFood.isPresent() && ringInvent.isEmpty()) {
                    BankInteraction.useItem(bankFood.get(), "Withdraw-1");
                    return;
                }
            }
            client.runScript(29);
            return;


        } else {
            Optional<TileObject> bankBooth = TileObjects.search().withName("Bank chest").nearestToPlayer();
            if (bankBooth.isPresent()) {
                MousePackets.queueClickPacket();
                TileObjectInteraction.interact(bankBooth.get(), "Use");
                setTimeout();
            }
        }
    }

    private void TeleportToBank() {
        Optional<EquipmentItemWidget> dueling = Equipment.search().matchesWildCardNoCase("Ring of dueling*").first();
        dueling.ifPresent(ring -> {
            InventoryInteraction.useItem(ring, "Castle Wars");
            setTimeout();
            return;
        });
        return;
    }


    private State DetermineState() {

        Optional<Widget> kitScreen = Widgets.search().withId(57081867).first();
        if (kitScreen.isPresent())
            return State.kitbanking;

        List<Widget> filledJars = Inventory.search().withAction("Release").withName(butterflyName).result();
        Optional<Widget> emptyJar = Inventory.search().withName("Butterfly jar").first();
        if (emptyJar.isEmpty()) {
            if (client.getLocalPlayer().getWorldLocation().getRegionID() == castleWarsChunkId) //castle wars{
            {
                if (filledJars.isEmpty())
                    return State.kitbanking;
                return State.banking;
            } else
                return State.toBank;
        }

        if (client.getLocalPlayer().getWorldLocation().getRegionID() == 6291) {
            return State.idle;
        } else {
            return State.travelling;
        }

    }

    private void doButterfly() {
        Optional<NPC> butterfly = NPCs.search().withName(butterflyName).withAction("Catch").nearestToPoint(opTileSpot);
        List<Widget> filledJars = Inventory.search().withAction("Release").withName(butterflyName).result();
        Optional<Widget> emptyJar = Inventory.search().withName("Butterfly jar").first();

        if (filledJars.isEmpty()) {

        }

        checkRunEnergy();


        if (EthanApiPlugin.isMoving()) return;

        if (client.getLocalPlayer().getInteracting() == null && emptyJar.isPresent()) {
            if (butterfly.isPresent()) {
//                log.info("CATCHING BUTTERFLY");
                MousePackets.queueClickPacket();
                NPCPackets.queueNPCAction(butterfly.get(), "Catch");
            }
        }

    }


    private void checkRunEnergy() {
        if (playerUtil.isRunning() && playerUtil.runEnergy() <= 10) {
            log.info("Run");
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetActionPacket(1, 10485787, -1, -1);
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