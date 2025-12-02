package com.BarracudaTrials.util;

import com.BarracudaTrials.model.Trial;
import com.BarracudaTrials.model.Difficulty;
import com.BarracudaTrials.model.RouteVariant;

public final class RouteResources
{
    private RouteResources() {}

    public static String buildRoutePath(
            Trial trial,
            Difficulty difficulty,
            RouteVariant variant,
            String fileName
    )
    {
        return String.format(
                "/routes/%s/%s/%s/%s",
                trial.getKey(),
                difficulty.getKey(),
                variant.getKey(),
                fileName
        );
    }

    public static String buildSuppliesPath(
            Trial trial,
            Difficulty difficulty,
            RouteVariant variant
    )
    {
        return buildRoutePath(trial, difficulty, variant, "supplies.json");
    }

    public static String buildCrystalMotesPath(
            Trial trial,
            Difficulty difficulty,
            RouteVariant variant
    )
    {
        return buildRoutePath(trial, difficulty, variant, "crystal_motes.json");
    }

    public static String buildRapidsPath(
            Trial trial,
            Difficulty difficulty,
            RouteVariant variant
    )
    {
        return buildRoutePath(trial, difficulty, variant, "rapids.json");
    }

    public static String buildPillarsPath(
            Trial trial,
            Difficulty difficulty,
            RouteVariant variant
    )
    {
        return buildRoutePath(trial, difficulty, variant, "pillars.json");
    }
}
