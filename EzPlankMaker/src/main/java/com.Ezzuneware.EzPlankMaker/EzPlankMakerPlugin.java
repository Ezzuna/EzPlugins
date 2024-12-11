package com.Ezzuneware.EzPlankMaker;

import com.Ezzuneware.EzApi.EzApi;
import com.example.EthanApiPlugin.Collections.*;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.BankInteraction;
import com.example.InteractionApi.BankInventoryInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.PacketUtils.WidgetID;
import com.example.PacketUtils.WidgetInfoExtended;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.Getter;
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
import net.runelite.client.util.Text;
import org.apache.commons.lang3.RandomUtils;

import javax.swing.text.html.Option;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@PluginDescriptor(
        name = "<html><font color=\"#00c27e\">[Ez]</font>EzPlankMaker</html>",
        description = "",
        enabledByDefault = false,
        tags = {"eco", "plugin", "ez"}
)
@Slf4j
public class EzPlankMakerPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzPlankMakerConfig config;
    @Inject
    private EzPlankMakerOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    @Getter
    private State state = State.idle;
    @Getter
    private boolean started = false;
    @Getter
    public int timeout = 0;
    @Getter
    private int regionId = 0;
    private final int CAMMY_CHUNK = 11062;
    private final int HOUSE_CHUNK = 32823;
    private final int HOUSE_OPTION_WIDGET = 7602207;
    //private final int HOUSE_OPTION_WIDGET = 7602184;
    private final int CALL_SERVANT_WIDGET = 24248342;
    //private final int CALL_SERVANT_WIDGET = 10551370;
    private final int CHAT_DIALOGUE_SEND = -1;
    private final int CHAT_DIALOGUE_CONFIRM = -1;
    public static final List<Integer> HOME_REGIONS = Arrays.asList(7513, 7514, 7769, 7770, 8025, 8026,
            13986, 13987, 13988, 14242, 14243, 14244, 14498, 14499, 14500);

    @Provides
    private EzPlankMakerConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzPlankMakerConfig.class);
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

    private boolean HaveEnoughCoins() {
        Optional<Widget> coins = Inventory.search().withName("Coins").first();
        if(coins.isEmpty())
            return false;

        return coins.get().getItemQuantity() > 27000;
    }

    private void Teleport(boolean house) {
        Optional<Widget> spell = Widgets.search().withId(house ? WidgetInfoExtended.SPELL_TELEPORT_TO_HOUSE.getId() : WidgetInfoExtended.SPELL_CAMELOT_TELEPORT.getId()).first();

        if (spell.isEmpty()) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "COULD NOT FIND TELEPORT. RUNES? BUG? STOPPING PLUGIN", null);
            started = false;
            return;
        }
        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetAction(spell.get(), "Cast");
        setTimeout();
    }

    private void Restock() {
        if (Widgets.search().withId(WidgetID.SHOP_INVENTORY_GROUP_ID).first().isPresent()) {
            client.runScript(29);
            setTimeout();
            return;
        }

        if (Inventory.getItemAmount(config.plankType().logId) > 0) {
            //we have logs
            if (Bank.isOpen()) {
                client.runScript(29);
                setTimeout();
                return;
            }
            Teleport(true);
            return;
        }

        if (Bank.isOpen()) {
            List<Widget> bankInv = BankInventory.search().result();
            for (Widget item : bankInv) {
                if (Text.removeTags(item.getName()).contains("plank")) {
                    MousePackets.queueClickPacket();
                    BankInventoryInteraction.useItem(item, "Deposit-All");
                    return;
                }

            }
            BankInteraction.withdrawX(Bank.search().withId(6333).first().get(), 26);
            setTimeout();
        } else {
//            Optional<TileObject> bankBooth = TileObjects.search().filter(tileObject -> {
//                ObjectComposition objectComposition = TileObjectQuery.getObjectComposition(tileObject);
//                return getName().toLowerCase().contains("bank") ||
//                        Arrays.stream(objectComposition.getActions()).anyMatch(action -> action != null && action.toLowerCase().contains("bank"));
//            }).nearestToPlayer();
//
//            if (bankBooth.isPresent()) {
//
//                MousePackets.queueClickPacket();
//                TileObjectInteraction.interact(bankBooth.get(), "Bank");
//            }

            TileObjects.search().withName("Bank chest").withinDistance(6).nearestToPlayer().ifPresent(tileObject -> {
                MousePackets.queueClickPacket();
                TileObjectInteraction.interact(tileObject, "Use");
            });

            setTimeout();
        }
    }

    protected boolean inPOH() {

        boolean status = Arrays.stream(client.getMapRegions()).anyMatch(HOME_REGIONS::contains);
        String logMessage = "";
        for (int r : client.getMapRegions()) {
            logMessage = logMessage + r + ",";
        }
        log.info(logMessage);

        return status;
    }

    private void DoHouse() {


        if (Inventory.getItemAmount(config.plankType().logId) == 0) {
            //out of logs
            Teleport(false);
            setTimeout();
            return;
        }

        Optional<Widget> chatOption = Widgets.search().withId(CHAT_DIALOGUE_SEND).first();
        if (chatOption.isEmpty())
            chatOption = Widgets.search().withId(CHAT_DIALOGUE_CONFIRM).first();

        if (chatOption.isPresent()) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueResumePause(chatOption.get().getId(), 0);
            setTimeout();
            return;
        }

        Optional<Widget> callServantOption = Widgets.search().withId(CALL_SERVANT_WIDGET).first();

        if (callServantOption.isPresent()) {
            var houseOptions3 = callServantOption.get();
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(callServantOption.get(), "Call Servant");
            setTimeout();
            return;
        }
        Optional<Widget> houseOptions = Widgets.search().withId(HOUSE_OPTION_WIDGET).first();
        if (houseOptions.isPresent()) {
            var houseOptions2 = houseOptions.get();
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(houseOptions.get(), "House Options");
            setTimeout();
            return;
        }

    }


    private State DetermineState() {

        if (!HaveEnoughCoins())
            return State.idle;

        if (regionId == CAMMY_CHUNK) {
            return State.resupply;
        }

        if (inPOH()) {
            return State.house;
        }


        return State.idle;
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
        regionId = client.getLocalPlayer().getWorldLocation().getRegionID();
        state = DetermineState();

        if (state == State.resupply)
            Restock();

        if (state == State.house)
            DoHouse();

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