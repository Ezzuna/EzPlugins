package com.Ezzuneware.EzHerbi;

import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.NPCInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.MovementPackets;
import com.example.Packets.WidgetPackets;
import com.google.inject.Provides;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import javax.inject.Inject;

import com.piggyplugins.PiggyUtils.API.PlayerUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.npcoverlay.HighlightedNpc;
import net.runelite.client.game.npcoverlay.NpcOverlayService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.herbiboars.HerbiboarPlugin;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.RandomUtils;

@Slf4j
@PluginDescriptor(
        name = "EzHerbi"
)
@PluginDependency(HerbiboarPlugin.class)
public class HerbiAfkPlugin extends Plugin {
    @Inject
    @Getter
    private Client client;

    @Inject
    private PlayerUtil playerUtil;


    @Inject
    private HerbiAfkConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private HerbiAfkOverlay overlay;

    @Inject
    private HerbiAfkMinimapOverlay minimapOverlay;

    @Inject
    private HerbiboarPlugin herbiboarPlugin;

    @Inject
    private NpcOverlayService npcOverlayService;

    @Getter
    private List<WorldPoint> pathLinePoints = new ArrayList<>();

    private int timeout;

    @Getter
    private WorldPoint startLocation, endLocation;
    private boolean chasing = false;
    private boolean shouldMove = false;

    private enum HerbiState {
        IDLE,
        FINDING_START,
        HUNTING,
        STUNNED,
    }

    private static boolean varbitChanged = false;
    private HerbiState herbiState;

    private int finishedId = -1;
    private int idleTicks = 0;

    private static final String HERBI_STUN = "You stun the creature";
    private static final String HERBI_KC = "Your herbiboar harvest count is:";
    private static final String HERBIBOAR_NAME = "Herbiboar";
    private static final String HERBI_CIRCLES = "The creature has successfully confused you with its tracks, leading you round in circles";

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(overlay);
        overlayManager.add(minimapOverlay);

        npcOverlayService.registerHighlighter(isHerbiboar);

        pathLinePoints = new ArrayList<>();

        herbiState = HerbiState.IDLE;
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(overlay);
        overlayManager.remove(minimapOverlay);

        npcOverlayService.unregisterHighlighter(isHerbiboar);

        resetTrailData();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        switch (event.getGameState()) {
            case HOPPING:
            case LOGGING_IN:
                resetTrailData();
                herbiState = HerbiState.IDLE;
                break;
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        varbitChanged = true;
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (timeout > 0) {
            timeout--;
            return;
        }

        if (Inventory.full()) {
            return;
        }
        if (!isInHerbiboarArea()) {
            if (herbiState != HerbiState.IDLE) {
                resetTrailData();
                herbiState = HerbiState.IDLE;
            }
            return;
        }

        if (client.getLocalPlayer() == null) {
            return;
        }

        checkRunEnergy();

        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

        if (shouldMove) {
            if (endLocation != null) {
                MovementPackets.queueMovement(endLocation);
                shouldMove = false;
                return;
            } else if (startLocation != null) {
                MovementPackets.queueMovement(startLocation);
                shouldMove = false;
                return;
            } else if (!varbitChanged) {
                shouldMove = false;
                updateStartAndEndLocation();
                setTimeout();
                return;
            }

        }

        if (varbitChanged) {
            updateStartAndEndLocation();
            varbitChanged = false;
            setTimeout();
            return;
        }

        Optional<NPC> targetNPC = NPCs.search().withName("Herbiboar").first();
        if (targetNPC.isPresent() && !chasing) {
            NPCInteraction.interact(targetNPC.get(), "Harvest");
            idleTicks = 0;
            chasing = true;
            return;
        }


        switch (herbiState) {
            case FINDING_START:
                startLocation = playerLocation;
                endLocation = Utils.getNearestStartLocation(playerLocation);
                break;

            case HUNTING:
                if (config.pathRelativeToPlayer()) {
                    startLocation = playerLocation;
                }

                break;

            case STUNNED:
                startLocation = config.pathRelativeToPlayer() ? playerLocation : HerbiAfkData.END_LOCATIONS.get(finishedId - 1);
                WorldPoint herbiLocation = getHerbiboarLocation();
                if (herbiLocation != null) {
                    endLocation = herbiLocation;
                }
                npcOverlayService.rebuild();
                break;

            case IDLE:
                break;
        }

        if (startLocation != null && endLocation != null) {
            pathLinePoints = Utils.getPathLinePoints(startLocation, endLocation);
        }

        if (endLocation != null) {

            List<TileObject> targetObj = TileObjects.search().withAction("Inspect").result();
            List<TileObject> targetObj2 = TileObjects.search().withAction("Attack").result();

            for (TileObject obj : targetObj) {
                if (!chasing && obj.getWorldLocation().distanceTo(endLocation) == 0) {
                    TileObjectInteraction.interact(obj, "Inspect");
                    chasing = true;
                    idleTicks = 0;
                    return;
                }
            }
            for (TileObject obj : targetObj2) {

                if (!chasing && obj.getWorldLocation().distanceTo(endLocation) == 0) {
                    TileObjectInteraction.interact(obj, "Attack");
                    chasing = true;
                    idleTicks = 0;
                    return;
                }


            }
        }
        if (idleTicks > 15) {
            idleTicks = 0;
            chasing = false;
        } else {
            ++idleTicks;
        }
    }

    private void checkRunEnergy() {
        if (playerUtil.isRunning() && playerUtil.runEnergy() >= 40) {
            log.info("Run");
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetActionPacket(1, 10485787, -1, -1);
        }
    }

