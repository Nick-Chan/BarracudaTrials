package com.BarracudaTrials.model;

import lombok.Getter;
import net.runelite.api.gameval.ObjectID;

@Getter
public enum SailType
{
    WOOD(
            new int[]{
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_WOOD,
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_WOOD,
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_WOOD,
            },
            20, 20, 20
    ),
    OAK(
            new int[]{
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_OAK,
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_OAK,
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_OAK,
            },
            22, 22, 22
    ),
    TEAK(
            new int[]{
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_TEAK,
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_TEAK,
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_TEAK,
            },
            24, 24, 24
    ),
    MAHOGANY(
            new int[]{
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_MAHOGANY,
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_MAHOGANY,
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_MAHOGANY,
            },
            26, 27, 27
    ),
    CAMPHOR(
            new int[]{
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_CAMPHOR,
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_CAMPHOR,
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_CAMPHOR,
            },
            30, 30, 30
    ),
    IRONWOOD(
            new int[]{
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_IRONWOOD,
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_IRONWOOD,
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_IRONWOOD,
            },
            33, 33, 33
    ),
    ROSEWOOD(
            new int[]{
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_ROSEWOOD,
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_ROSEWOOD,
                    ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_ROSEWOOD,
            },
            36, 36, 36
    );

    private final int[] gameObjectIds;

    private final int[] speedBoostDurations;

    SailType(int[] gameObjectIds, int raftDuration, int skiffDuration, int sloopDuration)
    {
        this.gameObjectIds = gameObjectIds;
        this.speedBoostDurations = new int[]{ raftDuration, skiffDuration, sloopDuration };
    }

    public static SailType fromGameObjectId(int id)
    {
        for (SailType type : values())
        {
            for (int objectId : type.gameObjectIds)
            {
                if (objectId == id)
                {
                    return type;
                }
            }
        }
        return null;
    }

    public int getSpeedBoostDuration(ShipType shipType)
    {
        if (shipType == null)
        {
            return -1;
        }

        int idx = shipType.ordinal(); // RAFT=0, SKIFF=1, SLOOP=2
        if (idx < 0 || idx >= speedBoostDurations.length)
        {
            return -1;
        }

        return speedBoostDurations[idx];
    }
}
