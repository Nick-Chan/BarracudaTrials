package com.BarracudaTrials.overlay;

import com.BarracudaTrials.BarracudaTrialsPlugin;
import com.BarracudaTrials.config.Config;
import com.BarracudaTrials.model.Difficulty;
import com.BarracudaTrials.model.Trial;
import com.BarracudaTrials.model.RouteVariant;
import com.BarracudaTrials.util.RouteResources;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Perspective;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

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
public class LostSuppliesOverlay extends Overlay
{
    private final Client client;
    private final BarracudaTrialsPlugin plugin;
    private final Config config;

    private static final int VARBIT_SAILING_BT_OBJECTIVE_BASE = 18448;
    private static final int MAX_OBJECTIVES = 96;

    // Cache
    private static final Map<String, Map<Integer, SupplyMeta>> SUPPLIES_CACHE = new HashMap<>();

    // Global cache
    private static final Map<Integer, SupplyMeta> ALL_SUPPLIES_META = new HashMap<>();

    public static boolean hasMetaForId(int objectId)
    {
        return ALL_SUPPLIES_META.containsKey(objectId);
    }

    @Inject
    public LostSuppliesOverlay(Client client,
                               BarracudaTrialsPlugin plugin,
                               Config config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        if (!plugin.isInTrial() || !config.highlightLostSupplies())
        {
            return null;
        }

        Map<Integer, SupplyMeta> suppliesMeta = getCurrentSuppliesMeta();
        if (suppliesMeta.isEmpty())
        {
            return null;
        }

        int currentOrder = plugin.getCurrentRouteOrder();

        for (GameObject o : plugin.getLostSupplies())
        {
            SupplyMeta meta = suppliesMeta.get(o.getId());
            if (meta == null)
            {
                continue;
            }

            // Only crates belonging to this route segment/order
            if (meta.order != currentOrder)
            {
                continue;
            }

            int objectiveIndex = meta.varbit;
            if (objectiveIndex < 0 || objectiveIndex >= MAX_OBJECTIVES)
            {
                continue;
            }

            int objectiveVarbitId = VARBIT_SAILING_BT_OBJECTIVE_BASE + objectiveIndex;
            int objectiveState = client.getVarbitValue(objectiveVarbitId);

            // If varbit == 0, this Lost Supply has been collected
            if (objectiveState == 0)
            {
                continue;
            }

            ObjectComposition def = client.getObjectDefinition(o.getId());
            if (def == null)
            {
                continue;
            }

            int tileRadius = config.lostSuppliesSmallHighlight() ? 1 : 5;
            Polygon poly = Perspective.getCanvasTileAreaPoly(client, o.getLocalLocation(), tileRadius);
            if (poly == null)
            {
                continue;
            }

            Color outline = config.lostSuppliesOutlineColor();
            if (outline == null)
            {
                outline = new Color(255, 215, 0, 160);
            }

            Color fill = new Color(0, 0, 0, 70);

            g.setColor(fill);
            g.fill(poly);
            OverlayUtil.renderPolygon(g, poly, outline);

            if (config.showLostSupplyNumbers())
            {
                String text = Integer.toString(meta.index);
                net.runelite.api.Point textLoc =
                        Perspective.getCanvasTextLocation(client, g, o.getLocalLocation(), text, 0);

                if (textLoc != null)
                {
                    OverlayUtil.renderTextLocation(g, textLoc, text, Color.WHITE);
                }
            }
        }

        return null;
    }

    private Map<Integer, SupplyMeta> getCurrentSuppliesMeta()
    {
        Trial trial = plugin.getCurrentTrial();
        Difficulty difficulty = plugin.getCurrentDifficulty();

        if (trial == null || difficulty == null)
        {
            return Collections.emptyMap();
        }

        RouteVariant variant = getActiveVariant(trial, difficulty);

        return getSuppliesMeta(trial, difficulty, variant);
    }

    private static Map<Integer, SupplyMeta> getSuppliesMeta(
            Trial trial,
            Difficulty difficulty,
            RouteVariant variant
    )
    {
        String key = trial.name() + "-" + difficulty.name() + "-" + variant.name();

        return SUPPLIES_CACHE.computeIfAbsent(key, k -> {
            String path = RouteResources.buildSuppliesPath(trial, difficulty, variant);
            Map<Integer, SupplyMeta> map = loadSuppliesMeta(path);

            // Merge into global objectId -> meta map so hasMetaForId works
            ALL_SUPPLIES_META.putAll(map);

            return map;
        });
    }

    // JSON mapping
    private static final class SupplyPoint
    {
        int varbit;
        int objectId;
        int order;
        int index;
    }

    private static final class SupplyMeta
    {
        final int varbit;
        final int objectId;
        final int order;
        final int index;

        SupplyMeta(int varbit, int objectId, int order, int index)
        {
            this.varbit = varbit;
            this.objectId = objectId;
            this.order = order;
            this.index = index;
        }
    }

    private static Map<Integer, SupplyMeta> loadSuppliesMeta(String resourcePath)
    {
        InputStream in = LostSuppliesOverlay.class.getResourceAsStream(resourcePath);
        if (in == null)
        {
            return Collections.emptyMap();
        }

        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
        {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<SupplyPoint>>(){}.getType();
            List<SupplyPoint> points = gson.fromJson(reader, listType);

            if (points == null)
            {
                return Collections.emptyMap();
            }

            return points.stream()
                    .collect(Collectors.toMap(
                            p -> p.objectId,
                            p -> new SupplyMeta(p.varbit, p.objectId, p.order, p.index)
                    ));
        }
        catch (Exception e)
        {
            return Collections.emptyMap();
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
