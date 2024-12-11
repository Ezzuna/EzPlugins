package com.Ezzuneware.EzApi.Combat;

import com.example.EthanApiPlugin.EthanApiPlugin;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

public class EzNpc {


    public static List<WorldPoint> RemoveCornersFromWorldPointList(List<WorldPoint> area) {
        int minX = area.stream().mapToInt(WorldPoint::getX).min().orElse(0);
        int maxX = area.stream().mapToInt(WorldPoint::getX).max().orElse(0);
        int minY = area.stream().mapToInt(WorldPoint::getY).min().orElse(0);
        int maxY = area.stream().mapToInt(WorldPoint::getY).max().orElse(0);
        area.removeIf(p -> {
            int x = p.getX();
            int y = p.getY();
            return (x <= minX && (y <= minY || y >= maxY)) || (x >= maxX && (y <= minY || y >= maxY));
        });
        return area;
    }

    public static List<WorldPoint> GetSurroundingTiles(NPC npc, int radius) {
        List<WorldPoint> reachableTiles  = EthanApiPlugin.reachableTiles();
        List<WorldPoint> surroundingTiles = new ArrayList<>();


        WorldPoint npcCenter = npc.getWorldLocation().dx((radius+1)/3).dy((radius+1)/3);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                // Skip the center tile and any tiles within the NPC's assumed size
                if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) continue;

                surroundingTiles.add(new WorldPoint(npcCenter.getX() + dx, npcCenter.getY() + dy, npc.getWorldLocation().getPlane()));
            }
        }
        surroundingTiles.removeAll(npc.getWorldArea().toWorldPointList());
        surroundingTiles.retainAll(reachableTiles);     //If we don't do this, we are considering tiles off screen

        return RemoveCornersFromWorldPointList(surroundingTiles);
    }
}
