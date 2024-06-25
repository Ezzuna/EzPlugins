package com.Ezzuneware.EzShopper;


import com.example.EthanApiPlugin.Collections.*;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.NPCInteraction;
import com.example.InteractionApi.ShopInteraction;
import com.example.InteractionApi.ShopInventoryInteraction;
import com.example.PacketUtils.WidgetID;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;
import org.apache.commons.lang3.RandomUtils;

import java.util.*;

@PluginDescriptor(
        name = "EzShopper",
        description = "Provide NPC name, shop/invent item, quantity to purchase/sell till. Will hop and go until stack is done",
        enabledByDefault = false,
        tags = {"eco", "plugin"}
)
@Slf4j
public class EzShopperPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzShopperConfig config;
    @Inject
    private EzShopperOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private WorldService worldService;
    @Inject
    private ClientThread clientThread;
    @Inject
    private ChatMessageManager chatMessageManager;
    @Getter
    private boolean started = false;
    private net.runelite.api.World quickHopTargetWorld;
    public int timeout = 0;
    private int displaySwitcherAttempts = 0;
    private boolean shouldHop = false;


    @Getter
    private State state = State.idle;

    @Provides
    private EzShopperConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzShopperConfig.class);
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

    private World findWorld(List<World> worlds, EnumSet<net.runelite.http.api.worlds.WorldType> currentWorldTypes, int totalLevel) {
        World world = worlds.get(new Random().nextInt(worlds.size()));

        EnumSet<net.runelite.http.api.worlds.WorldType> types = world.getTypes().clone();

        types.remove(net.runelite.http.api.worlds.WorldType.LAST_MAN_STANDING);

        if (types.contains(net.runelite.http.api.worlds.WorldType.SKILL_TOTAL)) {
            try {
                int totalRequirement = Integer.parseInt(world.getActivity().substring(0, world.getActivity().indexOf(" ")));

                if (totalLevel >= totalRequirement) {
                    types.remove(WorldType.SKILL_TOTAL);
                }
            } catch (NumberFormatException ex) {
                log.warn("Failed to parse total level requirement for target world", ex);
            }
        }

        if (currentWorldTypes.equals(types)) {
            int worldLocation = world.getLocation();

            return world;
        }

        return null;
    }


    private void hop() {
        clientThread.invoke(() -> {
            WorldResult worldResult = worldService.getWorlds();
            if (worldResult == null) {
                return;
            }

            net.runelite.http.api.worlds.World currentWorld = worldResult.findWorld(client.getWorld());

            if (currentWorld == null) {
                return;
            }

            EnumSet<net.runelite.http.api.worlds.WorldType> currentWorldTypes = currentWorld.getTypes().clone();

            currentWorldTypes.remove(net.runelite.http.api.worlds.WorldType.PVP);
            currentWorldTypes.remove(net.runelite.http.api.worlds.WorldType.HIGH_RISK);
            currentWorldTypes.remove(net.runelite.http.api.worlds.WorldType.BOUNTY);
            currentWorldTypes.remove(net.runelite.http.api.worlds.WorldType.SKILL_TOTAL);
            currentWorldTypes.remove(net.runelite.http.api.worlds.WorldType.LAST_MAN_STANDING);
            currentWorldTypes.remove(net.runelite.http.api.worlds.WorldType.QUEST_SPEEDRUNNING);
            currentWorldTypes.remove(net.runelite.http.api.worlds.WorldType.FRESH_START_WORLD);
            currentWorldTypes.remove(net.runelite.http.api.worlds.WorldType.DEADMAN);
            currentWorldTypes.remove(net.runelite.http.api.worlds.WorldType.BETA_WORLD);
            currentWorldTypes.remove(net.runelite.http.api.worlds.WorldType.NOSAVE_MODE);
            currentWorldTypes.remove(net.runelite.http.api.worlds.WorldType.TOURNAMENT);
            currentWorldTypes.remove(net.runelite.http.api.worlds.WorldType.SEASONAL);
            currentWorldTypes.remove(net.runelite.http.api.worlds.WorldType.PVP_ARENA);

            List<net.runelite.http.api.worlds.World> worlds = worldResult.getWorlds();


            int totalLevel = client.getTotalLevel();

            World world;
            do {
                world = findWorld(worlds, currentWorldTypes, totalLevel);
            }
            while (world == null || world == currentWorld);

            hop(world.getId());
        });
    }

    private void hop(int worldId) {
        WorldResult worldResult = worldService.getWorlds();
        // Don't try to hop if the world doesn't exist
        World world = worldResult.findWorld(worldId);
        if (world == null) {
            return;
        }

        final net.runelite.api.World rsWorld = client.createWorld();
        rsWorld.setActivity(world.getActivity());
        rsWorld.setAddress(world.getAddress());
        rsWorld.setId(world.getId());
        rsWorld.setPlayerCount(world.getPlayers());
        rsWorld.setLocation(world.getLocation());
        rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

        if (client.getGameState() == GameState.LOGIN_SCREEN) {
            client.changeWorld(rsWorld);
            return;
        }

        String chatMessage = new ChatMessageBuilder()
                .append(ChatColorType.NORMAL)
                .append("Hopping away from a player. New world: ")
                .append(ChatColorType.HIGHLIGHT)
                .append(Integer.toString(world.getId()))
                .append(ChatColorType.NORMAL)
                .append("..")
                .build();

        chatMessageManager
                .queue(QueuedMessage.builder()
                        .type(ChatMessageType.CONSOLE)
                        .runeLiteFormattedMessage(chatMessage)
                        .build());

        quickHopTargetWorld = rsWorld;
        displaySwitcherAttempts = 0;
    }

    private void resetQuickHopper() {
        displaySwitcherAttempts = 0;
        quickHopTargetWorld = null;
    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private void findNPC() {
        Optional<NPC> tarNpc = NPCs.search().withName(config.NPCName()).nearestToPlayer();

        if (tarNpc.isEmpty())
            return;

        NPCInteraction.interact(tarNpc.get(), "Trade");
        state = State.travelling;
        setTimeout();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        GameState estate = event.getGameState();
        if (estate == GameState.HOPPING) {
            state = State.hopping;
        } else if (estate == GameState.LOGGED_IN) {
            state = State.idle;
        }

    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded e) {
        if (e.getGroupId() == WidgetID.SHOP_INVENTORY_GROUP_ID) {
            state = State.shopping;
            setTimeout();
        }
    }

    private void handleUsing() {
        if (Widgets.search().withId(WidgetID.SHOP_INVENTORY_GROUP_ID).first().isPresent()) {
            client.runScript(29);
            setTimeout();
            return;
        }
        Optional<Widget> item1 = Inventory.search().matchesWildCardNoCase(config.itemToExchange()).first();
        Optional<Widget> item2 = Inventory.search().matchesWildCardNoCase(config.useOnItemName()).first();
        if (item1.isPresent() && item2.isPresent()) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetOnWidget(item1.get(), item2.get());
            setTimeout();
            return;
        } else {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "", null);
            state = State.idle;
        }

    }

    private void handleShopping() {


        if (config.buySell() == buysell.buying) {

            if (Inventory.full()) {
                if (config.useOnItem()) {
                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Invent full, using item on -> " + config.useOnItemName(), null);
                    state = State.using;
                    return;
                }
            }
            int localItems = Inventory.getItemAmount(config.itemToExchange());

            List<Widget> itemsd = Shop.search().matchesWildCardNoCase(config.itemToExchange()).result();//
            //List<Widget> itemsd = Shop.search().withId(200).result();

            //List<Widget> itemsd2 = Shop.search();

            if (itemsd.size() == 0) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "No shite.", null);
                client.runScript(29);
                shouldHop = true;
            }


            var stream = itemsd.stream().filter(i -> i.getItemQuantity() > config.quantityToTradeTill());
            if (stream.findAny().isEmpty()) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "No shite.", null);
                client.runScript(29);
                shouldHop = true;
                return;
            }
            Widget itemToEvaluate = itemsd.stream().filter(i -> i.getItemQuantity() > config.quantityToTradeTill())
                    .findFirst()
                    .get();


            int quantityStock = itemToEvaluate.getItemQuantity();

            if (quantityStock > config.quantityToTradeTill()) {
                int purchaseQuant = quantityStock - config.quantityToTradeTill();
                if (purchaseQuant >= 50) {
                    if (config.spamMode()) {
                        for (int i = 0; i < (RandomUtils.nextInt(3, 6)); i++) {
                            ShopInteraction.buyFifty(itemToEvaluate);
                        }
                        return;
                    }
                    ShopInteraction.buyFifty(itemToEvaluate);
                    return;
                }
                if (purchaseQuant >= 10) {
                    ShopInteraction.buyTen(itemToEvaluate);
                    return;
                }
                if (purchaseQuant >= 5) {
                    ShopInteraction.buyFive(itemToEvaluate);
                    return;
                }
                if (purchaseQuant >= 1) {
                    ShopInteraction.buyOne(itemToEvaluate);
                    return;
                }
            } else {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Out of shite.", null);
                client.runScript(29);
                shouldHop = true;
            }


        } else if (config.buySell() == buysell.selling) {
            List<Widget> itemsd = Shop.search().matchesWildCardNoCase(config.itemToExchange()).result();

            var stream = itemsd.stream().filter(i -> i.getItemQuantity() > config.quantityToTradeTill());
            if (stream.findAny().isPresent()) {
                client.runScript(29);
                shouldHop = true;
                return;
            }

            Optional<Widget> itemToEvaluate = ShopInventory.search().withName(config.itemToExchange()).first();
            Optional<Widget> itemToEvaluateTest = ShopInventory.search().first();


//            if(itemToEvaluate.isEmpty()){
//                started = false;
//                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "OUTTA SHITE",null);
//                return;
//            }
            final int[] quantityStock = {0};

            itemsd.stream().findFirst().ifPresent(d -> quantityStock[0] = d.getItemQuantity());


            if (quantityStock[0] < config.quantityToTradeTill()) {
                int sellQuant = config.quantityToTradeTill() - quantityStock[0];
                if (sellQuant >= 50) {
                    if (config.spamMode()) {
                        for (int i = 0; i < (RandomUtils.nextInt(3, 6)); i++) {
                            ShopInventoryInteraction.useItem(itemToEvaluate.get().getName(), "Sell 50");
                        }
                        return;
                    }
                    ShopInventoryInteraction.useItem(itemToEvaluate.get().getName(), "Sell 50");
                    return;
                }
                if (sellQuant >= 10) {
                    ShopInventoryInteraction.useItem(itemToEvaluate.get().getName(), "Sell 10");
                    return;
                }
                if (sellQuant >= 5) {
                    ShopInventoryInteraction.useItem(itemToEvaluate.get().getName(), "Sell 5");
                    return;
                }
                for (int i = 0; i < sellQuant; i++) {
                    ShopInventoryInteraction.useItem(itemToEvaluate.get().getName(), "Sell 1");
                }

            } else {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Out of shite.", null);
                client.runScript(29);
                shouldHop = true;
            }


        }
    }

    @Subscribe
    private void onGameTick(GameTick event) {
        if (timeout > 0) {
            timeout--;
            return;
        }
        if (!EthanApiPlugin.loggedIn() || !started || state == State.hopping) {
            return;
        }

        if (shouldHop) {
            shouldHop = false;
            hop();
            state = State.prehop;
            return;
        }

        if (state == State.using) {
            handleUsing();
            return;
        }

        if (state == State.shopping) {
            handleShopping();
            return;
        }

        if (state == State.idle) {
            findNPC();
            return;
        }

        if (quickHopTargetWorld == null)
            return;

        if (client.getWidget(WidgetInfo.WORLD_SWITCHER_LIST) == null) {
            client.openWorldHopper();


        } else {
            client.hopToWorld(quickHopTargetWorld);
            resetQuickHopper();
        }
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

    public void toggle() {
        if (!EthanApiPlugin.loggedIn()) {
            return;
        }
        started = !started;
    }
}