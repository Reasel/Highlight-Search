package com.bankhighlightsearch;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum HighlightStyle
{
	ITEM_OUTLINE("Item outline"),
	ITEM_OUTLINE_AND_FILL("Outline + fill"),
	BOX("Box border"),
	UNDERLINE("Underline"),
	FILL("Filled box");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
