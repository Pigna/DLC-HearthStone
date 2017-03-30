/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myronpigna.hearthstone;

import net.minecraft.server.v1_11_R1.PacketPlayOutChat;
import net.minecraft.server.v1_11_R1.PlayerConnection;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.OfflinePlayer;

/**
 * @author myron
 */
public class PlayerData
{
    private YamlConfiguration DataFile;
    private HearthStone hs;
    private File pluginFolder;
    private File dataFile;
    private YamlConfiguration playerDataConfig;
    private HashMap<String, Location> locations = new HashMap<>();
    private Player player;

    public PlayerData(Player player, HearthStone hs)
    {
        this.player = player;
        this.hs = hs;
        this.pluginFolder = hs.getDataFolder();
        this.dataFile = new File(pluginFolder, player.getUniqueId() + ".yml");
        CreateFile();
        LoadFile();
    }
    public PlayerData(OfflinePlayer offlinePlayer, HearthStone hs)
    {
        this.hs = hs;
        this.pluginFolder = hs.getDataFolder();
        this.dataFile = new File(pluginFolder, offlinePlayer.getUniqueId() + ".yml");
        LoadFile();
    }
    public Player getPlayer()
    {
        return player;
    }

    public Map<String, Location> getHomes()
    {
        return locations;
    }

    //World world, float x, float y, float z, double yaw, double pitch
    public boolean setHome(String name)
    {
        if (hasHome() >= hs.getRankLocationAmount(player) && !player.hasPermission("hearthstone.bypass.homes"))
        {
            return false;
        }

        String safename = name.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        Location location = player.getLocation();
        //Check if home already exists
        locations.put(safename, location);
        ArrayList<String> values = new ArrayList<String>();
        values.add(location.getWorld().getName());
        values.add(location.getX() + "");
        values.add(location.getY() + "");
        values.add(location.getZ() + "");
        values.add(location.getYaw() + "");
        values.add(location.getPitch() + "");
        playerDataConfig.set("location." + safename, values);
        try
        {
            playerDataConfig.save(dataFile);
        }
        catch (Exception ex)
        {
            Logger logger = hs.getLogger();
            logger.info("Error saving new home into player file. '" + player.getName() + "' '" + safename + "'");
            return false;
        }
        //Set cooldown on set.
        SetCooldown(Cooldown.SET);
        return true;
    }

    public boolean hasHome(String name)
    {
        return locations.containsKey(name);
    }

    public int hasHome()
    {
        return locations.size();
    }

    public String getHomeName()
    {
        return locations.entrySet().iterator().next().getKey();
    }

    public Location getHomeLocation()
    {
        return locations.entrySet().iterator().next().getValue();
    }

    public Location getHomeLocation(String name)
    {
        return locations.get(name);
    }

    public String getHomeLocationString(String name)
    {
        Location loc = locations.get(name);
        return "World:" + loc.getWorld().getName() + " [name:\"" + name + "\", x:" + Math.round(loc.getX()) + ", y:" + Math.round(loc.getY()) + ", z:" + Math.round(loc.getZ()) + "]";
    }

    public boolean removeHome(String name)
    {
        if (!hasHome(name))
        {
            return false;
        }

        locations.remove(name);
        playerDataConfig.set("location." + name, null);
        try
        {
            playerDataConfig.save(dataFile);
        }
        catch (Exception ex)
        {
            return false;
        }
        return true;
    }

