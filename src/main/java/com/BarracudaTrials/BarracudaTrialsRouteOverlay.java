package com.BarracudaTrials;

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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Singleton
public class BarracudaTrialsRouteOverlay extends Overlay
{
    private final Client client;
    private final BarracudaTrialsPlugin plugin;
    private final BarracudaTrialsConfig config;

    @Inject
    public BarracudaTrialsRouteOverlay(Client client, BarracudaTrialsPlugin plugin, BarracudaTrialsConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showRoute() || !plugin.isTrialRunning())
        {
            return null;
        }

        java.util.List<RegionTile> route = getCurrentRegionRoute();
        if (route == null || route.size() < 2)
        {
            return null;
        }

        graphics.setStroke(new BasicStroke(2.0f));
        graphics.setColor(new Color(0, 255, 255, 160));

        net.runelite.api.Point lastCanvas = null;

        for (RegionTile tile : route)
        {
            WorldPoint wp = tile.toWorldPoint();
            LocalPoint lp = LocalPoint.fromWorld(client, wp);
            if (lp == null)
            {
                continue;
            }

            net.runelite.api.Point canvasPoint = Perspective.localToCanvas(client, lp, client.getPlane());
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


    private static final java.util.List<RegionTile> THE_GWENITH_GLIDE_SWORDFISH_ROUTE =
            loadRoute("/routes/the_gwenith_glide_swordfish.json");

    private static final java.util.List<RegionTile> THE_GWENITH_GLIDE_SHARK_ROUTE =
            loadRoute("/routes/the_gwenith_glide_shark.json");

    private static final java.util.List<RegionTile> THE_GWENITH_GLIDE_MARLIN_ROUTE =
            loadRoute("/routes/thegwenithglide_marlin_wiki.json");


    private java.util.List<RegionTile> getCurrentRegionRoute()
    {
        int trialType = plugin.getTrialTypeThisRun();
        int order = plugin.getCurrentRouteOrder();

        java.util.List<RegionTile> base;

        // Get trial type difficulty
        switch (trialType)
        {
            case 2: // Swordfish
                base = THE_GWENITH_GLIDE_SWORDFISH_ROUTE;
                break;
            case 3: // Shark
                base = THE_GWENITH_GLIDE_SHARK_ROUTE;
                break;
            case 4: // Marlin
                base = THE_GWENITH_GLIDE_MARLIN_ROUTE;
                break;
            default:
                return java.util.Collections.emptyList();
        }

        // Keep only tiles from the current order
        return base.stream()
                .filter(t -> t.order == order)
                .collect(Collectors.toList());
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

    // JSON structure
    private static final class RoutePoint
    {
        int regionId;
        int regionX;
        int regionY;
        int z;
        int order;
    }

    // Load a route from JSON resource
    private static java.util.List<RegionTile> loadRoute(String resourcePath)
    {
        InputStream in = BarracudaTrialsRouteOverlay.class.getResourceAsStream(resourcePath);
        if (in == null)
        {
            return java.util.Collections.emptyList();
        }

        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
        {
            Gson gson = new Gson();
            Type listType = new TypeToken<java.util.List<RoutePoint>>(){}.getType();
            java.util.List<RoutePoint> points = gson.fromJson(reader, listType);

            if (points == null)
            {
                return java.util.Collections.emptyList();
            }

            return points.stream()
                    .map(p -> new RegionTile(p.regionId, p.regionX, p.regionY, p.z, p.order))
                    .collect(Collectors.toList());
        }
        catch (Exception e)
        {
            return java.util.Collections.emptyList();
        }
    }
}


