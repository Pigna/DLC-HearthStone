/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.daylightcraft.hearthstone;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * @author myron
 */
public class Invite implements Comparable<Invite>
{
    private final PlayerData sender;
    private final PlayerData receiver;
    private final Location location;
    private final String locationName;

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

    public void inviteExpired()
    {
        receiver.sendMessage(getSender().getName() + "'s invite to " + locationName + " has expired.");
        sender.sendMessage("Your invite to " + getReceiver().getName() + " has expired.");
    }

    public void inviteAccepted(boolean override)
    {
        receiver.sendMessage("You have accepted the HearthStone invite from " + getSender().getName() + ".");
        sender.sendMessage(getReceiver().getName() + " has accepted your HearthStone invite.");
        receiver.teleportPlayerLocation(locationName, location, Cooldown.ACCEPTED, override);
        sender.setCooldown(Cooldown.INVITE);
    }

    public void inviteDeclined()
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

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
