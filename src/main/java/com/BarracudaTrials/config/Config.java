package com.BarracudaTrials.config;

import com.BarracudaTrials.model.SpeedBoostDisplayMode;
import com.BarracudaTrials.model.RouteVariant;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.*;

@ConfigGroup("barracudatrials")
public interface Config extends net.runelite.client.config.Config
{
    // Sections
    @ConfigSection(
            name = "General",
            description = "General Settings",
            position = 0,
            closedByDefault = false
    )
    String generalSection = "generalSection";

    @ConfigSection(
            name = "Route Overlay",
            description = "Route line",
            position = 1,
            closedByDefault = false
    )
    String routeSection = "routeSection";

    @ConfigSection(
            name = "Lost Supplies Overlay",
            description = "Lost Supplies",
            position = 2,
            closedByDefault = false
    )
    String lostSuppliesSection = "lostSuppliesSection";

    @ConfigSection(
            name = "Tempor Tantrum",
            description = "Settings for The Tempor Tantrum",
            position = 3,
            closedByDefault = false
    )
    String theTemporTantrumSection = "theTemporTantrumSection";

    @ConfigSection(
            name = "Jubbly Jive",
            description = "Settings for The Jubbly Jive",
            position = 3,
            closedByDefault = false
    )
    String theJubblyJiveSection = "theJubblyJiveSection";

    @ConfigSection(
            name = "Gwenith Glide",
            description = "Settings for The Gwenith Glide",
            position = 4,
            closedByDefault = false
    )
    String theGwenithGlideSection = "theGwenithGlideSection";

    // General config
    @ConfigItem(
            position = 0,
            keyName = "showSpeedBoostOverlay",
            name = "Show Speed Boost",
            description = "Show a timer for the boat speed boost",
            section = generalSection
    )
    default boolean showSpeedBoostOverlay()
    {
        return true;
    }

    @ConfigItem(
            position = 1,
            keyName = "speedBoostDisplayMode",
            name = "Display Mode",
            description = "Show the speed boost as a pie overlay or text",
            section = generalSection
    )
    default SpeedBoostDisplayMode speedBoostDisplayMode()
    {
        return SpeedBoostDisplayMode.PIE;
    }

    @Alpha
    @ConfigItem(
            position = 2,
            keyName = "speedBoostColor",
            name = "Speed Boost Colour",
            description = "Colour of the speed boost display",
            section = generalSection
    )
    default Color speedBoostColor()
    {
        return new Color(0, 255, 0, 180);
    }

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

    @Alpha
    @ConfigItem(
            position = 1,
            keyName = "routeColor",
            name = "Line colour",
            description = "Colour of the route line",
            section = routeSection
    )
    default Color routeColor()
    {
        return new Color(0, 255, 255, 160);
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

    // Tempor Tantrum
    @ConfigItem(
            position = 0,
            keyName = "temporSwordfishVariant",
            name = "Swordfish Route",
            description = "Route variant for Tempor Tantrum (Swordfish)",
            section = theTemporTantrumSection
    )
    default RouteVariant temporSwordfishVariant()
    {
        return RouteVariant.WIKI;
    }

    @ConfigItem(
            position = 1,
            keyName = "temporSharkVariant",
            name = "Shark Route",
            description = "Route variant for Tempor Tantrum (Shark)",
            section = theTemporTantrumSection
    )
    default RouteVariant temporSharkVariant()
    {
        return RouteVariant.WIKI;
    }

    @ConfigItem(
            position = 2,
            keyName = "temporMarlinVariant",
            name = "Marlin Route",
            description = "Route variant for Tempor Tantrum (Marlin)",
            section = theTemporTantrumSection
    )
    default RouteVariant temporMarlinVariant()
    {
        return RouteVariant.WIKI;
    }

    // Jubbly Jive
    @ConfigItem(
            position = 0,
            keyName = "jubblySwordfishVariant",
            name = "Swordfish Route",
            description = "Route variant for Jubbly Jive (Swordfish)",
            section = theJubblyJiveSection
    )
    default RouteVariant jubblySwordfishVariant()
    {
        return RouteVariant.WIKI;
    }

    @ConfigItem(
            position = 1,
            keyName = "jubblySharkVariant",
            name = "Shark Route",
            description = "Route variant for Jubbly Jive (Shark)",
            section = theJubblyJiveSection
    )
    default RouteVariant jubblySharkVariant()
    {
        return RouteVariant.WIKI;
    }

    @ConfigItem(
            position = 2,
            keyName = "jubblyMarlinVariant",
            name = "Marlin Route",
            description = "Route variant for Jubbly Jive (Marlin)",
            section = theJubblyJiveSection
    )
    default RouteVariant jubblyMarlinVariant()
    {
        return RouteVariant.WIKI;
    }

    // The Gwenith Glide
    @ConfigItem(
            position = 0,
            keyName = "gwGlideSwordfishVariant",
            name = "Swordfish Route",
            description = "Route variant for Gwenith Glide (Swordfish)",
            section = theGwenithGlideSection
    )
    default RouteVariant gwGlideSwordfishVariant()
    {
        return RouteVariant.WIKI;
    }

    @ConfigItem(
            position = 1,
            keyName = "gwGlideSharkVariant",
            name = "Shark Route",
            description = "Route variant for Gwenith Glide (Shark)",
            section = theGwenithGlideSection
    )
    default RouteVariant gwGlideSharkVariant()
    {
        return RouteVariant.WIKI;
    }

    @ConfigItem(
            position = 2,
            keyName = "gwGlideMarlinVariant",
            name = "Marlin Route",
            description = "Route variant for Gwenith Glide (Marlin)",
            section = theGwenithGlideSection
    )
    default RouteVariant gwGlideMarlinVariant()
    {
        return RouteVariant.WIKI;
    }

    @ConfigItem(
            position = 3,
            keyName = "showCrystalMotes",
            name = "Highlight Crystal Motes",
            description = "Show highlight tiles for Crystal Motes",
            section = theGwenithGlideSection
    )
    default boolean showCrystalMotes()
    {
        return true;
    }

    @ConfigItem(
            position = 4,
            keyName = "crystalMotesSmallHighlight",
            name = "Small Hitbox Highlight",
            description = "Highlight Crystal Mote object only instead of hitbox",
            section = theGwenithGlideSection
    )
    default boolean crystalMotesSmallHighlight()
    {
        return false;
    }

    @Alpha
    @ConfigItem(
            position = 5,
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
