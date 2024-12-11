package com.Ezzuneware.EzPyramidPlunder;

import com.Ezzuneware.EzApi.Combat.EzInventory;
import com.Ezzuneware.EzApi.Combat.EzPlayerManagement;
import com.Ezzuneware.EzApi.EzApi;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.NPCInteraction;
import com.example.InteractionApi.PrayerInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.*;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
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
import org.apache.commons.lang3.RandomUtils;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import static net.runelite.api.ItemID.CAVIAR;

@PluginDescriptor(name = "<html><font color=\"#00c27e\">[Ez]</font>EzPyramidPlunder</html>", description = "", enabledByDefault = false, tags = {"eco", "plugin", "ez"})
@Slf4j
public class EzPyramidPlunderPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzPyramidPlunderConfig config;
    @Inject
    private EzPyramidPlunderOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    @Getter
    private boolean started = false;
    @Getter
    private State state = State.idle;
    private int doorCounter = 0;

    private boolean trap_passed = true;
    private boolean inRun = false;
    public int BoostedHitpoints, Hitpoints;
    public int BoostedPrayer, Prayer;
    public int Thieving;
    @Getter
    private int currentFloor = 0;
    private ArrayList<TileObject> searchedUrns = new ArrayList<>();
    private TileObject targetUrn;

    private String TRAP_MESSAGE = "You deactivate the trap!";

    private static final Set<Integer> START_ROOM = ImmutableSet.of(7749);
    private static final Set<Integer> GOOD_URNS = ImmutableSet.of(26580, 26600, 26601, 26602, 26603, 26604, 26605, 26606, 26607, 26608, 26610, 26611, 26612, 26613);
    private static final Integer EMERGENCYEXIT = 20931;
    private static final ArrayList<Integer> DOOR_IDS = new ArrayList<Integer>() {
        {
            add(26624);
            add(26622);
            add(26623);
            add(26625);
        }
    };
    @Getter
    int currentRegion;
    @Getter
    public int timeout = 0;
    private int poisonTimer = 0;
    private int dropTimer = 0;

    @Provides
    private EzPyramidPlunderConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzPyramidPlunderConfig.class);
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

    private Optional<NPC> GetMummy() {
        return NPCs.search().withName("Guardian mummy").first();
    }

    private Optional<TileObject> GetEmergencyDoor() {
        return TileObjects.search().withId(EMERGENCYEXIT).withinDistance(2).first();
    }

    private Optional<TileObject> GetClosestDoor() {
        return TileObjects.search().withName("Tomb Door").withAction("Pick-lock").withinDistance(10).nearestToPlayer();
    }

    private Optional<TileObject> GetNearestTrap() {
        return TileObjects.search().withName("Speartrap").nearestToPlayer();
    }

    //Quick-leave Tomb Door
    private Optional<TileObject> GetEntranceDoor() {
        return TileObjects.search().withName("Tomb Door").withAction("Pick-lock").withinDistance(8).nearestToPlayer();
    }

    private Optional<TileObject> GetFinalDoor() {
        return TileObjects.search().withName("Tomb Door").withAction("Quick-Leave").withinDistance(10).nearestToPlayer();
    }

    private Optional<TileObject> GetSarc() {
        return TileObjects.search().withName("Sarcophagus").withAction("Open").withinDistance(7).nearestToPlayer();
    }

    private Optional<TileObject> GetExitDoor() {
        return TileObjects.search().withId(20932).nearestToPlayer();
    }

    private Optional<TileObject> GetChest() {
        return TileObjects.search().withName("Grand Gold Chest").withAction("Search").withinDistance(7).nearestToPlayer();
    }

    private Optional<TileObject> GetUrn() {
        return TileObjects.search().withName("Urn").withAction("Search").filter(to -> GOOD_URNS.contains(to.getId()) && !searchedUrns.contains(to)).withinDistance(6).nearestToPlayer();
    }

    private void Drink() {
        Optional<Widget> moth = Inventory.search().matchesWildCardNoCase("Moonlight moth mix*").first();
        if (moth.isPresent()) {
            InventoryInteraction.useItem(moth.get(), "Drink");
            return;
        }

        Optional<Widget> prayPot = Inventory.search().matchesWildCardNoCase("Prayer pot*").first();
        if (prayPot.isPresent()) {
            InventoryInteraction.useItem(prayPot.get(), "Drink");
            return;
        }
    }

    private void Eat() {
        EzInventory.EatFirstFoodInCSS(config.foodList());
        return;
    }

    private boolean CanGoNextFloor() {
        return Thieving >= (currentFloor + 1) * 10 + 11;
    }

    private void DoDooring() {

        if (CanGoNextFloor()) {
            Optional<TileObject> door = GetClosestDoor();
            if (door.isPresent()) {
                TileObjectInteraction.interact(door.get(), "Pick-lock");
                setTimeout();
                return;
            }
        } else {
            Optional<TileObject> door = GetFinalDoor();
            if (door.isPresent()) {
                TileObjectInteraction.interact(door.get(), "Quick-leave");
                setTimeout();
                return;
            }
        }


    }

    @Subscribe
    private void onChatMessage(ChatMessage message) {
        if (message.getType() == ChatMessageType.SPAM) {
            //Handle failed teleports
            if (message.getMessage().contains("You deactivate the trap!")) {
                trap_passed = true;
            }
        }
    }

    private void DoTrap() {
        Optional<TileObject> trap = GetNearestTrap();
        if (trap.isPresent()) {
            TileObjectInteraction.interact(trap.get(), "Pass");
            setTimeout();
            return;
        }
    }

    private void DoLobby() {
        Optional<NPC> mummy = GetMummy();
        if (mummy.isPresent()) {
            NPCInteraction.interact(mummy.get(), "Start-minigame");
            setTimeout();
            return;
        } else {
            Optional<TileObject> tombExit = GetExitDoor();
            if (tombExit.isPresent()) {
                TileObjectInteraction.interact(tombExit.get(), "Leave Tomb");
                setTimeout();
                return;
            }

            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "UNABLE TO FIND EXIT DOOR. STOPPING.", null);
            started = false;
            return;
        }

    }

    private void TogglePrayer(boolean prayerState) {
        PrayerInteraction.setPrayerState(net.runelite.api.Prayer.PROTECT_FROM_MELEE, prayerState);
    }

    private boolean CheckInventoryGold(){

        return !Inventory.search().matchesWildCardNoCase("Gold*").empty() || CheckInventory(false);
    }

    private boolean CheckInventory(boolean dropping) {

        ArrayList<Widget> itemsToDrop = new ArrayList<Widget>();

        itemsToDrop.addAll(Inventory.search().matchesWildCardNoCase("Ivory comb").result());

        itemsToDrop.addAll(Inventory.search().matchesWildCardNoCase("Pottery*").result());


        itemsToDrop.addAll(Inventory.search().matchesWildCardNoCase("Stone*").result());


//        itemsToDrop.addAll(Inventory.search().matchesWildCardNoCase("Butterfly jar").result());
//
//
//        itemsToDrop.addAll(Inventory.search().matchesWildCardNoCase("Vial").result());

        if(!dropping)
            return !itemsToDrop.isEmpty();

        int counter = 0;
        for(Widget widget : itemsToDrop){
            if(counter < 4){
                ++counter;
                InventoryInteraction.useItem(widget, "Drop");
            }
            else
                break;
        }
        return !itemsToDrop.isEmpty();

    }

    private void DoTravelling() {
        if (EthanApiPlugin.isMoving()) return;
        Optional<TileObject> doorIn = TileObjects.search().withId(DOOR_IDS.get(doorCounter)).first();
        if (doorIn.isPresent()) {
            TileObjectInteraction.interact(doorIn.get(), "Search");
            setTimeout();
            return;
        }
    }

    private void DoLooting() {

        if (GetUrn().isPresent() && !CanGoNextFloor()) {
            targetUrn = GetUrn().get();
            TileObjectInteraction.interact(GetUrn().get(), "Search");
            setTimeout();
            return;
        }
        Optional<TileObject> chest = GetChest();
        if (chest.isPresent()) {
            TileObjectInteraction.interact(chest.get(), "Search");
            setTimeout();
            return;
        }

        Optional<TileObject> sarc = GetSarc();
        if (sarc.isPresent()) {
            TileObjectInteraction.interact(sarc.get(), "Open");
            setTimeout();
            return;
        }
    }

     @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
         final ItemContainer container = event.getItemContainer();

         if (container != client.getItemContainer(InventoryID.INVENTORY)) {
             return;
         }

         final Item[] inv = container.getItems();
        if(CanGoNextFloor())
            return;
        if(CheckInventoryGold() && targetUrn != null){
            searchedUrns.add(targetUrn);
            targetUrn = null;
        }

     }

    private void DoPoison() {
        int animId = client.getLocalPlayer().getAnimation();
        if (animId == 4340 || animId == 4341 || animId == 4342) {
            //we're digging through urns, we don't wanna interupt the anim here. Check next tick.
            ++poisonTimer;
            return;
        }

        Optional<Widget> book = Inventory.search().withName("Prayer book").first();

        if (book.isPresent() && Prayer > 2) {
            InventoryInteraction.useItem(book.get(), "Recite-prayer");
            return;
        }

        Optional<Widget> poisonPot = Inventory.search().matchesWildCardNoCase("*poison*").first();
        if (poisonPot.isPresent()) {
            InventoryInteraction.useItem(poisonPot.get(), "Drink");
            return;
        }

    }

    private State DetermineState() {

        int animId = client.getLocalPlayer().getAnimation();

        if (currentRegion == 7749 && (currentFloor <= 0 || !inRun)) return State.lobby;

        if (currentRegion == 13099) {
            if (Inventory.full()) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "FULL INVENT. NOT SORTED BANKING. STOPPING.", null);
                started = false;
                return State.busy;
            }
            inRun = false;
            return State.travelling;
        }

        if (animId == 4342) {
            if (targetUrn != null)
                searchedUrns.add(targetUrn);
            return State.busy;
        }
        if (animId == 4341) {
            targetUrn = null;
            return State.busy;
        }
        if (animId == 832) {
            setTimeout();
            return State.busy;
        }

        if (animId == 4344 || animId == 4345 || animId == 4340)
            return State.busy;
        if (!trap_passed) return State.pretrap;


        if (GetSarc().isPresent() || GetChest().isPresent()) return State.looting;

        if (currentFloor > 0)
            return State.dooring;


        return State.idle;
    }

    @Subscribe
    private void onGameTick(GameTick event) {
        if (poisonTimer > 0) {
            poisonTimer--;
            if (poisonTimer == 0)
                DoPoison();
        }
        if (dropTimer > 0) {
            dropTimer--;
            if (dropTimer == 0)
                CheckInventory(true);
        } else if (CheckInventory(false)) {
            dropTimer = RandomUtils.nextInt(2, 7);
        }
        if (timeout > 0) {
            timeout--;
            return;
        }
        if (!EthanApiPlugin.loggedIn() || !started) {
            return;
        }

        TogglePrayer(EzPlayerManagement.IsUnderAttack());

        if (!inRun) {
            inRun = GetEmergencyDoor().isPresent();
        }


        if (client.getVarbitValue(Varbits.PYRAMID_PLUNDER_ROOM) != currentFloor) {
            searchedUrns = new ArrayList<>();
            //idle when entering new room
            currentFloor = client.getVarbitValue(Varbits.PYRAMID_PLUNDER_ROOM);
            trap_passed = false;
            setTimeout();
            return;
        }

        Prayer = client.getRealSkillLevel(Skill.PRAYER);
        BoostedPrayer = client.getBoostedSkillLevel(Skill.PRAYER);
        Hitpoints = client.getRealSkillLevel(Skill.HITPOINTS);
        BoostedHitpoints = client.getBoostedSkillLevel(Skill.HITPOINTS);
        Thieving = client.getRealSkillLevel(Skill.HITPOINTS);

        if (BoostedPrayer < Prayer / 3 && currentFloor > 0) {
            Drink();
        }

        if (client.getVarpValue(VarPlayer.POISON) > 0 && poisonTimer == 0) {
            poisonTimer = RandomUtils.nextInt(3, 7);
        }

        if (config.eatAt() > Hitpoints) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "EATING THRESHOLD SET HIGHER THAN HP LEVEL. STOPPING.", null);
            started = false;
            return;
        }

        if (BoostedHitpoints < config.eatAt()) {
            Eat();
        }


        int animId = client.getLocalPlayer().getAnimation();


        currentFloor = client.getVarbitValue(Varbits.PYRAMID_PLUNDER_ROOM);


        if (currentRegion != client.getLocalPlayer().getWorldLocation().getRegionID() && client.getLocalPlayer().getWorldLocation().getRegionID() == 13099) {
            doorCounter = doorCounter < DOOR_IDS.size() - 1 ? doorCounter + 1 : 0;
        }
        currentRegion = client.getLocalPlayer().getWorldLocation().getRegionID();
        state = DetermineState();
//        if (currentFloor > 0 && GetEmergencyDoor().isPresent()) {
//            trap_passed = false;
//        }

        if (state == State.lobby) {
            DoLobby();
        }

        if (state == State.travelling) {
            DoTravelling();
        }

        if (state == State.pretrap) {
            DoTrap();
        }

        if (state == State.looting) {
            DoLooting();
        }

        if (state == State.dooring) {
            DoDooring();
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