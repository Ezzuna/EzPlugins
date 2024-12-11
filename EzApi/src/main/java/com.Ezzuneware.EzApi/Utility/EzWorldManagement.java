package com.Ezzuneware.EzApi.Utility;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.World;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.game.WorldService;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class EzWorldManagement {

    static Client client = RuneLite.getInjector().getInstance(Client.class);
    static ClientThread clientThread = RuneLite.getInjector().getInstance(ClientThread.class);
    static WorldService worldService = RuneLite.getInjector().getInstance(WorldService.class);
    static ChatMessageManager chatMessageManager = RuneLite.getInjector().getInstance(ChatMessageManager.class);

    public static void HopToTargetWorld(World targetWorld) {
        if (targetWorld == null)
            return;

        if (client.getWidget(WidgetInfo.WORLD_SWITCHER_LIST) == null) {
            client.openWorldHopper();


        } else {
            client.hopToWorld(targetWorld);
            targetWorld = null;
        }
    }

    public static net.runelite.http.api.worlds.World findWorld(List<net.runelite.http.api.worlds.World> worlds, EnumSet<WorldType> currentWorldTypes, int totalLevel) {
        net.runelite.http.api.worlds.World world = worlds.get(new Random().nextInt(worlds.size()));

        EnumSet<net.runelite.http.api.worlds.WorldType> types = world.getTypes().clone();

        types.remove(net.runelite.http.api.worlds.WorldType.LAST_MAN_STANDING);

        if (types.contains(net.runelite.http.api.worlds.WorldType.SKILL_TOTAL)) {
            try {
                int totalRequirement = Integer.parseInt(world.getActivity().substring(0, world.getActivity().indexOf(" ")));

                if (totalLevel >= totalRequirement) {
                    types.remove(WorldType.SKILL_TOTAL);
                }
            } catch (NumberFormatException ex) {
                System.out.println("Failed to parse total level requirement for target world; " + ex);
            }
        }

        if (currentWorldTypes.equals(types)) {
            int worldLocation = world.getLocation();

            return world;
        }

        return null;
    }

    public static void hop() {
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

            net.runelite.http.api.worlds.World world;
            do {
                world = findWorld(worlds, currentWorldTypes, totalLevel);
            }
            while (world == null || world == currentWorld);

            hop(world.getId());
        });
    }

    public static World hop(int worldId) {
        WorldResult worldResult = worldService.getWorlds();
        // Don't try to hop if the world doesn't exist
        net.runelite.http.api.worlds.World world = worldResult.findWorld(worldId);
        if (world == null) {
            return null;
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
            return null;
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
        if (client.getWidget(WidgetInfo.WORLD_SWITCHER_LIST) == null) {
            client.openWorldHopper();


        } else {
            client.hopToWorld(rsWorld);
        }

        return rsWorld;
    }

}
