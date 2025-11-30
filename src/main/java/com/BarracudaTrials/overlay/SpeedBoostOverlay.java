package com.BarracudaTrials.overlay;

import com.BarracudaTrials.BarracudaTrialsPlugin;
import com.BarracudaTrials.model.SpeedBoostDisplayMode;
import com.BarracudaTrials.config.Config;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.Player;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;

@Singleton
public class SpeedBoostOverlay extends Overlay
{
    private final Client client;
    private final BarracudaTrialsPlugin plugin;
    private final Config config;

    @Inject
    public SpeedBoostOverlay(Client client,
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
    public Dimension render(Graphics2D graphics)
    {
        if (!plugin.isTrialRunning() || !config.showSpeedBoostOverlay())
        {
            return null;
        }

        int remaining = plugin.getSpeedBoostTicksRemaining();
        int max = plugin.getSpeedBoostTicksMax();

        if (remaining <= 0 || max <= 0)
        {
            return null;
        }

        Player player = client.getLocalPlayer();
        if (player == null)
        {
            return null;
        }

        LocalPoint lp = player.getLocalLocation();
        if (lp == null)
        {
            return null;
        }

        net.runelite.api.Point base =
                Perspective.localToCanvas(client, lp, client.getPlane());
        if (base == null)
        {
            return null;
        }

        // Offset a bit above the player’s head
        int offsetY = 40;
        net.runelite.api.Point center =
                new net.runelite.api.Point(base.getX(), base.getY() - offsetY);

        SpeedBoostDisplayMode mode = config.speedBoostDisplayMode();
        switch (mode)
        {
            case PIE:
                renderPie(graphics, center, remaining, max);
                break;
            case TEXT:
                renderText(graphics, center, remaining);
                break;
        }

        return null;
    }

    private void renderPie(Graphics2D g, net.runelite.api.Point center, int remaining, int max)
    {
        float fraction = (float) remaining / (float) max;

        int radius = 15;
        int x = center.getX() - radius;
        int y = center.getY() - radius;
        int size = radius * 2;

        // Background circle
        g.setColor(new Color(0, 0, 0, 20));
        g.fillOval(x, y, size, size);

        // Pie arc
        int arcAngle = Math.round(-360 * fraction);
        Color boostColor = config.speedBoostColor();
        if (boostColor == null)
        {
            boostColor = new Color(0, 255, 0, 180);
        }
        g.setColor(boostColor);
        g.fillArc(x, y, size, size, 90, arcAngle);
    }

    private void renderText(Graphics2D g, net.runelite.api.Point center, int remaining)
    {
        String text = Integer.toString(remaining);
        Color boostColor = config.speedBoostColor();
        if (boostColor == null)
        {
            boostColor = Color.GREEN;
        }

        OverlayUtil.renderTextLocation(g, center, text, boostColor);
    }
}
