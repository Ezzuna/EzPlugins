package com.example.CalvarionHelper;

import com.Ezzuneware.EzApi.Combat.EzNpc;
import com.Ezzuneware.EzApi.Combat.EzPlayerManagement;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.NPCInteraction;
import com.example.Packets.MovementPackets;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import org.apache.commons.lang3.RandomUtils;

import java.util.*;
import java.util.stream.Collectors;

@PluginDescriptor(
        name = "<html><font color=\"#00c27e\">[Ez]</font> EzCalvarion </html>",
        description = "CalvarionHelper by EthanVann, maintained by Piggy Plugins, enhanced by Ezware",
        enabledByDefault = false,
        tags = {"ethan", "ez"}
)
public class CalvarionHelper extends Plugin {
    @Inject
    Client client;
    @Inject
    OverlayManager overlayManager;
    @Inject
    EzCalvarionOverlay ezCalvarionOverlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    CalvarionHelperConfig config;
    @Getter
    private State state = State.idle;
    HashMap<WorldPoint, Integer> lightning = new HashMap<>();
    HashMap<WorldPoint, Integer> swing = new HashMap<>();
    CalvarionHelperOverlay overlay;
    private WorldPoint resetLocal = new WorldPoint(1886, 11546, 1);
    private final Set<Integer> calvIds = Set.of(11993, 11994, 11995);

    @Getter
    private boolean started = false;

    @Getter
    public int timeout = 0;

    @Provides
    public CalvarionHelperConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(CalvarionHelperConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        overlay = new CalvarionHelperOverlay(client, this);
        overlayManager.add(ezCalvarionOverlay);
        keyManager.registerKeyListener(toggle);
        timeout = 0;
    }

    private boolean IsInside() {
        //do logic for checking inside calv uwu

        return client.getLocalPlayer().getWorldLocation().getRegionID() == 7604;
    }

    private boolean IsUnderVetion() {
        Optional<NPC> target = GetCalverion();
        if (target.isEmpty())
            target = GetHellhound();

        if (target == null)
            return false;

        return target.get().getWorldArea().toWorldPointList().contains(client.getLocalPlayer().getWorldLocation());
    }

    private Optional<NPC> GetHellhound() {
        return NPCs.search().nameContains("Skeleton Hellhound").alive().nearestToPlayer();
    }

    private Optional<NPC> GetCalverion() {
        Optional<NPC> target = Optional.empty();
        for (int x : calvIds) {
            target = NPCs.search().withId(x).nearestToPlayer();
            if (target.isPresent())
                return target;
        }

        return target;
    }


    private void DoKilling() {
        if (EzPlayerManagement.IsInCombat())
            return;
        NPCInteraction.interact(GetCalverion().get(), "Attack");
    }

    private void DoDogging() {
        if(IsUnderVetion() && !swing.isEmpty()){
            MovementPackets.queueMovement(resetLocal);
            return;
        }

        if (EzPlayerManagement.IsInCombat()) {
//            if (!Objects.equals(EzPlayerManagement.GetCombatTargetNPC().getName(), "Skeleton Hellhound") || !Objects.equals(EzPlayerManagement.GetCombatTargetNPC().getName(), "Greater Skeleton Hellhound")) {
//                NPCInteraction.interact(GetHellhound().get(), "Attack");
//
//            }
            return;
        } else {
            NPCInteraction.interact(GetHellhound().get(), "Attack");
            return;
        }


    }

    private void DoDodging() {
        List<WorldPoint> safeTiles;
        Optional<NPC> target = GetHellhound();
        if (target.isEmpty())
            target = GetCalverion();
        if (target.isEmpty()) {
            safeTiles = EthanApiPlugin.reachableTiles();
            safeTiles.removeIf(d -> lightning.keySet().stream().anyMatch(o -> d.distanceTo(o) <= 1));
            safeTiles.removeAll(swing.keySet());
            WorldPoint dest = safeTiles.stream().min(Comparator.comparingInt(o -> client.getLocalPlayer().getWorldLocation().distanceTo(o))).get();
            MovementPackets.queueMovement(dest);
            return;
        }

        safeTiles = EzNpc.GetSurroundingTiles(target.get(), 2);
        safeTiles.removeIf(d -> lightning.keySet().stream().anyMatch(o -> d.distanceTo(o) <= 1));
        safeTiles.removeAll(swing.keySet());
        safeTiles.removeAll(target.get().getWorldArea().toWorldPointList());
        if (!safeTiles.isEmpty()) {
            WorldPoint closestSafeTile = safeTiles.stream().min(Comparator.comparingInt(o -> client.getLocalPlayer().getWorldLocation().distanceTo(o))).get();
            MovementPackets.queueMovement(closestSafeTile);
            return;
        } else {
            //fallback for if no safe attackable tiles found
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "COULDN'T FIND A SAFE SPOT", null);
            safeTiles = EthanApiPlugin.reachableTiles();
            safeTiles.removeIf(d -> lightning.keySet().stream().anyMatch(o -> d.distanceTo(o) <= 1));
            safeTiles.removeAll(swing.keySet());
            safeTiles.removeAll(target.get().getWorldArea().toWorldPointList());
            WorldPoint dest = safeTiles.stream().min(Comparator.comparingInt(o -> resetLocal.distanceTo(o))).get();
            MovementPackets.queueMovement(dest);
        }


    }

    private State DetermineState() {

        if (!lightning.isEmpty() || !swing.isEmpty()) {

            if (lightning.keySet().stream().anyMatch((k) -> k.distanceTo(client.getLocalPlayer().getWorldLocation()) <= 1)) {
                return State.dodging;
            }
            if (swing.keySet().stream().anyMatch((k) -> k.distanceTo(client.getLocalPlayer().getWorldLocation()) == 0)) {
                return State.dodging;
            }

        }


        if (GetHellhound().isPresent())
            return State.dogging;

        if (GetCalverion().isPresent())
            return State.killing;

        return State.idle;
    }


    @Subscribe
    public void onGameTick(GameTick e) {

        if (!IsInside() && started) {
            started = false;
            return;
        }

        if (!EthanApiPlugin.loggedIn() || !started) {
            return;
        }

        if (timeout > 0) {
            timeout--;
            return;
        }
        lightning.forEach((k, v) -> lightning.put(k, v - 1));
        lightning.entrySet().removeIf(entry -> entry.getValue() <= 0);
        swing.forEach((k, v) -> swing.put(k, v - 1));
        swing.entrySet().removeIf(entry -> entry.getValue() <= 0);

        state = DetermineState();

        if (state == State.dogging) {
            DoDogging();
            return;
        }

        if (state == State.killing) {
            DoKilling();
            return;
        }

        if (state == State.dodging) {
            DoDodging();
            return;
        }

    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(overlay);
        overlayManager.remove(ezCalvarionOverlay);
        keyManager.unregisterKeyListener(toggle);
        timeout = 0;
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated e) {
        if (e.getGraphicsObject().getId() == 2346 || e.getGraphicsObject().getId() == 2347) {
            lightning.put(WorldPoint.fromLocal(client, e.getGraphicsObject().getLocation()), 4);
            return;
        }
        if (e.getGraphicsObject().getId() == 1446) {
            swing.put(WorldPoint.fromLocal(client, e.getGraphicsObject().getLocation()), 4);
        }
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
    }
}
