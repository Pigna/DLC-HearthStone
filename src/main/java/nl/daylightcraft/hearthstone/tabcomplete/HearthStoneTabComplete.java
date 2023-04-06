/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.daylightcraft.hearthstone.tabcomplete;

import nl.daylightcraft.hearthstone.HearthStone;
import nl.daylightcraft.hearthstone.PlayerData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * @author myron
 */
public class HearthStoneTabComplete implements TabCompleter
{
    private final HearthStone hs;

    public HearthStoneTabComplete(HearthStone hs)
    {
        this.hs = hs;
    }

    /**
     * AutoComplete commands when tab is pressed
     *
     * @param sender Player or Console
     * @param cmd Command used
     * @param commandLabel a
     * @param args Arguments given
     * @return A list of suggestions
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args)
    {
        if (!cmd.getName().equalsIgnoreCase("hs") && !cmd.getName().equalsIgnoreCase("hearthstone")) {
            return Collections.emptyList();
        }
        if (!(sender instanceof Player))
        {
            sender.sendMessage("TabComplete is not for console.");
            return Collections.emptyList();
        }

        Player player = (Player) sender;
        PlayerData pd = hs.getPlayerData(player);
        ArrayList<String> autoComplete = new ArrayList<>();
        if (args.length == 1)
        {
            //Adds homes that starts with arg
            autoComplete.addAll(pd.getHomeList(args[0]));
            autoComplete.addAll(defaultCommands(args[0]));
        }
        if (args.length >= 2)
        {
            switch (args[0].toLowerCase())
            {
                case "delete", "del" ->
                    //return list of current homes
                    autoComplete.addAll(pd.getHomeList(args[1]));
                case "invite", "inv" -> {
                    if (args.length == 2)
                    {
                        //return list of current online players names
                        autoComplete.addAll(hs.getOnlinePlayerNames(args[1]));
                    }
                    else if (args.length == 3)
                    {
                        //return list of current homes
                        autoComplete.addAll(pd.getHomeList(args[2]));
                    }
                }
                default -> {

                }
            }
        }
        return autoComplete;
    }

    /**
     * Returns main commands list
     *
     * @param arg String commands starts with. Give empty string for full list
     * @return List of commands starting with arg
     */
    private ArrayList<String> defaultCommands(String arg)
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
        if (!arg.isEmpty())
        {
            ArrayList<String> returnList = new ArrayList<>();
            for (String s : mainCommands)
            {
                if (s.toLowerCase().startsWith(arg.toLowerCase()))
                {
                    returnList.add(s);
                }
            }
            return returnList;
        }
        //If no args return full list of commands + homes

        return mainCommands;
    }
}
