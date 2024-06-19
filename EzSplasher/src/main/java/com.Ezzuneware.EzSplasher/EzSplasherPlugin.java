package com.Ezzuneware.EzSplasher;


import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.NPCInteraction;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import java.util.Random;

@PluginDescriptor(
        name = "EzSplasher",
        description = "Splashes for 6 hours",
        enabledByDefault = false,
        tags = {"eco", "plugin"}
)
@Slf4j
public class EzSplasherPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzSplasherConfig config;
    @Inject
    private EzSplasherOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    private boolean started = false;
    public int timeout = 0;
    private int game_timer = 0;
    private int game_timeout = 0;
    private int not_casting_timer = 0;
    private int not_casting_timout = 16;
    private NPC currentTarget = null;

    public int GetNotCastingTimer() { return not_casting_timer;}
    public int GetTimeoutTimer() { return game_timeout;}
    public int GetTimer() { return game_timer;}

    @Provides
    private EzSplasherConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzSplasherConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        keyManager.registerKeyListener(toggle);
        overlayManager.add(overlay);
        timeout = 0;
        started = true;
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

        final Player local = client.getLocalPlayer();
        final Actor interact = local.getInteracting();
        int animId = -1;
        int targetId = -1;
        animId = local.getAnimation();
        if (animId == -1)
            ++not_casting_timer;
        else if (animId == 1162) {
            not_casting_timer = 0;
        }
        if (not_casting_timer > not_casting_timout) {
            not_casting_timer = 0;
            AttackTarget();
        } else {
            ++game_timer;
        }
        if (game_timer > game_timeout) {
            game_timer = 0;
            game_timeout = GenerateNewTimeout();
        } else {
            ++game_timer;
        }
    }

    private void AttackTarget() {
        if (currentTarget != null) {
            NPCInteraction.interact(currentTarget, "Attack");
        } else {
            NPCs.search().withName("Chicken").nearestToPlayer().ifPresent(npc -> {
                MousePackets.queueClickPacket();
                NPCInteraction.interact(npc, "Attack");
            });

        }

    }

    private int GenerateNewTimeout() {
        Random r = new Random();
        int low = 650;
        int high = 940;
        return r.nextInt(high - low) + low;
    }

    public void onMenuEntryAdded(MenuEntryAdded menuEntryAdded) {
        if (client.getGameState() != GameState.LOGGED_IN)
            return;
        var me = menuEntryAdded.getMenuEntry();
        var targetNpc = menuEntryAdded.getMenuEntry().getNpc();
        if (targetNpc != null && me.getType() == MenuAction.NPC_FIRST_OPTION) {     //added the first option check to hopefully only remove top entry
            var npcName = targetNpc.getName().toLowerCase();
            if (npcName.equals("chicken")) {
                if (menuEntryAdded.getMenuEntry().getNpc().getAnimation() == 838) {
                    me.setOption("Splash")
                            .setType(MenuAction.RUNELITE_SUBMENU)
                            .onClick(e -> {
                                currentTarget = targetNpc;
                                AttackTarget();
                            });
                }

            }
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