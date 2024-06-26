package com.Ezzuneware.EzSlayerAssistant;


import com.Ezzuneware.EzApi.Combat.EzInventory;
import com.Ezzuneware.EzApi.Combat.EzPlayerManagement;
import com.Ezzuneware.EzApi.EzApi;
import com.Ezzuneware.EzApi.Looting.ezLooter;
import com.Ezzuneware.EzApi.Utility.EzHelperClass;
import com.example.EthanApiPlugin.Collections.Equipment;
import com.example.EthanApiPlugin.Collections.EquipmentItemWidget;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.NPCInteraction;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;
import net.runelite.http.api.item.ItemEquipmentStats;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;


@PluginDescriptor(
        name = "<html><font color=\"#00c27e\">[Ez]</font>EzSlayerAssistant</html>",
        description = "Heavy WIP",
        enabledByDefault = false,
        tags = {"eco", "plugin"}
)
@Slf4j
public class EzSlayerAssistantPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private EzSlayerAssistantConfig config;
    @Inject
    private EzSlayerAssistantOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private ItemManager itemManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    private Set<Integer> ignoredChunks = Set.of(12344, 12600, 12601);      //chunks where we dont want to autotele
    @Getter
    private boolean started = false;
    @Getter
    public int timeout = 0;
    private int previousLevel = -1;
    private int level = -1;
    public Queue<ItemStack> lootQueue = new LinkedList<>();
    private int food_amount_max = -1;
    public int BoostedHitpoints, Hitpoints;
    @Getter
    private int idleTicks = 0;

    public static boolean teleportedFromSkulledPlayer = false;

    @Getter
    private State state = State.idle;

    @Provides
    private EzSlayerAssistantConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzSlayerAssistantConfig.class);
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
    public void onNpcLootReceived(NpcLootReceived event) {
        if (!started) return;
        Collection<ItemStack> items = event.getItems();
        items.stream().filter(item -> {
            return (EzHelperClass.ItemInCSSWildcard(itemManager.getItemComposition(item.getId()).getName(), config.lootableItems()) || (EzHelperClass.ItemInCSSWildcard(itemManager.getItemComposition(item.getId()).getName(), config.Food()) && (food_amount_max > EzInventory.GetFoodCount(config.Food()) || BoostedHitpoints < client.getRealSkillLevel(Skill.HITPOINTS))));
        }).forEach(it -> {
            log.info("Adding to lootQueue: " + it.getId());
            lootQueue.add(it);
        });
    }

    private void CheckInventory() {
        Optional<Widget> ash = Inventory.search().matchesWildCardNoCase("*ashes").withAction("Scatter").first();
        if (ash.isPresent()) {
            InventoryInteraction.useItem(ash.get(), "Scatter");
        }
    }


    @Subscribe
    private void onGameTick(GameTick event) {
        if (!EthanApiPlugin.loggedIn() || ignoredChunks.contains(client.getLocalPlayer().getWorldLocation().getRegionID())) {
            return;
        }
        level = GetWildyLevel();
        if (level > 0) {
            DoTeleLogion();
        }
        if (timeout > 0) {
            timeout--;
            return;
        }

        if (!started)
            return;
        if (food_amount_max <= 0) {
            food_amount_max = EzInventory.GetFoodCount(config.Food());
        }
        if (EzInventory.AlchFirstItemInCSS(config.alchableItems()) && config.alchLoot()) {
            setTimeout();
            return;
        }
        CheckInventory();
        BoostedHitpoints = client.getBoostedSkillLevel(Skill.HITPOINTS);
        state = DetermineState();


        if (state == State.eating) {
            DoEating();
            setTimeout();
            idleTicks = 0;
        }
        if (state == State.looting) {
            DoLooting();
            setTimeout();
            idleTicks = 0;
        }

        if (state == State.chasing) {
            if (DoKilling()) {
                setTimeout();
                idleTicks = 0;
            }

        }
        if (state == State.fighting) {
            idleTicks = 0;
        }

        if (state == State.idle) {
            ++idleTicks;
        }


    }

    private boolean DoKilling() {

        if (EzPlayerManagement.IsInCombat())
            return false;

        Optional<NPC> enemy = NPCs.search().nameContains(config.npcName()).alive().nearestToPlayer();
        ;
        if (enemy.isPresent()) {
            NPCInteraction.interact(enemy.get(), "Attack");
            return true;
        }
        return false;
    }

    private void DoEating() {
        EzInventory.EatFirstFoodInCSS(config.Food());
        setTimeout();
    }

    private void DoLooting() {
        ezLooter.LootNearestItemInQueue(lootQueue, config.alchLoot(), config.alchableItems(), config.Food());
    }

    private int GetWildyLevel() {
        Widget wildernessLevel = client.getWidget(WidgetInfo.PVP_WILDERNESS_LEVEL);
        if (wildernessLevel != null && !wildernessLevel.getText().equals("")) {
            try {
                if (wildernessLevel.getText().contains("<br>")) {
                    String text = wildernessLevel.getText().split("<br>")[0];
                    return Integer.parseInt(text.replaceAll("Level: ", ""));
                } else {
                    return Integer.parseInt(wildernessLevel.getText().replaceAll("Level: ", ""));
                }
            } catch (NumberFormatException ex) {
                //ignore
            }
        }
        if (previousLevel != -1 && level == -1) {
            previousLevel = -1;
        }
        return -1;
    }


    private State DetermineState() {
        if (BoostedHitpoints <= config.eatAt())
            return State.eating;

        if ((Inventory.full() && EzInventory.GetFoodCount(config.Food()) >= 1) || (EzInventory.GetFoodCount(config.Food()) > food_amount_max) && BoostedHitpoints < client.getRealSkillLevel(Skill.HITPOINTS)) {
            return State.eating;
        }

        if (!lootQueue.isEmpty() && !Inventory.full())
            return State.looting;

        if (idleTicks > 4)
            return State.chasing;

        if (!NPCs.search().interactingWithLocal().result().isEmpty())
            return State.fighting;

        return State.idle;
    }


    private void setTimeout() {
        timeout = RandomUtils.nextInt(config.tickDelayMin(), config.tickDelayMax());
    }

    private void DoTeleLogion() {


        if (level > -1) {
            if (previousLevel == -1) {
                previousLevel = level;

                if (config.attemptToTeleOnPlayer()) {
                    if (EzInventory.CheckForTele(config.teleItemChosen())) {
                        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "<col=7cfc00>Entering wilderness with a teleport item: " + config.teleItemChosen().itemName +
                                " " +
                                "AutoTele is ready to save you", null);
                    } else {
                        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "<col=ff0000> Entering wilderness without selected " +
                                "teleport possible mistake?", null);
                    }
                }
            }

        }
        for (Player player : client.getPlayers()) {
            int lowRange = client.getLocalPlayer().getCombatLevel() - level;
            int highRange = client.getLocalPlayer().getCombatLevel() + level;
            if (player.equals(client.getLocalPlayer())) {
                continue;
            }
            if (config.pkerMaxDistance() > 0) {
                if (player.getWorldLocation().distanceTo(client.getLocalPlayer().getWorldLocation()) > config.pkerMaxDistance()) {
                    continue;
                }
            }
            if (player.getCombatLevel() >= lowRange && player.getCombatLevel() <= highRange) {
                boolean hadMage = false;
                boolean skippableWeapon = false;

                if (!player.isInteracting() && player.getInteracting() == client.getLocalPlayer()) {
                    ExecuteTele(player);
                    continue;
                }

                int mageBonus = 0;
                for (int equipmentId : player.getPlayerComposition().getEquipmentIds()) {
                    if (equipmentId == -1) {
                        continue;
                    }
                    if (equipmentId == 6512) {
                        continue;
                    }
                    if (equipmentId >= 512) {
                        return;
                    }
                    int realId = equipmentId - 512;
                    ItemEquipmentStats itemStats = itemManager.getItemStats(realId, false).getEquipment();
                    if (itemStats == null) {
                        continue;
                    }
                    mageBonus += itemStats.getAmagic();
                }
                if (mageBonus > 0) {
                    hadMage = true;
                }


                if (!config.weaponFilter().equals("")) {
                    List<String> filteredWeapons = getFilteredWeapons();
                    for (int equipment : player.getPlayerComposition().getEquipmentIds()) {
                        int equipmentId = equipment - 512;
                        if (equipmentId > 0) {
                            ItemComposition equipmentComp = itemManager.getItemComposition(equipmentId);
                            if (filteredWeapons.stream().anyMatch(item -> WildcardMatcher.matches(item.toLowerCase(),
                                    Text.removeTags(equipmentComp.getName().toLowerCase())))) {
                                skippableWeapon = true;
                            }
                        }
                    }
                }
                if (skippableWeapon) {
                    if (!hadMage) {
                        continue;
                    }
                }


                boolean teleported = false;
                ExecuteTele(player);

                return;
            }
        }
    }

    private void ExecuteTele(Player player) {
        if (config.attemptToLogOnPlayer()) {
            if (EzPlayerManagement.attempt_logout()) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Attempted to log out. If successful will still attempt tele logic as fallback.", null);
            }
        }
        if (config.attemptToTeleOnPlayer()) {
            if (EzInventory.DoTeleFromTeleItem(config.teleItemChosen())) {
                teleportedFromSkulledPlayer = EthanApiPlugin.getSkullIcon(player) != null;
                if (teleportedFromSkulledPlayer) {
                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Teleported from skulled player", null);
                } else {
                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Teleported from non-skulled player", null);
                }
            } else {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Something went wrong teleing from player", null);
            }
        }
    }

    public List<String> getFilteredWeapons() {
        List<String> itemNames = new ArrayList<>();
        for (String s : config.weaponFilter().split(",")) {
            if (StringUtils.isNumeric(s)) {
                itemNames.add(Text.removeTags(itemManager.getItemComposition(Integer.parseInt(s)).getName()));
            } else {
                itemNames.add(s);
            }
        }
        return itemNames;
    }

    public List<String> getScaryItems() {
        List<String> itemNames = new ArrayList<>();
        for (String s : config.scaryItems().split(",")) {
            if (StringUtils.isNumeric(s)) {
                itemNames.add(Text.removeTags(itemManager.getItemComposition(Integer.parseInt(s)).getName()));
            } else {
                itemNames.add(s);
            }
        }
        return itemNames;
    }

    public List<String> getLootableItems() {
        List<String> itemNames = new ArrayList<>();
        for (String s : config.lootableItems().split(",")) {
            if (StringUtils.isNumeric(s)) {
                itemNames.add(Text.removeTags(itemManager.getItemComposition(Integer.parseInt(s)).getName()));
            } else {
                itemNames.add(s);
            }
        }
        return itemNames;
    }

    public List<String> getAlchableItems() {
        List<String> itemNames = new ArrayList<>();
        for (String s : config.alchableItems().split(",")) {
            if (StringUtils.isNumeric(s)) {
                itemNames.add(Text.removeTags(itemManager.getItemComposition(Integer.parseInt(s)).getName()));
            } else {
                itemNames.add(s);
            }
        }
        return itemNames;
    }


    private Boolean HandleTeleFromPlayer() {
        Optional<EquipmentItemWidget> equipedItem = Equipment.search().matchesWildCardNoCase(config.teleItemChosen().itemName + "*").first();
        Optional<Widget> inventoryItem = Inventory.search().matchesWildCardNoCase(config.teleItemChosen().itemName + "*").first();

        if (equipedItem.isPresent()) {
            InventoryInteraction.useItem(equipedItem.get(), config.teleItemChosen().equipmentAction);
            return true;
        }

        if (inventoryItem.isPresent()) {
            InventoryInteraction.useItem(inventoryItem.get(), config.teleItemChosen().action);
            MousePackets.queueClickPacket();
            WidgetPackets.queueResumePause(14352385, config.teleItemChosen().selection);
            return true;
        }

        return false;
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