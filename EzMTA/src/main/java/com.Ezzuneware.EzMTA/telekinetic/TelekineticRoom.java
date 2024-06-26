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
package com.Ezzuneware.EzMTA.telekinetic;

import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.InteractionApi.NPCInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.MovementPackets;
import com.example.Packets.NPCPackets;
import com.example.Packets.WidgetPackets;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import com.Ezzuneware.EzMTA.MTAConfig;
import com.Ezzuneware.EzMTA.MTARoom;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomUtils;

import javax.inject.Inject;
import java.awt.*;
import java.util.List;
import java.util.Queue;
import java.util.*;

@Slf4j
public class TelekineticRoom extends MTARoom {
    private static final int MAZE_GUARDIAN_MOVING = NullNpcID.NULL_6778;
    private static final int TELEKINETIC_WALL = NullObjectID.NULL_10755;
    private static final int TELEKINETIC_FINISH = NullObjectID.NULL_23672;

    private final Client client;

    private final List<WallObject> telekineticWalls = new ArrayList<>();

    private Stack<Direction> moves = new Stack<>();
    private LocalPoint destination;
    private WorldPoint location;
    private WorldPoint finishLocation;
    private Rectangle bounds;
    private NPC guardian;
    private int numMazeWalls;
    @Getter
    public int timeout = 0;
    public int idleTicks = 0;

    private WorldPoint optimalStorage;

    private NPC toShootNPC;

    @Inject
    private TelekineticRoom(MTAConfig config, Client client) {
        super(config);
        this.client = client;
    }

    public void resetRoom() {
        finishLocation = null;
        telekineticWalls.clear();
    }


