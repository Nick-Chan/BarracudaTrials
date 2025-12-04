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
public class CrystalMoteOverlay extends Overlay
{
    private final Client client;
    private final BarracudaTrialsPlugin plugin;
    private final Config config;

    // Cache list of motes
    private final Map<String, List<CrystalMotePoint>> motesCache = new HashMap<>();

    @Inject
    public CrystalMoteOverlay(Client client,
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
        if (!plugin.isInTrial() || !config.showCrystalMotes())
        {
            return null;
        }

        Trial trial = plugin.getCurrentTrial();
        if (trial != Trial.GWENITH_GLIDE)
        {
            return null;
        }

        Difficulty difficulty;
        RouteVariant variant;

        switch (plugin.getTrialTypeThisRun())
        {
            case 2: // Swordfish
                difficulty = Difficulty.SWORDFISH;
                variant = config.gwGlideSwordfishVariant();
                break;
            case 3: // Shark
                difficulty = Difficulty.SHARK;
                variant = config.gwGlideSharkVariant();
                break;
            case 4: // Marlin
                difficulty = Difficulty.MARLIN;
                variant = config.gwGlideMarlinVariant();
                break;
            default:
                return null;
        }

        List<CrystalMotePoint> allMotes = getMotes(trial, difficulty, variant);
        if (allMotes.isEmpty())
        {
            return null;
        }

        int currentOrder = plugin.getCurrentRouteOrder();
        List<CrystalMotePoint> motesForThisOrder = allMotes.stream()
                .filter(p -> p.order == currentOrder)
                .collect(Collectors.toList());

        if (motesForThisOrder.isEmpty())
        {
            return null;
        }

        int tileRadius = config.crystalMotesSmallHighlight() ? 1 : 5;

        Color outline = config.crystalMoteColor();
        if (outline == null)
        {
            outline = new Color(166, 0, 255, 160);
        }

        Color fill = new Color(0, 0, 0, 70);

        graphics.setStroke(new BasicStroke(2.0f));

        for (CrystalMotePoint p : motesForThisOrder)
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

    private List<CrystalMotePoint> getMotes(Trial trial, Difficulty difficulty, RouteVariant variant)
    {
        String path = RouteResources.buildCrystalMotesPath(trial, difficulty, variant);
        return motesCache.computeIfAbsent(path, CrystalMoteOverlay::loadCrystalMotes);
    }

    // JSON mapping
    private static final class CrystalMotePoint
    {
        int regionId;
        int regionX;
        int regionY;
        int z;
        int order;
    }

    private static List<CrystalMotePoint> loadCrystalMotes(String resourcePath)
    {
        InputStream in = CrystalMoteOverlay.class.getResourceAsStream(resourcePath);
        if (in == null)
        {
            return Collections.emptyList();
        }

        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
        {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<CrystalMotePoint>>(){}.getType();
            List<CrystalMotePoint> points = gson.fromJson(reader, listType);

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
