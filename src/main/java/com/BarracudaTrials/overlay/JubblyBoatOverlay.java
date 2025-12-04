package com.BarracudaTrials.overlay;

import com.BarracudaTrials.BarracudaTrialsPlugin;
import com.BarracudaTrials.config.Config;
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
public class JubblyBoatOverlay extends Overlay
{
    private final Client client;
    private final BarracudaTrialsPlugin plugin;
    private final Config config;

    @Inject
    public JubblyBoatOverlay(Client client,
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
        if (!plugin.shouldHighlightJubblyBoat())
        {
            return null;
        }

        GameObject boat = plugin.getJubblyBoatObject();
        if (boat == null)
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

        // Collect text
        LocalPoint lp = boat.getLocalLocation();
        if (lp != null)
        {
            net.runelite.api.Point textLoc =
                    Perspective.getCanvasTextLocation(client, graphics, lp, "Collect", 0);

            if (textLoc != null)
            {
                Color text = config.collectBoatColor();
                if (text == null)
                {
                    text = new Color(255, 215, 0, 160);
                }

                graphics.setColor(text);
                graphics.drawString("Collect", textLoc.getX(), textLoc.getY());
            }
        }

        return null;
    }
}
