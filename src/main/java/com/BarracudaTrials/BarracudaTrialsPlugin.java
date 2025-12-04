/*
BarracudaTrialsPlugin
Plugin for Sailing Barracuda Trials including:
 - Routes
 - Pillar overlay
 - Collect boat overlay
 - Lost supply overlay
 - Crystal mote overlay
 - Speed boost overlay
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
import com.google.gson.Gson;
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

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@PluginDescriptor(
        name = "Barracuda Trials",
        enabledByDefault = false,
        description = "Shows a route overlay for Sailing Barracuda Trials & other helpful overlays"
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

    // Gwenith Glide
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

    // Jubbly Jive
    private static final int VARBIT_LOST_SUPPLY_TRIGGER_JJ1 = 18468;
    private static final int VARBIT_LOST_SUPPLY_TRIGGER_JJ2 = 18484;
    private static final int VARBIT_LOST_SUPPLY_TRIGGER_JJ3 = 18469;

    private List<JubblyPillarData.PillarDef> jubblyPillarRoute = Collections.emptyList();
    private int currentJubblyPillarOrder = 0;
    private int jubblyRound = 0;
    private int jubblyPillarIndex = 1;

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

    private static final int[] JUBBLY_BOAT_IDS = {
            ObjectID.SAILING_BT_JUBBLY_JIVE_TOAD_SUPPLIES_PARENT,
            ObjectID.SAILING_BT_JUBBLY_JIVE_TOAD_SUPPLIES_CHILD,
            ObjectID.SAILING_BT_JUBBLY_JIVE_TOAD_SUPPLIES_CHILD_NOOP
    };

    private static boolean isJubblyBoatId(int id)
    {
        for (int v : JUBBLY_BOAT_IDS)
        {
            if (v == id)
            {
                return true;
            }
        }
        return false;
    }

    private GameObject jubblyBoatObject;
    private boolean jubblyBoatHighlightActive = false;

    // Tempor Tantrum
    // North boat
    private static final int[] TEMPOR_NORTH_BOAT_IDS = {
            ObjectID.SAILING_BT_TEMPOR_TANTRUM_NORTH_LOC_PARENT,
            ObjectID.SAILING_BT_TEMPOR_TANTRUM_NORTH_LOC_CHILD,
            ObjectID.SAILING_BT_TEMPOR_TANTRUM_NORTH_LOC_CHILD_NOOP
    };

    // South boat
    private static final int[] TEMPOR_SOUTH_BOAT_IDS = {
            ObjectID.SAILING_BT_TEMPOR_TANTRUM_SOUTH_LOC_PARENT,
            ObjectID.SAILING_BT_TEMPOR_TANTRUM_SOUTH_LOC_CHILD,
            ObjectID.SAILING_BT_TEMPOR_TANTRUM_SOUTH_LOC_CHILD_NOOP
    };

    private static boolean isTemporNorthBoatId(int id)
    {
        for (int v : TEMPOR_NORTH_BOAT_IDS)
        {
            if (v == id)
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isTemporSouthBoatId(int id)
    {
        for (int v : TEMPOR_SOUTH_BOAT_IDS)
        {
            if (v == id)
            {
                return true;
            }
        }
        return false;
    }

    private GameObject temporNorthBoat;
    private GameObject temporSouthBoat;
    private boolean temporHighlightNorth;
    private boolean temporHighlightSouth;

    public GameObject getTemporNorthBoat()
    {
        return temporNorthBoat;
    }

    public GameObject getTemporSouthBoat()
    {
        return temporSouthBoat;
    }

    public boolean shouldHighlightTemporNorthBoat()
    {
        return temporHighlightNorth && temporNorthBoat != null;
    }

    public boolean shouldHighlightTemporSouthBoat()
    {
        return temporHighlightSouth && temporSouthBoat != null;
    }

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
    private Gson gson;

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

    @Inject
    private JubblyPillarData jubblyPillarData;

    @Inject
    private TemporBoatOverlay temporBoatOverlay;

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
        overlayManager.add(temporBoatOverlay);

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
        overlayManager.remove(temporBoatOverlay);

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

        // Lost supplies
        lostSupplies.add(o);

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

        // Jubbly boat
        if (isJubblyBoatId(o.getId()))
        {
            jubblyBoatObject = o;
        }

        // Jubbly pilars
        Integer pillarIdx = PILLAR_OBJECT_TO_INDEX.get(o.getId());
        if (pillarIdx != null)
        {
            jubblyPillarObjects.put(pillarIdx, o);
        }

        // Tempor Tantrum boats
        if (isTemporNorthBoatId(o.getId()))
        {
            temporNorthBoat = o;
        }
        if (isTemporSouthBoatId(o.getId()))
        {
            temporSouthBoat = o;
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

        // Collection boats
        if (jubblyBoatObject == gone)
        {
            jubblyBoatObject = null;
        }
        if (temporNorthBoat == gone)
        {
            temporNorthBoat = null;
        }
        if (temporSouthBoat == gone)
        {
            temporSouthBoat = null;
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

            if (currentTrial == Trial.TEMPOR_TANTRUM)
            {
                temporHighlightSouth = true;
                temporHighlightNorth = false;
            }
            else
            {
                temporHighlightSouth = false;
                temporHighlightNorth = false;
            }
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
            jubblyRound = 0;
            jubblyPillarIndex = 0;

            temporHighlightNorth = false;
            temporHighlightSouth = false;
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
                && jubblyPillarIndex >= 0
                && !jubblyPillarRoute.isEmpty())
        {
            JubblyPillarData.PillarDef current = jubblyPillarRoute.get(jubblyPillarIndex);

            if (varbitId == current.varbitId)
            {
                if (newVal >= 2)
                {
                    jubblyPillarIndex++;

                    int maxOrderForThisWave;
                    Difficulty diff = getCurrentDifficulty();

                    if (diff == Difficulty.MARLIN && jubblyRound == 1)
                    {
                        maxOrderForThisWave = 9;
                    }
                    else
                    {
                        maxOrderForThisWave = Integer.MAX_VALUE;
                    }

                    if (jubblyPillarIndex < jubblyPillarRoute.size())
                    {
                        JubblyPillarData.PillarDef next = jubblyPillarRoute.get(jubblyPillarIndex);

                        if (next.order <= maxOrderForThisWave)
                        {
                            currentJubblyPillarOrder = next.order;
                        }
                        else
                        {
                            currentJubblyPillarOrder = 0;
                            jubblyPillarIndex = -1;
                        }
                    }
                    else
                    {
                        currentJubblyPillarOrder = 0;
                        jubblyPillarIndex = -1;
                    }
                }
            }

            Difficulty currentDifficulty = getCurrentDifficulty();
            if (currentDifficulty == Difficulty.SHARK && varbitId == VARBIT_LOST_SUPPLY_TRIGGER_JJ1)
            {
                currentSplit++;
                currentRouteOrder++;
            }
            if (currentDifficulty == Difficulty.MARLIN && varbitId == VARBIT_LOST_SUPPLY_TRIGGER_JJ2)
            {
                currentSplit++;
                currentRouteOrder++;
            }
            if (currentDifficulty == Difficulty.MARLIN && varbitId == VARBIT_LOST_SUPPLY_TRIGGER_JJ3)
            {
                currentSplit++;
                currentRouteOrder++;
            }
        }

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

        if (message.startsWith("You successfully lure a jubbly to Gurtob"))
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

                    // Load the pillar route once
                    if (jubblyPillarRoute.isEmpty())
                    {
                        jubblyPillarRoute = jubblyPillarData.getPillars(Trial.JUBBLY_JIVE, diff, variant);
                    }

                    if (jubblyPillarRoute.isEmpty())
                    {
                        currentJubblyPillarOrder = 0;
                        jubblyPillarIndex = -1;
                        return;
                    }

                    // Wave logic
                    if (diff == Difficulty.MARLIN)
                    {
                        if (jubblyRound < 2)
                        {
                            jubblyRound++;
                        }
                    }
                    else
                    {
                        jubblyRound = 1;
                    }

                    if (jubblyRound == 1)
                    {
                        jubblyPillarIndex = -1;
                        for (int i = 0; i < jubblyPillarRoute.size(); i++)
                        {
                            if (jubblyPillarRoute.get(i).order >= 1 &&
                                    jubblyPillarRoute.get(i).order <= 9)
                            {
                                jubblyPillarIndex = i;
                                break;
                            }
                        }
                    }
                    else
                    {
                        jubblyPillarIndex = -1;
                        for (int i = 0; i < jubblyPillarRoute.size(); i++)
                        {
                            if (jubblyPillarRoute.get(i).order >= 10 &&
                                    jubblyPillarRoute.get(i).order <= 18)
                            {
                                jubblyPillarIndex = i;
                                break;
                            }
                        }
                    }

                    if (jubblyPillarIndex >= 0)
                    {
                        currentJubblyPillarOrder = jubblyPillarRoute.get(jubblyPillarIndex).order;
                    }
                    else
                    {
                        currentJubblyPillarOrder = 0;
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
        else if (currentTrial == Trial.TEMPOR_TANTRUM)
        {
            if (message.startsWith("You collect the rum shipment"))
            {
                temporHighlightSouth = false;
                temporHighlightNorth = true;

                currentSplit++;
                currentRouteOrder++;
            }
            if (message.startsWith("You deliver the rum shipment"))
            {
                temporHighlightSouth = true;
                temporHighlightNorth = false;

                currentSplit++;
                currentRouteOrder++;
            }
        }
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

    public int getTrialTypeThisRun()
    {
        return trialTypeThisRun;
    }
}
