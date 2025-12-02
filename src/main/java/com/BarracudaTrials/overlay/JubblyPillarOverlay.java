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

        // Load JSON pillar route
        List<JubblyPillarData.PillarDef> route =
                JubblyPillarData.getPillars(Trial.JUBBLY_JIVE, difficulty, variant);

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
        graphics.setColor(fill);
        graphics.fill(new Area(hull));
        graphics.setColor(outline);
        graphics.draw(hull);

        // Pillar number
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
