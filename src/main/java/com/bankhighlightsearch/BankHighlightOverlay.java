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
		final int blinkDuration = config.blinkDuration();
		final boolean blinking = blinkDuration == 0 || sinceSearch < blinkDuration * 1000L;
		if (blinking && (sinceSearch / BLINK_PERIOD_MS) % 2 == 1)
		{
			return; // off-phase of the blink
		}

		final Rectangle bounds = widgetItem.getCanvasBounds();
		final Color color = config.highlightColor();

		switch (config.highlightStyle())
		{
			case ITEM_OUTLINE:
				drawOutline(graphics, itemId, widgetItem, color);
				break;
			case ITEM_OUTLINE_AND_FILL:
				drawOutline(graphics, itemId, widgetItem, color);
				drawFill(graphics, bounds);
				break;
			case BOX:
				graphics.setColor(color);
				graphics.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
				break;
			case UNDERLINE:
				// Geometry mirrored from InventoryTagsOverlay lines 101-104:
				// heightOffSet = bounds.getY() + bounds.getHeight() + 2
				// drawLine(bounds.getX(), heightOffSet, bounds.getX() + bounds.getWidth(), heightOffSet)
				int heightOffSet = (int) bounds.getY() + (int) bounds.getHeight() + 2;
				graphics.setColor(color);
				graphics.drawLine((int) bounds.getX(), heightOffSet, (int) bounds.getX() + (int) bounds.getWidth(), heightOffSet);
				break;
			case FILL:
				drawFill(graphics, bounds);
				break;
		}
	}

	private void drawOutline(Graphics2D graphics, int itemId, WidgetItem widgetItem, Color color)
	{
		final BufferedImage outline = itemManager.getItemOutline(itemId, widgetItem.getQuantity(), color);
		final Rectangle bounds = widgetItem.getCanvasBounds();
		graphics.drawImage(outline, bounds.x, bounds.y, null);
	}

	private void drawFill(Graphics2D graphics, Rectangle bounds)
	{
		final Color fill = config.fillColor();
		if (fill.getAlpha() > 0)
		{
			graphics.setColor(fill);
			graphics.fill(bounds);
		}
	}
}
