package com.Ezzuneware.EzBlastFurnace;

import com.Ezzuneware.EzBlastFurnace.overlays.*;
import com.Ezzuneware.EzBlastFurnace.state.CoalBagState;
import com.Ezzuneware.EzBlastFurnace.state.FurnaceState;
import com.example.EthanApiPlugin.Collections.*;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.BankInteraction;
import com.example.InteractionApi.BankInventoryInteraction;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.PacketUtils.WidgetID;
import com.example.Packets.MousePackets;
import com.example.Packets.MovementPackets;
import com.example.Packets.WidgetPackets;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.Ezzuneware.EzBlastFurnace.state.BlastFurnaceState;
import com.Ezzuneware.EzBlastFurnace.utils.MethodHandler;
import com.Ezzuneware.EzBlastFurnace.utils.ObjectManager;
import com.Ezzuneware.EzBlastFurnace.utils.SessionStatistics;
import com.Ezzuneware.EzBlastFurnace.utils.Strings;
import com.piggyplugins.PiggyUtils.API.PlayerUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.RandomUtils;
import org.lwjgl.system.linux.Stat;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
        name = "EzBlastFurnace",
        description = "Only works for gold in current state if it does lmao"
)
public class EasyBlastFurnacePlugin extends Plugin {
    public static final int CONVEYOR_BELT = ObjectID.CONVEYOR_BELT;
    public static final int BAR_DISPENSER = NullObjectID.NULL_9092;
    public static final int BANK_CHEST = ObjectID.BANK_CHEST_26707;

    public static final WorldPoint PICKUP_POSITION = new WorldPoint(1940, 4962, 0);

    private static final Pattern COAL_FULL_MESSAGE = Pattern.compile(Strings.COAL_FULL);
    private static final Pattern COAL_EMPTY_MESSAGE = Pattern.compile(Strings.COAL_EMPTY);
    private State ezState = State.idle;
    @Getter
    private boolean started = false;
    public int timeout = 0;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private EasyBlastFurnaceConfig config;

    @Inject
    private BlastFurnaceState state;

    @Inject
    private ObjectManager objectManager;

    @Inject
    private InstructionOverlay instructionOverlay;

    @Inject
    private StatisticsOverlay statisticsOverlay;

    @Inject
    private ItemStepOverlay itemStepOverlay;

    @Inject
    private BankItemStepOverlay bankItemStepOverlay;

    @Inject
    private WidgetStepOverlay widgetStepOverlay;

    @Inject
    private ObjectStepOverlay objectStepOverlay;

    @Inject
    private TileStepOverlay tileStepOverlay;
    @Inject
    private PlayerUtil playerUtil;

    @Inject
    private CoalBagOverlay coalBagOverlay;

    @Inject
    private MethodHandler methodHandler;

    @Inject
    private SessionStatistics statistics;

    @Getter
    private boolean isEnabled = false;

    @Getter
    private int lastCheckTick = 0;
    private int oreOntoConveyorCount = 0;
    private final Set<Integer> REGION_IDS = Set.of(7757);


    private void CheckInventory() {
        Optional<Widget> ore = Inventory.search().matchesWildCardNoCase("*ore").first();
        if (ore.isPresent()) {
            ezState = State.conveying;
            return;
        }

        Optional<Widget> coal = Inventory.search().withName("Coal").first();
        if (coal.isPresent()) {
            ezState = State.conveying;
            return;
        }


        Optional<Widget> bar = Inventory.search().matchesWildCardNoCase("*bar").first();
        if (bar.isPresent()) {
            ezState = State.resupplying;
            return;
        }

        if (state.getFurnace().has(config.productName().getItemID())) {
            ezState = State.collecting;
            return;
        }


        if (!state.getFurnace().has(ItemID.IRON_ORE, ItemID.MITHRIL_ORE, ItemID.GOLD_ORE, ItemID.ADAMANTITE_ORE, ItemID.SILVER_ORE, ItemID.RUNITE_ORE)) {
            if (state.getFurnace().getOresOnConveyorBelt() == 0)
                ezState = State.resupplying;
            else {
                if (!Inventory.full()) {
                    if (!state.getCoalBag().isEmpty()) {
                        EmptySack();
                        ezState = State.conveying;
                        return;
                    } else
                        ezState = State.idle;
                } else
                    ezState = State.idle;

            }


        }


    }

    private void DoTraveling() {
        MousePackets.queueClickPacket();
        MovementPackets.queueMovement(PICKUP_POSITION);
        setTimeout();
    }

    private void EmptySack() {
        Optional<Widget> coalBag = Inventory.search().matchesWildCardNoCase("*oal bag*").first();
        if (coalBag.isPresent()) {
            InventoryInteraction.useItem(coalBag.get(), "Empty");
            return;
        }
    }


