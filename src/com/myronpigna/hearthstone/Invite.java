/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myronpigna.hearthstone;

import com.myronpigna.hearthstone.PlayerData.Cooldown;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * @author myron
 */
public class Invite implements Comparable<Invite>
{

    private PlayerData sender;
    private PlayerData reciever;
    private Location location;
    private String locationName;

    public Invite(PlayerData sender, PlayerData reciever, Location location, String locationName)
    {
        this.sender = sender;
        this.reciever = reciever;
        this.location = location;
        this.locationName = locationName;
    }

    public Player getSender()
    {
        return sender.getPlayer();
    }

    public Player getReciever()
    {
        return reciever.getPlayer();
    }

    public Location getLocation()
    {
        return location;
    }

    public String getLocationName()
    {
        return locationName;
    }

    public void InviteExpired()
    {
        reciever.sendMessage(getSender().getName() + "'s invite to " + locationName + " has expired.");
        sender.sendMessage("Your invite to " + getReciever().getName() + " has expired.");
    }

    public void InviteAccepted(boolean override)
    {
        reciever.sendMessage("You have accepted the HearthStone invite from " + getSender().getName() + ".");
        sender.sendMessage(getReciever().getName() + " has accepted your HearthStone invite.");
        reciever.teleportPlayerLocation(locationName, location, Cooldown.ACCEPTED, override);
        sender.SetCooldown(Cooldown.INVITE);
    }

    public void InviteDeclined()
    {
        reciever.sendMessage("You have declined the HearthStone invite from " + getSender().getName() + ".");
        sender.sendMessage(getReciever().getName() + " has declined your HearthStone invite.");
    }

    @Override
    public int compareTo(Invite o)
    {
        if (!getSender().getName().equals(o.getSender().getName())) return -1;
        if (!getReciever().getName().equals(o.getReciever().getName())) return -1;
        if (getLocation().equals(o.getLocation())) return -1;
        if (!getLocationName().equals(o.getLocationName())) return -1;
        return 1;
    }
}
