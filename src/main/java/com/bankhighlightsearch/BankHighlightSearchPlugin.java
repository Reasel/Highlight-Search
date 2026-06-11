package com.bankhighlightsearch;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.game.chatbox.ChatboxTextInput;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

@Slf4j
@PluginDescriptor(
	name = "Bank Highlight Search",
	description = "Search the bank with a hotkey and highlight matches instead of filtering them out",
	tags = {"bank", "search", "highlight", "find"}
)
public class BankHighlightSearchPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private KeyManager keyManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	@Inject
	private BankHighlightSearchConfig config;

	@Inject
	private BankHighlightOverlay overlay;

	private volatile Set<Integer> matches = Collections.emptySet();

	@Getter
	private volatile long searchTime;

	private boolean pendingScroll;
	private String lastQuery = "";
	private ChatboxTextInput searchInput;

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.searchKeybind())
	{
		@Override
		public void hotkeyPressed()
		{
			clientThread.invoke(() -> openSearch());
		}
	};

	@Provides
	BankHighlightSearchConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankHighlightSearchConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		keyManager.registerKeyListener(hotkeyListener);
	}

	@Override
	protected void shutDown()
	{
		keyManager.unregisterKeyListener(hotkeyListener);
		overlayManager.remove(overlay);
		matches = Collections.emptySet();
		pendingScroll = false;
		clientThread.invoke(() ->
		{
			if (searchInput != null)
			{
				chatboxPanelManager.close();
				searchInput = null;
			}
		});
	}

	boolean isMatched(int itemId)
	{
		return matches.contains(itemId);
	}

	// client thread
	private void openSearch()
	{
		if (searchInput != null)
		{
			return; // our prompt is already open
		}

		final Widget bankItems = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (bankItems == null || bankItems.isHidden())
		{
			return; // bank not open
		}
		if (client.getVarcIntValue(VarClientID.MESLAYERMODE) != 0)
		{
			return; // another chatbox input is active (chat, native search, ...)
		}

		searchInput = chatboxPanelManager.openTextInput("Highlight search:")
			.value(lastQuery)
			.onChanged(q -> clientThread.invoke(() -> updateMatches(q)))
			.onDone((Consumer<String>) q -> clientThread.invoke(() ->
			{
				updateMatches(q);
				goToAllTabAndScroll();
			}))
			.onClose(() -> searchInput = null)
			.build();
	}

	// client thread
	private void updateMatches(String query)
	{
		lastQuery = query == null ? "" : query;
		matches = computeMatches(lastQuery);
		searchTime = System.currentTimeMillis();
	}

	// client thread; recompute without restarting the blink animation
	private void refreshMatches()
	{
		matches = computeMatches(lastQuery);
	}

	// client thread
	private Set<Integer> computeMatches(String query)
	{
		final String q = query.trim();
		if (q.isEmpty())
		{
			return Collections.emptySet();
		}

		final ItemContainer bank = client.getItemContainer(InventoryID.BANK);
		if (bank == null)
		{
			return matches; // container unavailable; keep current highlights
		}

		final List<SearchMatcher.BankItem> items = new ArrayList<>();
		for (Item item : bank.getItems())
		{
			final int id = item.getId();
			if (id <= 0)
			{
				continue;
			}
			final ItemComposition comp = itemManager.getItemComposition(id);
			if (comp.getPlaceholderTemplateId() != -1 && !config.highlightPlaceholders())
			{
				continue;
			}
			items.add(new SearchMatcher.BankItem(id, comp.getName()));
		}

		return SearchMatcher.match(items, q, config.includeVariations(), ItemVariationMapping::map);
	}

	// client thread
	private void goToAllTabAndScroll()
	{
		if (matches.isEmpty())
		{
			return;
		}

		if (client.getVarbitValue(VarbitID.BANK_CURRENTTAB) != 0)
		{
			client.setVarbit(VarbitID.BANK_CURRENTTAB, 0);
		}

		final Widget bankItems = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (bankItems == null)
		{
			return;
		}

		final Object[] buildArgs = bankItems.getOnInvTransmitListener();
		if (buildArgs == null)
		{
			return;
		}

		// rebuild the bank the same way BankSearch.layoutBank() does; BANKMAIN_FINISHBUILDING
		// fires during this call and onScriptPostFired performs the scroll
		pendingScroll = true;
		client.runScript(buildArgs);
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.BANKMAIN_FINISHBUILDING && pendingScroll)
		{
			pendingScroll = false;
			scrollToMatches();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.BANK && !matches.isEmpty())
		{
			refreshMatches();
		}
	}

	// client thread (ScriptPostFired handler)
	private void scrollToMatches()
	{
		final Widget bankItems = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (bankItems == null || matches.isEmpty())
		{
			return;
		}

		final List<Integer> centers = new ArrayList<>();
		for (Widget w : bankItems.getDynamicChildren())
		{
			if (!w.isSelfHidden() && matches.contains(w.getItemId()))
			{
				centers.add(w.getRelativeY() + w.getHeight() / 2);
			}
		}

		final int scrollY = ScrollCalculator.centroidScrollY(centers, bankItems.getHeight(), bankItems.getScrollHeight());
		if (scrollY < 0)
		{
			return;
		}

		client.runScript(ScriptID.UPDATE_SCROLLBAR, InterfaceID.Bankmain.SCROLLBAR, InterfaceID.Bankmain.ITEMS, scrollY);
		client.setVarcIntValue(VarClientID.BANK_SCROLLPOS, scrollY);
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN && event.isUnload())
		{
			matches = Collections.emptySet();
			pendingScroll = false;
		}
	}
}
