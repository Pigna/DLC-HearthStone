/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.daylightcraft.hearthstone;

import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutChat;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.network.chat.ChatMessageType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;

/**
 * @author myron
 */
public class PlayerData
{
    private YamlConfiguration DataFile;
    private final HearthStone hs;
    private final File pluginFolder;
    private final File playerDataFolder;
    private final File dataFile;
    private YamlConfiguration playerDataConfig;
    private final HashMap<String, Location> locations = new HashMap<>();
    private Player player;

    public PlayerData(Player player, HearthStone hs)
    {
        this.player = player;
        this.hs = hs;
        this.pluginFolder = hs.getDataFolder();
        this.playerDataFolder = hs.getPlayerDataFolder();
        this.dataFile = new File(playerDataFolder, player.getUniqueId() + ".yml");
        CreateFile();
        LoadFile();
    }

    public PlayerData(OfflinePlayer offlinePlayer, HearthStone hs)
    {
        this.hs = hs;
        this.pluginFolder = hs.getDataFolder();
        this.playerDataFolder = hs.getPlayerDataFolder();
        this.dataFile = new File(playerDataFolder, offlinePlayer.getUniqueId() + ".yml");
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
    /**
     * Gets homes that starts with arg
     * @param arg
     * @return List of home names that start with arg
     */
    public ArrayList<String> getHomeList(String arg)
    {
        ArrayList<String> returnList = new ArrayList<>();
        if(arg.equals(""))
        {
            for(String s : locations.keySet())
            {
                returnList.add(s);
            }
            return returnList;
        }
        for(String s : locations.keySet())
        {
            if(s.toLowerCase().startsWith(arg.toLowerCase()))
            {
                returnList.add(s);
            }
        }
        return returnList;
    }
    //World world, float x, float y, float z, double yaw, double pitch
    public boolean setHome(String name)
    {
        String safename = name.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (hasHome() >= hs.getRankLocationAmount(player) && !player.hasPermission("hearthstone.bypass.homes"))
        {
            if (!hasHome(safename))
                return false;
        }
        
        Location location = player.getLocation();
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
        catch (IOException ex)
        {
            Logger.getLogger(HearthStone.class.getName()).log(Level.SEVERE, "IOException : Error saving new home into player file. '" + player.getName() + "' '" + safename + "'", ex);
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
        catch (IOException ex)
        {
            Logger.getLogger(HearthStone.class.getName()).log(Level.SEVERE, "IOException : Error removing home from player file. '" + player.getName() + "' '" + name + "'", ex);
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
                Logger.getLogger(HearthStone.class.getName()).log(Level.SEVERE, "Exception : Error creating plugin directory", ex);
            }
        }
        if (!dataFile.exists())
        {
            try
            {

                dataFile.createNewFile();
                //TODO: check config if new player message is true;
                sendMessage("This server is using the plugin HearthStone to replace 'homes'. Type /hs help for more information.");
            }
            catch (IOException ex)
            {
                Logger.getLogger(HearthStone.class.getName()).log(Level.SEVERE, "IOException : Error creating playerfile", ex);
            }
        }
        playerDataConfig = YamlConfiguration.loadConfiguration(dataFile);
        playerDataConfig.set("Player", player.getName());
        try
        {
            playerDataConfig.save(dataFile);
        }
        catch (IOException ex)
        {
            Logger.getLogger(HearthStone.class.getName()).log(Level.SEVERE, "IOException : Error updating playername onload : " + player.getName(), ex);
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
                        catch (IOException ex)
                        {
                            Logger.getLogger(HearthStone.class.getName()).log(Level.SEVERE, "IOException : Error removing location from non-existing world from saved hearthstones", ex);
                        }
                    }
                }
            }
        }
        else hs.getLogger().info("Error loading player file. File does not exist.");
    }

    public void sendMessage(String message)
    {
        player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "HS" + ChatColor.DARK_GRAY + "]" + ChatColor.YELLOW + message);
    }

    public void sendChatPacket(PacketPlayOutChat packet)
    {
        PlayerConnection targetpConnection = ((CraftPlayer) player).getHandle().b;
        targetpConnection.sendPacket(packet);
    }

    public void teleportPlayerLocation(String lname, Location l, Cooldown cd, boolean override) //arg 0 use of hearthstone - 1 invite - 4 useother
    {
        //TODO: Use cooldown from player file, instead of the hs list. ERROR needs fixing no cooldown ever registrated or check does not work.
        Long cooldown = getCooldown(cd);
        if (hasCooldown(cd) && !player.hasPermission("hearthstone.bypass.cooldown"))
        {
            sendMessage("Your HearthStone is still in cooldown for " + hs.getTimeRemaining(cooldown, Cooldown.USAGE) + ".");
            return;
        }

        Location locationBefore = player.getLocation();
        if (player.hasPermission("hearthstone.bypass.delay"))
        {
            player.teleport(l);
            SetCooldown(cd);
            return;
        }

        sendMessage("Teleport commencing in " + hs.getTPDelaySec() + " Sec. Please don't move.");
        //Delay
        hs.getServer().getScheduler().scheduleSyncDelayedTask(hs, () ->
        {
            //Check movement
            if (locationBefore.getX() != player.getLocation().getX() || locationBefore.getY() != player.getLocation().getY() || locationBefore.getZ() != player.getLocation().getZ())
            {
                sendMessage("You have moved. Teleport is canceled.");
            }
            else if (!LocationSafeCheck(l) && !override)
            {
                sendMessage("HearthStone location is not safe for teleport.");
                String clickableOverride = "";
                if (cd == Cooldown.ACCEPTED) //TODO deze strings zijn bijna hetzelfde hak ze op in stukken en doe de ef alleen om het stuk dat anders is
                {
                    clickableOverride = "[\"\",{\"text\":\"Add a '!' or click -> \",\"color\":\"white\"},{\"text\":\"[Override]\",\"color\":\"red\",\"bold\":true,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/hs accept !\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"\",\"extra\":[{\"text\":\"Override safety precautions at your own risk!\",\"color\":\"red\"}]}}},{\"text\":\" at your own risk!\",\"color\":\"white\",\"bold\":false}]";
                }
                else
                {
                    clickableOverride = "[\"\",{\"text\":\"Add a '!' or click -> \",\"color\":\"white\"},{\"text\":\"[Override]\",\"color\":\"red\",\"bold\":true,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/hs " + lname + " !\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"\",\"extra\":[{\"text\":\"Override safety precautions at your own risk!\",\"color\":\"red\"}]}}},{\"text\":\" at your own risk!\",\"color\":\"white\",\"bold\":false}]";
                }
                PacketPlayOutChat packet = new PacketPlayOutChat(IChatBaseComponent.ChatSerializer.a(clickableOverride), ChatMessageType.c, UUID.randomUUID());
                sendChatPacket(packet);
            }
            else
            {
                player.teleport(l);
                SetCooldown(cd);
            }
        }, hs.getTPDelayTick());
        //TODO: Check combat on teleport Delay
    }

    private boolean LocationSafeCheck(Location loc)
    {
        Location locOther = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY() + 1, loc.getBlockZ());
        Block block = locOther.getBlock();
        //check head location
        if (!CheckBlockType(block, Material.AIR))
        {
            return false;
        }
        //check feet
        locOther.setY(locOther.getY() - 1);
        block = locOther.getBlock();
        if (CheckBlockType(block, Material.AIR) || CheckBlockType(block, Material.WATER))
        {
            //check underneeth
            locOther.setY(locOther.getY() - 1);
            block = locOther.getBlock();
            return !(CheckBlockType(block, Material.AIR) || block.isLiquid());
        }
        //check feet for slabs or stairs or carpet or fence
        if (IsSlabUnsolid(block))
        {
            //Check above head for air
            locOther.setY(locOther.getY() + 2);
            block = locOther.getBlock();
            return CheckBlockType(block, Material.AIR);
        }

        return false;
        /*
        1 rood is air
        2 bruin is air en oranje is niet liquid of air. DAN MAG JE ER HEEN
        2 of als bruin een block is en niet lava of vuur en geel air. DAN MAG JE ER HEEN
        */
    }

    private boolean CheckBlockType(Block block, Material expected)
    {
        return block.getType().equals(expected);
    }

    private boolean IsSlabUnsolid(Block block)
    {
        Material blockMaterial = block.getType();
        //Solid block
        if (!blockMaterial.isSolid() && !CheckBlockType(block, (Material.FIRE))) return true;
        List<Material> materialList = hs.getMaterialList();
        return materialList.stream().anyMatch((m) -> (m.equals(blockMaterial)));
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
        long wait_time = getCooldown(cd);

        switch (cd)
        {
            case USAGE:
                wait_time += (hs.getTPCooldownSec() * 1000);
                break;
            case INVITE:
                wait_time += (hs.getSetLocationInviteCooldownSec() * 1000);
                break;
            case ACCEPTED:
                wait_time += (hs.getLocationInviteAcceptCooldownSec() * 1000);
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
        catch (IOException ex)
        {
            Logger.getLogger(HearthStone.class.getName()).log(Level.SEVERE, "IOException : Error Saving the reset cooldowns to file", ex);
        }
    }

    private void setInviteCooldown()
    {
        playerDataConfig.set("inviteCooldown", System.currentTimeMillis());
        hs.addCooldown(this, Cooldown.INVITE);
        try
        {
            playerDataConfig.save(dataFile);
        }
        catch (IOException ex)
        {
            Logger.getLogger(HearthStone.class.getName()).log(Level.SEVERE, "IOException : Error saving the invite cooldown to file", ex);
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
        catch (IOException ex)
        {
            Logger.getLogger(HearthStone.class.getName()).log(Level.SEVERE, "IOException : Error saving the invite cooldown to file", ex);
        }
    }

    private long getInviteAcceptCooldown()
    {
        return playerDataConfig.getLong("inviteAcceptCooldown", 0);
    }

    private void setUsageCooldown()
    {
        playerDataConfig.set("usageCooldown", System.currentTimeMillis());
        hs.addCooldown(this, Cooldown.USAGE);
        try
        {
            playerDataConfig.save(dataFile);
        }
        catch (IOException ex)
        {
            Logger.getLogger(HearthStone.class.getName()).log(Level.SEVERE, "IOException : Error saving the use cooldown to file", ex);
        }
    }

    private long getUsageCooldown()
    {
        return playerDataConfig.getLong("usageCooldown", 0);
    }
}
