package com.bankhighlightsearch;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SearchMatcherTest
{
	private static final IntUnaryOperator NO_VARIATIONS = id -> id;

	// groups 1+2 together (e.g. two doses of the same potion), 3 alone
	private static final IntUnaryOperator GROUP_1_2 = id -> id == 2 ? 1 : id;

	private static final List<SearchMatcher.BankItem> ITEMS = Arrays.asList(
		new SearchMatcher.BankItem(1, "Prayer potion(4)"),
		new SearchMatcher.BankItem(2, "Prayer potion(1)"),
		new SearchMatcher.BankItem(3, "Lobster"),
		new SearchMatcher.BankItem(4, "Super restore(4)")
	);

	@Test
	public void matchesCaseInsensitiveSubstring()
	{
		Set<Integer> m = SearchMatcher.match(ITEMS, "PRAYER POT", false, NO_VARIATIONS);
		assertEquals(Set.of(1, 2), m);
	}

	@Test
	public void emptyOrBlankQueryMatchesNothing()
	{
		assertTrue(SearchMatcher.match(ITEMS, "", false, NO_VARIATIONS).isEmpty());
		assertTrue(SearchMatcher.match(ITEMS, "   ", false, NO_VARIATIONS).isEmpty());
	}

	@Test
	public void variationsExpandToSameBaseGroup()
	{
		// "potion(4)" only names item 1 directly; variation flag pulls in item 2 via shared base
		Set<Integer> m = SearchMatcher.match(ITEMS, "potion(4)", true, GROUP_1_2);
		assertEquals(Set.of(1, 2), m);
	}

	@Test
	public void variationsOffDoesNotExpand()
	{
		Set<Integer> m = SearchMatcher.match(ITEMS, "potion(4)", false, GROUP_1_2);
		assertEquals(Set.of(1), m);
	}

	@Test
	public void variationsDoNotBleedAcrossGroups()
	{
		Set<Integer> m = SearchMatcher.match(ITEMS, "lobster", true, GROUP_1_2);
		assertEquals(Set.of(3), m);
	}

	@Test
	public void noItemsMatchesNothing()
	{
		assertTrue(SearchMatcher.match(Collections.emptyList(), "lobster", true, NO_VARIATIONS).isEmpty());
	}
}
