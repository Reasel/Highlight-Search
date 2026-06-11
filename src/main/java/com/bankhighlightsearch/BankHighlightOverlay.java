package com.bankhighlightsearch;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

class BankHighlightOverlay extends WidgetItemOverlay
{
	private static final long BLINK_PERIOD_MS = 300;
	private static final long PULSE_PERIOD_MS = 900;
	private static final int MAX_FEATHER = 6;

	private final BankHighlightSearchPlugin plugin;
	private final BankHighlightSearchConfig config;
	private final ItemManager itemManager;

	private final Map<Long, BufferedImage> glowCache = new HashMap<>();

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

		final HighlightStyle style = config.highlightStyle();
		final long sinceSearch = System.currentTimeMillis() - plugin.getSearchTime();
		final int blinkDuration = config.blinkDuration();
		final boolean animating = blinkDuration == 0 || sinceSearch < blinkDuration * 1000L;

		if (style != HighlightStyle.FEATHERED_PULSE && animating && (sinceSearch / BLINK_PERIOD_MS) % 2 == 1)
		{
			return; // off-phase of the blink
		}

		final Rectangle bounds = widgetItem.getCanvasBounds();
		final Color color = config.highlightColor();

		switch (style)
		{
			case ITEM_OUTLINE:
				drawOutline(graphics, itemId, widgetItem, color);
				break;
			case FEATHERED_PULSE:
				if (animating)
				{
					drawGlow(graphics, itemId, widgetItem, color, pulseFeather(sinceSearch));
				}
				else
				{
					drawOutline(graphics, itemId, widgetItem, color);
				}
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

	private int pulseFeather(long sinceSearch)
	{
		final double phase = (sinceSearch % PULSE_PERIOD_MS) / (double) PULSE_PERIOD_MS;
		final double tri = phase < 0.5 ? phase * 2 : 2 - phase * 2;
		return 1 + (int) Math.round(tri * (MAX_FEATHER - 1));
	}

	private void drawGlow(Graphics2D graphics, int itemId, WidgetItem widgetItem, Color color, int feather)
	{
		final int quantity = widgetItem.getQuantity();
		final long key = ((long) itemId << 36) | ((long) quantity << 4) | feather;
		BufferedImage glow = glowCache.get(key);
		if (glow == null)
		{
			if (glowCache.size() > 512)
			{
				glowCache.clear();
			}
			glow = buildGlow(itemManager.getItemOutline(itemId, quantity, color), feather);
			glowCache.put(key, glow);
		}
		final Rectangle bounds = widgetItem.getCanvasBounds();
		graphics.drawImage(glow, bounds.x - feather, bounds.y - feather, null);
	}

	private static BufferedImage buildGlow(BufferedImage outline, int feather)
	{
		final BufferedImage img = new BufferedImage(outline.getWidth() + 2 * feather, outline.getHeight() + 2 * feather, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		// stamp the outline in expanding rings with falling alpha for a soft feathered halo
		for (int d = feather; d >= 1; d--)
		{
			final float alpha = 0.6f * (1f - (float) (d - 1) / feather);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
			for (int dx = -1; dx <= 1; dx++)
			{
				for (int dy = -1; dy <= 1; dy++)
				{
					if (dx != 0 || dy != 0)
					{
						g.drawImage(outline, feather + dx * d, feather + dy * d, null);
					}
				}
			}
		}
		g.setComposite(AlphaComposite.SrcOver);
		g.drawImage(outline, feather, feather, null);
		g.dispose();
		return img;
	}

	void invalidateGlowCache()
	{
		glowCache.clear();
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
