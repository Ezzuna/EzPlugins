package com.Ezzuneware.EzScurrius;


import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.NPCInteraction;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.API.PrayerUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.NpcUtil;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@PluginDescriptor(
        name = "EzScurrius",
        description = "",
        enabledByDefault = false,
        tags = {"eco", "plugin"}
)
@Slf4j
public class EzScurriusPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzScurriusConfig config;
    @Inject
    private EzScurriusOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    @Inject
    private NpcUtil npcUtil;
    @Inject
    private ItemManager itemManager;
    @Getter
    private State state = State.idle;
    public int BoostedHitpoints, Hitpoints;
    @Getter
    private int projectileTimer = 0;
    private int backupTimer = 0;
    private final int MAGIC_ATTACK_ID = 2640;
    private final int RANGED_ATTACK_ID = 2642;
    private final int quickPrayerWidgetID = WidgetInfo.MINIMAP_QUICK_PRAYER_ORB.getPackedId();
    @Getter
    private int rocksCount = 0;
    private Projectile rangeProjectile;
    private Projectile mageProjectile;
    private Boolean resolved = false;
    HashMap<WorldPoint, Integer> rocks = new HashMap<>();
    private final int SCURRIUS_EATING_ANIM_ID = 10689;      //P: ??
    private int scurriusEatCount = 0;
    @Getter
    private float Scurr_health = 0f;
    @Getter
    private Boolean finalPhase = false;
    private List<ezProjectile> projectiles = new ArrayList<ezProjectile>();
    @Getter
    private int ratTimer = 0;
    private Boolean spawnChecked = false;
    List<NPC> rats = new ArrayList<NPC>();


    @Getter
    private boolean started = false;
    @Getter
    public int timeout = 0;
    public int idleTicks = 0;
    private boolean prayChanged = false;

    @Provides
    private EzScurriusConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzScurriusConfig.class);
    }

    private boolean IsInScurrius() {
        if (client.getTopLevelWorldView().getScene().isInstance()) {
            return true;
        }
        return false;
    }

    @Override
    protected void startUp() throws Exception {
        keyManager.registerKeyListener(toggle);
        overlayManager.add(overlay);
        timeout = 0;
        prayChanged = true;
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

    private void CheckProjectiles() {
        int curr = client.getGameCycle();

        List<ezProjectile> toRemove = new ArrayList<ezProjectile>();

        int diff = 0;
        for (ezProjectile proj : projectiles) {
            diff = proj.projectileTimer - curr;
            if (curr > proj.projectileTimer) {
                toRemove.add(proj);
                continue;
            }
            if (diff <= 37) {
                switch (proj.projectileType) {
                    case magic:
                        Optional<Widget> toggleMage = Widgets.search().nameMatchesWildCardNoCase("*Protect from Magic*").withAction("Toggle").first();

                        if (toggleMage.isPresent()) {
                            if (!prayChanged) {
                                MousePackets.queueClickPacket();
                                WidgetPackets.queueWidgetAction(toggleMage.get(), "Toggle");
                            }

                            prayChanged = true;
                            projectiles.remove(toRemove);
                            return;
                        }
                        break;

                    case ranged:
                        Optional<Widget> toggleRange = Widgets.search().nameMatchesWildCardNoCase("*Protect from Missiles*").withAction("Toggle").first();

                        if (toggleRange.isPresent()) {
                            if (!prayChanged) {
                                MousePackets.queueClickPacket();
                                WidgetPackets.queueWidgetAction(toggleRange.get(), "Toggle");
                            }

                            prayChanged = true;
                            projectiles.remove(toRemove);
                            return;
                        }
                        break;
                }
            }
        }
        Optional<Widget> toggleMelee = Widgets.search().nameMatchesWildCardNoCase("*Protect from Melee*").withAction("Toggle").first();

        if (toggleMelee.isPresent() && prayChanged) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(toggleMelee.get(), "Toggle");
            prayChanged = false;
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned e) {

        if (!EthanApiPlugin.loggedIn() || !IsInScurrius()) {
            return;
        }


        if (Objects.equals(e.getNpc().getName(), "Giant rat")) {
            if (!spawnChecked) {
                rats.clear();
                spawnChecked = true;
            }

            rats.add(e.getNpc());
            ratTimer = rats.size();
        }

    }

    @Subscribe
    private void onProjectileMoved(ProjectileMoved event) {
        if (client.getGameState() != GameState.LOGGED_IN || !IsInScurrius()) {
            return;
        }
        if (resolved)
            return;
        Projectile projectile = event.getProjectile();
        if (projectile.getId() == MAGIC_ATTACK_ID) {
            System.out.println("Magic end:" + projectile.getEndCycle());


            if (projectiles.stream().anyMatch(proj -> proj.projectileTimer == projectile.getEndCycle()))
                return;

            projectiles.add(new ezProjectile(projectile.getId(), ezProjectileType.magic, projectile.getEndCycle()));

//
//            if (mageProjectile == null && rangeProjectile == null && projectileTimer == 0) {
//                projectileTimer = finalPhase ? 4 : 4;
//                mageProjectile = projectile;
//                rangeProjectile = null;
//
//
//            }
        }

        if (projectile.getId() == RANGED_ATTACK_ID) {

            if (projectiles.stream().anyMatch(proj -> proj.projectileTimer == projectile.getEndCycle()))
                return;

            projectiles.add(new ezProjectile(projectile.getId(), ezProjectileType.ranged, projectile.getEndCycle()));

            System.out.println("Ranged end:" + projectile.getEndCycle());
//            if (rangeProjectile == null && mageProjectile == null && projectileTimer == 0) {
//
//                projectileTimer = finalPhase ? 4 : 4;
//
//                rangeProjectile = projectile;
//                mageProjectile = null;
//            }
        }
    }


    @Subscribe
    private void onGameTick(GameTick event) {
        if (projectileTimer > 0) {
            --projectileTimer;

        }


        System.out.println("Current Game Cycle: " + client.getGameCycle());

        resolved = false;
        rocksCount = rocks.size();
        spawnChecked = false;
        BoostedHitpoints = client.getBoostedSkillLevel(Skill.HITPOINTS);

//        rocks.forEach((k, v) -> rocks.put(k, v - 1));
//        rocks.entrySet().removeIf(entry -> entry.getValue() <= 0);
        if (started && !IsInScurrius())
            started = false;


        if (!EthanApiPlugin.loggedIn() || !started) {
            return;
        }


        state = DetermineState();

        Optional<NPC> scurrius = NPCs.search().withId(NpcID.SCURRIUS).first();
        scurrius.ifPresent(rat -> {
//            if (rat.getHealthRatio() < 0)
//                return;
//            finalPhase = rat.getHealthRatio() < 30;
//            Scurr_health = rat.getHealthRatio() * 5;
        });
        Optional<NPC> scurrius2 = NPCs.search().withId(NpcID.SCURRIUS_7222).first();
        scurrius2.ifPresent(rat -> {
//            if (rat.getHealthRatio() < 0)
//                return;
//            finalPhase = rat.getHealthRatio() < 30;
//            Scurr_health = rat.getHealthRatio() * 5;
        });
        Optional<NPC> scurriusBaby = NPCs.search().withName("Giant rat").first();
        if (scurrius.isPresent() || scurrius2.isPresent() || scurriusBaby.isPresent()) {
            //DoPrayer();
            CheckProjectiles();
            if (projectileTimer == 0) {
                mageProjectile = null;
                rangeProjectile = null;
            }

            if (client.getVarbitValue(Varbits.QUICK_PRAYER) == 1) {
                togglePrayer();
            }

            togglePrayer();
        } else if (client.getVarbitValue(Varbits.QUICK_PRAYER) == 1) {
            togglePrayer();
            finalPhase = false;
        } else {
            finalPhase = false;
        }

        if (timeout > 0) {
            timeout--;
            return;
        }

        if (state == State.eating) {
            DoEating();
            setTimeout();
        }

        if (state == State.dodging) {
            DoDodging();
        }

        if (state == State.killing) {
            DoKilling();
            setTimeout();
        }

        if (state == State.babies) {
            DoBabyKilling();
        }


    }

    private void DoPrayer() {
        if (mageProjectile != null && projectileTimer == 0) {
            Optional<Widget> toggleRange = Widgets.search().nameMatchesWildCardNoCase("*Protect from Magic*").withAction("Toggle").first();

            if (toggleRange.isPresent()) {
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetAction(toggleRange.get(), "Toggle");
                prayChanged = true;
                mageProjectile = null;
                rangeProjectile = null;
                resolved = true;
            }
//            if (!PrayerUtil.isPrayerActive(Prayer.PROTECT_FROM_MAGIC)) {
//                PrayerUtil.togglePrayer(Prayer.PROTECT_FROM_MAGIC);
//            }
        } else if (rangeProjectile != null && projectileTimer == 0) {
            Optional<Widget> toggleRange = Widgets.search().nameMatchesWildCardNoCase("*Protect from Missiles*").withAction("Toggle").first();

            if (toggleRange.isPresent()) {
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetAction(toggleRange.get(), "Toggle");
                prayChanged = true;
                mageProjectile = null;
                rangeProjectile = null;
                resolved = true;
            }
        } else if (prayChanged) {
            Optional<Widget> toggleRange = Widgets.search().nameMatchesWildCardNoCase("*Protect from Melee*").withAction("Toggle").first();

            if (toggleRange.isPresent()) {
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetAction(toggleRange.get(), "Toggle");
                prayChanged = false;
            }
        }
    }

    private void DoDodging() {
        var rockTiles = rocks.keySet().stream().filter(k -> k.distanceTo(client.getLocalPlayer().getWorldLocation()) == 0).collect(Collectors.toList());
        ;

        var reachableTiles = EthanApiPlugin.reachableTiles().stream().filter(d -> !rocks.keySet().contains(d)).collect(Collectors.toList());     //reachable tiles w/o rocks
        Optional<NPC> scurrius = NPCs.search().withId(NpcID.SCURRIUS).first();
        if (scurrius.isEmpty())
            scurrius = NPCs.search().withId(NpcID.SCURRIUS_7222).first();

        Optional<NPC> finalScurrius = scurrius;
        var scurriusAttackTiles = reachableTiles.stream().filter(d -> d.distanceTo(finalScurrius.get().getWorldLocation()) == 3).collect(Collectors.toList());

        var x = scurriusAttackTiles.stream().sorted((wp1, wp2) -> {
            var local = client.getLocalPlayer().getWorldLocation();
            return wp1.distanceTo(local) - wp2.distanceTo(local);
        }).findFirst();

        MovementPackets.queueMovement(x.get());

    }

    private void DoBabyKilling() {
        Optional<NPC> scurrius = Optional.empty();

        if (ratTimer == 0 || !config.onetapBabies()) {
            scurrius = NPCs.search().withName("Giant rat").alive().nearestToPlayer();
        } else if (ratTimer <= rats.size()) {
            NPCInteraction.interact(rats.get(ratTimer - 1), "Attack");
        }

        if (scurrius.isPresent()) {
            NPCInteraction.interact(scurrius.get(), "Attack");
        }

        if (ratTimer > 0)
            --ratTimer;


    }

    private void DoKilling() {

        Player play = client.getLocalPlayer();

        if (play.isInteracting() && state == State.killing)
            return;

        Optional<NPC> scurrius = NPCs.search().withName("Giant rat").alive().nearestToPlayer();
        if (scurrius.isEmpty())
            scurrius = NPCs.search().withId(NpcID.SCURRIUS).first();
        if (scurrius.isEmpty())
            scurrius = NPCs.search().withId(NpcID.SCURRIUS_7222).first();
        if (scurrius.isPresent()) {
            NPCInteraction.interact(scurrius.get(), "Attack");
            return;
        }
    }

    private State DetermineState() {
        State oRet = State.idle;

        if (client.getBoostedSkillLevel(Skill.PRAYER) < 1) {
            return State.exiting;
        }

        if (BoostedHitpoints <= config.eatAt())
            return State.eating;


        if (rocksCount > 0) {
            rocks.forEach((k, v) -> rocks.put(k, v - 1));
            rocks.entrySet().removeIf(entry -> entry.getValue() <= 0);

            if (!EthanApiPlugin.isMoving()) {
                if (rocks.keySet().stream().anyMatch((k) -> k.distanceTo(client.getLocalPlayer().getWorldLocation()) == 0)) {
                    oRet = State.dodging;
                    return oRet;
                }
            }


        }

        Optional<NPC> scurrius = NPCs.search().withName("Giant rat").alive().first();
        if (scurrius.isEmpty())
            scurrius = NPCs.search().withId(NpcID.SCURRIUS).first();
        else
            return State.babies;
        if (scurrius.isEmpty())
            scurrius = NPCs.search().withId(NpcID.SCURRIUS_7222).first();
        if (scurrius.isPresent()) {
            return State.killing;
        }


        return oRet;
    }

    private void DoEating() {
        InventoryInteraction.useItem(config.Food().toString(), "Eat");
        setTimeout();
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

    private final HotkeyListener toggle = new HotkeyListener(() -> config.toggle()) {
        @Override
        public void hotkeyPressed() {
            toggle();
        }
    };

    private void setTimeout() {
        timeout = RandomUtils.nextInt(config.tickDelayMin(), config.tickDelayMax());
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated e) {
        if (e.getGraphicsObject().getId() == 2644) {
            rocks.put(WorldPoint.fromLocal(client, e.getGraphicsObject().getLocation()), 12);
            return;
        }

    }

    public void toggle() {
        if (!EthanApiPlugin.loggedIn()) {
            return;
        }
        started = !started;
    }
}