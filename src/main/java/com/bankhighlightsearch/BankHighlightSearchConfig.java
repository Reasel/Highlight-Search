package com.bankhighlightsearch;

import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(BankHighlightSearchConfig.GROUP)
public interface BankHighlightSearchConfig extends Config
{
	String GROUP = "bankhighlightsearch";

	@ConfigItem(
		keyName = "searchKeybind",
		name = "Search hotkey",
		description = "Opens the highlight search input while the bank is open",
		position = 0
	)
	default Keybind searchKeybind()
	{
		return new Keybind(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	}

	@Alpha
	@ConfigItem(
		keyName = "highlightColor",
		name = "Highlight color",
		description = "Outline color for matching items",
		position = 1
	)
	default Color highlightColor()
	{
		return Color.ORANGE;
	}

	@Alpha
	@ConfigItem(
		keyName = "fillColor",
		name = "Fill color",
		description = "Fill drawn over matching items (set alpha to 0 to disable)",
		position = 2
	)
	default Color fillColor()
	{
		return new Color(255, 165, 0, 40);
	}

	@Range(max = 30)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "blinkDuration",
		name = "Blink duration",
		description = "How long highlights blink after a search before turning solid (0 = never blink)",
		position = 3
	)
	default int blinkDuration()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "highlightPlaceholders",
		name = "Highlight placeholders",
		description = "Also highlight bank placeholders whose name matches",
		position = 4
	)
	default boolean highlightPlaceholders()
	{
		return true;
	}

	@ConfigItem(
		keyName = "includeVariations",
		name = "Match variations",
		description = "Also highlight charge/dose variations of matched items",
		position = 5
	)
	default boolean includeVariations()
	{
		return true;
	}
}
