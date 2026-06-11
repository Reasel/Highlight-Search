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

	@ConfigItem(
		keyName = "highlightStyle",
		name = "Highlight style",
		description = "How matching items are highlighted",
		position = 1
	)
	default HighlightStyle highlightStyle()
	{
		return HighlightStyle.ITEM_OUTLINE;
	}

	@Alpha
	@ConfigItem(
		keyName = "highlightColor",
		name = "Highlight color",
		description = "Outline color for matching items",
		position = 2
	)
	default Color highlightColor()
	{
		return Color.ORANGE;
	}

	@Alpha
	@ConfigItem(
		keyName = "fillColor",
		name = "Fill color",
		description = "Fill color for the fill styles",
		position = 3
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
		description = "How long highlights blink after a search before turning solid (0 = blink forever)",
		position = 4
	)
	default int blinkDuration()
	{
		return 3;
	}

	@Range(min = 1, max = 10)
	@Units(Units.PIXELS)
	@ConfigItem(
		keyName = "pulseMinFeather",
		name = "Pulse min thickness",
		description = "Feathered pulse: outline thickness at the low point of the pulse",
		position = 5
	)
	default int pulseMinFeather()
	{
		return 1;
	}

	@Range(min = 1, max = 10)
	@Units(Units.PIXELS)
	@ConfigItem(
		keyName = "pulseMaxFeather",
		name = "Pulse max thickness",
		description = "Feathered pulse: outline thickness at the peak of the pulse",
		position = 6
	)
	default int pulseMaxFeather()
	{
		return 4;
	}

	@ConfigItem(
		keyName = "highlightPlaceholders",
		name = "Highlight placeholders",
		description = "Also highlight bank placeholders whose name matches",
		position = 7
	)
	default boolean highlightPlaceholders()
	{
		return true;
	}

	@ConfigItem(
		keyName = "includeVariations",
		name = "Match variations",
		description = "Also highlight charge/dose variations of matched items",
		position = 8
	)
	default boolean includeVariations()
	{
		return true;
	}
}
