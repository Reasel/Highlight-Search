package com.bankhighlightsearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

final class ScrollCalculator
{
	private ScrollCalculator()
	{
	}

	/**
	 * Picks the scroll position that makes the most matched items visible at once:
	 * a sliding window of the viewport height over the sorted item centers finds the
	 * densest cluster (ties go to the topmost), which is then centered in the viewport.
	 *
	 * @return scrollY clamped to [0, scrollHeight], or -1 when matchCenterYs is empty
	 */
	static int densestClusterScrollY(Collection<Integer> matchCenterYs, int viewportHeight, int scrollHeight)
	{
		if (matchCenterYs.isEmpty())
		{
			return -1;
		}

		final List<Integer> centers = new ArrayList<>(matchCenterYs);
		Collections.sort(centers);

		int bestCount = 0;
		int bestFirst = 0;
		int bestLast = 0;
		int j = 0;
		for (int i = 0; i < centers.size(); i++)
		{
			if (j < i)
			{
				j = i;
			}
			while (j + 1 < centers.size() && centers.get(j + 1) - centers.get(i) <= viewportHeight)
			{
				j++;
			}
			final int count = j - i + 1;
			if (count > bestCount)
			{
				bestCount = count;
				bestFirst = i;
				bestLast = j;
			}
		}

		final int clusterMid = (centers.get(bestFirst) + centers.get(bestLast)) / 2;
		return Math.max(0, Math.min(scrollHeight, clusterMid - viewportHeight / 2));
	}
}
