package com.Ezzuneware.EzNaguaSlayer;


import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.TileItems;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.NPCInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
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
import org.apache.commons.lang3.RandomUtils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

@PluginDescriptor(
        name = "EzNaguaSlayer",
        description = "Kills Nagua, picks up drops and resets aggro.",
        enabledByDefault = false,
        tags = {"eco", "plugin"}
)
@Slf4j
public class EzNaguaSlayerPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzNaguaSlayerConfig config;
    @Inject
    private EzNaguaSlayerOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    @Inject
    public ItemManager itemManager;
    @Getter
    private boolean started = false;
    @Getter
    public int timeout = 0;
    private Set<Integer> itemsToCollect = Set.of(29084, 562, 29087, 561, 554, 448, 443, 437, 454, 441, 439, 560);  //29084 = sulphur blades
    private final int fightingChunkId = 5525;
    private final int aggroResetChunkId = 5782;
    private final int aggroResetChunkAltId = 6037;
    private final int naguaId = 13033;
    private Player player;
    WorldPoint lootTile = null;
    @Getter
    private int prayerBaseLvl = 0;
    @Getter
    private int currentPrayLvl = 0;
    @Getter
    private int aggroTimer = 0;
    @Getter
    private int idleTicks = 0;
    private boolean looting = false;
    @Getter
    private State state = State.IDLE;
    private final int quickPrayerWidgetID = WidgetInfo.MINIMAP_QUICK_PRAYER_ORB.getPackedId();

    private boolean drink = false;

    private final int EXIT_DOOR = 51375;
    private final int ENTRY_DOOR = 51376;
    private final int ENTRY_DOOR_ALT = 51377;
    private final WorldPoint fightingPoint = new WorldPoint(1373, 9558, 0);
    public Queue<ItemStack> lootQueue = new LinkedList<>();

    @Provides
    private EzNaguaSlayerConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzNaguaSlayerConfig.class);
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

    private void togglePrayer() {
        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetActionPacket(1, quickPrayerWidgetID, -1, -1);
    }

    private void togglePrayer(Boolean on) {
        if (client.getVarbitValue(Varbits.QUICK_PRAYER) != Boolean.compare(on, false)) {
            togglePrayer();
        }
    }

    @Subscribe
    private void onGameTick(GameTick event) {
        if (aggroTimer > 0)
            aggroTimer--;
        if (timeout > 0) {
            timeout--;
            return;
        }
        if (!EthanApiPlugin.loggedIn() || !started) {
            return;
        }
        if (lootQueue.isEmpty()) looting = false;

        if (looting && idleTicks < 4) {
            timeout = 3;
            ++idleTicks;
            return;
        } else if (looting) {
            //catch all we couldn't loot for 12 ticks
            lootQueue = new LinkedList<>();
        }
        if (lootTile == null) looting = false;
        prayerBaseLvl = client.getRealSkillLevel(Skill.PRAYER);
        currentPrayLvl = client.getBoostedSkillLevel(Skill.PRAYER);

        if (state == State.TRAVELINGIN) {
            MovementPackets.queueMovement(fightingPoint);
            togglePrayer(true);
            state = State.MOVEIN;
            timeout = 10;
            return;
        }


        state = discoverCurrentState();

        if (state == State.KILLING) {
            togglePrayer(true);
        }
        if (drink) {
            if (Inventory.search().nameContains("Moonlight potion").first().isPresent()) {
                Inventory.search().nameContains("Moonlight potion").first().ifPresent(cake -> {
                    MousePackets.queueClickPacket();
                    InventoryInteraction.useItem(cake, "Drink");
                });
            }
            drink = false;
            return;
        }


        if (currentPrayLvl <= prayerBaseLvl / 3) {
            drink = true;
            setTimeout();
        }

        if (!lootQueue.isEmpty()) {
            looting = true;
            ItemStack itemStack = lootQueue.peek();
//            WorldPoint stackLocation = WorldPoint.fromLocal(client, itemStack.getLocation);
            TileItems.search().withId(itemStack.getId()).withinDistanceToPoint(6, client.getLocalPlayer().getWorldLocation()).first().ifPresent(item -> {
                ItemComposition comp = itemManager.getItemComposition(item.getTileItem().getId());
                log.info("Looting: " + comp.getName());
                if (comp.isStackable() || comp.getNote() != -1) {
                    log.info("stackable loot " + comp.getName());

                    item.interact(false);

                }
                if (!Inventory.full()) {
                    item.interact(false);
                } else {
                    EthanApiPlugin.sendClientMessage("Inventory full, stopping. May handle in future update");
                    EthanApiPlugin.stopPlugin(this);
                }
            });
            timeout = 3;
            lootQueue.remove();
            return;
        }


        if (idleTicks > 50) {
            resetAggroTimer();
        }

        if (state == State.IDLE) {
            NPCs.search().withId(naguaId).nearestToPlayer().ifPresent(myNagua -> {
                NPCInteraction.interact(myNagua, "Attack");
                idleTicks = 0;
                state = State.KILLING;
            });
        }

    }

    private State discoverCurrentState() {

        if (Inventory.search().nameContains("Moonlight potion").first().isEmpty()) {
            if (client.getLocalPlayer().getWorldLocation().getRegionID() == aggroResetChunkId) {
                started = false;
                togglePrayer(false);
                return State.OUTOFSUPPLIES;
            } else if (client.getLocalPlayer().getWorldLocation().getRegionID() == aggroResetChunkAltId) {
                started = false;
                togglePrayer(false);
                return State.OUTOFSUPPLIES;

            } else {
                resetAggroTimer();
                return State.OUTOFSUPPLIES;
            }

        }

        if (aggroTimer <= 0 && client.getLocalPlayer().getWorldLocation().getRegionID() == fightingChunkId) {
            resetAggroTimer();
            return State.TRAVELIGOUT;
        }
        var loc = client.getLocalPlayer().getWorldLocation().getRegionID();
        if (client.getLocalPlayer().getWorldLocation().getRegionID() == aggroResetChunkId) {
            if (Inventory.search().nameContains("Moonlight potion").first().isEmpty()) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "OUT OF POTIONS. CANCELLING", null);
                started = false;
            }
            togglePrayer(true);
            aggroTimer = RandomUtils.nextInt(675, 750);
            TileObjectInteraction.interactNearest(ENTRY_DOOR, "Pass-through");
            timeout = RandomUtils.nextInt(6, 12);
            return State.TRAVELINGIN;
        } else if (client.getLocalPlayer().getWorldLocation().getRegionID() == aggroResetChunkAltId) {
            if (Inventory.search().nameContains("Moonlight potion").first().isEmpty()) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "OUT OF POTIONS. CANCELLING", null);
                started = false;
            }
            togglePrayer(true);
            aggroTimer = RandomUtils.nextInt(675, 750);
            TileObjectInteraction.interactNearest(ENTRY_DOOR_ALT, "Pass-through");
            timeout = RandomUtils.nextInt(6, 12);
            return State.TRAVELINGIN;
        }

        player = client.getLocalPlayer();
        final Actor interact = player.getInteracting();
        int animId = player.getAnimation();

        if (interact == null) {
            if (idleTicks > 15) {
                return State.IDLE;
            } else
                ++idleTicks;
        } else {
            idleTicks = 0;
            return State.KILLING;
        }

        if (idleTicks > 7)
            return State.IDLE;
        else
            return state;
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event) {
        if (!started || !config.lootEnabled()) return;
        Collection<ItemStack> items = event.getItems();
        items.stream().filter(item -> {
            return itemsToCollect.contains(item.getId());
        }).forEach(it -> {
            log.info("Adding to lootQueue: " + it.getId());
            lootQueue.add(it);
        });
    }


    private void setTimeout() {
        timeout = RandomUtils.nextInt(config.tickDelayMin(), config.tickDelayMax());
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
            aggroTimer = 0;
            lootQueue = new LinkedList<>();
        }

    }

    private void resetAggroTimer() {
        resetAggroTimer(22, 26);
    }

    private void resetAggroTimer(int delayMin, int delayMax) {
        //togglePrayer(false);
        aggroTimer = RandomUtils.nextInt(670, 750);
        TileObjectInteraction.interactNearest(EXIT_DOOR, "Pass-through");
        timeout = RandomUtils.nextInt(delayMin, delayMax);
        idleTicks = 0;
    }
}