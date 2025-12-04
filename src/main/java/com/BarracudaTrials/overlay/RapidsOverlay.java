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
public class RapidsOverlay extends Overlay
{
    private final Client client;
    private final BarracudaTrialsPlugin plugin;
    private final Config config;

    // Cache - rapids list
    private final Map<String, List<RapidsPoint>> rapidsCache = new HashMap<>();

    @Inject
    public RapidsOverlay(Client client,
                              BarracudaTrialsPlugin plugin,
                              Config config)
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
        if (!plugin.isInTrial() || !config.showRapids())
        {
            return null;
        }

        Trial trial = plugin.getCurrentTrial();
        if (trial != Trial.JUBBLY_JIVE && trial != Trial.TEMPOR_TANTRUM )
        {
            return null;
        }

        Difficulty difficulty;
        RouteVariant variant;

        switch (plugin.getTrialTypeThisRun())
        {
            case 2: // Swordfish
                difficulty = Difficulty.SWORDFISH;
                variant = config.jubblySwordfishVariant();
                break;
            case 3: // Shark
                difficulty = Difficulty.SHARK;
                variant = config.jubblySharkVariant();
                break;
            case 4: // Marlin
                difficulty = Difficulty.MARLIN;
                variant = config.jubblyMarlinVariant();
                break;
            default:
                return null;
        }

        List<RapidsPoint> allRapids = getRapids(trial, difficulty, variant);
        if (allRapids.isEmpty())
        {
            return null;
        }

        int currentOrder = plugin.getCurrentRouteOrder();
        List<RapidsPoint> rapidsForThisOrder = allRapids.stream()
                .filter(p -> p.order == currentOrder)
                .collect(Collectors.toList());

        if (rapidsForThisOrder.isEmpty())
        {
            return null;
        }

        int tileRadius = 3;

        Color outline = config.rapidsColor();
        if (outline == null)
        {
            outline = new Color(166, 0, 255, 160);
        }

        Color fill = new Color(0, 0, 0, 70);

        graphics.setStroke(new BasicStroke(2.0f));

        for (RapidsPoint p : rapidsForThisOrder)
        {
            WorldPoint wp = WorldPoint.fromRegion(p.regionId, p.regionX, p.regionY, p.z);
            LocalPoint lp = LocalPoint.fromWorld(client, wp);
            if (lp == null)
            {
                continue;
            }

            Polygon poly = Perspective.getCanvasTileAreaPoly(client, lp, tileRadius);
            if (poly == null)
            {
                continue;
            }

            graphics.setColor(fill);
            graphics.fill(poly);
            graphics.setColor(outline);
            graphics.draw(poly);
        }

        return null;
    }

    private List<RapidsPoint> getRapids(Trial trial, Difficulty difficulty, RouteVariant variant)
    {
        String path = RouteResources.buildRapidsPath(trial, difficulty, variant);
        return rapidsCache.computeIfAbsent(path, RapidsOverlay::loadRapids);
    }

    // JSON mapping
    private static final class RapidsPoint
    {
        int regionId;
        int regionX;
        int regionY;
        int z;
        int order;
    }

    private static List<RapidsPoint> loadRapids(String resourcePath)
    {
        InputStream in = RapidsOverlay.class.getResourceAsStream(resourcePath);
        if (in == null)
        {
            return Collections.emptyList();
        }

        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
        {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<RapidsPoint>>(){}.getType();
            List<RapidsPoint> points = gson.fromJson(reader, listType);

            if (points == null)
            {
                return Collections.emptyList();
            }

            return points;
        }
        catch (Exception e)
        {
            return Collections.emptyList();
        }
    }
}
