package com.BarracudaTrials.overlay;

import com.BarracudaTrials.BarracudaTrialsPlugin;
import com.BarracudaTrials.config.Config;
import com.BarracudaTrials.model.Difficulty;
import com.BarracudaTrials.model.RouteVariant;
import com.BarracudaTrials.model.Trial;
import com.BarracudaTrials.util.JubblyPillarData;
import com.google.common.collect.ImmutableMap;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.awt.geom.Area;
import java.util.List;
import java.util.Map;

@Singleton
public class JubblyPillarOverlay extends Overlay
{
    private final Client client;
    private final BarracudaTrialsPlugin plugin;
    private final Config config;

    private static final int JUBBLY_PILLAR_RANGE_RADIUS_TILES = 15;

    @Inject
    private JubblyPillarData jubblyPillarData;

    private static boolean isInRangeShape(int dx, int dy, int radius)
    {
        int ax = Math.abs(dx);
        int ay = Math.abs(dy);

        if (ax > radius || ay > radius)
        {
            return false;
        }

        // base
        int dist2 = ax * ax + ay * ay;
        if (dist2 <= radius * radius)
        {
            return true;
        }

        // vertical
        if (ax <= 1 && ay <= radius)
        {
            return true;
        }
        // horizontal
        if (ay <= 1 && ax <= radius)
        {
            return true;
        }

        return false;
    }

    private static final Map<Integer, Color> PILLAR_COLORS = ImmutableMap.<Integer, Color>builder()
            .put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_1_PARENT, new Color(0xC8C628))
            .put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_2_PARENT, new Color(0x752622))
            .put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_3_PARENT, new Color(0x213C64))
            .put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_4_PARENT, new Color(0xA77422))
            .put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_5_PARENT, new Color(0x589889))
            .put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_6_PARENT, new Color(0x8F5594))
            .put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_7_PARENT, new Color(0xA69FA9))
            .build();

    @Inject
    public JubblyPillarOverlay(Client client,
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
        if (!plugin.isInTrial())
        {
            return null;
        }

        if (!config.showPillars())
        {
            return null;
        }

        Trial trial = plugin.getCurrentTrial();
        Difficulty difficulty = plugin.getCurrentDifficulty();

        if (trial != Trial.JUBBLY_JIVE || difficulty == null)
        {
            return null;
        }

        // Only highlight after collecting toads
        int currentOrder = plugin.getCurrentJubblyPillarOrder();
        if (currentOrder <= 0)
        {
            return null;
        }

        RouteVariant variant;
        switch (difficulty)
        {
            case SWORDFISH:
                variant = config.jubblySwordfishVariant();
                break;
            case SHARK:
                variant = config.jubblySharkVariant();
                break;
            case MARLIN:
                variant = config.jubblyMarlinVariant();
                break;
            default:
                variant = RouteVariant.WIKI;
        }

        List<JubblyPillarData.PillarDef> route =
                jubblyPillarData.getPillars(Trial.JUBBLY_JIVE, difficulty, variant);

        if (route == null || route.isEmpty())
        {
            return null;
        }

        JubblyPillarData.PillarDef target = route.stream()
                .filter(p -> p.order == currentOrder)
                .findFirst()
                .orElse(null);

        if (target == null)
        {
            return null;
        }

        GameObject pillarObj = plugin.getJubblyPillarObject(target.pillar);
        if (pillarObj == null)
        {
            return null;
        }

        Shape hull = pillarObj.getConvexHull();
        if (hull == null)
        {
            return null;
        }

        Color outline = PILLAR_COLORS.getOrDefault(pillarObj.getId(), new Color(0xFFCC00));
        Color fill = new Color(0, 0, 0, 70);

        graphics.setStroke(new BasicStroke(2.0f));

        //Pillar
        graphics.setColor(fill);
        graphics.fill(new Area(hull));
        graphics.setColor(outline);
        graphics.draw(hull);

        // Range circle
        LocalPoint centerLp = pillarObj.getLocalLocation();
        if (centerLp != null && config.showPillarsRange())
        {
            Area rangeArea = new Area();
            int r = JUBBLY_PILLAR_RANGE_RADIUS_TILES;

            for (int dy = -r; dy <= r; dy++)
            {
                for (int dx = -r; dx <= r; dx++)
                {
                    if (!isInRangeShape(dx, dy, r))
                    {
                        continue;
                    }

                    LocalPoint tileLp = new LocalPoint(
                            centerLp.getX() + dx * Perspective.LOCAL_TILE_SIZE,
                            centerLp.getY() + dy * Perspective.LOCAL_TILE_SIZE
                    );

                    Polygon tilePoly = Perspective.getCanvasTilePoly(client, tileLp);
                    if (tilePoly != null)
                    {
                        rangeArea.add(new Area(tilePoly));
                    }
                }
            }

            Color rangeOutline = new Color(
                    outline.getRed(),
                    outline.getGreen(),
                    outline.getBlue(),
                    160 // alpha
            );

            //graphics.setColor(fill);
            //graphics.fill(rangeArea);
            graphics.setColor(rangeOutline);
            graphics.draw(rangeArea);
        }

        // Order number
        String text = Integer.toString(target.order);
        net.runelite.api.Point txt = pillarObj.getCanvasTextLocation(graphics, text, 0);
        if (txt != null)
        {
            graphics.setColor(Color.WHITE);
            graphics.drawString(text, txt.getX(), txt.getY());
        }

        return null;
    }
}
