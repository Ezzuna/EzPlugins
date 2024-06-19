package com.Ezzuneware.EzWintertodt;


import com.example.EthanApiPlugin.Collections.*;
import com.example.EthanApiPlugin.Collections.query.ItemQuery;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.BankInteraction;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.API.BankUtil;
import com.piggyplugins.PiggyUtils.API.InventoryUtil;
import com.piggyplugins.PiggyUtils.BreakHandler.ReflectBreakHandler;
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
import net.runelite.client.plugins.puzzlesolver.solver.pathfinding.Pathfinder;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static com.example.EthanApiPlugin.EthanApiPlugin.canPathToTile;
import static com.example.EthanApiPlugin.EthanApiPlugin.pathToGoal;
import static net.runelite.api.ItemID.*;

@PluginDescriptor(
        name = "EzWintertodt",
        description = "Makes Wintertodt nice and easy",
        enabledByDefault = false,
        tags = {"eco", "plugin"}
)
@Slf4j
public class EzWintertodtPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzWintertodtConfig config;
    @Inject
    private EzWintertodtOverlay overlay;
    @Inject
    private ReflectBreakHandler breakHandler;
    @Inject
    private KeyManager keyManager;
    private Player player;
    private final Set<Integer> REGION_IDS = Set.of(6462, 6461);
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    private boolean started = false;
    @Getter
    private State state = State.timeout;
    private Instant timer;
    public int timeout = 0;
    @Getter
    private boolean test = false;
    @Getter
    private boolean gameRunning = false;
    @Getter
    private boolean startWT = false;
    private boolean hopping = false;
    private TileObject gapToHop;
    private int inventCount = 0;
    @Getter
    private int idleTicks = 0;
    @Getter
    private int logsBurned = 0;
    @Getter
    private int cratesEarned = 0;
    private int dbmTimer = 0;
    private int pyroDownTimer = 0;
    private int logsAquired = 0;
    private int cratesCount = 0;
    private int logCount = 0;
    private boolean eatingHandled = false;
    private int minLogCount = 8;
    private boolean inventCountsSetup = false;
    private final String FAILED = "You did not earn enough points to be worthy of a gift from the citizens of Kourend this time.";
    @Getter
    private int cratesFailed = 0;
    @Getter
    private int foodInInvent = 0;
    private boolean skipReignite = false;
    private WorldPoint safeStart = new WorldPoint(1630, 3994, 0);
    private WorldPoint safeStart2 = new WorldPoint(1630, 3973, 0);
    private WorldPoint safeStart3 = new WorldPoint(1615, 4010, 0);

    @Provides
    private EzWintertodtConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzWintertodtConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        timer = Instant.now();
        keyManager.registerKeyListener(toggle);
        overlayManager.add(overlay);
        breakHandler.registerPlugin(this);
        timeout = 0;
        startWT = false;
        cratesCount = 0;
        logCount = 0;
        logsAquired = 0;
        cratesEarned = 0;
        skipReignite = false;
        cratesFailed = 0;
        cratesCount = Inventory.getItemAmount(20703);
    }

    private void setUpInventTotals() {
        try {
            ItemQuery localCountQuery = Inventory.search().withName("Supply crate");
            if (!localCountQuery.result().isEmpty()) {
                cratesCount = localCountQuery.result().size();
            } else cratesCount = 0;
        } catch (Exception e) {
            cratesCount = 0;
        }

        cratesEarned = 0;
        cratesFailed = 0;

        inventCountsSetup = true;
    }

    @Override
    protected void shutDown() throws Exception {
        keyManager.unregisterKeyListener(toggle);
        overlayManager.remove(overlay);
        breakHandler.unregisterPlugin(this);
        timeout = 0;
    }

    private void resetVals() {
        startWT = false;
        started = false;
        timer = null;
        inventCount = 0;
        logsBurned = 0;
        skipReignite = false;


        logCount = 0;
        logsAquired = 0;
    }

    private boolean isBankPinOpen() {
        Widget bankPinWidget = client.getWidget(213, 0);
        if (bankPinWidget == null) {
            return false;
        }
        return !bankPinWidget.isHidden();
    }

    private boolean runIsOff() {
        return EthanApiPlugin.getClient().getVarpValue(173) == 0;
    }

    private void resetValsNoTimer() {
        startWT = false;
        started = false;
        inventCount = 0;
        logsBurned = 0;
        skipReignite = false;
        logCount = 0;
        logsAquired = 0;
    }

    public String getElapsedTime() {
        if (!startWT) {
            return "00:00:00";
        }
        Duration duration = Duration.between(timer, Instant.now());
        long durationInMillis = duration.toMillis();
        long second = (durationInMillis / 1000) % 60;
        long minute = (durationInMillis / (1000 * 60)) % 60;
        long hour = (durationInMillis / (1000 * 60 * 60)) % 24;

        return String.format("%02d:%02d:%02d", hour, minute, second);
    }

    @Subscribe
    public void onStatChanged(StatChanged event) {
        if (!started) return;
        if (client.getBoostedSkillLevel(Skill.HITPOINTS) <= config.eatAt() && !eatingHandled) {
            eatFood();
        }
    }

    private void eatFood() {
        Optional<Widget> food = InventoryUtil.nameContainsNoCase(config.foodName()).first();
        if (food.isPresent()) {
            MousePackets.queueClickPacket();
            InventoryInteraction.useItem(food.get(), "Eat");
            eatingHandled = true;
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        final ItemContainer container = event.getItemContainer();

        if (container != client.getItemContainer(InventoryID.INVENTORY)) {
            return;
        }

        int localCratesCount = 0;
        int localLogCount = 0;

        final Item[] inv = container.getItems();
        for (Item item : inv) {
            switch (item.getId()) {
                case BRUMA_ROOT:
                case BRUMA_KINDLING:
                    ++localLogCount;
                    break;

                case SUPPLY_CRATE:
                    ++localCratesCount;

            }
        }
        if (localLogCount < logsAquired) {
            ++logsBurned;
        }
        logsAquired = localLogCount;

        if (localCratesCount > cratesCount)
            ++cratesEarned;
        cratesCount = localCratesCount;
    }


    @Subscribe
    private void onGameTick(GameTick event) {
        player = client.getLocalPlayer();
        if (player == null
                || !startWT
                || !REGION_IDS.contains(client.getLocalPlayer().getWorldLocation().getRegionID())
                || breakHandler.isBreakActive(this)) {
            return;
        }
        if (!checkReqs()) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Please go get a knife, hammer, axe and tinderbox", null);
            resetValsNoTimer();
            this.state = State.timeout;
            return;
        }
        gameRunning = TileObjects.search().withId(29308).first().isPresent();
        if (!inventCountsSetup) {
            setUpInventTotals();
            return;
        }
         foodInInvent = Inventory.search().matchesWildCardNoCase("*" + config.foodName()).result().size();

        Optional<Widget> exitMessage = Widgets.search().withId(14352385).withTextContains("Leave and lose all progress.").first();
        if (exitMessage.isPresent()) {
            Widget exitWidget = exitMessage.get();
            WidgetPackets.queueResumePause(exitWidget.getId(), 1);
            return;
        }
        if (client.getBoostedSkillLevel(Skill.HITPOINTS) <= config.eatAt() && !eatingHandled) {
            eatFood();
            return;
        }
        eatingHandled = false;


        logsBurned = gameRunning ? logsBurned : 0;

        hopping = playerOnHoppingTile();


        final Actor interact = player.getInteracting();
        int animId = player.getAnimation();
        idleTicks = animId == -1 ? idleTicks + 1 : 0;
        ++pyroDownTimer;

        if (pyroDownTimer > 10 && skipReignite) {
            skipReignite = false;
        }
        if (idleTicks > 60) {
            idleTicks = 1;
        } else if (idleTicks > 30 && canPathToTile(safeStart2).isReachable() && !EthanApiPlugin.isMoving()) {
            MovementPackets.queueMovement(safeStart);
            skipReignite = true;
            pyroDownTimer = 0;

        } else if (idleTicks > 20 && canPathToTile(safeStart).isReachable() && !EthanApiPlugin.isMoving()) {
            MovementPackets.queueMovement(safeStart2);
            skipReignite = true;
            pyroDownTimer = 0;
        }

        Optional<TileObject> snowfall = TileObjects.search().withId(26690).withinDistance(4).first();
        if (snowfall.isPresent()) {

            if (dbmTimer < 1) {
                ++dbmTimer;
                Optional<TileObject> dangerfall = TileObjects.search().withId(26690).withinDistance(1).first();
                if (dangerfall.isPresent()) {
                    var dangerSpot = dangerfall.get().getWorldLocation();
                    int x, y, z = 0;
                    var loc = client.getLocalPlayer().getWorldLocation();
                    x = dangerSpot.getX() - loc.getX();

                    y = dangerSpot.getY() - loc.getY();
                    if (y == 0 && x == 0)
                        --y;
                    WorldPoint wp1 = new WorldPoint(loc.getX() + -x * 2, loc.getY() + -y * 2, loc.getPlane());
                    if (EthanApiPlugin.canPathToTile(wp1).isReachable()) {
                        MovementPackets.queueMovement(wp1);
                        return;
                    }
                    WorldPoint wp2 = new WorldPoint(loc.getX() + -x * -2, loc.getY() + -y * 2, loc.getPlane());
                    if (EthanApiPlugin.canPathToTile(wp2).isReachable()) {
                        MovementPackets.queueMovement(wp2);
                        return;
                    }

                    WorldPoint wp3 = new WorldPoint(loc.getX() + -x * 2, loc.getY() + -y * -2, loc.getPlane());
                    if (EthanApiPlugin.canPathToTile(wp3).isReachable()) {
                        MovementPackets.queueMovement(wp3);
                        return;
                    }

                    WorldPoint wp4 = new WorldPoint(loc.getX() + -x * -2, loc.getY() + -y * -2, loc.getPlane());
                    if (EthanApiPlugin.canPathToTile(wp4).isReachable()) {
                        MovementPackets.queueMovement(wp4);
                        return;
                    }


                } else if (dbmTimer > 3)
                    dbmTimer = 0;
                return;
            } else
                ++dbmTimer;

        } else
            dbmTimer = 0;

        state = getCurrentState();
        switch (state) {
            case burning:
                if (interact == null && !skipReignite) {
                    //add reignite and repair stuff here
                    Optional<TileObject> exstinguished = TileObjects.search().withId(29312).withinDistance(6).first();
                    Optional<TileObject> demonlished = TileObjects.search().withId(29313).withinDistance(6).first();
                    if (exstinguished.isPresent()  && dbmTimer < 1) {
                        TileObject brazier = TileObjects.search().withId(29312).nearestToPlayer().get();
                        MousePackets.queueClickPacket();
                        TileObjectInteraction.interact(brazier, "Light");
                    } else if (demonlished.isPresent() && dbmTimer < 1 ) {
                        TileObject brazier = TileObjects.search().withId(29313).nearestToPlayer().get();
                        MousePackets.queueClickPacket();
                        TileObjectInteraction.interact(brazier, "Fix");
                    } else if (idleTicks > 2  || animId == 867) {
                        doFiremaking();
                        inventCount++;
                    }


                }
                break;
            case loading:
                if (client.getLocalPlayer().getWorldLocation().getRegionID() == 6462) {
                    if (canPathToTile(safeStart).isReachable()) {
                        MovementPackets.queueMovement(safeStart);
                    }
                }
                break;
            case timeout:
                timeout--;
                break;
            case chopping:
                if (interact == null && animId == -1) {
                    startChopping();
                }
                break;
            case igniting:
                break;
            case fletching:
                if (interact == null && animId == -1 && idleTicks > 2) {
                    fletchLogs();
                }
                break;
            case repairing:
                break;
            case bank_run:
                if (interact == null && player.getRunAnimation() != -1 && animId == -1 && !skipReignite) {
                    clickDoor();
                }
                break;
            case handle_break:
                breakHandler.startBreak(this);
                timeout = 10;
                break;
            case traveling_in:
                if (interact == null && animId == -1) {
                    clickDoor();
                }
                break;
            case bank_pin_wait:
                break;
            case pillar_hopper:
                if (interact == null && animId == -1) {
                    handleHopping();
                }
                break;
            case restock_items:
                if (interact == null && player.getRunAnimation() != -1 && animId == -1) {
                    restockItems();
                }
                break;
            case pyro_down_delay:
                break;
            case waiting_for_spawn:
                break;
        }


    }

    @Subscribe
    private void onChatMessage(ChatMessage event) {
        if (client.getGameState() != GameState.LOGGED_IN || !started) {
            return;
        }

        if (event.getType() != ChatMessageType.SPAM && event.getType() != ChatMessageType.GAMEMESSAGE) return;
        if (event.getMessage().contains(FAILED)) {
            ++cratesFailed;
        }
    }

    private void startChopping() {
        TileObject roots = TileObjects.search().withId(29311).nearestToPlayer().get();
        MousePackets.queueClickPacket();
        TileObjectInteraction.interact(roots, "Chop");
    }

    @Subscribe
    private void onGameObjectSpawned(GameObjectSpawned event) {
        player = client.getLocalPlayer();
        if (player == null
                || !startWT
                || !REGION_IDS.contains(client.getLocalPlayer().getWorldLocation().getRegionID())
                || breakHandler.isBreakActive(this)) {
            return;
        }
        final GameObject gameObject = event.getGameObject();
        WorldPoint loc = gameObject.getWorldLocation();
        EthanApiPlugin.PathResult result;
        switch (gameObject.getId()) {
            case 29312:
                result = canPathToTile(loc);
                if (result.getDistance() < 5 && result.isReachable()) {
                    TileObject brazier = TileObjects.search().withId(29312).nearestToPlayer().get();
                    MousePackets.queueClickPacket();
                    TileObjectInteraction.interact(brazier, "Light");
                }
                break;
            case 29313:
                result = canPathToTile(loc);
                if (result.getDistance() < 5 && result.isReachable()) {
                    TileObject brazier = TileObjects.search().withId(29313).nearestToPlayer().get();
                    MousePackets.queueClickPacket();
                    TileObjectInteraction.interact(brazier, "Fix");
                }
                break;

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
        startWT = !startWT;

        if (!startWT) {
            resetValsNoTimer();
            this.state = State.timeout;
            inventCountsSetup = true;
            breakHandler.stopPlugin(this);
        } else {
            inventCountsSetup = false;
            breakHandler.startPlugin(this);
            timer = Instant.now();
            inventCountsSetup = false;


        }
    }

    private State getCurrentState() {
        if (breakHandler.shouldBreak(this)) {
            return State.handle_break;
        }
        if (!shouldRestock() && player.getWorldLocation().getRegionID() == 6461) {
            return State.traveling_in;
        }
        if (isBankPinOpen()) {
            return State.bank_pin_wait;
        }

        if(hopping){
            return State.pillar_hopper;
        }

        if (shouldRestock()) {
            if (player.getWorldLocation().getRegionID() == 6461) {
                return State.restock_items;
            } else if (player.getWorldLocation().getRegionID() == 6462) {
                return State.bank_run;
            } else if (canPathToTile(safeStart).isReachable()) {
                MovementPackets.queueMovement(safeStart);
            }
        }

        if (!gameRunning && config.hopIdle() && !shouldRestock()) {
            return State.pillar_hopper;
        }
        player = client.getLocalPlayer();




        final Actor interact = player.getInteracting();
        int animId = player.getAnimation();
        if (Inventory.full() && interact == null && idleTicks > 1) {
            if (Inventory.search().withId(20695).first().isPresent()) {
                //if we have bruma roots
                if (config.doFletch() && logsBurned == 0) {
                    return State.fletching;
                } else {
                    return State.burning;
                }
            } else if (Inventory.search().withId(20696).first().isPresent()) {
                return State.burning;
            }
        }
        if (logsAquired > minLogCount) {
            if (logsBurned > 12 || (Inventory.getItemAmount("Bruma root") == 0 && logsBurned > 0)) {
                //all logs been processed
                return State.burning;
            } else if (logsAquired > 0 && logsBurned > 12 && animId != 838 && idleTicks > 1) {
                return State.burning;
            } else if (!Inventory.full() && idleTicks > 1) {
                return State.chopping;
            }

        } else if (!Inventory.full() && idleTicks > 3) {
            return State.chopping;
        }


        if (idleTicks > 1 && !gameRunning)
            return State.loading;
        else
            return state;
    }

    private void clickDoor() {
        Optional<TileObject> gate = TileObjects.search().withName("Doors of Dinh").nearestToPlayer();
        if (idleTicks > 25 && canPathToTile(safeStart2).isReachable()) {
            MovementPackets.queueMovement(safeStart2);
            idleTicks = 0;
            return;
        }
        if (!gate.isPresent()) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "No gate found.", null);
            if (canPathToTile(safeStart).isReachable()) {
                MovementPackets.queueMovement(safeStart);
                return;
            }

        }
        MousePackets.queueClickPacket();
        TileObjectInteraction.interact(gate.get(), "Enter");
        //possible widget opens, handled in state loop
    }

    private int tickDelay() {
        return config.tickDelay() ? ThreadLocalRandom.current().nextInt(config.tickDelayMin(), config.tickDelayMax()) : 0;
    }

    private boolean shouldRestock() {

        if (Inventory.getItemAmount("Supply crate") >= config.crateCount())
            return true;

        if (config.foodName().isEmpty()) {
            return false;
        }

        if (!gameRunning && !config.foodName().isBlank() && state != State.bank_run && state != State.bank_pin_wait) {
            if (foodInInvent <= 1) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Wintertodt has been subdued and we have less than 1 food my dude. Let's bank.", null);
                return true;
            }
        }

        if (!config.foodName().isBlank()) {
            return InventoryUtil.nameContainsNoCase(config.foodName()).empty();
        }


        return false;
    }

    private void fletchLogs() {
        Widget knife = Inventory.search().withName("Knife").first().get();
        Widget brumaRoots = Inventory.search().withId(20695).first().get();
        MousePackets.queueClickPacket();
        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetOnWidget(knife, brumaRoots);
    }

    private void doFiremaking() {
        TileObject brazier = TileObjects.search().withId(29314).withinDistance(15 + idleTicks / 4).nearestToPlayer().get();
        MousePackets.queueClickPacket();
        TileObjectInteraction.interact(brazier, "Feed");
    }

    public boolean playerOnHoppingTile() {
        WorldPoint loc = player.getWorldLocation();
        if (Objects.equals(loc, new WorldPoint(1629, 4023, 0)))
            return true;
        else return Objects.equals(loc, new WorldPoint(1631, 4023, 0));
    }

    private void handleHopping() {
        if (EthanApiPlugin.isMoving())
            return;
        if (client.getLocalPlayer().getWorldLocation().distanceTo(safeStart) < 10) {
            //fringe scenario player could run between gaps inbetween ticks

            if (canPathToTile(safeStart3).isReachable()) {
                MovementPackets.queueMovement(safeStart3);
            }
            return;
        }
        if (gapToHop != null) {
            MousePackets.queueClickPacket();
            TileObjectInteraction.interact(gapToHop, "Jump");
            gapToHop = null;
        } else {

            Optional<TileObject> pillar = TileObjects.search().withName("Gap").nearestToPlayer();
            MousePackets.queueClickPacket();
            if (pillar != null) {
                MousePackets.queueClickPacket();
                TileObjectInteraction.interact(pillar.get(), "Jump");
                gapToHop = pillar.get();
            } else if (canPathToTile(safeStart).isReachable()) {
                MovementPackets.queueMovement(safeStart);
            }

        }
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Hop.", null);
    }

    private Boolean checkReqs() {
        Optional<Widget> knife = Inventory.search().withName("Knife").first();
        Optional<Widget> hammer = Inventory.search().withId(2347).first();
        Optional<Widget> tinderbox = Inventory.search().withName("Tinderbox").first();
        Optional<Widget> axe = Inventory.search().matchesWildCardNoCase("*axe").first();
        return (knife.isPresent() && hammer.isPresent() && (tinderbox.isPresent() || config.skipLookForTinderBox()) && axe.isPresent());
    }

    private void restockItems() {
        if (Bank.isOpen()) {

            Optional<Widget> cake = BankInventory.search().matchesWildCardNoCase("*cake*").first();
            if (cake.isPresent()) {
                BankInteraction.useItem(cake.get(), "Deposit-All");
                return;
            }
            Optional<Widget> supplyCrate = BankInventory.search().withName("Supply crate").first();
            if (supplyCrate.isPresent()) {
                BankInteraction.useItem(supplyCrate.get(), "Deposit-All");
                return;
            }
            if (!config.foodName().isEmpty() && !InventoryUtil.hasItem(config.foodName())) {
                Optional<Widget> bankFood = BankUtil.nameContainsNoCase(config.foodName()).first();
                if (bankFood.isPresent()) {
                    BankInteraction.withdrawX(bankFood.get(), config.foodCount());
                    return;
                }

                if (!config.keepGoing()) {
                    state = State.timeout;
                    return;
                }
            }

        } else {
            Optional<TileObject> bankBooth = TileObjects.search().filter(tileObject -> {
                ObjectComposition objectComposition = TileObjectQuery.getObjectComposition(tileObject);
                return getName().toLowerCase().contains("bank") ||
                        Arrays.stream(objectComposition.getActions()).anyMatch(action -> action != null && action.toLowerCase().contains("bank"));
            }).nearestToPlayer();
            if (bankBooth.isPresent()) {
                MousePackets.queueClickPacket();
                TileObjectInteraction.interact(bankBooth.get(), "Bank");
                timeout = tickDelay();
            }
        }
    }
}