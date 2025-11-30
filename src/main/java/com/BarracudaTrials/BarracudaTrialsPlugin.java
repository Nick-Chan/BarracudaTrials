/*
BarracudaTrialsPlugin
Plugin for Sailing Barracuda Trials including:
 - Routes
 - Timer
Created by Vanik
Initial date: 28/11/2025
*/

package com.BarracudaTrials;

import com.BarracudaTrials.config.Config;
import com.BarracudaTrials.model.Ship;
import com.BarracudaTrials.overlay.CrystalMoteOverlay;
import com.BarracudaTrials.overlay.LostSuppliesOverlay;
import com.BarracudaTrials.overlay.RouteOverlay;
import com.BarracudaTrials.overlay.SpeedBoostOverlay;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;
import net.runelite.api.GameObject;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.WorldViewUnloaded;


// debug

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@PluginDescriptor(
        name = "Barracuda Trials",
        enabledByDefault = false,
        description = "Shows a route overlay for Sailing Barracuda Trials"
)
public class BarracudaTrialsPlugin extends Plugin
{
    // Trial state & per-run config
    private int currentSplit = 0;
    private int lastTimeStart = 0;
    private int lastCompletedCount = 0;
    private int currentRouteOrder = 1;
    private boolean trialRunning = false;
    private int totalSplitsThisRun = 0;
    private int trialTypeThisRun = 0;
    private boolean seenCrystal7 = false;
    private boolean advancedAfterBox = false;
    private int lastBoxVarbitValue = -1;

    // Gwenith Glide IDs
    private static final int VARBIT_SAILING_BT_IN_TRIAL_GWENITH_GLIDE = 18410;
    private static final int VARP_SAILING_BT_TIME_GWENITH_GLIDE_START = 4987;
    private static final int VARP_SAILING_BT_TRIAL_GWENITH_GLIDE_COMPLETED = 5000;
    private static final int VARBIT_LOST_SUPPLY_TRIGGER = 18524;

    // Lost Supplies currently in the scene
    private final Set<GameObject> lostSupplies = new HashSet<>();

    // Speed boost
    private Ship ship;
    private int speedBoostTicksRemaining = 0;
    private int speedBoostTicksMax = 0;
    private static final int ICON_ID_LUFF = 7075;
    private static final String CHAT_LUFF_SAIL = "You trim the sails, catching the wind for a burst of speed!";
    private static final String CHAT_LUFF_STORED = "You release the wind mote for a burst of speed!";

    public Set<GameObject> getLostSupplies()
    {
        return lostSupplies;
    }

    public int getCurrentRouteOrder()
    {
        return currentRouteOrder;
    }

    public int getSpeedBoostTicksRemaining()
    {
        return speedBoostTicksRemaining;
    }

    public int getSpeedBoostTicksMax()
    {
        return speedBoostTicksMax;
    }

    @Inject
    private Client client;

    @Inject
    private Config config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private RouteOverlay routeOverlay;

    @Inject
    private LostSuppliesOverlay lostSuppliesOverlay;

    @Inject
    private CrystalMoteOverlay crystalMoteOverlay;

    @Inject
    private SpeedBoostOverlay speedBoostOverlay;

