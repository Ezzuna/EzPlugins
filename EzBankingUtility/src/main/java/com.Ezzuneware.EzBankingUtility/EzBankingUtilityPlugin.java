package com.Ezzuneware.EzBankingUtility;


import com.example.EthanApiPlugin.Collections.BankInventory;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.BankInventoryInteraction;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@PluginDescriptor(
        name = "EzBankingUtility",
        description = "A generic plugin for banking help. Useful for filling/emptying objects (herb sack/gem bag/etc) in a bank.",
        enabledByDefault = false,
        tags = {"eco", "plugin"}
)
@Slf4j
public class EzBankingUtilityPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzBankingUtilityConfig config;
    @Inject
    private EzBankingUtilityOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    private boolean started = false;
    public int timeout = 0;
    public boolean shouldCheck = false;

    @Provides
    private EzBankingUtilityConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzBankingUtilityConfig.class);
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
    public void onWidgetLoaded(WidgetLoaded e) {
        if (e.getGroupId() == WidgetID.BANK_GROUP_ID) {

            shouldCheck = true;

        }
    }


    @Subscribe
    private void onGameTick(GameTick event) {
        if (timeout > 0) {
            timeout--;
            return;
        }
        if (!EthanApiPlugin.loggedIn()) {
            return;
        }
        if (!shouldCheck)
            return;
        CheckHerbSack();
        CheckFurPouch();
        CheckMeatSack();

        shouldCheck = false;
    }

    private void CheckHerbSack() {
        if (!config.DoHerbSack())
            return;

        Optional<Widget> herbSack = BankInventory.search().matchesWildCardNoCase("*erb sack").first();

        herbSack.ifPresent(erbSack -> {
            BankInventoryInteraction.useItem(erbSack, "Empty");
        });
    }

    private void CheckMeatSack() {
        if (!config.DoMeatSack())
            return;

        Optional<Widget> meatSack = BankInventory.search().matchesWildCardNoCase("*eat pouch*").first();

        meatSack.ifPresent(eatSack -> {
            BankInventoryInteraction.useItem(eatSack, "Empty");
        });
    }

    private void CheckFurPouch() {
        if (!config.DoFurPouch())
            return;

        Optional<Widget> furPouch = BankInventory.search().matchesWildCardNoCase("*fur pouch*").first();

        furPouch.ifPresent(urPouch -> {
            BankInventoryInteraction.useItem(urPouch, "Empty");
        });
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