    private void setTimeout() {
        timeout = RandomUtils.nextInt(config.tickDelayMin(), config.tickDelayMax());
    }

    private void updateStartAndEndLocation() {
        List<? extends Enum<?>> currentPath = herbiboarPlugin.getCurrentPath();
        int currentPathSize = currentPath.size();
        if (currentPathSize < 1) {
            return;
        }

        WorldPoint newStartLocation;
        WorldPoint newEndLocation;

        if (herbiboarPlugin.getFinishId() > 0) {
            newStartLocation = HerbiboarSearchSpot.valueOf(currentPath.get(currentPathSize - 1).toString()).getLocation();
            finishedId = herbiboarPlugin.getFinishId();
            newEndLocation = HerbiAfkData.END_LOCATIONS.get(finishedId - 1);
        } else if (currentPathSize == 1) {
            newStartLocation = herbiboarPlugin.getStartPoint();
            newEndLocation = HerbiboarSearchSpot.valueOf(currentPath.get(0).toString()).getLocation();
        } else {
            newStartLocation = HerbiboarSearchSpot.valueOf(currentPath.get(currentPathSize - 2).toString()).getLocation();
            newEndLocation = HerbiboarSearchSpot.valueOf(currentPath.get(currentPathSize - 1).toString()).getLocation();
        }

        startLocation = newStartLocation;
        endLocation = newEndLocation;
        chasing = false;
        shouldMove = true;

        herbiState = HerbiState.HUNTING;
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned e) {
        if (Objects.equals(e.getNpc().getName(), "Herbiboar")) {
            chasing = false;
            idleTicks = 0;
        }

    }


    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() == ChatMessageType.GAMEMESSAGE) {
            String message = Text.sanitize(Text.removeTags(event.getMessage()));
            if (message.contains(HERBI_STUN)) {
                if (config.noLootMode()) {
                    herbiState = HerbiState.FINDING_START;
                } else {
                    herbiState = HerbiState.STUNNED;
                }
            } else if (message.contains(HERBI_KC) || message.contains(HERBI_CIRCLES)) {
                resetTrailData();
                herbiState = HerbiState.FINDING_START;
                if (config.botMode()) {
                    chasing = false;
                    setTimeout();
                }

            }
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (!isInHerbiboarArea()) {
            return;
        }

        if (config.dynamicMenuEntrySwap()) {
            swapTrailMenuEntries(event);

            if (config.noLootMode() && herbiState == HerbiState.FINDING_START && event.getTarget().contains(HERBIBOAR_NAME)) {
                event.getMenuEntry().setDeprioritized(true);
            }
        }
        if (config.npcMenuEntrySwap()) {
            hideNpcMenuEntries(event);
        }
    }

    private void swapTrailMenuEntries(MenuEntryAdded event) {
        String target = event.getTarget();
        for (String menuTarget : HerbiAfkData.TRAIL_MENU_ENTRY_TARGETS) {
            if (target.contains(menuTarget)) {
                MenuEntry entry = event.getMenuEntry();
                WorldPoint entryTargetPoint = WorldPoint.fromScene(client, entry.getParam0(), entry.getParam1(), client.getPlane());

                switch (herbiState) {
                    case FINDING_START:
                    case HUNTING:
                        if (!entryTargetPoint.equals(endLocation)) {
                            entry.setDeprioritized(true);
                        }
                        break;
                    case STUNNED:
                        entry.setDeprioritized(true);
                        break;
                }

                return;
            }
        }
    }

    private void hideNpcMenuEntries(MenuEntryAdded event) {
        String target = event.getTarget();
        for (String menuTarget : HerbiAfkData.NPC_MENU_ENTRY_TARGETS) {
            if (target.contains(menuTarget)) {
                MenuEntry entry = event.getMenuEntry();

                switch (herbiState) {
                    case FINDING_START:
                    case HUNTING:
                    case STUNNED:
                        entry.setDeprioritized(true);
                        break;
                }

                return;
            }
        }
    }

    private WorldPoint getHerbiboarLocation() {
        final NPC[] cachedNPCs = client.getCachedNPCs();
        for (NPC npc : cachedNPCs) {
            if (npc != null) {
                if (npc.getName() != null && npc.getName().equals(HERBIBOAR_NAME)) {
                    return npc.getWorldLocation();
                }
            }
        }
        return null;
    }

    public final Function<NPC, HighlightedNpc> isHerbiboar = (n) -> {
        boolean isHighlight = config.highlightHerbiHull() || config.highlightHerbiTile() || config.highlightHerbiOutline();
        if (isHighlight && n.getName() != null && n.getName().equals(HERBIBOAR_NAME)) {
            Color color = config.getHerbiboarColor();
            return HighlightedNpc.builder()
                    .npc(n)
                    .highlightColor(color)
                    .fillColor(ColorUtil.colorWithAlpha(color, color.getAlpha() / 12))
                    .hull(config.highlightHerbiHull())
                    .tile(config.highlightHerbiTile())
                    .outline(config.highlightHerbiOutline())
                    .build();
        }
        return null;
    };

    private void resetTrailData() {
        pathLinePoints.clear();

        startLocation = null;
        endLocation = null;

        finishedId = -1;
    }

    public boolean isInHerbiboarArea() {
        return herbiboarPlugin.isInHerbiboarArea();
    }

    @Provides
    HerbiAfkConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(HerbiAfkConfig.class);
    }
}
