package com.Ezzuneware.EzConstruction;

import com.Ezzuneware.EzApi.EzApi;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.NPCInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcSpawned;
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

import java.util.Objects;
import java.util.Optional;

@PluginDescriptor(
        name = "<html><font color=\"#00c27e\">[Ez]</font>EzConstruction</html>",
        description = "",
        enabledByDefault = false,
        tags = {"eco", "plugin", "ez"}
)
@Slf4j
public class EzConstructionPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzConstructionConfig config;
    @Inject
    private EzConstructionOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    @Getter
    private boolean started = false;
    @Getter
    private State state = State.idle;
    @Getter
    public int timeout = 0;
    private final int CONWIDGET = 30015491;
    private final int REMOVEWIDGET = 14352385;

    @Provides
    private EzConstructionConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzConstructionConfig.class);
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
    public void onNpcSpawned(NpcSpawned e) {

        if (!EthanApiPlugin.loggedIn() || !started) {
            return;
        }

        if (e.getNpc().getName().contains("utler")) {
            setTimeout();
            return;
        }
    }

    private Optional<TileObject> GetRemoveTileObject() {
        return TileObjects.search().nameContains(config.BuildSpot().wildcardToSearchFor).withAction("Remove").withinDistance(10).first();
    }

    private Optional<Widget> GetRemoveWidget() {
        return Widgets.search().withId(REMOVEWIDGET).first();
    }

    private Optional<Widget> GetConWidget() {
        return Widgets.search().withId(CONWIDGET).first();
    }

    private Optional<NPC> GetButler() {
        Optional<NPC> butler = Optional.empty();
        butler = NPCs.search().withName("Butler").first();
        if (butler.isEmpty())
            butler = NPCs.search().withName("Demon butler").first();
        return butler;
    }

    private void DoButler() {

        Optional<Widget> removeWidget = GetRemoveWidget();
        if (removeWidget.isPresent()) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueResumePause(removeWidget.get().getId(), 1);
            setTimeout();
            return;
        }

        NPC butler = GetButler().get();
        NPCInteraction.interact(butler, "Talk-to");
        setTimeout();
        return;
    }

    private void DoConstruction() {
        Optional<Widget> removeWidget = GetRemoveWidget();
        if (removeWidget.isPresent()) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueResumePause(removeWidget.get().getId(), 1);
            setTimeout();
            return;
        }

        Optional<Widget> conWidget = GetConWidget();
        if (conWidget.isPresent()) {
            Optional<Widget> target = Optional.empty();
            switch (config.BuildSpot()) {
                case LARDER:
                    switch (config.plankType()) {
                        case OAK_PLANK:
                            target = Widgets.search().withId(30015490).first();

                            break;
                    }
                    break;
            }
            Widget targetGot = target.get();
            MousePackets.queueClickPacket();
            WidgetPackets.queueResumePause(target.get().getId(), config.optionValue());
            setTimeout();
            return;
        }

        Optional<TileObject> buildSpot = TileObjects.search().withId(config.BuildSpot().buildSpot).first();
        if (buildSpot.isPresent()) {
            TileObjectInteraction.interact(buildSpot.get(), "Build");
            setTimeout();
            return;
        }
        //oak larder 30015493 could remove text

        Optional<TileObject> alreadyBuild = GetRemoveTileObject();
        if (alreadyBuild.isPresent()) {
            TileObjectInteraction.interact(alreadyBuild.get(), "Remove");
            setTimeout();
            return;
        }

    }


    private State DetermineState() {

        if (Inventory.getItemAmount(config.plankType().plankId) <= config.plankCountForButlering() && GetButler().isPresent()) {
            return State.butlering;
        }

        if (Inventory.getItemAmount(config.plankType().plankId) >= config.planksPerItem()) {
            return State.building;
        }

        if (GetRemoveTileObject().isPresent())
            return State.building;


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
        state = DetermineState();

        if (state == State.building)
            DoConstruction();

        if (state == State.butlering)
            DoButler();

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