/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myronpigna.hearthstone.events;

import com.myronpigna.hearthstone.HearthStone;
import com.myronpigna.hearthstone.PlayerData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 *
 * @author myron
 */
public class PlayerQuit implements Listener{
    HearthStone hs;
    public PlayerQuit(HearthStone hs)
    {
        this.hs = hs;
    }
    @EventHandler
    public void onPlayerLeaveEvent(PlayerQuitEvent event){
        //remove player from playerlist
        PlayerData pd = hs.getPlayerData(event.getPlayer());
        if(pd != null)
        {
            hs.removePlayerData(pd);
        }    
    }
}
