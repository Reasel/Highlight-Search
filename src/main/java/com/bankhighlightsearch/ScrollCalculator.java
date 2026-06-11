package com.bankhighlightsearch;

import java.util.Collection;

final class ScrollCalculator
{
	private ScrollCalculator()
	{
	}

	/**
	 * Mirrors ClueScrollPlugin.scrollToWidget: center the average match position in the viewport.
	 *
	 * @return scrollY clamped to [0, scrollHeight], or -1 when matchCenterYs is empty
	 */
	static int centroidScrollY(Collection<Integer> matchCenterYs, int viewportHeight, int scrollHeight)
	{
		if (matchCenterYs.isEmpty())
		{
			return -1;
		}

		long sum = 0;
		for (int y : matchCenterYs)
		{
			sum += y;
		}
		final int centroid = (int) (sum / matchCenterYs.size());
		return Math.max(0, Math.min(scrollHeight, centroid - viewportHeight / 2));
	}
}
