/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myronpigna.hearthstone;

import com.myronpigna.hearthstone.commands.HearthStoneCmd;
import com.myronpigna.hearthstone.events.PlayerJoin;
import com.myronpigna.hearthstone.events.PlayerQuit;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author myron
 */
public class HearthStone extends JavaPlugin{
    public ArrayList<PlayerData> playerList = new ArrayList<>();
    private HashMap<String, Long> currentUsageCooldowns = new HashMap<String, Long>();
    private HashMap<String, Invite> currentInvites = new HashMap<String, Invite>();
    private HashMap<String, Integer> rankLocationAmount = new HashMap<String,Integer>();
    private File playerListFile;
    private YamlConfiguration playerListConfig;
    @Override
    public void onEnable(){
        PluginDescriptionFile pdfFile = getDescription();
        Logger logger = getLogger();
        registerCommands();
        registerEvents();
        registerConfig();
        for(Player player : getServer().getOnlinePlayers())
        {
            playerList.add(new PlayerData(player,this));
        }
        createPlayerListFile();
        logger.info(pdfFile.getName() + " v." + pdfFile.getVersion() + " has been enabled!");
    }
    @Override
    public void onDisable(){
        PluginDescriptionFile pdfFile = getDescription();
        Logger logger = getLogger();
        
        logger.info(pdfFile.getName() + " v." + pdfFile.getVersion() + " has been disabled!");
    }
    //Teleportation Delay
    public int getTPDelayTick()
    {
        return getTPDelaySec()*20;
    }
    public int getTPDelaySec()
    {
        return getConfig().getInt("teleport-delay", 3);
    }
    //Invite Timeout
    public int getInviteTimeoutTick()
    {
        return getInviteTimeoutSec()*20;
    }
    public int getInviteTimeoutSec()
    {
        return getConfig().getInt("invite-timeout", 60);
    }
    //Teleportation Cooldown
    public int getTPCooldownTick()
    {
        return getTPCooldownSec()*20;
    }
    public int getTPCooldownSec()
    {
        return (getConfig().getInt("teleport-cooldown", 1200));
    }
    //Invite Cooldown after setting a location
    public int getSetLocationInviteCooldownTick()
    {
        return getSetLocationInviteCooldownSec()*20;
    }
    public int getSetLocationInviteCooldownSec()
    {
        return getConfig().getInt("set-location-invite-cooldown", 600);
    }
    //Invite Cooldown after sending a invite
    public int getLocationInviteCooldownTick()
    {
        return getLocationInviteCooldownSec()*20;
    }
    public int getLocationInviteCooldownSec()
    {
        return getConfig().getInt("location-invite-cooldown", 600);
    }
    //Invite Cooldown after sending a invite
    public int getLocationInviteAcceptCooldownTick()
    {
        return getLocationInviteAcceptCooldownSec()*20;
    }
    public int getLocationInviteAcceptCooldownSec()
    {
        return getConfig().getInt("location-invite-accept-cooldown", 600);
    }
    public int getRankLocationAmount(String rank)
    {
        if(rankLocationAmount.containsKey(rank))
            return rankLocationAmount.get(rank);
        else
            return getRankLocationAmountDefault();
    }
    public int getRankLocationAmount(Player player)
    {
        for(String rank : rankLocationAmount.keySet())
        {
            if(player.hasPermission("hearthstone.location." + rank))
                return rankLocationAmount.get(rank);
        }
        return getRankLocationAmountDefault();
    }
    public void setRankLocationAmount(String rank, Integer amount) {
        rankLocationAmount.put(rank, amount);
    }
    public int getRankLocationAmountDefault() {
        return getConfig().getInt("set-location-amount-default", 1);
    }    
    private void registerCommands(){
        getCommand("hearthstone").setExecutor(new HearthStoneCmd(this));
    }
    private void registerEvents(){
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerJoin(this),this);
        pm.registerEvents(new PlayerQuit(this),this);
    }
    private void registerConfig(){
        saveDefaultConfig();
        for(String rank : getConfig().getConfigurationSection("set-location-amount").getKeys(false))
        {
            setRankLocationAmount(rank, getConfig().getInt("set-location-amount." + rank, 1));
        }
    }
    public PlayerData getPlayerData(Player player)
    {
        for(PlayerData playerData : playerList)
        {
            if(playerData.getPlayer().equals(player))
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
        if(currentUsageCooldowns.containsKey(pname))
            return currentUsageCooldowns.get(pname);
        return null;
    }
    public String getTimeRemaining(Long time)
    {
        long maxWait = getTPCooldownSec() * 1000;
        long diff = maxWait - (System.currentTimeMillis() - time);
        long seconds = diff / 1000 % 60;  
        long minutes = diff / (60 * 1000) % 60; 
        long hours = diff / (60 * 60 * 1000);
        if(hours == 0)
            if(minutes == 0)
                return seconds + " Sec";
            else    
                return minutes + " Min " + seconds + " Sec";
        else
            return hours + " Hours " + minutes + " Min " + seconds + " Sec";
    }
    public void addCooldown(PlayerData pd)
    {
        currentUsageCooldowns.put(pd.getPlayer().getName(), System.currentTimeMillis());
        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
        {
            public void run()
            {
                deleteCooldown(pd.getPlayer().getName());
                pd.sendMessage("Your HearthStone is no longer in cooldown.");
            }
        }, getTPCooldownTick());
    }

    public void deleteCooldown(String pname)
    {
        if(currentUsageCooldowns.containsKey(pname))
            currentUsageCooldowns.remove(pname);
    }
    public Invite getInvite(String pname)
    {
        if(currentInvites.containsKey(pname))
            return currentInvites.get(pname);
        return null;
    }
    public boolean getInviteExists(String pname, Invite invite)
    {
        if(currentInvites.containsKey(pname))
            if(currentInvites.get(pname).equals(invite))
                return true;

        return false;
    }
    public void addInvite(PlayerData pTarget, PlayerData sender, String locationName)
    {
        Invite newInvite = new Invite(sender, pTarget, sender.getHomeLocation(locationName), locationName);
        currentInvites.put(pTarget.getPlayer().getName(), newInvite);
        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
        {
            public void run()
            {
                //Check invite still there.
                if(getInviteExists(pTarget.getPlayer().getName(), newInvite))
                {
                    newInvite.InviteExpired();
                    deleteInvite(pTarget.getPlayer().getName());
                }
            }
        }, getInviteTimeoutTick());
    }
    public void deleteInvite(String pname)
    {
        if(currentInvites.containsKey(pname))
            currentInvites.remove(pname);
    }

    private void createPlayerListFile()
    {
        this.playerListFile = new File(getDataFolder(), "PlayerUsernameUUID.yml");
        if(playerListFile.exists() == false)
        {
            try
            {
                playerListFile.createNewFile();
            }
            catch(Exception ex)
            {
                //
            }
        }
        playerListConfig = YamlConfiguration.loadConfiguration(playerListFile);
    }
    public void addtoPlayerListFile(Player player)
    {
        playerListConfig.set("players." + player, player.getUniqueId());
        try
        {
            playerListConfig.save(playerListFile); 
        }
        catch(Exception ex)
        {
            Logger logger = getLogger();
            logger.info("Error saving new player into playerListFile.");
        }
    }
    public UUID getUUIDfromPlayerListFile(String playerName)
    {
        for(String name : playerListConfig.getConfigurationSection("players").getKeys(false))
        {
            if(name.equalsIgnoreCase(playerName))
            {
                return UUID.fromString(playerListConfig.getString("players." + playerName));
            }
        }
        return null;
    }
}
