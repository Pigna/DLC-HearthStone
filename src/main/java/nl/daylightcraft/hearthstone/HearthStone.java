/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.daylightcraft.hearthstone;

import nl.daylightcraft.hearthstone.commands.HearthStoneCmd;
import nl.daylightcraft.hearthstone.events.PlayerJoin;
import nl.daylightcraft.hearthstone.events.PlayerQuit;
import nl.daylightcraft.hearthstone.tabcomplete.HearthStoneTabComplete;

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
    public List<PlayerData> playerList = new ArrayList<>();
    private final HashMap<String, Invite> currentInvites = new HashMap<>();
    private final HashMap<String, Integer> rankLocationAmount = new HashMap<>();
    private File playerListFile;
    private YamlConfiguration playerListConfig;
    private List<Material> materialList;

    public HearthStone() {
        // Constructor
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

    //Invite Cooldown after sending an invitation
    public int getLocationInviteCooldownTick()
    {
        return getLocationInviteCooldownSec() * 20;
    }

    public int getLocationInviteCooldownSec()
    {
        return getConfig().getInt("location-invite-cooldown", 600);
    }

    //Invite Cooldown after sending an invitation
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
        return switch (cooldown) {
            case USAGE -> getTPCooldownSec();
            case INVITE -> getLocationInviteCooldownSec();
            case ACCEPTED -> getLocationInviteAcceptCooldownSec();
            case SET -> getSetLocationInviteCooldownSec();
            default -> getTPCooldownSec();
        };
    }
    
    public int getRankLocationAmount(String rank)
    {
        if (rankLocationAmount.containsKey(rank)) return rankLocationAmount.get(rank);
        else return getRankLocationAmountDefault();
    }

    public int getRankLocationAmount(Player player)
    {
        for (Map.Entry<String, Integer> rank : rankLocationAmount.entrySet())
        {
            if (player.hasPermission("hearthstone.location." + rank.getKey()))
                return rankLocationAmount.get(rank.getKey());
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

    public String getTimeRemaining(Long time, Cooldown cooldownType)
    {
        long maxWait = getCooldownSec(cooldownType) * 1000L;
        long diff = maxWait - (System.currentTimeMillis() - time);
        long seconds = diff / 1000 % 60;
        long minutes = diff / (60 * 1000) % 60;
        long hours = diff / (60 * 60 * 1000);

        if ((hours > 0)) {
            return (hours + " Hours " + minutes + " Min " + seconds + " Sec");
        }
        else {
            if (minutes > 0)
                return minutes + " Min " + seconds + " Sec";
            return seconds + " Sec";
        }
    }

    public void addCooldown(PlayerData pd, Cooldown usage)
    {
        getServer().getScheduler().scheduleSyncDelayedTask(this, () ->
        {
            deleteCooldown(pd.getPlayer().getName());
            switch (usage) {
                case USAGE ->//Hearthstone usage
                    pd.sendMessage("Your HearthStone is no longer in cooldown.");
                case SET,//Set hS
                    INVITE,//Invite usage
                    ACCEPTED ->//Invite Accept
                    pd.sendMessage("Your HearthStone invite is no longer in cooldown.");
                case OTHER ->//Use other
                    pd.sendMessage("A cool down of your Hearthstone has expired.");
            }
        }, getTPCooldownTick());
    }

    public void deleteCooldown(String playerName)
    {
        //TODO: On reset cool-downs remove cooldown message scheduler
    }

    public Invite getInvite(String playerName)
    {
        return currentInvites.getOrDefault(playerName, null);
    }

    public boolean getInviteExists(String playerName, Invite invite)
    {
        if (currentInvites.containsKey(playerName))
            return currentInvites.get(playerName).equals(invite);

        return false;
    }

    /**
     * Add an invitation to the currentInvite list
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
                newInvite.inviteExpired();
                deleteInvite(pTarget.getPlayer().getName());
            }
        }, getInviteTimeoutTick());
    }

    public void deleteInvite(String playerName)
    {
        currentInvites.remove(playerName);
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
                if(playerListFile.createNewFile())
                    Logger.getLogger(HearthStone.class.getName()).log(Level.INFO, "Player list file created.");
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
    public void addToPlayerListFile(Player player)
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
    public UUID getUuidFromPlayerListFile(String playerName)
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
    public List<String> getOnlinePlayerNames(String arg)
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
        File playerDataFolder = new File(getDataFolder(), "playerData");
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
