package com.Ezzuneware.BJTest;


import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.NPCInteraction;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
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
import net.runelite.api.events.MenuEntryAdded;

import java.util.ArrayList;
import java.util.List;

@PluginDescriptor(
        name = "BJTest",
        description = "",
        enabledByDefault = false,
        tags = {"eco", "plugin"}
)
@Slf4j
public class BJTestPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private BJTestConfig config;
    @Inject
    private BJTestOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    private boolean started = false;
    public int timeout = 0;
    public Boolean beenHit = false;
    public int gameTimer = 0;
    private List<Integer> WineItems = new ArrayList<Integer>();
    private int minimumHealthPoints = 5;

    @Provides
    private BJTestConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(BJTestConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        keyManager.registerKeyListener(toggle);
        overlayManager.add(overlay);
        timeout = 0;

        WineItems.add(1993);
        started = !started;
    }

    @Override
    protected void shutDown() throws Exception {
        keyManager.unregisterKeyListener(toggle);
        overlayManager.remove(overlay);
        timeout = 0;
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

    private void DrinkWine() {
        try {
            Widget part = Inventory.search().idInList(WineItems).first().get();
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(part, "Drink");
        } catch (Exception e) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Couldn't find the wine!", null);
        }

    }

    @Subscribe
    private void onGameTick(GameTick event) {
        if (beenHit) {
            gameTimer++;
            if (gameTimer > 4) {
                gameTimer = 0;
                beenHit = false;
            }

        }
        if (timeout > 0) {
            timeout--;
            return;
        }
        if (!EthanApiPlugin.loggedIn() || !started) {
            return;
        }

        int hitpoints = this.client.getBoostedSkillLevel(Skill.HITPOINTS);

        if (hitpoints <= minimumHealthPoints) {
            DrinkWine();
        }

    }


    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded menuEntryAdded) {
        if (client.getGameState() != GameState.LOGGED_IN)
            return;
        var me = menuEntryAdded.getMenuEntry();
        var targetNpc = menuEntryAdded.getMenuEntry().getNpc();
        if (targetNpc != null && me.getType() == MenuAction.NPC_FIRST_OPTION) {     //added the first option check to hopefully only remove top entry
            var npcName = targetNpc.getName().toLowerCase();
            if (npcName.equals("bandit") || npcName.equals("menaphite thug")) {
                if (menuEntryAdded.getMenuEntry().getNpc().getAnimation() == 838) {
                    me.setOption("jack his shit")
                            .setType(MenuAction.NPC_THIRD_OPTION);

                } else if (menuEntryAdded.getMenuEntry().getNpc().getAnimation() == 305 || menuEntryAdded.getMenuEntry().getNpc().getAnimation() == 1976) {
                    beenHit = true;
                    me.setOption("STOP!")
                            .setType(MenuAction.NPC_THIRD_OPTION);
                } else if (menuEntryAdded.getMenuEntry().getNpc().getAnimation() == 808 || menuEntryAdded.getMenuEntry().getNpc().getAnimation() == -1) {
                    if (beenHit) {
                        me.setOption("STOP!")
                                .setType(MenuAction.NPC_THIRD_OPTION);
                    } else {
                        me.setOption("Hit this fool")
                                .setType(MenuAction.NPC_FIFTH_OPTION);
                    }


                    return;
                } else if (menuEntryAdded.getMenuEntry().getNpc().isInteracting()) {
                    beenHit = true;
                    me.setOption("STOP!")
                            .setType(MenuAction.NPC_THIRD_OPTION);
                }
            }
        }
    }
}

//                      MenuEntry madeEntry = client.createMenuEntry(0)
//                        .setOption("Knock the cunt out")
//                        .setType(MenuAction.RUNELITE)
//                        .onClick(e -> {
//                            NPCInteraction.interact(targetNpc, "Knock-out");
//
//                            });