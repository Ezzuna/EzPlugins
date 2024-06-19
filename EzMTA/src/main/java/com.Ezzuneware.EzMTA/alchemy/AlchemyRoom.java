/*
 * Copyright (c) 2018, Jasper Ketelaar <Jasper0781@gmail.com>
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
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
package com.Ezzuneware.EzMTA.alchemy;

import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import com.Ezzuneware.EzMTA.MTAConfig;
import com.Ezzuneware.EzMTA.EzMTAPlugin;
import com.Ezzuneware.EzMTA.MTARoom;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import org.apache.commons.lang3.RandomUtils;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static net.runelite.api.ObjectID.*;

@Slf4j
public class AlchemyRoom extends MTARoom {
    private static final int MTA_ALCH_REGION = 13462;

    private static final int IMAGE_Z_OFFSET = 150;
    private static final int NUM_CUPBOARDS = 6;
    private static final int INFO_ITEM_START = 7;
    private static final int INFO_POINT_START = 12;
    private static final int INFO_LENGTH = 5;
    private static final int BEST_POINTS = 30;

    private static final String YOU_FOUND = "You found:";
    private static final String EMPTY = "The cupboard is empty.";

    private final Cupboard[] cupboards = new com.Ezzuneware.EzMTA.alchemy.Cupboard[NUM_CUPBOARDS];

    private final EzMTAPlugin plugin;
    private final Client client;
    private final ItemManager itemManager;
    private final InfoBoxManager infoBoxManager;

    private AlchemyItem best;
    private Cupboard suggestion;
    private boolean hintSet;
    private TileObject bestPillar;

    private Integer timeout = 0;
    private alchState state = alchState.idle;

    @Inject
    private AlchemyRoom(Client client, MTAConfig config, EzMTAPlugin plugin, ItemManager itemManager, InfoBoxManager infoBoxManager) {
        super(config);
        this.client = client;
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.infoBoxManager = infoBoxManager;
    }

    private alchState DetermineState() {

        if (Inventory.getItemAmount(best.getId()) > 0)
            return alchState.alching;
        else if (Inventory.full()) {
            return alchState.fullalch;
        } else
            return alchState.stealing;

    }

    private void DoStealing() {
        if (hintSet) {
            if (bestPillar == null) {
                TileObjectInteraction.interact(TileObjects.search().withName("Cupboard").nearestToPlayer().get(), "Search");
                setTimeout();
            } else {
                TileObjectInteraction.interact(bestPillar, "Take-5");
                setTimeout();
            }

        } else {
            TileObjectInteraction.interact(TileObjects.search().withName("Cupboard").nearestToPlayer().get(), "Search");
        }

    }

    private void DoAlchemyOnAnything() {
        Optional<Widget> spell = Widgets.search().withId(14286892).first();

        Optional<Widget> item = Optional.empty();

        for (AlchemyItem aItem : AlchemyItem.values()) {
            item = Inventory.search().withId(aItem.getId()).first();
            if(item.isPresent())
                break;
        }
        if (item.isEmpty())
            return;

        if (spell.isPresent()) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetOnWidget(spell.get(), item.get());
            setTimeout();
        }
    }

    private void DoAlchemy() {
        Optional<Widget> spell = Widgets.search().withId(14286892).first();

        Optional<Widget> item = Inventory.search().withId(best.getId()).first();

//        for (String shape : shapes) {
//            item = Inventory.search().withName(shape).first();
//            if (item.isPresent())
//                break;
//        }

        if (item.isEmpty())
            return;

        if (spell.isPresent()) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetOnWidget(spell.get(), item.get());
            setTimeout();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!inside() || !config.alchemy()) {
            return;
        }

        if (timeout > 0) {
            timeout--;
            return;
        }

        AlchemyItem bestItem = getBest();
        if (best == null || best != bestItem) {
            if (best != null) {
                infoBoxManager.removeIf(e -> e instanceof com.Ezzuneware.EzMTA.alchemy.AlchemyRoomTimer);
                infoBoxManager.addInfoBox(new AlchemyRoomTimer(plugin));
            }

            log.debug("Item change to {}!", best);

            best = bestItem;
            // Reset items to unknown
            Arrays.stream(cupboards)
                    .filter(Objects::nonNull)
                    .forEach(e -> e.alchemyItem = AlchemyItem.UNKNOWN);

            client.clearHintArrow();
            bestPillar = null;
        }

        suggestion = getSuggestion();

        state = DetermineState();

        switch (state) {
            case stealing:
                DoStealing();
                break;
            case alching:
                DoAlchemy();
                break;
            case fullalch:
                DoAlchemyOnAnything();
                break;
        }
    }


    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        if (!inside()) {
            return;
        }

        GameObject spawn = event.getGameObject();
        int cupboardId;

        switch (spawn.getId()) {
            // Closed and opened versions of each
            case CUPBOARD_23678:
            case CUPBOARD_23679:
                cupboardId = 0;
                break;

            case CUPBOARD_23680:
            case CUPBOARD_23681:
                cupboardId = 1;
                break;

            case CUPBOARD_23682:
            case CUPBOARD_23683:
                cupboardId = 2;
                break;

            case CUPBOARD_23684:
            case CUPBOARD_23685:
                cupboardId = 3;
                break;

            case CUPBOARD_23686:
            case CUPBOARD_23687:
                cupboardId = 4;
                break;

            case CUPBOARD_23688:
            case CUPBOARD_23689:
                cupboardId = 5;
                break;

            default:
                return;

        }
        Cupboard cupboard = cupboards[cupboardId];
        if (cupboard != null) {
            cupboard.gameObject = spawn;
        } else {
            cupboard = new Cupboard();
            cupboard.gameObject = spawn;
            cupboard.alchemyItem = AlchemyItem.UNKNOWN;
            cupboards[cupboardId] = cupboard;
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            if (!inside()) {
                reset();
                if (hintSet) {
                    client.clearHintArrow();
                    hintSet = false;
                    bestPillar = null;
                }
            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage wrapper) {
        if (!inside() || !config.alchemy()) {
            return;
        }

        String message = wrapper.getMessage();

        if (wrapper.getType() == ChatMessageType.GAMEMESSAGE) {
            if (message.contains(YOU_FOUND)) {
                String item = message.replace(YOU_FOUND, "").trim();
                AlchemyItem alchemyItem = AlchemyItem.find(item);
                Cupboard clicked = getClicked();
                if (clicked.alchemyItem != alchemyItem) {
                    fill(clicked, alchemyItem);
                }
            } else if (message.equals(EMPTY)) {
                Cupboard clicked = getClicked();
                if (clicked.alchemyItem != AlchemyItem.EMPTY) {
                    fill(clicked, AlchemyItem.EMPTY);
                }
            }
        }
    }


    private void reset() {
        Arrays.fill(cupboards, null);
    }

    @Override
    public boolean inside() {
        Player player = client.getLocalPlayer();
        return player != null && player.getWorldLocation().getRegionID() == MTA_ALCH_REGION
                && player.getWorldLocation().getPlane() == 2;
    }

    private AlchemyItem getBest() {
        for (int i = 0; i < INFO_LENGTH; i++) {
            Widget textWidget = client.getWidget(InterfaceID.MTA_ALCHEMY, INFO_ITEM_START + i);
            if (textWidget == null) {
                return null;
            }

            String item = textWidget.getText();
            Widget pointsWidget = client.getWidget(InterfaceID.MTA_ALCHEMY, INFO_POINT_START + i);
            int points = Integer.parseInt(pointsWidget.getText());

            if (points == BEST_POINTS) {
                return AlchemyItem.find(item);
            }
        }

        return null;
    }

    private Cupboard getClicked() {
        Cupboard nearest = null;
        double distance = Double.MAX_VALUE;

        WorldPoint mine = client.getLocalPlayer().getWorldLocation();

        for (Cupboard cupboard : cupboards) {
            if (cupboard == null) {
                continue;
            }

            double objectDistance = cupboard.gameObject.getWorldLocation().distanceTo(mine);

            if (nearest == null || objectDistance < distance) {
                nearest = cupboard;
                distance = objectDistance;
            }
        }

        return nearest;
    }

    private void fill(Cupboard cupboard, AlchemyItem alchemyItem) {
        int idx = Arrays.asList(cupboards).indexOf(cupboard);
        assert idx != -1;

        int itemIdx = alchemyItem.ordinal();

        log.debug("Filling cupboard {} with {}", idx, alchemyItem);

        for (int i = 0; i < NUM_CUPBOARDS; ++i) {
            int cupIdx = (idx + i) % NUM_CUPBOARDS;
            int itemIndex = (itemIdx + i) % NUM_CUPBOARDS;
            cupboards[cupIdx].alchemyItem = itemIndex <= 4 ? AlchemyItem.values()[itemIndex] : AlchemyItem.EMPTY;
        }
    }

    @Override
    public void under(Graphics2D graphics) {
        if (!getConfig().alchemy() || best == null || !inside()) {
            return;
        }

        boolean found = false;

        for (Cupboard cupboard : cupboards) {
            if (cupboard == null) {
                continue;
            }

            GameObject object = cupboard.gameObject;
            AlchemyItem alchemyItem = cupboard.alchemyItem;

            if (alchemyItem == AlchemyItem.EMPTY) {
                continue;
            }

            if (alchemyItem == best) {
                client.setHintArrow(object.getWorldLocation());
                bestPillar = object;
                found = true;
                hintSet = true;
            }

            BufferedImage image = itemManager.getImage(alchemyItem.getId());
            Point canvasLoc = Perspective.getCanvasImageLocation(client, object.getLocalLocation(), image, IMAGE_Z_OFFSET);

            if (canvasLoc != null) {
                graphics.drawImage(image, canvasLoc.getX(), canvasLoc.getY(), null);
            }
        }

        if (!found && suggestion != null) {
            client.setHintArrow(suggestion.gameObject.getWorldLocation());
            bestPillar = suggestion.gameObject;
            hintSet = true;
        }

    }

    private Cupboard getSuggestion() {
        // check if a cupboard has the best item in it
        if (best != null) {
            for (Cupboard cupboard : cupboards) {
                if (cupboard != null && cupboard.alchemyItem == best) {
                    return cupboard;
                }
            }
        }

        return null;
    }


    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem) {
        assert inside();
        if (best == null || best.getId() != itemId || !config.alchemy()) {
            return;
        }

        drawItem(graphics, widgetItem, Color.GREEN);
    }

    private void drawItem(Graphics2D graphics, WidgetItem item, Color border) {
        Rectangle bounds = item.getCanvasBounds();
        graphics.setColor(border);
        graphics.draw(bounds);
    }

    private void setTimeout() {
        timeout = RandomUtils.nextInt(config.tickDelayMin(), config.tickDelayMax());
    }

}
