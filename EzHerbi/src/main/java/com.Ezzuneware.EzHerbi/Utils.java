package com.Ezzuneware.EzHerbi;

import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

public class Utils
{
	private static final Integer PATH_LINE_DIVISION = 10;

	public static WorldPoint getNearestStartLocation(WorldPoint playerLocation)
	{
		WorldPoint neartestPoint = null;
		double shortestDistance = Double.MAX_VALUE;

		for (WorldPoint startPoint : HerbiAfkData.START_LOCATIONS)
		{
			double distance = playerLocation.distanceTo2D(startPoint);
			if (distance < shortestDistance)
			{
				neartestPoint = startPoint;
				shortestDistance = distance;
			}
		}

		return neartestPoint;
	}

	public static List<WorldPoint> getPathLinePoints(WorldPoint start, WorldPoint end)
	{
		List<WorldPoint> pathLinePoints = new ArrayList<>();

		double distance = start.distanceTo2D(end);
		int divisions = (int) Math.ceil(distance / PATH_LINE_DIVISION);

		pathLinePoints.add(start);

		if (divisions == 1)
		{
			pathLinePoints.add(end);
			return pathLinePoints;
		}

		double angle = Math.atan2((end.getY() - start.getY()), (end.getX() - start.getX()));
		double deltaH = distance / divisions;
		int deltaX = (int) (deltaH * Math.cos(angle));
		int deltaY = (int) (deltaH * Math.sin(angle));

		int currentX = start.getX();
		int currentY = start.getY();

		for (int i = 1; i < divisions; i++)
		{
			currentX += deltaX;
			currentY += deltaY;
			pathLinePoints.add(new WorldPoint(currentX, currentY, 0));
		}

		pathLinePoints.add(end);

		return pathLinePoints;
	}
}
