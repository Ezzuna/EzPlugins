package com.Ezzuneware.EzHerbi;

import com.google.common.collect.ImmutableList;
import net.runelite.api.coords.WorldPoint;

import java.util.List;

public class HerbiAfkData
{
	public static final List<WorldPoint> START_LOCATIONS = ImmutableList.of(
		new WorldPoint(3686, 3870, 0),
		new WorldPoint(3751, 3850, 0),
		new WorldPoint(3695, 3800, 0),
		new WorldPoint(3704, 3810, 0),
		new WorldPoint(3705, 3830, 0)
	);

	public static final List<WorldPoint> END_LOCATIONS = ImmutableList.of(
		new WorldPoint(3693, 3798, 0),
		new WorldPoint(3702, 3808, 0),
		new WorldPoint(3703, 3826, 0),
		new WorldPoint(3710, 3881, 0),
		new WorldPoint(3700, 3877, 0),
		new WorldPoint(3715, 3840, 0),
		new WorldPoint(3751, 3849, 0),
		new WorldPoint(3685, 3869, 0),
		new WorldPoint(3681, 3863, 0)
	);

	public static final String[] TRAIL_MENU_ENTRY_TARGETS = new String[]{
		"Tunnel", "Mushroom", "Smelly mushroom", "Muddy patch", "Seaweed", "Rock", "Driftwood"
	};

	public static final String[] NPC_MENU_ENTRY_TARGETS = new String[]{
		"Ancient Fungi", "Ancient Zygomite", "Ammonite Crab", "Tool Leprechaun"
	};
}
