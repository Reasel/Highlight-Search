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
import net.runelite.api.MenuAction;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
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
import net.runelite.client.events.ConfigChanged;
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

	private static final int POTIONSTORE_TAB = 15;

	private volatile Set<Integer> matches = Collections.emptySet();

	@Getter
	private volatile long searchTime;

	private volatile boolean pendingScroll;
	private volatile boolean searchSubmitted;
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
			if (searchInput != null && chatboxPanelManager.getCurrentInput() == searchInput)
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

		matches = Collections.emptySet();
		lastQuery = "";
		searchSubmitted = false;

		searchInput = chatboxPanelManager.openTextInput("Highlight search:")
			.onChanged(q -> clientThread.invoke(() -> updateMatches(q)))
			.onDone((Consumer<String>) q ->
			{
				searchSubmitted = true;
				clientThread.invoke(() ->
				{
					updateMatches(q);
					goToAllTabAndScroll();
				});
			})
			.onClose(() ->
			{
				searchInput = null;
				if (!searchSubmitted)
				{
					matches = Collections.emptySet();
				}
			})
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

		return SearchMatcher.match(items, q, config.includeVariations(), id -> ItemVariationMapping.map(itemManager.canonicalize(id)));
	}

	// client thread
	private void goToAllTabAndScroll()
	{
		if (matches.isEmpty())
		{
			return;
		}

		// closing the potion store via its button is required before switching tabs;
		// leaving it open in the background breaks deposits (see core TabInterface)
		if (client.getVarbitValue(VarbitID.BANK_CURRENTTAB) == POTIONSTORE_TAB)
		{
			client.menuAction(-1, InterfaceID.Bankmain.POTIONSTORE_BUTTON, MenuAction.CC_OP, 1, -1, "Potion store", "");
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
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (event.getActionParam1() == InterfaceID.Bankmain.SEARCH
			&& event.getOption().equals("Search"))
		{
			client.createMenuEntry(-1)
				.setOption("Highlight search")
				.setTarget(event.getTarget())
				.setType(MenuAction.RUNELITE)
				.onClick(e -> openSearch());
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.BANKMAIN_FINISHBUILDING && pendingScroll)
		{
			pendingScroll = false;
			// defer: runScript is not reentrant and BANKMAIN_FINISHBUILDING fires nested
			// inside bankmain_build; scroll after all scripts for this tick finish
			clientThread.invokeAtTickEnd(this::scrollToMatches);
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

	// runs at tick end on the client thread; scrolls so the densest cluster of matches is visible
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

		final int scrollY = ScrollCalculator.densestClusterScrollY(centers, bankItems.getHeight(), bankItems.getScrollHeight());
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

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (BankHighlightSearchConfig.GROUP.equals(event.getGroup()))
		{
			clientThread.invoke(() ->
			{
				overlay.invalidateGlowCache();
				refreshMatches();
			});
		}
	}
}
