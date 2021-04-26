/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myronpigna.hearthstone;

import com.myronpigna.hearthstone.commands.HearthStoneCmd;
import com.myronpigna.hearthstone.events.PlayerJoin;
import com.myronpigna.hearthstone.events.PlayerQuit;
import com.myronpigna.hearthstone.tabcomplete.HearthStoneTabComplete;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author myron
 */
public class HearthStone extends JavaPlugin
{
    public ArrayList<PlayerData> playerList = new ArrayList<>();
    private HashMap<String, Long> currentUsageCooldowns = new HashMap<>();
    private HashMap<String, Invite> currentInvites = new HashMap<>();
    private HashMap<String, Integer> rankLocationAmount = new HashMap<>();
    private File playerListFile;
    private File playerDataFolder;
    private YamlConfiguration playerListConfig;
    private List<Material> materialList;// Arrays.asList(Material.WOOD_STEP, Material.WOOD_STAIRS, Material.STONE_SLAB2, Material.ACACIA_STAIRS, Material.SPRUCE_WOOD_STAIRS, Material.DARK_OAK_STAIRS, Material.SNOW, Material.BIRCH_WOOD_STAIRS, Material.JUNGLE_WOOD_STAIRS, Material.IRON_PLATE, Material.WOOD_PLATE, Material.BED, Material.CHEST, Material.CAULDRON, Material.HOPPER, Material.DAYLIGHT_DETECTOR, Material.DAYLIGHT_DETECTOR_INVERTED, Material.TRAP_DOOR, Material.IRON_TRAPDOOR, Material.TRAPPED_CHEST, Material.STONE_PLATE, Material.GOLD_PLATE, Material.SANDSTONE_STAIRS, Material.NETHER_BRICK_STAIRS, Material.PURPUR_SLAB, Material.PURPUR_STAIRS, Material.STEP, Material.SOUL_SAND, Material.GRASS_PATH, Material.SOIL);

    public HearthStone() {
//        Stream.Of()

        //materialList = Stream.of(Material.values()).filter(Material::isSolid).collect(Collectors.toList());
    }

    @Override
    public void onEnable()
    {
        PluginDescriptionFile pdfFile = getDescription();
        Logger logger = getLogger();
        registerCommands();
        registerEvents();
        registerConfig();
        for (Player player : getServer().getOnlinePlayers())
        {
            playerList.add(new PlayerData(player, this));
        }
        createPlayerListFile();
        materialList = Stream.of(Material.values()).filter(Material::isSolid).collect(Collectors.toList());
        logger.info(pdfFile.getName() + " v." + pdfFile.getVersion() + " has been enabled!");
    }

    @Override
    public void onDisable()
    {
        PluginDescriptionFile pdfFile = getDescription();
        Logger logger = getLogger();

        logger.info(pdfFile.getName() + " v." + pdfFile.getVersion() + " has been disabled!");
    }

    public List<Material> getMaterialList()
    {
        return materialList;
    }
    //Teleportation Delay
    public int getTPDelayTick()
    {
        return getTPDelaySec() * 20;
    }

    public int getTPDelaySec()
    {
        return getConfig().getInt("teleport-delay", 3);
    }

    //Invite Timeout
    public int getInviteTimeoutTick()
    {
        return getInviteTimeoutSec() * 20;
    }

    public int getInviteTimeoutSec()
    {
        return getConfig().getInt("invite-timeout", 60);
    }

    //Teleportation Cooldown
    public int getTPCooldownTick()
    {
        return getTPCooldownSec() * 20;
    }

    public int getTPCooldownSec()
    {
        return (getConfig().getInt("teleport-cooldown", 1200));
    }

    //Invite Cooldown after setting a location
    public int getSetLocationInviteCooldownTick()
    {
        return getSetLocationInviteCooldownSec() * 20;
    }

    public int getSetLocationInviteCooldownSec()
    {
        return getConfig().getInt("set-location-invite-cooldown", 600);
    }

    //Invite Cooldown after sending a invite
    public int getLocationInviteCooldownTick()
    {
        return getLocationInviteCooldownSec() * 20;
    }

