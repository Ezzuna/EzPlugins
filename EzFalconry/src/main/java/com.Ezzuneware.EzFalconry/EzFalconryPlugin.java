package com.Ezzuneware.EzFalconry;


import com.example.EthanApiPlugin.Collections.Equipment;
import com.example.EthanApiPlugin.Collections.EquipmentItemWidget;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.NPCInteraction;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.API.EquipmentUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
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
import net.runelite.client.util.Text;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@PluginDescriptor(
        name = "EzFalconry",
        description = "Will automatically retrieve the gyr falcon after you catch the kebbit. And try to continue catching. For best results, stand next to a spawn then click the kebbit. This can be safely turned on at all times.",
        enabledByDefault = false,
        tags = {"eco", "plugin", "hunter"}
)
@Slf4j
public class EzFalconryPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzFalconryConfig config;
    @Inject
    private EzFalconryOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    private final Set<Integer> REGION_IDS = Set.of(9528, 9527);
    @Inject
    private ClientThread clientThread;
    private boolean started = false;
    public int timeout = 0;
    public int idleTicks = 0;
    @Getter
    private State state = State.idle;
    private NPC holdingNpc = null;
    private int birdTicks = 0;
    private final String CONTRACT_TEXT = "You find a rare piece of the creature!";

    private String targetName = "";
    private WorldPoint startLocation = null;

    @Provides
    private EzFalconryConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzFalconryConfig.class);
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
        if (birdTicks > 0) {
            birdTicks--;

        }

        if (timeout > 0) {
            timeout--;
            return;
        }
        if (!EthanApiPlugin.loggedIn() || !REGION_IDS.contains(client.getLocalPlayer().getWorldLocation().getRegionID())) {
            targetName = "";
            startLocation = null;
            state = State.idle;
            return;
        }
        if (state == State.complete)
            return;
        final Player local = client.getLocalPlayer();
        final Actor interact = local.getInteracting();
        int animId = -1;


        if (interact != null) {
            animId = local.getAnimation();
        }
        if (animId == 827) {
            state = State.plucking;
            idleTicks = 0;
            return;
        }
        NPC targetNpc = client.getHintArrowNpc();

        Optional<Widget> bone = Inventory.search().withName("Bones").first();
        bone.ifPresent(widget -> InventoryInteraction.useItem(widget, "Drop"));

        if (state == State.idle) {


            if (targetNpc != null) {
                if (!Objects.equals(targetNpc.getName(), "Gyr Falcon"))
                    return;
                NPCInteraction.interact(targetNpc, "Retrieve");
                state = State.traveling;
                holdingNpc = null;
                return;
            } else if (holdingNpc != null && birdTicks == 0) {
                try {
                    NPCInteraction.interact(holdingNpc, "Catch");
                } catch (Exception ex) {
                    holdingNpc = null;
                }


            } else if (startLocation != null) {
                if (client.getLocalPlayer().getWorldLocation().distanceTo(startLocation) != 0) {
                    MovementPackets.queueMovement(startLocation);
                }
            } else {
                ++idleTicks;
            }
        }


        if (animId == -1) {
            state = State.idle;
        }


//827

    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() == ChatMessageType.GAMEMESSAGE) {
            String message = Text.sanitize(Text.removeTags(event.getMessage()));
            if (message.contains(CONTRACT_TEXT)) {
                startLocation = null;
                state = State.complete;
            }
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned e) {

        if (!EthanApiPlugin.loggedIn() || !REGION_IDS.contains(client.getLocalPlayer().getWorldLocation().getRegionID()) || state == State.complete) {
            return;
        }
        if (holdingNpc != null)
            return;
        if (Objects.equals(e.getNpc().getName(), targetName)) {
            if (e.getNpc().getWorldLocation().distanceTo(client.getLocalPlayer().getWorldLocation()) < 4) {
                NPCInteraction.interact(e.getNpc(), "Catch");
                state = State.traveling;
                holdingNpc = e.getNpc();
            }


        }

    }

    @Subscribe
    private void onProjectileMoved(ProjectileMoved event) {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        Projectile projectile = event.getProjectile();
        if (projectile.getId() == 922) {
            if (projectile.getTarget() == client.getLocalPlayer().getLocalLocation()) {
                if (birdTicks == 0 && holdingNpc != null) {
                    //bird came back
                    birdTicks = 4;
                }
            }
        }


    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded mea) {
        if (client.getGameState() != GameState.LOGGED_IN)
            return;
        if (!EthanApiPlugin.loggedIn() || !REGION_IDS.contains(client.getLocalPlayer().getWorldLocation().getRegionID()) || state == State.complete) {
            targetName = "";
            startLocation = null;
            state = State.idle;
            return;
        }
        var targetNpc = mea.getMenuEntry().getNpc();
        if (targetNpc.getName().contains("kebbit")) {
            mea.getMenuEntry().onClick(i -> {
                targetName = targetNpc.getName();
                holdingNpc = targetNpc;
                startLocation = client.getLocalPlayer().getWorldLocation();
            });
        } else if (Objects.equals(targetNpc.getName(), "Matthias")) {
            mea.getMenuEntry().onClick(i -> {
                Optional<EquipmentItemWidget> gloves = Equipment.search().matchesWildCardNoCase("*gloves*").first();
                gloves.ifPresent(equipmentItemWidget -> {
                    if (!equipmentItemWidget.getName().contains("Falcon"))
                        InventoryInteraction.useItem(equipmentItemWidget, "Remove");
                });

                Optional<EquipmentItemWidget> weapon = Equipment.search().matchesWildCardNoCase("*axe*").first();
                weapon.ifPresent(equipmentItemWidget -> InventoryInteraction.useItem(equipmentItemWidget, "Remove"));

                Optional<EquipmentItemWidget> weapon2 = Equipment.search().matchesWildCardNoCase("*spear*").first();
                weapon2.ifPresent(equipmentItemWidget -> InventoryInteraction.useItem(equipmentItemWidget, "Remove"));

                Optional<EquipmentItemWidget> weapon3 = Equipment.search().matchesWildCardNoCase("*net*").first();
                weapon3.ifPresent(equipmentItemWidget -> InventoryInteraction.useItem(equipmentItemWidget, "Remove"));

                Optional<EquipmentItemWidget> shield = EquipmentUtil.getItemInSlot(EquipmentUtil.EquipmentSlot.OFF_HAND);
                shield.ifPresent(equipmentItemWidget -> InventoryInteraction.useItem(equipmentItemWidget, "Remove"));
            });


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