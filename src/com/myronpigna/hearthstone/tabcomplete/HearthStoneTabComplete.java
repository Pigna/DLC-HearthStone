/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myronpigna.hearthstone.tabcomplete;

import com.myronpigna.hearthstone.HearthStone;
import com.myronpigna.hearthstone.PlayerData;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 *
 * @author myron
 */
public class HearthStoneTabComplete implements TabCompleter{
    private HearthStone hs;
    public HearthStoneTabComplete(HearthStone hs)
    {
        this.hs = hs;
    }
    /**
     * AutoComplete commands when tab is pressed
     * @param sender
     * @param cmd
     * @param commandLabel
     * @param args
     * @return 
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args)
    {
        if(cmd.getName().equalsIgnoreCase("hs") || cmd.getName().equalsIgnoreCase("hearthstone"))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage("TabCompelte is not for console.");
                return null;
            }

            Player player = (Player) sender;
            PlayerData pd = hs.getPlayerData(player);
            ArrayList<String> autoComplete = new ArrayList<>();
            if(args.length == 1)
            {
                //Addes homes that starts with arg
                autoComplete.addAll(pd.getHomeList(args[0]));
                autoComplete.addAll(DefaultCommands(args[0]));
            }
            if(args.length == 2)
            {
                
            }
            return autoComplete;
        }
        return null;
    }
    /**
     * Returns main commands list
     * @param arg String commands starts with. Give empty string for full list
     * @return List of commands starting with arg
     */
    private ArrayList<String> DefaultCommands(String arg)
    {
        //Default commands for player
        ArrayList<String> mainCommands = new ArrayList<>();
        mainCommands.add("set");
        mainCommands.add("delete");
        mainCommands.add("accept");
        mainCommands.add("decline");
        mainCommands.add("invite");
        mainCommands.add("help");
        mainCommands.add("info");
        
        //if args -> get commands that start with args.
        if(!arg.equals(""))
        {
            ArrayList<String> returnList = new ArrayList<>();
            for(String s : mainCommands)
            {
                if(s.toLowerCase().startsWith(arg.toLowerCase()))
                {
                    returnList.add(s);
                }
            }
            return returnList;
        }
        //If no args return full list of commands + homes
        else
        {
            return mainCommands;
        }
    }
}