    public int getLocationInviteCooldownSec()
    {
        return getConfig().getInt("location-invite-cooldown", 600);
    }

    //Invite Cooldown after sending a invite
    public int getLocationInviteAcceptCooldownTick()
    {
        return getLocationInviteAcceptCooldownSec() * 20;
    }

    public int getLocationInviteAcceptCooldownSec()
    {
        return getConfig().getInt("location-invite-accept-cooldown", 600);
    }

    public int getCooldownSec(Cooldown cooldown)
    {
        switch(cooldown)
        {
            case USAGE:
                return getTPCooldownSec();
            case INVITE:
                return getLocationInviteCooldownSec();
            case ACCEPTED:
                return getLocationInviteAcceptCooldownSec();
            case SET:
                return getSetLocationInviteCooldownSec();
            default:
                return getTPCooldownSec();
        }
    }
    
    public int getRankLocationAmount(String rank)
    {
        if (rankLocationAmount.containsKey(rank)) return rankLocationAmount.get(rank);
        else return getRankLocationAmountDefault();
    }

    public int getRankLocationAmount(Player player)
    {
        for (String rank : rankLocationAmount.keySet())
        {
            if (player.hasPermission("hearthstone.location." + rank)) return rankLocationAmount.get(rank);
        }
        return getRankLocationAmountDefault();
    }

    public void setRankLocationAmount(String rank, Integer amount)
    {
        rankLocationAmount.put(rank, amount);
    }

    public int getRankLocationAmountDefault()
    {
        return getConfig().getInt("set-location-amount-default", 1);
    }

    private void registerCommands()
    {
        getCommand("hearthstone").setExecutor(new HearthStoneCmd(this));
        getCommand("hearthstone").setTabCompleter(new HearthStoneTabComplete(this));
    }

