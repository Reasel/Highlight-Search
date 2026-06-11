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
		assertEquals(-1, ScrollCalculator.centroidScrollY(Collections.emptyList(), 800, 2000));
	}

	@Test
	public void centroidCenteredInViewport()
	{
		// centers average to 1000; viewport 800 -> top of view at 1000 - 400 = 600
		assertEquals(600, ScrollCalculator.centroidScrollY(Arrays.asList(900, 1100), 800, 2000));
	}

	@Test
	public void clampsToZeroForMatchesNearTop()
	{
		// centroid 50 - 400 = -350 -> clamp 0
		assertEquals(0, ScrollCalculator.centroidScrollY(Arrays.asList(50), 800, 2000));
	}

	@Test
	public void clampsToScrollHeightForMatchesNearBottom()
	{
		// centroid 5000 - 400 = 4600 -> clamp to scrollHeight 2000
		assertEquals(2000, ScrollCalculator.centroidScrollY(Arrays.asList(5000), 800, 2000));
	}
}
