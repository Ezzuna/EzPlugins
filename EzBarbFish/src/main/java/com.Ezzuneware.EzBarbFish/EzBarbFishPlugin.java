package com.Ezzuneware.EzBarbFish;


import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InteractionHelper;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.NPCInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.BreakHandler.ReflectBreakHandler;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NpcChanged;
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

import java.util.*;

import static net.runelite.api.ItemID.*;

@PluginDescriptor(name = "EzBarbFish", description = "One click tick barb fish", enabledByDefault = false, tags = {"eco", "plugin"})
@Slf4j
public class EzBarbFishPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzBarbFishConfig config;
    @Inject
    private ReflectBreakHandler breakHandler;
    @Inject
    private EzBarbFishOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    private boolean started = false;
    public int timeout = 0;
    public int fishingSpotId = 1542;
    public int backupFishingSpotId = 1526;
    private int fishPartsCount = 0;
    private int fullFishCount = 0;
    private int fallbackFullFishCount = 0;
    private int herbCount = 0;
    private int tarCount = 0;
    private int knifeCount = 0;
    private int lastInventTotal = 0;
    private NPC currentTarget = null;
    private Boolean isFishing = false;
    private Boolean runOnce = false;
    private List<Integer> HerbItems = new ArrayList<Integer>();
    private List<Integer> FishItems = new ArrayList<Integer>();
    private List<Integer> FishPartItems = new ArrayList<Integer>();
    private List<Integer> FallBackFishItems = new ArrayList<Integer>();
    private int tickFailChance = 12;
    private int ticksSinceCastRod = 0;

    @Provides
    private EzBarbFishConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzBarbFishConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        keyManager.registerKeyListener(toggle);
        overlayManager.add(overlay);
        timeout = 0;
        HerbItems.add(249);     //guam
        HerbItems.add(255);     //harry
        HerbItems.add(259);     //irit

        FishItems.add(11328);   //LTrout
        FishItems.add(11330);   //LSalmon
        FishItems.add(11332);   //LSturgeon

        FishPartItems.add(11324);   //Roe
        FishPartItems.add(11326);   //Caviar

        FallBackFishItems.add(331);
        FallBackFishItems.add(335);
        started = !started;

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