    private void registerEvents()
    {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerJoin(this), this);
        pm.registerEvents(new PlayerQuit(this), this);
    }

    private void registerConfig()
    {
        saveDefaultConfig();
        for (String rank : getConfig().getConfigurationSection("set-location-amount").getKeys(false))
        {
            setRankLocationAmount(rank, getConfig().getInt("set-location-amount." + rank, 1));
        }
    }

    public PlayerData getPlayerData(Player player)
    {
        for (PlayerData playerData : playerList)
        {
            if (playerData.getPlayer().equals(player))
            {
                return playerData;
            }
        }
        return null;
    }

    public void removePlayerData(PlayerData pd)
    {
        playerList.remove(pd);
    }

    public Long getCooldown(String pname)
    {
        if (currentUsageCooldowns.containsKey(pname)) return currentUsageCooldowns.get(pname);
        return null;
    }

    public String getTimeRemaining(Long time, Cooldown cooldownType)
    {
        long maxWait = getCooldownSec(cooldownType) * 1000;
        long diff = maxWait - (System.currentTimeMillis() - time);
        long seconds = diff / 1000 % 60;
        long minutes = diff / (60 * 1000) % 60;
        long hours = diff / (60 * 60 * 1000);

        return hours > 0 ? hours + " Hours " + minutes + " Min " + seconds + " Sec" : minutes > 0 ? minutes + " Min " + seconds + " Sec" : seconds + " Sec";
    }

    public void addCooldown(PlayerData pd, Cooldown usage)
    {
        getServer().getScheduler().scheduleSyncDelayedTask(this, () ->
        {
            deleteCooldown(pd.getPlayer().getName());
            switch (usage)
            {
                case USAGE://Hearthstone usage
                    pd.sendMessage("Your HearthStone is no longer in cooldown.");
                    break;
                case SET://Set hS
                case INVITE://Invite usage
                case ACCEPTED://Invite Accept
                    pd.sendMessage("Your HearthStone invite is no longer in cooldown.");
                    break;
                case OTHER://Use other
                    pd.sendMessage("A cool down of your Hearthstone has expired.");
                    break;
            }
        }, getTPCooldownTick());
    }

    public void deleteCooldown(String pname)
    {
        if (currentUsageCooldowns.containsKey(pname)) currentUsageCooldowns.remove(pname);
    }

    public Invite getInvite(String pname)
    {
        return currentInvites.containsKey(pname) ? currentInvites.get(pname) : null;
    }

    public boolean getInviteExists(String pname, Invite invite)
    {
        if (currentInvites.containsKey(pname)) if (currentInvites.get(pname).equals(invite)) return true;

        return false;
    }

    /**
     * Add a invite to the currentInvite list
     * @param pTarget Player that receives the invite
     * @param sender Player that send the invite
     * @param locationName Location the receiver is invited to
     */
    public void addInvite(PlayerData pTarget, PlayerData sender, String locationName)
    {
        Invite newInvite = new Invite(sender, pTarget, sender.getHomeLocation(locationName), locationName);
        currentInvites.put(pTarget.getPlayer().getName(), newInvite);
        getServer().getScheduler().scheduleSyncDelayedTask(this, () ->
        {
            //Check invite still there.
            if (getInviteExists(pTarget.getPlayer().getName(), newInvite))
            {
                newInvite.InviteExpired();
                deleteInvite(pTarget.getPlayer().getName());
            }
        }, getInviteTimeoutTick());
    }

    public void deleteInvite(String pname)
    {
        if (currentInvites.containsKey(pname)) currentInvites.remove(pname);
    }

    /**
     * Creation of the PlayerListFile
     */
    private void createPlayerListFile()
    {
        this.playerListFile = new File(getDataFolder(), "PlayerUsernameUUID.yml");
        if (!playerListFile.exists())
        {

            try
            {
                playerListFile.createNewFile();
            }
            catch (IOException ex)
            {
                Logger.getLogger(HearthStone.class.getName()).log(Level.SEVERE, "IOException : Error creating PlayerListFile", ex);
            }

        }
        playerListConfig = YamlConfiguration.loadConfiguration(playerListFile);
    }

    /**
     * Add Player to the playerListFile to be able to get the UUID when he is offline
     * @param player Player to add to the file
     */
    public void addtoPlayerListFile(Player player)
    {
        playerListConfig.set("players." + player.getName(), player.getUniqueId().toString());
        try
        {
            playerListConfig.save(playerListFile);
        }
        catch (IOException ex)
        {
            Logger.getLogger(HearthStone.class.getName()).log(Level.SEVERE, "IOException : Error adding Player to PlayerListFile", ex);
        }
    }

    /**
     * Get the UUID of a playerName from the player/uuid list
     * @param playerName Name of the player
     * @return UUID of given player's name
     */
    public UUID getUUIDfromPlayerListFile(String playerName)
    {
        ConfigurationSection playerSection = playerListConfig.getConfigurationSection("players");
        if (playerSection != null)
        {
            for (String name : playerSection.getKeys(false))
            {
                if (name.equalsIgnoreCase(playerName))
                {
                    return UUID.fromString(playerSection.getString(name));
                }
            }
        }
        return null;
    }
    /**
     * Gets a list of all online players starting with arg
     * @param arg beginning of a name or leave empty
     * @return ArrayList<String> containing names of all online players starting with arg
     */
    public ArrayList<String> getOnlinePlayerNames(String arg)
    {
        ArrayList<String> playerNames = new ArrayList<>();
        if(!arg.isEmpty())
        {
            for(Player p : getServer().getOnlinePlayers())
            {
                if(p.getName().toLowerCase().startsWith(arg.toLowerCase()))
                {
                    playerNames.add(p.getName());
                }
            }
            return playerNames;
        }
        for(Player p : getServer().getOnlinePlayers())
        {
            playerNames.add(p.getName());
        }
        return playerNames;
    }

    public File getPlayerDataFolder()
    {
        this.playerDataFolder = new File(getDataFolder(), "playerData");
        if (!playerDataFolder.exists())
        {
            try
            {
                playerDataFolder.mkdir();
            }
            catch (Exception ex)
            {
                Logger.getLogger(HearthStone.class.getName()).log(Level.SEVERE, "Exception : Error creating player data directory", ex);
            }
        }
        return playerDataFolder;
    }
}
