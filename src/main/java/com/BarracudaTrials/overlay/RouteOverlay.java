package com.BarracudaTrials.overlay;

import com.BarracudaTrials.BarracudaTrialsPlugin;
import com.BarracudaTrials.config.Config;
import com.BarracudaTrials.model.Difficulty;
import com.BarracudaTrials.model.RouteVariant;
import com.BarracudaTrials.model.Trial;
import com.BarracudaTrials.util.RouteResources;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class RouteOverlay extends Overlay
{
    private final Client client;
    private final BarracudaTrialsPlugin plugin;
    private final Config config;
    private final Gson gson;

    private final Map<String, List<RegionTile>> routeCache = new HashMap<>();

    @Inject
    public RouteOverlay(Client client,
                        BarracudaTrialsPlugin plugin,
                        Config config,
                        Gson gson)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.gson = gson;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showRoute() || !plugin.isInTrial())
        {
            return null;
        }

        List<RegionTile> route = getCurrentRegionRoute();
        if (route == null || route.size() < 2)
        {
            return null;
        }

        graphics.setStroke(new BasicStroke(2.0f));

        Color routeColor = config.routeColor();
        if (routeColor == null)
        {
            routeColor = new Color(0, 255, 255, 160);
        }
        graphics.setColor(routeColor);

        net.runelite.api.Point lastCanvas = null;

        for (RegionTile tile : route)
        {
            WorldPoint wp = tile.toWorldPoint();
            LocalPoint lp = LocalPoint.fromWorld(client, wp);
            if (lp == null)
            {
                continue;
            }

            net.runelite.api.Point canvasPoint =
                    Perspective.localToCanvas(client, lp, client.getPlane());
            if (canvasPoint == null)
            {
                continue;
            }

            if (lastCanvas != null)
            {
                graphics.drawLine(
                        lastCanvas.getX(), lastCanvas.getY(),
                        canvasPoint.getX(), canvasPoint.getY()
                );
            }

            lastCanvas = canvasPoint;
        }

        return null;
    }

    private List<RegionTile> getCurrentRegionRoute()
    {
        Trial trial = plugin.getCurrentTrial();
        Difficulty difficulty = plugin.getCurrentDifficulty();

        if (trial == null || difficulty == null)
        {
            return Collections.emptyList();
        }

        RouteVariant variant = getActiveVariant(trial, difficulty);

        String path = RouteResources.buildRoutePath(
                trial,
                difficulty,
                variant,
                "route.json"
        );

        List<RegionTile> base = loadRoute(path);
        if (base == null || base.isEmpty())
        {
            return Collections.emptyList();
        }

        int order = plugin.getCurrentRouteOrder();

        return base.stream()
                .filter(t -> t.order == order)
                .collect(Collectors.toList());
    }

    private List<RegionTile> getRoute(Trial trial,
                                      Difficulty difficulty,
                                      RouteVariant variant)
    {
        String key = trial.name() + "-" + difficulty.name() + "-" + variant.name();
        return routeCache.computeIfAbsent(key, k -> {
            String path = RouteResources.buildRoutePath(
                    trial,
                    difficulty,
                    variant,
                    "route.json"
            );
            return loadRoute(path);
        });
    }

    private static final class RegionTile
    {
        final int regionId;
        final int regionX;
        final int regionY;
        final int plane;
        final int order;

        RegionTile(int regionId, int regionX, int regionY, int plane, int order)
        {
            this.regionId = regionId;
            this.regionX = regionX;
            this.regionY = regionY;
            this.plane = plane;
            this.order = order;
        }

        WorldPoint toWorldPoint()
        {
            return WorldPoint.fromRegion(regionId, regionX, regionY, plane);
        }
    }

    // JSON
    private static final class RoutePoint
    {
        int regionId;
        int regionX;
        int regionY;
        int z;
        int order;
    }

    private List<RegionTile> loadRoute(String resourcePath)
    {
        InputStream in = RouteOverlay.class.getResourceAsStream(resourcePath);
        if (in == null)
        {
            return Collections.emptyList();
        }

        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
        {
            Type listType = new TypeToken<List<RoutePoint>>() {}.getType();
            List<RoutePoint> points = gson.fromJson(reader, listType);

            if (points == null)
            {
                return Collections.emptyList();
            }

            return points.stream()
                    .map(p -> new RegionTile(p.regionId, p.regionX, p.regionY, p.z, p.order))
                    .collect(Collectors.toList());
        }
        catch (Exception e)
        {
            return Collections.emptyList();
        }
    }

    private RouteVariant getActiveVariant(Trial trial, Difficulty difficulty)
    {
        switch (trial)
        {
            case GWENITH_GLIDE:
                switch (difficulty)
                {
                    case SWORDFISH:
                        return config.gwGlideSwordfishVariant();
                    case SHARK:
                        return config.gwGlideSharkVariant();
                    case MARLIN:
                        return config.gwGlideMarlinVariant();
                }
                break;

            case JUBBLY_JIVE:
                switch (difficulty)
                {
                    case SWORDFISH:
                        return config.jubblySwordfishVariant();
                    case SHARK:
                        return config.jubblySharkVariant();
                    case MARLIN:
                        return config.jubblyMarlinVariant();
                }
                break;

            case TEMPOR_TANTRUM:
                switch (difficulty)
                {
                    case SWORDFISH:
                        return config.temporSwordfishVariant();
                    case SHARK:
                        return config.temporSharkVariant();
                    case MARLIN:
                        return config.temporMarlinVariant();
                }
                break;
        }

        // Fallback
        return RouteVariant.WIKI;
    }
}
