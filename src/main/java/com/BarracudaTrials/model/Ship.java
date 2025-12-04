package com.BarracudaTrials.model;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

@Getter
public class Ship
{
    private final int worldViewId;

    @Setter
    private GameObject hull;

    @Setter
    private GameObject sail;

    @Setter
    private ShipType shipType;

    @Setter
    private SailType sailType;

    public Ship(int worldViewId)
    {
        this.worldViewId = worldViewId;
    }

    public void updateFromGameObject(GameObject obj)
    {
        // ship type
        ShipType ship = ShipType.fromGameObjectId(obj.getId());
        if (ship != null)
        {
            this.shipType = ship;
            this.hull = obj;
        }

        // sail type
        SailType sail = SailType.fromGameObjectId(obj.getId());
        if (sail != null)
        {
            this.sailType = sail;
            this.sail = obj;
        }
    }

    public void removeGameObject(GameObject obj)
    {
        if (hull == obj)
        {
            hull = null;
            shipType = null;
        }
        if (sail == obj)
        {
            sail = null;
            sailType = null;
        }
    }

    public int getSpeedBoostDurationTicks()
    {
        if (sailType != null && shipType != null)
        {
            return sailType.getSpeedBoostDuration(shipType);
        }
        return -1;
    }

    public LocalPoint getCenterLocalPoint()
    {
        if (hull == null)
        {
            return null;
        }
        return hull.getLocalLocation();
    }

    public WorldPoint getCenterWorldPoint(Client client)
    {
        LocalPoint lp = getCenterLocalPoint();
        if (lp == null)
        {
            return null;
        }
        return WorldPoint.fromLocal(client, lp);
    }

    public boolean isValid()
    {
        return hull != null && shipType != null && sail != null && sailType != null;
    }
}
