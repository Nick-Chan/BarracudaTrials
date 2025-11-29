package com.BarracudaTrials;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BarracudaTrialsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BarracudaTrialsPlugin.class);
		RuneLite.main(args);
	}
}