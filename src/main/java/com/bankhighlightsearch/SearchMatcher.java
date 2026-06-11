package com.bankhighlightsearch;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import lombok.Value;

final class SearchMatcher
{
	@Value
	static class BankItem
	{
		int id;
		String name;
	}

	private SearchMatcher()
	{
	}

	/**
	 * @param variationBase maps an item id to its variation-group base id
	 *                      (pass ItemVariationMapping::map in production)
	 */
	static Set<Integer> match(Collection<BankItem> items, String query, boolean includeVariations, IntUnaryOperator variationBase)
	{
		final String q = query.trim().toLowerCase();
		final Set<Integer> matched = new HashSet<>();
		if (q.isEmpty())
		{
			return matched;
		}

		final Set<Integer> matchedBases = new HashSet<>();
		for (BankItem item : items)
		{
			if (item.getName().toLowerCase().contains(q))
			{
				matched.add(item.getId());
				if (includeVariations)
				{
					matchedBases.add(variationBase.applyAsInt(item.getId()));
				}
			}
		}

		if (includeVariations)
		{
			for (BankItem item : items)
			{
				if (!matched.contains(item.getId()) && matchedBases.contains(variationBase.applyAsInt(item.getId())))
				{
					matched.add(item.getId());
				}
			}
		}

		return matched;
	}
}
