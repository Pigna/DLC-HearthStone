/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myronpigna.hearthstone;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * @author myron
 */
public class Invite implements Comparable<Invite>
{
    private PlayerData sender;
    private PlayerData receiver;
    private Location location;
    private String locationName;

    public Invite(PlayerData sender, PlayerData receiver, Location location, String locationName)
    {
        this.sender = sender;
        this.receiver = receiver;
        this.location = location;
        this.locationName = locationName;
    }

    public Player getSender()
    {
        return sender.getPlayer();
    }

    public Player getReceiver()
    {
        return receiver.getPlayer();
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
        receiver.sendMessage(getSender().getName() + "'s invite to " + locationName + " has expired.");
        sender.sendMessage("Your invite to " + getReceiver().getName() + " has expired.");
    }

    public void InviteAccepted(boolean override)
    {
        receiver.sendMessage("You have accepted the HearthStone invite from " + getSender().getName() + ".");
        sender.sendMessage(getReceiver().getName() + " has accepted your HearthStone invite.");
        receiver.teleportPlayerLocation(locationName, location, Cooldown.ACCEPTED, override);
        sender.SetCooldown(Cooldown.INVITE);
    }

    public void InviteDeclined()
    {
        receiver.sendMessage("You have declined the HearthStone invite from " + getSender().getName() + ".");
        sender.sendMessage(getReceiver().getName() + " has declined your HearthStone invite.");
    }

    @Override
    public int compareTo(Invite o)
    {
        if (!getSender().getName().equals(o.getSender().getName())) return -1;
        if (!getReceiver().getName().equals(o.getReceiver().getName())) return -1;
        if (getLocation().equals(o.getLocation())) return -1;
        if (!getLocationName().equals(o.getLocationName())) return -1;
        return 1;
    }
}
