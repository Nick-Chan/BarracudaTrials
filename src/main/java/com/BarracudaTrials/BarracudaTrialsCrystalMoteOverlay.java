package com.BarracudaTrials;

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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class BarracudaTrialsCrystalMoteOverlay extends Overlay
{
    private final Client client;
    private final BarracudaTrialsPlugin plugin;
    private final BarracudaTrialsConfig config;

    // Load Crystal Mote positions for Gwenith Glide Marlin
    // Adjust path if your resource is named differently
    private static final List<CrystalMotePoint> THE_GWENITH_GLIDE_MARLIN_MOTES =
            loadCrystalMotes("/routes/thegwenithglid_marlin_wiki_crystalmote.json");

    @Inject
    public BarracudaTrialsCrystalMoteOverlay(Client client,
                                             BarracudaTrialsPlugin plugin,
                                             BarracudaTrialsConfig config)
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
        if (!plugin.isTrialRunning() || !config.showCrystalMotes())
        {
            return null;
        }

        // Optional: only for Marlin if that JSON is Marlin-only
        if (plugin.getTrialTypeThisRun() != 4) // 4 = Marlin in your code
        {
            return null;
        }

        int currentOrder = plugin.getCurrentRouteOrder();
        List<CrystalMotePoint> motesForThisOrder = getMotesForOrder(currentOrder);

        if (motesForThisOrder.isEmpty())
        {
            return null;
        }

        // 3x3 highlight
        int tileRadius = 3;

        // Highlight colour
        Color outline = config.crystalMoteColor();
        if (outline == null)
        {
            outline = new Color(166, 0, 255, 160);
        }

        // Fill
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

            // fill then outline
            graphics.setColor(fill);
            graphics.fill(poly);
            graphics.setColor(outline);
            graphics.draw(poly);
        }

        return null;
    }

    private List<CrystalMotePoint> getMotesForOrder(int order)
    {
        if (THE_GWENITH_GLIDE_MARLIN_MOTES == null || THE_GWENITH_GLIDE_MARLIN_MOTES.isEmpty())
        {
            return Collections.emptyList();
        }

        return THE_GWENITH_GLIDE_MARLIN_MOTES.stream()
                .filter(p -> p.order == order)
                .collect(Collectors.toList());
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
        InputStream in = BarracudaTrialsCrystalMoteOverlay.class.getResourceAsStream(resourcePath);
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
