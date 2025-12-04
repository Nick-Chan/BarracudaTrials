package com.BarracudaTrials.overlay;

import com.BarracudaTrials.BarracudaTrialsPlugin;
import com.BarracudaTrials.config.Config;
import com.BarracudaTrials.model.Trial;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;

@Singleton
public class TemporBoatOverlay extends Overlay
{
    private final Client client;
    private final BarracudaTrialsPlugin plugin;
    private final Config config;

    @Inject
    public TemporBoatOverlay(Client client,
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
        if (!plugin.isInTrial() || plugin.getCurrentTrial() != Trial.TEMPOR_TANTRUM)
        {
            return null;
        }

        if (!config.showCollectBoat())
        {
            return null;
        }

        GameObject boat = null;
        String label = null;

        if (plugin.shouldHighlightTemporSouthBoat())
        {
            boat = plugin.getTemporSouthBoat();
            label = "Collect";
        }
        else if (plugin.shouldHighlightTemporNorthBoat())
        {
            boat = plugin.getTemporNorthBoat();
            label = "Deliver";
        }

        if (boat == null || label == null)
        {
            return null;
        }

        Shape hull = boat.getConvexHull();
        if (hull == null)
        {
            return null;
        }

        Color outline = config.collectBoatColor();
        if (outline == null)
        {
            outline = new Color(255, 215, 0, 160);
        }

        Color fill = new Color(0, 0, 0, 70);

        graphics.setStroke(new BasicStroke(2.0f));
        graphics.setColor(fill);
        graphics.fill(hull);
        graphics.setColor(outline);
        graphics.draw(hull);

        // Label text ("Collect" / "Deliver")
        LocalPoint lp = boat.getLocalLocation();
        if (lp != null)
        {
            net.runelite.api.Point textLoc =
                    Perspective.getCanvasTextLocation(client, graphics, lp, label, 0);

            if (textLoc != null)
            {
                Color text = outline;
                graphics.setColor(text);
                graphics.drawString(label, textLoc.getX(), textLoc.getY());
            }
        }

        return null;
    }
}
