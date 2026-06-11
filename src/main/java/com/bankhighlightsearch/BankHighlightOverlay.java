package com.bankhighlightsearch;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

class BankHighlightOverlay extends WidgetItemOverlay
{
	private static final long BLINK_PERIOD_MS = 300;

	private final BankHighlightSearchPlugin plugin;
	private final BankHighlightSearchConfig config;
	private final ItemManager itemManager;

	@Inject
	BankHighlightOverlay(BankHighlightSearchPlugin plugin, BankHighlightSearchConfig config, ItemManager itemManager)
	{
		this.plugin = plugin;
		this.config = config;
		this.itemManager = itemManager;
		showOnBank();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!plugin.isMatched(itemId))
		{
			return;
		}

		final long sinceSearch = System.currentTimeMillis() - plugin.getSearchTime();
		if (sinceSearch < config.blinkDuration() * 1000L && (sinceSearch / BLINK_PERIOD_MS) % 2 == 1)
		{
			return; // off-phase of the blink
		}

		final Rectangle bounds = widgetItem.getCanvasBounds();
		final BufferedImage outline = itemManager.getItemOutline(itemId, widgetItem.getQuantity(), config.highlightColor());
		graphics.drawImage(outline, bounds.x, bounds.y, null);

		final Color fill = config.fillColor();
		if (fill.getAlpha() > 0)
		{
			graphics.setColor(fill);
			graphics.fill(bounds);
		}
	}
}
