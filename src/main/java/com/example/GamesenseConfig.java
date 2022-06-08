package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Steelseries Gamesense")
public interface GamesenseConfig extends Config
{
	@ConfigItem(
		keyName = "greeting",
		name = "Port number:",
		description = "This is the port to which we should send events so that gamesense can find it."
	)
	default String greeting()
	{
		return "Set port here";
	}
}