    @Subscribe
    public void onWallObjectSpawned(WallObjectSpawned event) {
        final WallObject wall = event.getWallObject();
        if (wall.getId() != TELEKINETIC_WALL) {
            return;
        }

        telekineticWalls.add(wall);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOADING) {
            // Game objects are nulled when loading new scenes, thus never trigger their respective
            // ObjectDespawned events.
            resetRoom();
        }
    }

    @Subscribe
    public void onGroundObjectSpawned(GroundObjectSpawned event) {
        final GroundObject object = event.getGroundObject();
        if (object.getId() == TELEKINETIC_FINISH) {
            finishLocation = object.getWorldLocation();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!inside() || !config.telekinetic()) {
            numMazeWalls = 0;
            moves.clear();
            timeout = 0;
            return;
        }

        if (timeout > 0) {
            timeout--;
            return;
        }

        Optional<NPC> completedGuardian = NPCs.search().withId(6779).first();
        if (completedGuardian.isPresent()) {
            NPCInteraction.interact(completedGuardian.get(), "New-maze");
            idleTicks = 0;
            setTimeout();
            return;
        }

        if (toShootNPC != null && this.moves.size() > 0) {

            try {
                if (client.getLocalPlayer().getWorldLocation().distanceTo(optimal()) == 0) {
                    Widget teleGrab = client.getWidget(14286875);
                    MousePackets.queueClickPacket();
                    NPCPackets.queueWidgetOnNPC(toShootNPC, teleGrab);
                    toShootNPC = null;
                    setTimeout();
                    idleTicks = 0;
                    return;
                }
            } catch (Exception ex) {
                setTimeout();
                return;
            }


        }


        if (telekineticWalls.size() != numMazeWalls) {
            bounds = getBounds(telekineticWalls.toArray(new WallObject[0]));
            numMazeWalls = telekineticWalls.size();
            client.clearHintArrow();
        } else if (guardian != null) {
            WorldPoint current;
            if (guardian.getId() == MAZE_GUARDIAN_MOVING) {
                destination = getGuardianDestination();
                current = WorldPoint.fromLocal(client, destination);
            } else {
                destination = null;
                current = guardian.getWorldLocation();
            }

            //Prevent unnecessary updating when the guardian has not moved
            if (current.equals(location)) {
                if (toShootNPC == null)
                    toShootNPC = guardian;
                return;
            }

            log.debug("Updating guarding location {} -> {}", location, current);

            location = current;


            if (location.equals(finishLocation)) {
                client.clearHintArrow();
            } else {
                log.debug("Rebuilding moves due to guardian move");
                this.moves = build();
            }

            if (toShootNPC != null) {

                try {
                    if (client.getLocalPlayer().getWorldLocation().distanceTo(optimal()) == 0) {
                        Widget teleGrab = client.getWidget(14286875);
                        MousePackets.queueClickPacket();
                        NPCPackets.queueWidgetOnNPC(toShootNPC, teleGrab);
                        toShootNPC = null;
                        setTimeout();
                        idleTicks = 0;
                        return;
                    }
                } catch (Exception ex) {
                    setTimeout();
                    return;
                }


            }

        } else {
            client.clearHintArrow();
            moves.clear();
            idleTicks++;
            if (idleTicks > 10) {
                WorldPoint runPoint = TileObjects.search().withId(10755).nearestToPlayer().get().getWorldLocation();
                MovementPackets.queueMovement(runPoint);
                TileObjectInteraction.interactNearest(10755, "Walk here");
                setTimeout();
            }

            if (idleTicks > 100) {
                Widget bigLogoutButton = client.getWidget(182, 8);
                Widget worldSwitcherLogout = client.getWidget(69, 25);
                Widget logoutButton = bigLogoutButton != null ? bigLogoutButton : worldSwitcherLogout;
                if (logoutButton == null) {
                    System.err.println("Can't find logout button.");
                    return;
                }
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetAction(logoutButton, "Logout");
            }
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event) {
        NPC npc = event.getNpc();

        if (npc.getId() == NpcID.MAZE_GUARDIAN || npc.getId() == MAZE_GUARDIAN_MOVING) {
            guardian = npc;
            if (npc.getId() == NpcID.MAZE_GUARDIAN) {
                toShootNPC = npc;
                setTimeout();
            }

        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        NPC npc = event.getNpc();

        if (npc == guardian) {
            guardian = null;
        }
    }


    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded menuEntryAdded) {
        if (client.getGameState() != GameState.LOGGED_IN)
            return;
        var me = menuEntryAdded.getMenuEntry();
        if (menuEntryAdded.getMenuEntry().getNpc() == null)
            return;
        if (menuEntryAdded.getMenuEntry().getNpc().getId() == NpcID.MAZE_GUARDIAN) {
            me.onClick(i -> {
                toShootNPC = menuEntryAdded.getMenuEntry().getNpc();
            });
        }
    }

    @Override
    public boolean inside() {
        return client.getWidget(InterfaceID.MTA_TELEKINETIC, 0) != null;
    }

    @Override
    public void under(Graphics2D graphics2D) {
        if (inside() && numMazeWalls > 0 && guardian != null) {
            if (destination != null) {
                graphics2D.setColor(Color.ORANGE);
                renderLocalPoint(graphics2D, destination);
            }
            if (!moves.isEmpty()) {
                if (moves.peek() == getPosition()) {
                    graphics2D.setColor(Color.GREEN);
                } else {
                    graphics2D.setColor(Color.RED);
                }

                Polygon tile = Perspective.getCanvasTilePoly(client, guardian.getLocalLocation());
                if (tile != null) {
                    graphics2D.drawPolygon(tile);
                }

                WorldPoint optimal = optimal();

                if (optimal != null) {
                    client.setHintArrow(optimal);
                    renderWorldPoint(graphics2D, optimal);
                    if (optimalStorage == null) {
                        toShootNPC = guardian;
                        MousePackets.queueClickPacket();
                        MovementPackets.queueMovement(optimal);
                        optimalStorage = optimal;
                        idleTicks = 0;
                    }

                    if (optimalStorage.distanceTo(optimal) > 0) {
                        toShootNPC = guardian;
                        MousePackets.queueClickPacket();
                        MovementPackets.queueMovement(optimal);
                        optimalStorage = optimal;
                        idleTicks = 0;
                    }


                }
            }
        }
    }

    private WorldPoint optimal() {
        WorldPoint current = client.getLocalPlayer().getWorldLocation();

        Direction next = moves.pop();
        WorldArea areaNext = getIndicatorLine(next);
        WorldPoint nearestNext = nearest(areaNext, current);

        if (moves.isEmpty()) {
            moves.push(next);

            return nearestNext;
        }

        Direction after = moves.peek();
        moves.push(next);
        WorldArea areaAfter = getIndicatorLine(after);
        WorldPoint nearestAfter = nearest(areaAfter, nearestNext);

        return nearest(areaNext, nearestAfter);
    }

    private static int manhattan(WorldPoint point1, WorldPoint point2) {
        return Math.abs(point1.getX() - point2.getX()) + Math.abs(point2.getY() - point1.getY());
    }

    private WorldPoint nearest(WorldArea area, WorldPoint worldPoint) {
        int dist = Integer.MAX_VALUE;
        WorldPoint nearest = null;

        for (WorldPoint areaPoint : area.toWorldPointList()) {
            int currDist = manhattan(areaPoint, worldPoint);
            if (nearest == null || dist > currDist) {
                nearest = areaPoint;
                dist = currDist;
            }
        }

        return nearest;
    }

    private void renderWorldPoint(Graphics2D graphics, WorldPoint worldPoint) {
        renderLocalPoint(graphics, LocalPoint.fromWorld(client, worldPoint));
    }

    private void renderLocalPoint(Graphics2D graphics, LocalPoint local) {
        if (local != null) {
            Polygon canvasTilePoly = Perspective.getCanvasTilePoly(client, local);
            if (canvasTilePoly != null) {
                graphics.drawPolygon(canvasTilePoly);
            }
        }
    }

    private Stack<Direction> build() {
        if (guardian.getId() == MAZE_GUARDIAN_MOVING) {
            WorldPoint converted = WorldPoint.fromLocal(client, getGuardianDestination());
            return build(converted);
        } else {
            return build(guardian.getWorldLocation());
        }
    }

    private LocalPoint getGuardianDestination() {
        Angle angle = new Angle(guardian.getOrientation());
        Direction facing = angle.getNearestDirection();
        return neighbour(guardian.getLocalLocation(), facing);
    }

    private Stack<Direction> build(WorldPoint start) {
        Queue<WorldPoint> visit = new LinkedList<>();
        Set<WorldPoint> closed = new HashSet<>();
        Map<WorldPoint, Integer> scores = new HashMap<>();
        Map<WorldPoint, WorldPoint> edges = new HashMap<>();
        scores.put(start, 0);
        visit.add(start);

        while (!visit.isEmpty()) {
            WorldPoint next = visit.poll();
            closed.add(next);

            LocalPoint localNext = LocalPoint.fromWorld(client, next);
            LocalPoint[] neighbours = neighbours(localNext);

            for (LocalPoint neighbour : neighbours) {
                if (neighbour == null) {
                    continue;
                }

                WorldPoint nghbWorld = WorldPoint.fromLocal(client, neighbour);

                if (!nghbWorld.equals(next)
                        && !closed.contains(nghbWorld)) {
                    int score = scores.get(next) + 1;

                    if (!scores.containsKey(nghbWorld) || scores.get(nghbWorld) > score) {
                        scores.put(nghbWorld, score);
                        edges.put(nghbWorld, next);
                        visit.add(nghbWorld);
                    }
                }
            }
        }

        return build(edges, finishLocation);
    }

    private Stack<Direction> build(Map<WorldPoint, WorldPoint> edges, WorldPoint finish) {
        Stack<Direction> path = new Stack<>();
        WorldPoint current = finish;

        while (edges.containsKey(current)) {
            WorldPoint next = edges.get(current);

            if (next.getX() > current.getX()) {
                path.add(Direction.WEST);
            } else if (next.getX() < current.getX()) {
                path.add(Direction.EAST);
            } else if (next.getY() > current.getY()) {
                path.add(Direction.SOUTH);
            } else {
                path.add(Direction.NORTH);
            }

            current = next;
        }

        return path;
    }

    private LocalPoint[] neighbours(LocalPoint point) {
        return new LocalPoint[]
                {
                        neighbour(point, Direction.NORTH), neighbour(point, Direction.SOUTH),
                        neighbour(point, Direction.EAST), neighbour(point, Direction.WEST)
                };
    }

    private LocalPoint neighbour(LocalPoint point, Direction direction) {
        WorldPoint worldPoint = WorldPoint.fromLocal(client, point);
        WorldArea area = worldPoint.toWorldArea();

        int dx, dy;

        switch (direction) {
            case NORTH:
                dx = 0;
                dy = 1;
                break;
            case SOUTH:
                dx = 0;
                dy = -1;
                break;
            case EAST:
                dx = 1;
                dy = 0;
                break;
            case WEST:
                dx = -1;
                dy = 0;
                break;
            default:
                throw new IllegalStateException();
        }

        while (area.canTravelInDirection(client.getTopLevelWorldView(), dx, dy)) {
            worldPoint = area.toWorldPoint()
                    .dx(dx)
                    .dy(dy);
            area = worldPoint.toWorldArea();
        }

        return LocalPoint.fromWorld(client, worldPoint);
    }

    private Rectangle getBounds(WallObject[] walls) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;

        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (WallObject wall : walls) {
            WorldPoint point = wall.getWorldLocation();
            minX = Math.min(minX, point.getX());
            minY = Math.min(minY, point.getY());

            maxX = Math.max(maxX, point.getX());
            maxY = Math.max(maxY, point.getY());
        }

        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    private Direction getPosition() {
        WorldPoint mine = client.getLocalPlayer().getWorldLocation();

        if (mine.getY() >= bounds.getMaxY() && mine.getX() < bounds.getMaxX() && mine.getX() > bounds.getX()) {
            return Direction.NORTH;
        } else if (mine.getY() <= bounds.getY() && mine.getX() < bounds.getMaxX() && mine.getX() > bounds.getX()) {
            return Direction.SOUTH;
        } else if (mine.getX() >= bounds.getMaxX() && mine.getY() < bounds.getMaxY() && mine.getY() > bounds.getY()) {
            return Direction.EAST;
        } else if (mine.getX() <= bounds.getX() && mine.getY() < bounds.getMaxY() && mine.getY() > bounds.getY()) {
            return Direction.WEST;
        }

        return null;
    }

    private WorldArea getIndicatorLine(Direction direction) {
        switch (direction) {
            case NORTH:
                return new WorldArea(bounds.x + 1, (int) bounds.getMaxY(), bounds.width - 1, 1, 0);
            case SOUTH:
                return new WorldArea(bounds.x + 1, bounds.y, bounds.width - 1, 1, 0);
            case WEST:
                return new WorldArea(bounds.x, bounds.y + 1, 1, bounds.height - 1, 0);
            case EAST:
                return new WorldArea((int) bounds.getMaxX(), bounds.y + 1, 1, bounds.height - 1, 0);
        }

        return null;
    }

    private void setTimeout() {
        timeout = RandomUtils.nextInt(config.tickDelayMin(), config.tickDelayMax());
    }
}