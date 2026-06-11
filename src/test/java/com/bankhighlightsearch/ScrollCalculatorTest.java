package com.bankhighlightsearch;

import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ScrollCalculatorTest
{
	@Test
	public void noMatchesReturnsMinusOne()
	{
		assertEquals(-1, ScrollCalculator.densestClusterScrollY(Collections.emptyList(), 800, 2000));
	}

	@Test
	public void singleClusterCenteredInViewport()
	{
		// all matches fit one window; cluster spans 900-1100, midpoint 1000 -> 1000 - 400 = 600
		assertEquals(600, ScrollCalculator.densestClusterScrollY(Arrays.asList(1100, 900), 800, 2000));
	}

	@Test
	public void denseClusterBeatsOutlier()
	{
		// three items near the top, one far outlier; cluster mid (100+140)/2=120 -> 120-400 clamps to 0
		assertEquals(0, ScrollCalculator.densestClusterScrollY(Arrays.asList(3000, 100, 140, 120), 800, 2000));
	}

	@Test
	public void tiePrefersTopmostCluster()
	{
		// two clusters of two; equal count -> topmost wins; mid (100+150)/2=125 -> clamp 0
		assertEquals(0, ScrollCalculator.densestClusterScrollY(Arrays.asList(2000, 100, 2050, 150), 800, 2000));
	}

	@Test
	public void clampsToZeroForMatchesNearTop()
	{
		assertEquals(0, ScrollCalculator.densestClusterScrollY(Arrays.asList(50), 800, 2000));
	}

	@Test
	public void clampsToScrollHeightForMatchesNearBottom()
	{
		// cluster mid 5025 - 400 = 4625 -> clamp to scrollHeight 2000
		assertEquals(2000, ScrollCalculator.densestClusterScrollY(Arrays.asList(5000, 5050), 800, 2000));
	}
}
