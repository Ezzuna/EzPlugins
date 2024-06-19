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
package com.Ezzuneware.EzMTA.graveyard;

import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import com.Ezzuneware.EzMTA.MTAConfig;
import com.Ezzuneware.EzMTA.EzMTAPlugin;
import com.Ezzuneware.EzMTA.MTARoom;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import org.apache.commons.lang3.RandomUtils;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.Optional;

import static net.runelite.api.ItemID.*;

public class GraveyardRoom extends MTARoom {
    private static final int MTA_GRAVEYARD_REGION = 13462;

    static final int MIN_SCORE = 16;

    private final Client client;
    private final EzMTAPlugin plugin;
    private final ItemManager itemManager;
    private final InfoBoxManager infoBoxManager;
    private int score;
    private int foodCount;
    @Getter
    private int natureCount;
    @Getter
    private int waterCount;
    @Getter
    private int earthCount;
    @Getter
    public int BoostedHitpoints;
    private int timeout;

    private final int BONES_TO_BANANAS = 14286865;
    private final int BONES_TO_PEACHES = 14286898;
    private final WorldPoint BONE_SPOT = new WorldPoint(3352, 9637, 1);
    private final WorldPoint DEPOSIT_SPOT = new WorldPoint(3354, 9639, 1);
    private gyState state = gyState.idle;
    @Getter
    private foodType foodtype;

    private GraveyardCounter counter;

    @Inject
    private GraveyardRoom(MTAConfig config, Client client, EzMTAPlugin plugin,
                          ItemManager itemManager, InfoBoxManager infoBoxManager) {
        super(config);
        this.client = client;
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.infoBoxManager = infoBoxManager;
    }

    public gyState DeterminState() {

        if (BoostedHitpoints <= config.graveyardEatAt() && foodCount > 0) {
            return gyState.eating;
        }

        if (foodCount > 0) {
            return gyState.depositing;
        }

        if (score > MIN_SCORE) {
            return gyState.casting;

        }

        if (!Inventory.full())
            return gyState.boning;
        else
            return gyState.exit;


    }

    @Override
    public boolean inside() {
        Player player = client.getLocalPlayer();
        return player != null && player.getWorldLocation().getRegionID() == MTA_GRAVEYARD_REGION
                && player.getWorldLocation().getPlane() == 1;
    }

    private void CheckInventory() {
        foodCount = Inventory.getItemAmount(config.graveyardFood().toString());

        Inventory.search().withName("Nature rune").first().
                ifPresentOrElse((rune) -> {
                            natureCount = rune.getItemQuantity();
                        },
                        () -> natureCount = 0);
        Inventory.search().withName("Water rune").first().
                ifPresentOrElse((rune) -> {
                            waterCount = rune.getItemQuantity();
                        },
                        () -> waterCount = 0);
        Inventory.search().withName("Earth rune").first().
                ifPresentOrElse((rune) -> {
                            earthCount = rune.getItemQuantity();
                        },
                        () -> earthCount = 0);

    }

    private void DoBoning() {
        Optional<TileObject> boneSpot = TileObjects.search().atLocation(BONE_SPOT).first();
        if (boneSpot.isPresent()) {
            TileObjectInteraction.interact(boneSpot.get(), "Grab");
        }
    }

    private void DoDepositing() {
        Optional<TileObject> depositSpot = TileObjects.search().atLocation(DEPOSIT_SPOT).first();
        if (depositSpot.isPresent()) {
            TileObjectInteraction.interact(depositSpot.get(), "Deposit");
            setTimeout();
        }
    }

    private void DoCasting() {
        int spellId = config.graveyardFood() == foodType.Banana ? BONES_TO_BANANAS : BONES_TO_PEACHES;
        Optional<Widget> spell = Widgets.search().withId(spellId).first();
        if (spell.isPresent()) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(spell.get(), "Cast");
        }
    }

    private void DoEating(){
        InventoryInteraction.useItem(config.graveyardFood().toString(), "Eat");
        setTimeout();
    }

    private void DoExit(){
        TileObject escape = TileObjects.search().withAction("Enter").first().get();
        TileObjectInteraction.interact(escape, "Enter");
    }


    @Subscribe
    public void onGameTick(GameTick tick) {
        if (!inside() || !config.graveyard()) {
            if (this.counter != null) {
//				infoBoxManager.removeIf(e -> e instanceof graveyard.GraveyardCounter);
                this.counter = null;
                return;
            }
            return;
        }
        if (timeout > 0) {
            timeout--;
            return;
        }
        CheckInventory();
        BoostedHitpoints = client.getBoostedSkillLevel(Skill.HITPOINTS);

        state = DeterminState();

        switch (state) {
            case boning:
                DoBoning();
                break;
            case depositing:
                DoDepositing();
                break;
            case casting:
                DoCasting();
                break;
            case eating:
                DoEating();
                break;
            case exit:
                DoExit();
                break;
        }

        return;

    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (!inside()) {
            return;
        }

        ItemContainer container = event.getItemContainer();

        if (container == client.getItemContainer(InventoryID.INVENTORY)) {
            this.score = score(container.getItems());

            if (counter == null) {
                BufferedImage image = itemManager.getImage(ANIMALS_BONES);
                counter = new GraveyardCounter(image, plugin);
                infoBoxManager.addInfoBox(counter);
            }
            counter.setCount(score);
        }
    }

    private int score(Item[] items) {
        int score = 0;

        if (items == null) {
            return score;
        }

        for (Item item : items) {
            score += getPoints(item.getId());
        }

        return score;
    }

    private int getPoints(int id) {
        switch (id) {
            case ANIMALS_BONES:
                return 1;
            case ANIMALS_BONES_6905:
                return 2;
            case ANIMALS_BONES_6906:
                return 3;
            case ANIMALS_BONES_6907:
                return 4;
            default:
                return 0;
        }
    }

    private void setTimeout() {
        timeout = RandomUtils.nextInt(config.tickDelayMin(), config.tickDelayMax());
    }
}