    public void CreateFile()
    {
        if (!pluginFolder.exists())
        {
            try
            {
                pluginFolder.mkdir();
            }
            catch (Exception ex)
            {
                //
            }
        }
        if (!dataFile.exists())
        {
            try
            {
                dataFile.createNewFile();
            }
            catch (Exception ex)
            {
                //
            }
        }
        playerDataConfig = YamlConfiguration.loadConfiguration(dataFile);
        playerDataConfig.set("Player", player.getName());
        try
        {
            playerDataConfig.save(dataFile);
        }
        catch (Exception ex)
        {
            Logger logger = hs.getLogger();
            logger.info("Error updating player name on load.");
        }
    }
    public void LoadFile()
    {
        if (dataFile.exists())
        {
            playerDataConfig = YamlConfiguration.loadConfiguration(dataFile);
            //Load the locations and add them to the list
            if (playerDataConfig.getConfigurationSection("location") != null)
            {
                for (String lName : playerDataConfig.getConfigurationSection("location").getKeys(false))
                {
                    ArrayList<String> lInfo = (ArrayList<String>) playerDataConfig.getStringList("location." + lName);
                    World world = hs.getServer().getWorld(lInfo.get(0));
                    if (world != null)
                    {
                        Location location = new Location(hs.getServer().getWorld(lInfo.get(0)), Double.parseDouble(lInfo.get(1)), Double.parseDouble(lInfo.get(2)), Double.parseDouble(lInfo.get(3)), Float.parseFloat(lInfo.get(4)), Float.parseFloat(lInfo.get(5)));
                        locations.put(lName, location);
                    }
                    else
                    {
                        playerDataConfig.set("location." + lName, null);
                        try
                        {
                            playerDataConfig.save(dataFile);
                        }
                        catch (Exception ex)
                        {
                            Logger logger = hs.getLogger();
                            logger.info("Error removing location from non-existing world from saved hearthstones.");
                        }
                    }
                }
            }
        }
        else
            hs.getLogger().info("Error loading player file. File does not exist.");
    }
    public void sendMessage(String message)
    {
        player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "HS" + ChatColor.DARK_GRAY + "]" + ChatColor.YELLOW + message);
    }

    public void sendChatPacket(PacketPlayOutChat packet)
    {
        PlayerConnection targetpConnection = ((CraftPlayer) player).getHandle().playerConnection;
        targetpConnection.sendPacket(packet);
    }

    public void teleportPlayerLocation(Location l, Cooldown cd, boolean override) //arg 0 use of hearthstone - 1 invite - 4 useother
    {
        //TODO: Use cooldown from player file, instead of the hs list. ERROR needs fixing no cooldown ever registrated or check does not work.
        Long cooldown = getCooldown(cd);
        if (hasCooldown(cd) && !player.hasPermission("hearthstone.bypass.cooldown"))
        {
            sendMessage("Your HearthStone is still in cooldown for " + hs.getTimeRemaining(cooldown) + ".");
        }
        else
        {
            Location locationBefore = player.getLocation();
            if (player.hasPermission("hearthstone.bypass.delay"))
            {
                player.teleport(l);
                SetCooldown(cd);
            }
            else
            {
                sendMessage("Teleport commencing in " + hs.getTPDelaySec() + " Sec. Please don't move.");
                //Delay
                hs.getServer().getScheduler().scheduleSyncDelayedTask(hs, () ->
                {
                    //Check movement
                    if (locationBefore.getX() != player.getLocation().getX()
                        || locationBefore.getY() != player.getLocation().getY()
                        || locationBefore.getZ() != player.getLocation().getZ())
                    {
                        sendMessage("You have moved. Teleport is canceled.");
                    }
                    else if (!LocationSafeCheck(l) && !override)
                    {
                        sendMessage("Location is not safe to teleport to, contact a server Admin to check the location or add a ! after the homename.");
                    }
                    else
                    {
                        player.teleport(l);
                        SetCooldown(cd);
                    }
                }, hs.getTPDelayTick());
                //TODO: Check combat on teleport Delay
                //Todo: test the override to a home teleport
            }
        }
    }

    private boolean LocationSafeCheck(Location loc)
    {
        Location locOther = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (locOther.getBlock().getType() != Material.AIR)
        {
            return false;
        }
        locOther.setY(locOther.getY() + 1);
        if (locOther.getBlock().getType() != Material.AIR)
        {
            return false;
        }
        locOther.setY(locOther.getY() - 2);

        if (!(locOther.getBlock().getType() == Material.AIR || locOther.getBlock().isLiquid()))
        {
            return true;
        }
        else return false;
    }

    public void SetCooldown(Cooldown cd) //arg 0 use of hearthstone - 1 invite - 2 inviteAccept - 3 setHS - 4 useother
    {
        if (!player.hasPermission("hearthstone.bypass.cooldown"))
        {
            switch (cd)
            {
                case USAGE://Hearthstone usage
                    setUsageCooldown();
                    break;
                case INVITE://Invite usage
                    setInviteCooldown();
                    break;
                case ACCEPTED://Invite Accept
                    setInviteCooldown();
                    setInviteAcceptCooldown();
                    break;
                case SET://Set hS
                    setInviteCooldown();
                    break;
                case OTHER://Use other
                    setUsageCooldown();
                    break;
            }
        }
    }

    public boolean hasCooldown(Cooldown cd)
    {
        long wait_time;

        switch (cd)
        {
            case USAGE:
                wait_time = getUsageCooldown() + (hs.getTPCooldownSec() * 1000);
                break;
            case INVITE:
                wait_time = getInviteCooldown() + (hs.getSetLocationInviteCooldownSec() * 1000);
                break;
            case ACCEPTED:
                wait_time = getInviteAcceptCooldown() + (hs.getLocationInviteAcceptCooldownSec() * 1000);
                break;
            default: //TODO Extend with other cooldowns;
                return false;
        }

        return System.currentTimeMillis() <= wait_time;
    }

    public long getCooldown(Cooldown cd)
    {
        switch (cd)
        {
            case USAGE:
                return getUsageCooldown();
            case INVITE:
                return getInviteCooldown();
            case ACCEPTED:
                return getInviteAcceptCooldown();
            default:
                return 0;
        }
    }

    public void resetCooldown()
    {
        playerDataConfig.set("inviteCooldown", 0);
        playerDataConfig.set("inviteAcceptCooldown", 0);
        playerDataConfig.set("usageCooldown", 0);
        try
        {
            playerDataConfig.save(dataFile);
        }
        catch (Exception ex)
        {
            Logger logger = hs.getLogger();
            logger.info("Error resetting Invite Cooldown to file");
        }
    }

    private void setInviteCooldown()
    {
        playerDataConfig.set("inviteCooldown", System.currentTimeMillis());
        try
        {
            playerDataConfig.save(dataFile);
        }
        catch (Exception ex)
        {
            Logger logger = hs.getLogger();
            logger.info("Error saving Invite Cooldown to file");
        }
    }

    private long getInviteCooldown()
    {
        return playerDataConfig.getLong("inviteCooldown", 0);
    }

    private void setInviteAcceptCooldown()
    {
        playerDataConfig.set("inviteAcceptCooldown", System.currentTimeMillis());
        try
        {
            playerDataConfig.save(dataFile);
        }
        catch (Exception ex)
        {
            Logger logger = hs.getLogger();
            logger.info("Error saving Invite Accept Cooldown to file");
        }
    }

    private long getInviteAcceptCooldown()
    {
        return playerDataConfig.getLong("inviteAcceptCooldown", 0);
    }

    private void setUsageCooldown()
    {
        playerDataConfig.set("usageCooldown", System.currentTimeMillis());
        try
        {
            playerDataConfig.save(dataFile);
        }
        catch (Exception ex)
        {
            Logger logger = hs.getLogger();
            logger.info("Error saving usage cooldown to file");
        }
    }

    private long getUsageCooldown()
    {
        return playerDataConfig.getLong("usageCooldown", 0);
    }

    public enum Cooldown
    {
        USAGE,
        INVITE,
        ACCEPTED,
        SET,
        OTHER
    }
}
