package com.bankhighlightsearch;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BankHighlightSearchPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BankHighlightSearchPlugin.class);
		RuneLite.main(args);
	}
}