//        if (breakHandler.shouldBreak(this)) {
//            breakHandler.startBreak(this);
//            return;
//        }
        final Player local = client.getLocalPlayer();
        final Actor interact = local.getInteracting();
        int animId = -1;
        int targetId = -1;
        if (currentTarget != null) {
            targetId = currentTarget.getId();
        }
        if (interact != null) {
            animId = local.getAnimation();
        }

        if (interact == null) {
            isFishing = false;
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Player isn't interacting.", null);
        } else if (animId == 5249 || animId == 6702) {
            //if we're grinding herbs or cutting fish, we need to stop right now. So lets just click to walk under ourselves
            isFishing = false;
            Optional<TileObject> safeTile = TileObjects.search().withAction("Walk here").nearestToPlayer();
            if (safeTile.isPresent()) {
                TileObjectInteraction.interact(safeTile.get(), "Walk here");
                TileObjectInteraction.interact(safeTile.get(), "Walk here");
                TileObjectInteraction.interact(safeTile.get(), "Walk here");
            }
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Player was doing tick animation (aka idle/risking using mats).", null);
        } else if ((targetId == fishingSpotId || targetId == backupFishingSpotId) && (animId == 9350 || animId == 9349)) {
            isFishing = true;
            timeout = timeout == 0 ? 2 : timeout - 1;
        }
        if (isFishing && timeout == 2) {
            runOnce = true;
            HandleFishing(currentTarget, true);

        } else if (isFishing) {

//            Random r = new Random();
//            int low = 0;
//            int high = 100;
//            int result = r.nextInt(high - low) + low;
//            if (result > 4) //4% chance to miss a tick. We're cosplaying good players.
//                ++ticksSinceCastRod;
        } else {

        }
        runOnce = false;
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

    private void EatRoute(NPC npc, Boolean forceRun) {
        MenuEntry eatEntry = client.createMenuEntry(-1).setOption("One-tick Fish").setType(MenuAction.RUNELITE).onClick(e -> {
            DoBarbFishingEatRoute(npc);
        });
        if (forceRun)
            DoBarbFishingEatRoute(npc);

    }

    private void HerbRoute(NPC npc, Boolean forceRun) {
        //herb, tar, knife, fish, fishing spot
        if (herbCount == 0 || tarCount < 15) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Player lacks requirements to grind herbs.", null);
            return;
        }


        MenuEntry fishEntry = client.createMenuEntry(-1).setOption("One-tick Fish").setType(MenuAction.RUNELITE).onClick(e -> {

            doBarbFishing(npc);

        });
        if (forceRun) {
            doBarbFishing(npc);
//            NPCInteraction.interact(npc, "One-tick Fish");
//            currentTarget = npc;
//            runOnce = true;
        }

    }

    private void doBarbFishing(NPC npc) {
        if (config.ThreeTicking() && fullFishCount >= config.minFishGoods()) {
            Random r = new Random();
            int low = 0;
            int high = 100;
            int result = r.nextInt(high - low) + low;

            if (result > tickFailChance) {
                Widget herb = Inventory.search().idInList(HerbItems).first().get();
                Widget tar = Inventory.search().withId(1939).first().get();
                MousePackets.queueClickPacket();
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetOnWidget(herb, tar);
            }
        }

        if (fullFishCount > 0 && knifeCount > 0) {
            Widget fish = Inventory.search().idInList(FishItems).first().get();
            Widget knife = Inventory.search().withId(946).first().get();
            MousePackets.queueClickPacket();
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetOnWidget(fish, knife);

        } else if (fallbackFullFishCount > 0 && knifeCount > 0) {
            Widget part = Inventory.search().idInList(FallBackFishItems).first().get();
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(part, "Drop");
        }
        String option = npc.getId() == fishingSpotId ? "Use-rod" : "Lure";
        NPCInteraction.interact(npc, option);
        currentTarget = npc;
        isFishing = true;
        runOnce = true;
    }

    private void DoBarbFishingEatRoute(NPC npc) {
        Widget part = Inventory.search().idInList(FishPartItems).first().get();
        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetAction(part, "Eat");

        if (fullFishCount > 0 && knifeCount > 0) {
            Widget fish = Inventory.search().idInList(FishItems).first().get();
            Widget knife = Inventory.search().withId(946).first().get();
            MousePackets.queueClickPacket();
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetOnWidget(fish, knife);

        } else if (config.ThreeTicking()) {
            Widget herb = Inventory.search().idInList(HerbItems).first().get();
            Widget tar = Inventory.search().withId(1939).first().get();
            MousePackets.queueClickPacket();
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetOnWidget(herb, tar);
        }
        NPCInteraction.interact(npc, "Use-rod");
        currentTarget = npc;
        isFishing = true;
        runOnce = true;
    }

    @Override
    public String toString() {
        return super.toString();
    }


    @Subscribe
    public void onNpcChanged(NpcChanged npc) {
        if (npc.getNpc() == currentTarget) {
            currentTarget = null;
        }
    }

    private void HandleFishing(NPC npc, Boolean forceRun) {
        if (fishPartsCount > config.minFishGoods()) {
            EatRoute(npc, forceRun);
        } else {
            HerbRoute(npc, forceRun);
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded menuEntryAdded) {
        if (client.getGameState() != GameState.LOGGED_IN) return;
        var me = menuEntryAdded.getMenuEntry();
        var targetNpc = menuEntryAdded.getMenuEntry().getNpc();
        if (targetNpc != null && me.getType() == MenuAction.NPC_FIRST_OPTION) {     //added the first option check to hopefully only remove top entry
            if (targetNpc.getId() == fishingSpotId || targetNpc.getId() == backupFishingSpotId) {
                HandleFishing(targetNpc, false);
            }

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
        fishPartsCount = 0;
        fullFishCount = 0;
        herbCount = 0;
        tarCount = 0;
        knifeCount = 0;
        fallbackFullFishCount = 0;

        for (Item item : inv) {
            switch (item.getId()) {
                case CAVIAR:
                    ++fishPartsCount;
                    break;
                case ROE:
                    ++fishPartsCount;
                    break;
                case LEAPING_SALMON:
                case LEAPING_STURGEON:
                case LEAPING_TROUT:
                    ++fullFishCount;
                    ++currentInventTotal;
                    break;
                case GUAM_LEAF:
                case IRIT_LEAF:
                case HARRALANDER:
                    ++herbCount;
                    break;
                case SWAMP_TAR:
                    tarCount += item.getQuantity();
                    break;
                case KNIFE:
                    ++knifeCount;
                    break;
                case RAW_TROUT:
                case RAW_SALMON:
                    ++fallbackFullFishCount;
                    ++currentInventTotal;
                    break;
            }
        }
        if (currentTarget != null && !runOnce && currentInventTotal > lastInventTotal) {
            runOnce = true;
            lastInventTotal = currentInventTotal;
            //ticksSinceCastRod = 0;
            //HandleFishing(currentTarget, true);

        } else {
            lastInventTotal = currentInventTotal;
        }
    }

}