    private void DoConveying() {
        Optional<Widget> iceGloves = Inventory.search().withId(776).first();
        if (iceGloves.isPresent()) {
            InventoryInteraction.useItem(iceGloves.get(), "Wear");
            return;
        }

        TileObjectInteraction.interactNearest(9100, "Put-ore-on");
        setTimeout();
        return;
    }

    private void DoCollecting() {
        Optional<Widget> iceGloves = Inventory.search().withId(1580).first();
        if (iceGloves.isPresent()) {
            InventoryInteraction.useItem(iceGloves.get(), "Wear");
            return;
        }
        Optional<Widget> collection = Widgets.search().withId(17694733).first();
        if (collection.isPresent()) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueResumePause(17694734, 27);
        }

        TileObjectInteraction.interactNearest(9092, "Take");
        setTimeout();
        return;
    }

    private void checkRunEnergy() {
        if (playerUtil.isRunning() && playerUtil.runEnergy() >= 40) {
            log.info("Run");
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetActionPacket(1, 10485787, -1, -1);
        }
    }

    private void Restock() {
        if (Bank.isOpen()) {
            List<Widget> bankInv = BankInventory.search().result();
            for (Widget item : bankInv) {
                if (!Objects.equals(Text.removeTags(item.getName()), "Ice gloves") && !Objects.equals(Text.removeTags(item.getName()), "Goldsmith gauntlets") && !Objects.equals(Text.removeTags(item.getName()), "Coal bag")) {
                    MousePackets.queueClickPacket();
                    BankInventoryInteraction.useItem(item, "Deposit-All");
                }

            }

            Optional<Widget> coalBag = BankInventory.search().matchesWildCardNoCase("*oal bag*").first();
            Optional<Widget> coalOre = Optional.empty();

            if (coalBag.isPresent() && state.getCoalBag().isEmpty()) {
                MousePackets.queueClickPacket();
                BankInventoryInteraction.useItem(coalBag.get(), "Fill");
                return;
            }

            switch (config.productName()) {
                case GOLD_BAR:
                    Optional<Widget> goldOre = Bank.search().withName("Gold ore").first();
                    goldOre.ifPresent(widget -> BankInteraction.useItem(widget, "Withdraw-All"));
                    setTimeout();
                    break;

                case STEEL_BAR:

                    Optional<Widget> ironOre = Bank.search().withName("Iron ore").first();
                    ironOre.ifPresent(widget -> BankInteraction.useItem(widget, "Withdraw-All"));
                    setTimeout();
                    break;

                case MITHRIL_BAR:

                    if (state.getFurnace().isCoalRunNext(2)) {
                        coalOre = Bank.search().withName("Coal").first();
                        coalOre.ifPresent(widget -> BankInteraction.useItem(widget, "Withdraw-All"));
                    } else {
                        Optional<Widget> mithOre = Bank.search().withName("Mithril ore").first();
                        mithOre.ifPresent(widget -> BankInteraction.useItem(widget, "Withdraw-All"));
                    }
                    setTimeout();
                    break;

                case ADAMANTITE_BAR:

                    if (state.getFurnace().isCoalRunNext(3)) {
                        coalOre = Bank.search().withName("Coal").first();
                        coalOre.ifPresent(widget -> BankInteraction.useItem(widget, "Withdraw-All"));
                    } else {
                        Optional<Widget> mithOre = Bank.search().withName("Adamant ore").first();
                        mithOre.ifPresent(widget -> BankInteraction.useItem(widget, "Withdraw-All"));
                    }
                    setTimeout();
                    break;
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
            }

            TileObjects.search().withName("Bank chest").nearestToPlayer().ifPresent(tileObject -> {
                MousePackets.queueClickPacket();
                TileObjectInteraction.interact(tileObject, "Use");
            });
        }
    }


    @Override
    protected void startUp() {
        overlayManager.add(instructionOverlay);
        overlayManager.add(statisticsOverlay);
        overlayManager.add(coalBagOverlay);
        overlayManager.add(itemStepOverlay);
        overlayManager.add(bankItemStepOverlay);
        overlayManager.add(widgetStepOverlay);
        overlayManager.add(objectStepOverlay);
        overlayManager.add(tileStepOverlay);
    }

    @Override
    protected void shutDown() {
        statistics.clear();
        methodHandler.clear();

        overlayManager.remove(instructionOverlay);
        overlayManager.remove(statisticsOverlay);
        overlayManager.remove(coalBagOverlay);
        overlayManager.remove(itemStepOverlay);
        overlayManager.remove(bankItemStepOverlay);
        overlayManager.remove(widgetStepOverlay);
        overlayManager.remove(objectStepOverlay);
        overlayManager.remove(tileStepOverlay);
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        GameObject gameObject = event.getGameObject();

        switch (gameObject.getId()) {
            case BANK_CHEST:
                objectManager.add(gameObject);
                break;
            case CONVEYOR_BELT:
            case BAR_DISPENSER:
                objectManager.add(gameObject);
                isEnabled = true;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (timeout > 0) {
            timeout--;
            return;
        }
        if (!EthanApiPlugin.loggedIn() || !REGION_IDS.contains(client.getLocalPlayer().getWorldLocation().getRegionID())) {
            return;
        }

        checkRunEnergy();
        CheckInventory();

        if (ezState == State.conveying) {
            DoConveying();
        }
        if (ezState == State.resupplying) {
            Restock();
        }
        if (ezState == State.collecting) {
            DoCollecting();
        }
        if (ezState == State.idle) {
            DoTraveling();
        }


    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        GameObject gameObject = event.getGameObject();

        switch (gameObject.getId()) {
            case CONVEYOR_BELT:
            case BAR_DISPENSER:
                if (config.clearMethodOnExit()) methodHandler.clear();
                if (config.clearStatisticsOnExit()) statistics.clear();
                isEnabled = false;
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() != GameState.LOGGED_IN) {
            if (config.clearMethodOnLogout()) methodHandler.clear();
            if (config.clearStatisticsOnLogout()) statistics.clear();
            isEnabled = false;
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (!isEnabled) return;

        if (event.getContainerId() == InventoryID.INVENTORY.getId()) {
            methodHandler.setMethodFromInventory();
            state.update();
        }

        // handle any inventory or bank changes
        methodHandler.next();
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        if (!isEnabled) return;

        statistics.onFurnaceUpdate();
        state.update();

        // handle furnace ore/bar quantity changes
        methodHandler.next();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (Objects.equals(event.getGroup(), "easy-blastfurnace") && Objects.equals(event.getKey(), "potionMode")) {
            clientThread.invokeLater(() -> methodHandler.next());
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (!isEnabled) return;
        if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM) return;

        String message = event.getMessage();
        int maxConveyorCount = state.getCoalBag().getMaxCoal() == 27 ? 2 : 3;
        Matcher emptyMatcher = COAL_EMPTY_MESSAGE.matcher(message);
        Matcher filledMatcher = COAL_FULL_MESSAGE.matcher(message);

        if (emptyMatcher.matches()) {
            state.getCoalBag().empty();
        }

        if (filledMatcher.matches()) {
            int addedCoal = Integer.parseInt(filledMatcher.group(1));
            state.getCoalBag().setCoal(state.getCoalBag().getCoal() + addedCoal);
        }

        if (message.equals("All your ore goes onto the conveyor belt.")) {
            if (state.getInventory().has(ItemID.COAL)) {
                oreOntoConveyorCount++;
            } else {
                oreOntoConveyorCount = 1;
            }
        }

        // After emptying coal bag onto conveyor, ensure coal amount is 0.
        if (maxConveyorCount == oreOntoConveyorCount) {
            oreOntoConveyorCount = 0;
            if (state.getCoalBag().getCoal() > 1) state.getCoalBag().setCoal(0);
        }

        // handle coal bag changes
        methodHandler.next();
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (!isEnabled) return;

        if (event.getMenuOption().equals(Strings.DRINK)) statistics.drinkStamina();

        // Because menu option events can happen multiple times per tick, this is needed to prevent duplicate coal bag empty events.
        final int currentTick = client.getTickCount();
        if (lastCheckTick == currentTick) {
            return;
        }
        lastCheckTick = currentTick;

        if (event.getMenuOption().equals(Strings.EMPTY)) state.getCoalBag().empty();

        // handle coal bag changes
        methodHandler.next();
    }

    @Subscribe
    public void onOverlayMenuClicked(OverlayMenuClicked event) {
        if (event.getOverlay() == instructionOverlay &&
                event.getEntry().getOption().equals(InstructionOverlay.RESET_ACTION)) {
            methodHandler.clear();
        }
        if (event.getOverlay() == statisticsOverlay &&
                event.getEntry().getOption().equals(StatisticsOverlay.CLEAR_ACTION)) {
            statistics.clear();
        }
    }

    @Provides
    EasyBlastFurnaceConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(EasyBlastFurnaceConfig.class);
    }

    private void setTimeout() {
        timeout = RandomUtils.nextInt(config.tickDelayMin(), config.tickDelayMax());
    }

    public void toggle() {
        if (!EthanApiPlugin.loggedIn()) {
            return;
        }
        started = !started;
    }
}
