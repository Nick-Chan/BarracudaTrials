package com.BarracudaTrials.overlay;

import com.BarracudaTrials.BarracudaTrialsPlugin;
import com.BarracudaTrials.config.Config;
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

    // Metadata per objectId loaded from JSON: objectId -> SupplyMeta
    private static final Map<Integer, SupplyMeta> MARLIN_SUPPLIES_META =
            loadSuppliesMeta("/routes/thegwenithglide_marlin_wiki_supplies.json");

    static boolean hasMetaForId(int objectId)
    {
        return MARLIN_SUPPLIES_META.containsKey(objectId);
    }

    private SupplyMeta getSupplyMetaForObject(int objectId)
    {
        return MARLIN_SUPPLIES_META.get(objectId);
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
        if (!plugin.isTrialRunning() || !config.highlightLostSupplies())
        {
            return null;
        }

        int currentOrder = plugin.getCurrentRouteOrder();

        for (GameObject o : plugin.getLostSupplies())
        {
            SupplyMeta meta = getSupplyMetaForObject(o.getId());
            if (meta == null)
            {
                continue;
            }

            // Only crates belonging to this route order
            if (meta.order != currentOrder)
            {
                continue;
            }

            int objectiveIndex = meta.varbit;
            if (objectiveIndex < 0 || objectiveIndex >= 96)
            {
                continue;
            }

            int objectiveVarbitId = VARBIT_SAILING_BT_OBJECTIVE_BASE + objectiveIndex;
            int objectiveState = client.getVarbitValue(objectiveVarbitId);

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
}