    @Provides
    Config provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(Config.class);
    }

    @Override
    protected void startUp()
    {
        overlayManager.add(routeOverlay);
        overlayManager.add(lostSuppliesOverlay);
        overlayManager.add(crystalMoteOverlay);
        overlayManager.add(speedBoostOverlay);

        ship = null;
        speedBoostTicksRemaining = 0;
        speedBoostTicksMax = 0;
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(routeOverlay);
        overlayManager.remove(lostSuppliesOverlay);
        overlayManager.remove(crystalMoteOverlay);
        overlayManager.remove(speedBoostOverlay);

        ship = null;
        speedBoostTicksRemaining = 0;
        speedBoostTicksMax = 0;
    }

    @Subscribe
    public void onWorldViewUnloaded(WorldViewUnloaded e)
    {
        if (e.getWorldView().isTopLevel())
        {
            lostSupplies.clear();
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned e)
    {
        GameObject o = e.getGameObject();

        // Only track objects we know about from the Lost Supplies overlay metadata
        if (LostSuppliesOverlay.hasMetaForId(o.getId()))
        {
            lostSupplies.add(o);
        }

        if (client.getLocalPlayer() == null)
        {
            return;
        }

        int wvId = client.getLocalPlayer().getWorldView().getId();

        if (ship == null || ship.getWorldViewId() != wvId)
        {
            ship = new Ship(wvId);
        }

        ship.updateFromGameObject(e.getGameObject());
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned e)
    {
        GameObject gone = e.getGameObject();

        // Remove any tracked object at the same location with the same id
        lostSupplies.removeIf(o ->
                o.getId() == gone.getId()
                        && o.getWorldLocation().equals(gone.getWorldLocation())
        );

        if (ship != null)
        {
            ship.removeGameObject(e.getGameObject());
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        int timeStart = client.getVarpValue(VARP_SAILING_BT_TIME_GWENITH_GLIDE_START);
        int completedCount = client.getVarpValue(VARP_SAILING_BT_TRIAL_GWENITH_GLIDE_COMPLETED);
        int trialTypeVar = client.getVarbitValue(VARBIT_SAILING_BT_IN_TRIAL_GWENITH_GLIDE);

        // Start of trial
        if (!trialRunning && lastTimeStart == 0 && timeStart != 0)
        {
            trialRunning = true;
            currentSplit = 0;

            lastCompletedCount = completedCount;
            trialTypeThisRun = trialTypeVar;

            currentRouteOrder = 1;

            seenCrystal7 = false;
            advancedAfterBox = false;
            lastBoxVarbitValue = client.getVarbitValue(VARBIT_LOST_SUPPLY_TRIGGER);

            switch (trialTypeThisRun)
            {
                case 2: // Swordfish
                    totalSplitsThisRun = 5;
                    break;
                case 3: // Shark
                    totalSplitsThisRun = 7;
                    break;
                case 4: // Marlin
                    totalSplitsThisRun = 10;
                    break;
                default:
                    totalSplitsThisRun = 0;
                    break;
            }
        }

        // End/reset of trial
        if (trialRunning && lastTimeStart != 0 && timeStart == 0)
        {
            trialRunning = false;
            currentSplit = 0;
            trialTypeThisRun = 0;
            totalSplitsThisRun = 0;
            currentRouteOrder = 1;

            seenCrystal7 = false;
            advancedAfterBox = false;
            lastBoxVarbitValue = -1;

            speedBoostTicksRemaining = 0;
            speedBoostTicksMax = 0;
        }

        lastTimeStart = timeStart;
        lastCompletedCount = completedCount;

        // Speed boost countdown
        if (speedBoostTicksRemaining > 0)
        {
            speedBoostTicksRemaining--;
        }

    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        /*
        int id = event.getVarbitId();
        int value = event.getValue();

        client.addChatMessage(
                ChatMessageType.GAMEMESSAGE,
                "",
                "Varbit changed during trial: id=" + id + " value=" + value,
                null
        );*/

        if (!trialRunning)
        {
            return;
        }

        if (event.getVarbitId() != VARBIT_LOST_SUPPLY_TRIGGER)
        {
            return;
        }

        int newVal = event.getValue();

        if (seenCrystal7 && !advancedAfterBox && lastBoxVarbitValue == 1 && newVal == 0)
        {
            currentRouteOrder++;
            advancedAfterBox = true;
        }

        lastBoxVarbitValue = newVal;
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE)
        {
            return;
        }

        String message = Text.removeTags(event.getMessage());

        // Speed boost trigger messages
        if (CHAT_LUFF_SAIL.equals(message) || CHAT_LUFF_STORED.equals(message))
        {
            int durationTicks = -1;
            if (ship != null && ship.isValid())
            {
                durationTicks = ship.getSpeedBoostDurationTicks();
            }
            if (durationTicks <= 0)
            {
                durationTicks = 1; // fallback
            }

            speedBoostTicksRemaining = durationTicks;
            speedBoostTicksMax = durationTicks;
        }

        // Gwenith Glide
        if (!message.startsWith("You imbue the Crystal of "))
        {
            return;
        }

        if (!trialRunning)
        {
            return;
        }

        // Extract "(x/8)" part
        int openIdx = message.indexOf('(');
        int slashIdx = message.indexOf('/', openIdx);
        int closeIdx = message.indexOf(')', slashIdx);

        if (openIdx == -1 || slashIdx == -1 || closeIdx == -1)
        {
            return;
        }

        int imbueNumber;
        try
        {
            String numStr = message.substring(openIdx + 1, slashIdx);
            imbueNumber = Integer.parseInt(numStr);
        }
        catch (NumberFormatException e)
        {
            return;
        }

        if (imbueNumber < 1 || imbueNumber > 8 || imbueNumber <= currentSplit)
        {
            return;
        }

        currentSplit = imbueNumber;

        if (imbueNumber <= 6)
        {
            currentRouteOrder++;
        }
        else if (imbueNumber == 7)
        {
            currentRouteOrder++;
            seenCrystal7 = true;
            advancedAfterBox = false;
            lastBoxVarbitValue = client.getVarbitValue(VARBIT_LOST_SUPPLY_TRIGGER);
        }
    }

    // Overlay
    public boolean isTrialRunning()
    {
        return trialRunning;
    }

    public int getCurrentSplit()
    {
        return currentSplit;
    }

    public int getTrialTypeThisRun()
    {
        return trialTypeThisRun;
    }

    public int getTotalSplitsThisRun()
    {
        return totalSplitsThisRun;
    }
}
