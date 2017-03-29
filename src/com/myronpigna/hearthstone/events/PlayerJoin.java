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
import org.bukkit.event.player.PlayerJoinEvent;

/**
 *
 * @author myron
 */
public class PlayerJoin implements Listener{
    HearthStone hs;
    public PlayerJoin(HearthStone hs)
    {
        this.hs = hs;
    }
    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        //Load data from file and add player to playerlist
        hs.playerList.add(new PlayerData(event.getPlayer(),hs));
        //Set the player in the PlayerListFile if not in it already
        if(hs.getUUIDfromPlayerListFile(event.getPlayer().getUniqueId().toString()).equals(null))
        {
            hs.addtoPlayerListFile(event.getPlayer());
        }
    }
}
