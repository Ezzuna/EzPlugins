package com.Ezzuneware.EzEdgeSmelter;


import com.Ezzuneware.EzEdgeSmelter.tasks.*;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.API.InventoryUtil;
import com.piggyplugins.PiggyUtils.API.PlayerUtil;
import com.piggyplugins.PiggyUtils.strategy.AbstractTask;
import com.piggyplugins.PiggyUtils.strategy.TaskManager;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

@PluginDescriptor(
        name = "EzEdgeSmelterPlugin",
        description = "",
        enabledByDefault = false,
        tags = {"piggy", "plugin"}
)
@Slf4j
public class EzEdgeSmelterPlugin extends Plugin {
    @Inject
    @Getter
    private Client client;
    @Inject
    public EzEdgeSmelterConfig config;
    @Inject
    private EzEdgeSmelterOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    @Getter
    private ClientThread clientThread;
    public boolean started = false;
    public int timeout = 0;
    public TaskManager taskManager = new TaskManager();
    public boolean isSmithing;
    public int idleTicks = 0;
    @Inject
    PlayerUtil playerUtil;

    @Provides
    private EzEdgeSmelterConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EzEdgeSmelterConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(overlay);
        timeout = 0;
        isSmithing = false;
        keyManager.registerKeyListener(toggle);
        log.info(config.bar().getName() + " - " + config.item().toString());
    }

    @Override
    protected void shutDown() throws Exception {
        isSmithing = false;
        timeout = 0;
        idleTicks = 0;
        started = false;
        keyManager.unregisterKeyListener(toggle);
        overlayManager.remove(overlay);
    }


    @Subscribe
    private void onGameTick(GameTick event) {
        if (client.getGameState() != GameState.LOGGED_IN || !started) {
            return;
        }

        if (playerUtil.isInteracting() || client.getLocalPlayer().getAnimation() == -1) {
            idleTicks++;
        } else {
            idleTicks = 0;
        }

        if (timeout > 0) {
            timeout--;
            if (idleTicks > 10 || (!hasEnoughOres() && config.product() == Product.BAR || (!hasCorrectMould() || !hasEnoughBars() || !hasEnoughGems() ))) {
                timeout = 0;
                isSmithing = false;
            }
            return;
        }


        if (isSmithing) {
            if(!hasEnoughOres() && config.product() == Product.BAR || (!hasCorrectMould() || !hasEnoughBars() || !hasEnoughGems() )){
                isSmithing = false;
            }
            if (hasEnoughOres() && config.product() == Product.BAR  || (hasCorrectMould() || hasEnoughBars() || hasEnoughGems()))
                return;
        }

        checkRunEnergy();
        if (taskManager.hasTasks()) {
            for (AbstractTask t : taskManager.getTasks()) {
                if (t.validate()) {
                    t.execute();
                    return;
                }
            }
        }

    }


    public boolean hasEnoughOres() {
        return (Inventory.getItemAmount(config.bar().getName()) > 0);
    }

    public boolean hasEnoughBars() {
        return (Inventory.getItemAmount(config.gem().getBarCompanion()) > 0);
    }

    public boolean hasEnoughGems() {
        return (Inventory.getItemAmount(config.gem().getName()) > 0);
    }

    public boolean hasAnyProduct() {
        if(config.gem() == Gem.RedTopaz){
            return (Inventory.getItemAmount("Topaz " + config.product().getName()) > 0);
        }
        else
            return (Inventory.getItemAmount(config.gem().getName() + " " + config.product().getName()) > 0);
    }

    public boolean hasCorrectMould() {
        return (Inventory.getItemAmount(config.product().getMouldId()) > 0);
    }

    private void checkRunEnergy() {
        if (runIsOff() && client.getEnergy() >= 30 * 100) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetActionPacket(1, 10485787, -1, -1);
        }
    }

    private boolean runIsOff() {
        return EthanApiPlugin.getClient().getVarpValue(173) == 0;
    }

    private final HotkeyListener toggle = new HotkeyListener(() -> config.toggle()) {
        @Override
        public void hotkeyPressed() {
            toggle();
        }
    };

    public void toggle() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        started = !started;
        if (started) {
            taskManager.addTask(new OpenBank(this, config));
            taskManager.addTask(new Banking(this, config));
            taskManager.addTask(new OpenFurnace(this, config));
            taskManager.addTask(new DoSmithing(this, config));
            taskManager.addTask(new DoCrafting(this, config));
        } else {
            taskManager.clearTasks();
        }
    }
}
//Strategy Abstract tasks written by poly j