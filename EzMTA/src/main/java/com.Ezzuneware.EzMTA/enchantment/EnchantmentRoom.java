/*
 * Copyright (c) 2018, Jasper Ketelaar <Jasper0781@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.Ezzuneware.EzMTA.enchantment;

import com.Ezzuneware.EzMTA.graveyard.foodType;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import com.Ezzuneware.EzMTA.MTAConfig;
import com.Ezzuneware.EzMTA.MTARoom;
import org.apache.commons.lang3.RandomUtils;

import javax.inject.Inject;
import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class EnchantmentRoom extends MTARoom {
    private static final int MTA_ENCHANT_REGION = 13462;

    private final Client client;
    private final List<WorldPoint> dragonstones = new ArrayList<>();
    private boolean hintSet;
    private int timeout = 0;
    private enchState state = enchState.idle;
    private Set<String> shapes = Set.of("Cylinder", "Pentamid", "Cube", "Icosahedron", "Dragonstone");

    @Inject
    private EnchantmentRoom(MTAConfig config, Client client) {
        super(config);
        this.client = client;
    }

    private void DoDigging() {
        Optional<TileObject> pile = TileObjects.search().withAction("Take-from").nearestToPlayer();
        if (pile.isPresent()) {
            TileObjectInteraction.interact(pile.get(), "Take-from");
            setTimeout();
        }
    }

    private void DoCasting() {
        int spellId = config.enchantingSpell().widgetId;
        Optional<Widget> spell = Widgets.search().withId(spellId).first();

        Optional<Widget> item = Optional.empty();

        for(String shape : shapes){
            item = Inventory.search().withName(shape).first();
            if(item.isPresent())
                break;
        }

        if(item.isEmpty())
            return;

        if (spell.isPresent()) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetOnWidget(spell.get(), item.get());
            setTimeout();
        }
    }

    private void DoDepositing() {
        Optional<TileObject> hole = TileObjects.search().withId(23698).first();
        if (hole.isPresent()) {
            TileObjectInteraction.interact(hole.get(), "Deposit");
            setTimeout();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOADING) {
            dragonstones.clear();
            if (hintSet) {
                client.clearHintArrow();
                hintSet = false;
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!inside() || !config.enchantment()) {
            return;
        }

        if (timeout > 0) {
            timeout--;
            return;
        }

        WorldPoint nearest = findNearestStone();
        if (nearest != null) {
            client.setHintArrow(nearest);
            hintSet = true;
        } else {
            client.clearHintArrow();
            hintSet = false;
        }

        state = DetermineState();

        switch (state) {
            case digging:
                DoDigging();
                break;
            case depositing:
                DoDepositing();
                break;
            case enchanting:
                DoCasting();
                break;

        }
    }

    private enchState DetermineState() {

        if (!Inventory.full())
            return enchState.digging;
        else {
            int remainingCount = 0;
            for (String shape : shapes) {
                remainingCount += Inventory.getItemAmount(shape);
            }
            if (remainingCount > 0)
                return enchState.enchanting;
            else
                return enchState.depositing;
        }


    }

    private WorldPoint findNearestStone() {
        WorldPoint nearest = null;
        double dist = Double.MAX_VALUE;
        WorldPoint local = client.getLocalPlayer().getWorldLocation();
        for (WorldPoint worldPoint : dragonstones) {
            double currDist = local.distanceTo(worldPoint);
            if (nearest == null || currDist < dist) {
                dist = currDist;
                nearest = worldPoint;
            }
        }
        return nearest;
    }

    @Subscribe
    public void onItemSpawned(ItemSpawned itemSpawned) {
        final TileItem item = itemSpawned.getItem();
        final Tile tile = itemSpawned.getTile();

        if (item.getId() == ItemID.DRAGONSTONE_6903) {
            WorldPoint location = tile.getWorldLocation();
            log.debug("Adding dragonstone at {}", location);
            dragonstones.add(location);
        }
    }

    @Subscribe
    public void onItemDespawned(ItemDespawned itemDespawned) {
        final TileItem item = itemDespawned.getItem();
        final Tile tile = itemDespawned.getTile();

        if (item.getId() == ItemID.DRAGONSTONE_6903) {
            WorldPoint location = tile.getWorldLocation();
            log.debug("Removed dragonstone at {}", location);
            dragonstones.remove(location);
        }
    }

    @Override
    public boolean inside() {
        Player player = client.getLocalPlayer();
        return player != null && player.getWorldLocation().getRegionID() == MTA_ENCHANT_REGION
                && player.getWorldLocation().getPlane() == 0;
    }

    private void setTimeout() {
        timeout = RandomUtils.nextInt(config.tickDelayMin(), config.tickDelayMax());
    }
}
