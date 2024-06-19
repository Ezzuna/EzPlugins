package com.Ezzuneware.EzSlayerAssistant;


import com.example.EthanApiPlugin.Collections.Equipment;
import com.example.EthanApiPlugin.Collections.EquipmentItemWidget;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;
import net.runelite.http.api.item.ItemEquipmentStats;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@PluginDescriptor(
        name = "EzSlayerAssistant",
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
    private Set<Integer> ignoredChunks = Set.of();      //chunks where we dont want to autotele
    private boolean started = false;
    public int timeout = 0;
    private int previousLevel = -1;
    private int level = -1;

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
    private void onGameTick(GameTick event) {
        if (timeout > 0) {
            timeout--;
            return;
        }
        if (!EthanApiPlugin.loggedIn() || !started) {
            return;
        }

        Widget wildernessLevel = client.getWidget(WidgetInfo.PVP_WILDERNESS_LEVEL);
        level = -1;
        if (wildernessLevel != null && !wildernessLevel.getText().equals("")) {
            try {
                if (wildernessLevel.getText().contains("<br>")) {
                    String text = wildernessLevel.getText().split("<br>")[0];
                    level = Integer.parseInt(text.replaceAll("Level: ", ""));
                } else {
                    level = Integer.parseInt(wildernessLevel.getText().replaceAll("Level: ", ""));
                }
            } catch (NumberFormatException ex) {
                //ignore
            }
        }
        if (previousLevel != -1 && level == -1) {
            previousLevel = -1;
        }

        if (level > -1) {
            if (previousLevel == -1) {
                previousLevel = level;

                if (config.attemptToTeleOnPlayer()) {
                    CheckForTele();
                }
            }

        }

        state = DetermineState();


    }

    private State DetermineState() {
        if (level > 0) {
            if (ShouldTele()) {
                return State.teleing;
            }
        }

        return State.idle;
    }

    private Boolean ShouldTele() {
        if (ignoredChunks.contains(client.getLocalPlayer().getWorldLocation().getRegionID()))
            return false;
        for (Player player : client.getPlayers()) {
            int lowRange = client.getLocalPlayer().getCombatLevel() - level;
            int highRange = client.getLocalPlayer().getCombatLevel() + level;
            if (player.equals(client.getLocalPlayer())) {
                continue;
            }
            if (player.getCombatLevel() >= lowRange && player.getCombatLevel() <= highRange) {
                boolean hadMage = false;
                boolean skippableWeapon = false;
                if (config.scaryOnly()) {
                    int mageBonus = 0;
                    for (int equipmentId : player.getPlayerComposition().getEquipmentIds()) {
                        if (equipmentId == -1) {
                            continue;
                        }
                        if (equipmentId == 6512) {
                            continue;
                        }
                        if (equipmentId >= 512) {
                            continue;
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


                    //boolean teleported = HandleTeleFromPlayer();

                    //needs to have more verbose logging than this to explain why teled. What item matched if scary, etc.

//                if (teleported) {
//                    teleportedFromSkulledPlayer = EthanApiPlugin.getSkullIcon(player) != null;
//                    if (teleportedFromSkulledPlayer) {
//                        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Teleported from skulled player", null);
//                    } else {
//                        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Teleported from non-skulled player", null);
//                    }
//                }
                    return true;
                }
            }

            return false;
        }
        return false;
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

    private Boolean CheckForTele() {
        Optional<EquipmentItemWidget> equipedItem = Equipment.search().matchesWildCardNoCase(config.teleItemChosen().itemName + "*").first();
        Optional<Widget> inventoryItem = Inventory.search().matchesWildCardNoCase(config.teleItemChosen().itemName + "*").first();
        if (equipedItem.isPresent())
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Prepared to Ez auto-tele using " + config.teleItemChosen().itemName + " which is currently equipped.", null);
        else if (inventoryItem.isPresent())
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Prepared to Ez auto-tele using " + config.teleItemChosen().itemName + " which is currently in inventory.", null);
        return (equipedItem.isPresent() || inventoryItem.isPresent());
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