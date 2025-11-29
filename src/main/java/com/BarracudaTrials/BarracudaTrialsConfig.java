package com.BarracudaTrials;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.*;

@ConfigGroup("barracudatrials")
public interface BarracudaTrialsConfig extends Config
{
    // Sections
    @ConfigSection(
            name = "Route Overlay",
            description = "Route line",
            position = 0,
            closedByDefault = false
    )
    String routeSection = "routeSection";

    @ConfigSection(
            name = "Lost Supplies Overlay",
            description = "Lost Supplies",
            position = 1,
            closedByDefault = false
    )
    String lostSuppliesSection = "lostSuppliesSection";

    @ConfigSection(
            name = "The Gwenith Glide",
            description = "Settings for The Gwenith Glide",
            position = 2,
            closedByDefault = false
    )
    String theGwenithGlideSection = "theGwenithGlideSection";

    // Route config
    @ConfigItem(
            position = 0,
            keyName = "showRoute",
            name = "Show Trials Route",
            description = "Toggle the Trials route overlay",
            section = routeSection
    )
    default boolean showRoute()
    {
        return true;
    }

    // Lost Supplies Config
    @ConfigItem(
            position = 0,
            keyName = "highlightLostSupplies",
            name = "Highlight Lost Supplies",
            description = "Highlight Lost Supplies",
            section = lostSuppliesSection
    )
    default boolean highlightLostSupplies()
    {
        return true;
    }

    @ConfigItem(
            position = 1,
            keyName = "lostSuppliesSmallHighlight",
            name = "Small Hitbox Highlight",
            description = "Highlight Lost Supplies box only instead of hitbox",
            section = lostSuppliesSection
    )
    default boolean lostSuppliesSmallHighlight()
    {
        return false;
    }

    @ConfigItem(
            position = 2,
            keyName = "showLostSupplyNumbers",
            name = "Show Number Count Overlay",
            description = "Show the Lost Supply number over each crate",
            section = lostSuppliesSection
    )
    default boolean showLostSupplyNumbers()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
            position = 3,
            keyName = "lostSuppliesOutlineColor",
            name = "Outline colour",
            description = "Colour of the Lost Supplies highlight outline",
            section = lostSuppliesSection
    )
    default Color lostSuppliesOutlineColor()
    {
        return new Color(255, 215, 0, 160);
    }

    // The Gwenith Glide
    @ConfigItem(
            position = 0,
            keyName = "showCrystalMotes",
            name = "Highlight Crystal Motes",
            description = "Show highlight tiles for Crystal Motes",
            section = theGwenithGlideSection
    )
    default boolean showCrystalMotes()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
            position = 1,
            keyName = "crystalMoteColor",
            name = "Crystal Outline colour",
            description = "Colour of the Crystal Mote highlight outline",
            section = theGwenithGlideSection
    )
    default Color crystalMoteColor()
    {
        return new Color(166, 0, 255, 160);
    }
}
