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
import com.BarracudaTrials.model.Trial;
import com.BarracudaTrials.model.Difficulty;
import com.BarracudaTrials.model.RouteVariant;
import com.BarracudaTrials.overlay.*;
import com.BarracudaTrials.util.JubblyPillarData;
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
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.WorldViewUnloaded;

// debug

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
    private Trial currentTrial = null;
    private boolean inTrial = false;

    public boolean isInTrial()
    {
        return inTrial;
    }

    // Gwenith Glide IDs
    private static final int VARBIT_SAILING_BT_IN_TRIAL = 18410;

    private static final int VARP_SAILING_BT_TIME_START = 4987;
    private static final int VARP_SAILING_BT_TRIAL_COMPLETED = 5000;
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

    // Jubbly Jive pillar route state
    private List<JubblyPillarData.PillarDef> jubblyPillarRoute = Collections.emptyList();
    private int currentJubblyPillarOrder = 0;

    private final java.util.Map<Integer, GameObject> jubblyPillarObjects = new java.util.HashMap<>();

    public GameObject getJubblyPillarObject(int pillarIndex)
    {
        return jubblyPillarObjects.get(pillarIndex);
    }

    private static final java.util.Map<Integer, Integer> PILLAR_OBJECT_TO_INDEX =
            new java.util.HashMap<>();

    static
    {
        PILLAR_OBJECT_TO_INDEX.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_1_PARENT, 1);
        PILLAR_OBJECT_TO_INDEX.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_2_PARENT, 2);
        PILLAR_OBJECT_TO_INDEX.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_3_PARENT, 3);
        PILLAR_OBJECT_TO_INDEX.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_4_PARENT, 4);
        PILLAR_OBJECT_TO_INDEX.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_5_PARENT, 5);
        PILLAR_OBJECT_TO_INDEX.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_6_PARENT, 6);
        PILLAR_OBJECT_TO_INDEX.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_7_PARENT, 7);
    }

    private static final int JUBBLY_BOAT_OBJECT_ID = 59170;
    private GameObject jubblyBoatObject;
    private boolean jubblyBoatHighlightActive = false;

    public GameObject getJubblyBoatObject()
    {
        return jubblyBoatObject;
    }

    public boolean shouldHighlightJubblyBoat()
    {
        return jubblyBoatHighlightActive && jubblyBoatObject != null;
    }

    public int getCurrentJubblyPillarOrder()
    {
        return currentJubblyPillarOrder;
    }

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

    @Inject
    private RapidsOverlay rapidsOverlay;

    @Inject
    private JubblyPillarOverlay pillarOverlay;

    @Inject
    private JubblyBoatOverlay jubblyBoatOverlay;

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
        overlayManager.add(rapidsOverlay);
        overlayManager.add(pillarOverlay);
        overlayManager.add(jubblyBoatOverlay);

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
        overlayManager.remove(rapidsOverlay);
        overlayManager.remove(pillarOverlay);
        overlayManager.remove(jubblyBoatOverlay);

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
            jubblyBoatObject = null;
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned e)
    {
        GameObject o = e.getGameObject();

        // DEBUG: log *any* 59170
        if (o.getId() == 59170)
        {
            client.addChatMessage(
                    ChatMessageType.GAMEMESSAGE,
                    "",
                    "DEBUG: Jubbly boat candidate spawned id=59170 at " +
                            o.getWorldLocation(),
                    null
            );
        }

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

        Integer pillarIdx = PILLAR_OBJECT_TO_INDEX.get(o.getId());
        if (pillarIdx != null)
        {
            jubblyPillarObjects.put(pillarIdx, o);
        }

        // DEBUG tracking for boat
        if (o.getId() == 59170)
        {
            jubblyBoatObject = o;
            client.addChatMessage(
                    ChatMessageType.GAMEMESSAGE,
                    "",
                    "DEBUG: jubblyBoatObject set",
                    null
            );
        }

        if (o.getId() == JUBBLY_BOAT_OBJECT_ID)
        {
            jubblyBoatObject = o;
        }
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

        Integer idx = PILLAR_OBJECT_TO_INDEX.get(gone.getId());
        if (idx != null && jubblyPillarObjects.get(idx) == gone)
        {
            jubblyPillarObjects.remove(idx);
        }

        if (jubblyBoatObject == gone)
        {
            jubblyBoatObject = null;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        int timeStart = client.getVarpValue(VARP_SAILING_BT_TIME_START);
        int completedCount = client.getVarpValue(VARP_SAILING_BT_TRIAL_COMPLETED);
        int trialTypeVar = client.getVarbitValue(VARBIT_SAILING_BT_IN_TRIAL);

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

            jubblyBoatHighlightActive = currentTrial == Trial.JUBBLY_JIVE;
        }

        // End/reset of trial
        if (trialRunning && lastTimeStart != 0 && timeStart == 0)
        {
            trialRunning = false;
            inTrial = false;
            currentTrial = null;

            currentSplit = 0;
            trialTypeThisRun = 0;
            totalSplitsThisRun = 0;
            currentRouteOrder = 1;

            seenCrystal7 = false;
            advancedAfterBox = false;
            lastBoxVarbitValue = -1;

            speedBoostTicksRemaining = 0;
            speedBoostTicksMax = 0;

            jubblyPillarRoute = Collections.emptyList();
            currentJubblyPillarOrder = 0;
            jubblyBoatHighlightActive = false;
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
        // Jubbly Jive pillars
        int varbitId = event.getVarbitId();
        int newVal = event.getValue();

        if (inTrial
                && currentTrial == Trial.JUBBLY_JIVE
                && currentJubblyPillarOrder > 0
                && !jubblyPillarRoute.isEmpty())
        {
            // Find the currently highlighted pillar
            JubblyPillarData.PillarDef current = jubblyPillarRoute.stream()
                    .filter(p -> p.order == currentJubblyPillarOrder)
                    .findFirst()
                    .orElse(null);

            if (current != null && varbitId == current.varbitId)
            {
                // Varbit 2+ = done
                if (newVal >= 2)
                {
                    int idx = jubblyPillarRoute.indexOf(current);

                    if (idx >= 0 && idx + 1 < jubblyPillarRoute.size())
                    {
                        // Move to next pillar
                        currentJubblyPillarOrder = jubblyPillarRoute.get(idx + 1).order;
                    }
                    else
                    {
                        // No more pillars
                        currentJubblyPillarOrder = 0;
                    }
                }
            }
        }

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

        // Current trial detect
        if (message.equals("You prepare to begin the Gwenith Glide...") || message.equals("You reset your progress in the Gwenith Glide."))
        {
            currentTrial = Trial.GWENITH_GLIDE;
            inTrial = true;
        }
        else if (message.equals("You prepare to begin the Jubbly Jive...") || message.equals("You reset your progress in the Jubbly Jive."))
        {
            currentTrial = Trial.JUBBLY_JIVE;
            inTrial = true;
            jubblyBoatHighlightActive = true;
        }
        else if (message.equals("You prepare to begin the Tempor Tantrum...") || message.equals("You reset your progress in the Tempor Tantrum."))
        {
            currentTrial = Trial.TEMPOR_TANTRUM;
            inTrial = true;
        }

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

        if (message.equals("You successfully lure a jubby to Gurtob!"))
        {
            if (currentTrial == Trial.JUBBLY_JIVE && inTrial)
            {
                jubblyBoatHighlightActive = true;
            }
        }

        if (message.endsWith("balloon toads. Time to lure some jubblies!"))
        {
            if (currentTrial == Trial.JUBBLY_JIVE)
            {
                Difficulty diff = getCurrentDifficulty();
                if (diff != null)
                {
                    RouteVariant variant;
                    switch (diff)
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

                    jubblyPillarRoute =
                            JubblyPillarData.getPillars(Trial.JUBBLY_JIVE, diff, variant);

                    if (jubblyPillarRoute.isEmpty())
                    {
                        currentJubblyPillarOrder = 0;
                    }
                    else
                    {
                        // highlight the first pillar
                        recalcCurrentJubblyPillarOrder();
                    }
                }
            }
        }


        // Gwenith Glide
        if (currentTrial == Trial.GWENITH_GLIDE)
        {
            // Original Gwenith Glide logic
            if (!message.startsWith("You imbue the Crystal of "))
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
        else if (currentTrial == Trial.JUBBLY_JIVE)
        {
            if (!message.endsWith("balloon toads. Time to lure some jubblies!"))
            {
                return;
            }

            jubblyBoatHighlightActive = false;

            currentSplit++;
            currentRouteOrder++;
        }
    }

    private void recalcCurrentJubblyPillarOrder()
    {
        if (!inTrial || currentTrial != Trial.JUBBLY_JIVE || jubblyPillarRoute.isEmpty())
        {
            currentJubblyPillarOrder = 0;
            return;
        }

        // Find the first pillar in the route whose varbit state is NOT fully done
        // With states: 0 = inactive, 1 = active, 2 = clicked, 3 = jubbly in tree
        // We treat 2 & 3 as "done" and 0 or 1 as "next/active target".
        for (JubblyPillarData.PillarDef def : jubblyPillarRoute)
        {
            int state = client.getVarbitValue(def.varbitId);
            if (state <= 1) // 0 or 1 -> not finished yet
            {
                currentJubblyPillarOrder = def.order;
                return;
            }
        }

        currentJubblyPillarOrder = 0;
    }


    // Trial / Difficulty for overlays
    public Trial getCurrentTrial()
    {
        if (!inTrial)
        {
            return null;
        }

        return currentTrial;
    }

    public Difficulty getCurrentDifficulty()
    {
        if (!inTrial)
        {
            return null;
        }

        switch (trialTypeThisRun)
        {
            case 2:
                return Difficulty.SWORDFISH;
            case 3:
                return Difficulty.SHARK;
            case 4:
                return Difficulty.MARLIN;
            default:
                return null;
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
