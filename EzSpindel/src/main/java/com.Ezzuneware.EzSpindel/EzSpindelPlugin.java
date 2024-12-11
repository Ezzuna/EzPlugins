package com.Ezzuneware.EzSpindel;

import com.Ezzuneware.EzApi.EzApi;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
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
import org.apache.commons.lang3.RandomUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@PluginDescriptor(
        name = "<html><font color=\"#00c27e\">[Ez]</font>EzSpindel</html>",
        description = "WIP. Intended for spindel.",
        enabledByDefault = false,
        tags = {"eco", "plugin", "ez"}
)
@Slf4j
public class EzSpindelPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private EzSpindelConfig config;
    @Inject
    private EzSpindelOverlay overlay;
    @Inject
    private KeyManager keyManager;
    private Instant timer;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    private boolean started = false;
    public int timeout = 0;
    private boolean spider_alternator = false;


    private int SPIDER = 0;
    private int SPINDEL = 0;
    private int WEB_ATTACK = 0;
    private int RANGED_ATTACK = 0;
    private int MAGE_ATTACK = 0;

    @Provides
    private EzSpindelConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzSpindelConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        keyManager.registerKeyListener(toggle);
        overlayManager.add(overlay);
        timeout = 0;
        timer = Instant.now();
    }

    @Override
    protected void shutDown() throws Exception {
        keyManager.unregisterKeyListener(toggle);
        overlayManager.remove(overlay);
        timeout = 0;
        timer = null;
    }

    private List<NPC> GetBabies() {
        return NPCs.search().withId(SPIDER).alive().result();
    }

    private Optional<NPC> GetSpindel() {
        return NPCs.search().withId(SPINDEL).alive().first();
    }

    private void EquipRangedWeapon() {
        Optional<Widget> rangedWeapon = Inventory.search().matchesWildCardNoCase(config.rangedItem()).first();
        if (rangedWeapon.isPresent()) {
            InventoryInteraction.useItem(rangedWeapon.get(), "Equip");
        } else
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "NO RANGED WEAPON.", null);
    }

    private void EquipMeleeWeapon() {
        Optional<Widget> meleeWeapon = Inventory.search().matchesWildCardNoCase(config.meleeItem()).first();
        if (meleeWeapon.isPresent()) {
            InventoryInteraction.useItem(meleeWeapon.get(), "Equip");
        } else
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "NO MELEE WEAPON...LET'S BE HONEST, YOU'VE FUCKED UP THE CONFIG HAVEN'T YOU.", null);
    }

    private void KillBabies() {
        NPC spider;
        spider_alternator = !spider_alternator;

        List<NPC> spiderlings = GetBabies();
        if (spiderlings.size() > 1) {
            spider = spiderlings.get(spider_alternator ? 0 : 1);
        } else if (spiderlings.size() == 1) {
            spider = spiderlings.get(0);
        } else {
            KillSpindel();
        }

    }

    private void KillSpindel() {

    }

    private State DetermineState(){

        if(GetBabies().size() > 0){
            return State.babies;
        }
        if(GetSpindel().isPresent())
            return State.killing;

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

    public String getElapsedTime() {
        Duration duration = Duration.between(timer, Instant.now());
        long durationInMillis = duration.toMillis();
        long second = (durationInMillis / 1000) % 60;
        long minute = (durationInMillis / (1000 * 60)) % 60;
        long hour = (durationInMillis / (1000 * 60 * 60)) % 24;

        return String.format("%02d:%02d:%02d", hour, minute, second);
    }

    public void toggle() {
        if (!EthanApiPlugin.loggedIn()) {
            return;
        }
        started = !started;

        if (started) {
            timer = Instant.now();
        }
    }
}