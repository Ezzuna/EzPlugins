package com.Ezzuneware.EzPitfallHunter;


import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.NPCInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import org.apache.commons.lang3.RandomUtils;
import org.lwjgl.system.linux.Stat;

import java.util.Optional;
import java.util.Set;

@PluginDescriptor(
        name = "EzPitfallHunter",
        description = "",
        enabledByDefault = false,
        tags = {"eco", "plugin"}
)
@Slf4j
public class EzPitfallHunterPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzPitfallHunterConfig config;
    @Inject
    private EzPitfallHunterOverlay overlay;
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
    public int jumpTimeout = 0;
    public int targetTimeout = 0;
    private final Set<Integer> REGION_IDS = Set.of(6291);
    private int logCount = 0;
    private int antlerCount = 0;
    private int idleTicks = 0;
    private NPC holdingNPC = null;
    //deadnpc trap = "Dismantle Collapsed trap"
    //"Jump Spiked trap
    //Tease Moonlight antelope

    private int empty1 = 51679;
    private int empty2 = 51680;
    private int empty3 = 51681;

    private final WorldPoint killPoint = new WorldPoint(1553, 9419, 0);
    private final WorldPoint trapPoint = new WorldPoint(1555, 9419, 0);
    private final WorldPoint backupKillPoint = new WorldPoint(1558, 9419, 0);

    public int BoostedHitpoints;
    public int minHitpoints = 17;
    private final int ROOTS_ID = 51746;
    @Getter
    private State state = State.idle;

    @Provides
    private EzPitfallHunterConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzPitfallHunterConfig.class);
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
    public void onWidgetLoaded(WidgetLoaded e) {
        if (e.getGroupId() == WidgetID.BANK_GROUP_ID) {


        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded mea) {
        if (client.getGameState() != GameState.LOGGED_IN)
            return;
        if (!EthanApiPlugin.loggedIn() || !REGION_IDS.contains(client.getLocalPlayer().getWorldLocation().getRegionID())) {
            started = false;
            state = State.idle;
            return;
        }
        var targetNpc = mea.getMenuEntry().getNpc();
        if (targetNpc == null)
            return;
        if (targetNpc.getName().contains("Moonlight antelope")) {
            mea.getMenuEntry().onClick(i -> {
                holdingNPC = targetNpc;
                setTimeoutTripple();
            });
        }
    }


    @Subscribe
    private void onGameTick(GameTick event) {
        if (targetTimeout > 0)
            --targetTimeout;
        if (timeout > 0) {
            timeout--;
            return;
        }
        if (!EthanApiPlugin.loggedIn() || !started) {
            return;
        }

        BoostedHitpoints = client.getBoostedSkillLevel(Skill.HITPOINTS);
        CheckInventory();

        if (holdingNPC != null) {
            holdingNPC = NPCStillActive() ? holdingNPC : null;
        }
        state = DiscoverState();

        if (state == State.too_weak) {
            Optional<Widget> food = Inventory.search().matchesWildCardNoCase(config.foodName()).first();
            if (food.isPresent()) {
                InventoryInteraction.useItem(food.get(), "Eat");
                setTimeout();
                return;
            } else {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "OUTTA FOOD", null);
                started = false;
            }
        }


        if (state == State.chiseling) {
            ++idleTicks;
            return;
        }

        if (state == State.cleaning) {
            idleTicks = 0;
            GatherLoot();
            return;
        }

        if (state == State.hopping) {
            Optional<TileObject> spikePit = TileObjects.search().withName("Spiked pit").first();
            spikePit.ifPresent(pit -> {
                TileObjectInteraction.interact(pit, "Jump");
                setTimeout();
                return;
            });
            idleTicks = 0;
            return;
        }

        if (state == State.building) {
            idleTicks = 0;
            BuildTrap();
            return;
        }

        if (state == State.gatheringRoots) {
            idleTicks = 0;
            GatherRoots();
            return;
        }

        if (state == State.tagging) {
            idleTicks = 0;
            holdingNPC = NPCs.search().withName("Moonlight antelope").nearestToPoint(backupKillPoint).stream().findFirst().get();
            NPCInteraction.interact(holdingNPC, "Tease");
            setTimeout();
        }

    }

    private Boolean NPCStillActive() {
        return holdingNPC.isInteracting();
    }

    private State DiscoverState() {

        if (idleTicks > 8) {
            state = State.idle;
        }
        if (state == State.chiseling && antlerCount > 0) {
            return state;
        }

        if (CheckForLoot()) {
            return State.cleaning;
        }

        if (holdingNPC == null && logCount < 3) {

            return State.gatheringRoots;


        }


        Optional<TileObject> trapTile = TileObjects.search().withName("Pit").atLocation(trapPoint).first();
        if (trapTile.isPresent())
            return State.building;

        Optional<TileObject> spikePit = TileObjects.search().withName("Spiked pit").first();

        if (holdingNPC == null) {
            if (BoostedHitpoints <= minHitpoints)
                return State.too_weak;
            else
                return State.tagging;
        }

        if (spikePit.isPresent()) {
            var x = spikePit.get().getWorldLocation();
            return State.hopping;
        }


        Optional<TileObject> lootPit = TileObjects.search().withName("Collapsed trap").first();
        if (lootPit.isPresent())
            return State.cleaning;


        return State.idle;
    }

    private Boolean CheckForLoot() {
        Optional<TileObject> collapsedPit = TileObjects.search().withName("Collapsed trap").nearestToPlayer();

        return collapsedPit.isPresent();
    }

    private void GatherLoot() {
        Optional<TileObject> collapsedPit = TileObjects.search().withName("Collapsed trap").nearestToPlayer();
        collapsedPit.ifPresent(obj -> {
            TileObjectInteraction.interact(obj, "Dismantle");
            setTimeout();
            return;
        });
    }

    private void BuildTrap() {
        Optional<TileObject> trapTile = TileObjects.search().withName("Pit").atLocation(trapPoint).first();
        trapTile.ifPresent(trap -> TileObjectInteraction.interact(trap, "Trap"));
        setTimeout();
    }

    private void GatherRoots() {
        TileObjectInteraction.interactNearest(ROOTS_ID, "Take-log");
        setTimeout();
        return;
    }

    private void CheckInventory() {
        logCount = Inventory.getItemAmount("Logs");
        antlerCount = Inventory.getItemAmount("Moonlight antelope antler");

        if (state == State.chiseling)
            return;

        Optional<Widget> antler = Inventory.search().withName("Moonlight antelope antler").first();
        Optional<Widget> chisel = Inventory.search().withName("Chisel").first();

        if (antler.isPresent() && chisel.isPresent()) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetOnWidget(antler.get(), chisel.get());
            state = State.chiseling;
            return;
        }


        Optional<Widget> bone = Inventory.search().matchesWildCardNoCase("*ones").first();
        bone.ifPresent(widget -> InventoryInteraction.useItem(widget, "Drop"));

        Optional<Widget> fur = Inventory.search().matchesWildCardNoCase("*ur").first();
        fur.ifPresent(widget -> InventoryInteraction.useItem(widget, "Drop"));

        Optional<Widget> meat = Inventory.search().matchesWildCardNoCase("Raw*").first();
        meat.ifPresent(widget -> InventoryInteraction.useItem(widget, "Drop"));
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

    private void setTimeout() {
        timeout = RandomUtils.nextInt(config.tickDelayMin(), config.tickDelayMax());
    }

    private void setTimeoutTripple() {
        targetTimeout = RandomUtils.nextInt(config.tickDelayMax() * 2, config.tickDelayMax() * 3);
    }

}