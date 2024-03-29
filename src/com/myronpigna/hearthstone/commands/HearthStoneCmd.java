/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myronpigna.hearthstone.commands;

import com.myronpigna.hearthstone.Cooldown;
import com.myronpigna.hearthstone.HearthStone;
import com.myronpigna.hearthstone.Invite;
import com.myronpigna.hearthstone.PlayerData;
import com.sun.org.apache.bcel.internal.classfile.Unknown;
import net.minecraft.server.v1_16_R1.ChatMessageType;
import net.minecraft.server.v1_16_R1.IChatBaseComponent;
import net.minecraft.server.v1_16_R1.PacketPlayOutChat;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author myron
 */
public class HearthStoneCmd implements CommandExecutor
{
    private HearthStone hs;

    public HearthStoneCmd(HearthStone hs)
    {
        this.hs = hs;
    }

    private void setNewHeartStone(CommandSender sender, String[] args, PlayerData pd)
    {
        if (!sender.hasPermission("hearthstone.set")) NoPermission(pd);
        else if (args.length == 2)
            if (pd.setHome(args[1])) pd.sendMessage("HearthStone " + args[1] + " is set.");
            else pd.sendMessage("You have reached your maximum amount of locations.");
        else pd.sendMessage("Set Syntax : /hs set [name]");
    }

    private void deleteHearthStone(String[] args, PlayerData pd)
    {
        if (args.length == 2)
        {
            if (pd.removeHome(args[1]))
            {
                pd.sendMessage("HearthStone " + args[1] + " is deleted.");
            }
            else
            {
                pd.sendMessage("No such HearthStone with that name.");
            }
        }
        else
        {
            pd.sendMessage("Delete Syntax : /hs delete [name]");
        }
    }

    private void acceptHeartStone(String[] args, Player player, PlayerData pd)
    {
        boolean override = checkOverride(args);
        Invite inviteAccepted = hs.getInvite(player.getName());
        if (inviteAccepted != null)
        {
            if (!pd.hasCooldown(Cooldown.ACCEPTED))
            {
                inviteAccepted.InviteAccepted(override);
                hs.deleteInvite(player.getName());
            }
            else
            {
                pd.sendMessage("You have a invite teleport cooldown active for " + hs.getTimeRemaining(pd.getCooldown(Cooldown.ACCEPTED), Cooldown.ACCEPTED));
            }
        }
        else pd.sendMessage("You have no HearthStone invites active.");
    }

    private void declineHeartStone(Player player, PlayerData pd)
    {
        Invite inviteDeclined = hs.getInvite(player.getName());
        if (inviteDeclined != null)
        {
            inviteDeclined.InviteDeclined();
            hs.deleteInvite(player.getName());
        }
        else
        {
            pd.sendMessage("You have no HearthStone invites active.");
        }
    }

    private void inviteHeartStone(CommandSender sender, String[] args, Player player, PlayerData pd)
    {
        if (!sender.hasPermission("hearthstone.invite"))
        {
            NoPermission(pd);
        }
        else if (args.length == 1)
        {
            //show help
            pd.sendMessage("Invite Syntax : /hs invite [Player name] <HearthStone name>");
        }
        else if (args.length == 2)
        {
            Player targetPlayer = hs.getServer().getPlayer(args[1]);
            if (targetPlayer != null)
            {
                int amount = pd.hasHome();
                if (amount > 0)
                {
                    if (amount > 1)
                    {
                        //error
                        pd.sendMessage("Give a HearthStone name as parameter.");
                    }
                    else if (!pd.hasCooldown(Cooldown.INVITE))
                    {
                        SendInvite(targetPlayer, player, pd, pd.getHomeName());
                    }
                    else
                    {
                        //error
                        pd.sendMessage("You have a invite cooldown active for " + hs.getTimeRemaining(pd.getCooldown(Cooldown.INVITE), Cooldown.INVITE));
                    }
                }
                else
                {
                    //error
                    pd.sendMessage("You have no set HearthStone to invite someone to.");
                }
            }
            else
            {
                pd.sendMessage("Target Player is not online or invalid.");
            }
        }
        else if (args.length == 3)
        {
            int amount = pd.hasHome();
            if (amount > 0)
            {
                Player targetPlayer = hs.getServer().getPlayer(args[1]);
                if (targetPlayer == null)
                {
                    //error
                    pd.sendMessage("No online player named '" + args[1] + "' could be found.");
                }
                else if (pd.hasHome(args[2]))
                {
                    pd.sendMessage("Invite has been send to '" + targetPlayer.getName() + "'.");
                    SendInvite(targetPlayer, player, pd, args[2]);
                }
                else
                {
                    //error
                    pd.sendMessage("You have no HearthStone named '" + args[2] + "'.");
                }
            }
            else
            {
                //error
                pd.sendMessage("You have no set HearthStone to invite someone to.");
            }
        }
    }

    private void locateHeartStone(String[] args, Player player, PlayerData pd)
    {
        if (player.hasPermission("hearthstone.locate"))
        {
            if (args.length != 3)
            {
                pd.sendMessage("Locate Syntax : /hs locate [Player name] [Location name]");
            }
            else
            {
                Player targetPlayer = hs.getServer().getPlayer(args[1]);
                if (targetPlayer != null)
                {
                    PlayerData targetPlayerData = hs.getPlayerData(targetPlayer);
                    if (!targetPlayerData.hasHome(args[2]))
                        pd.sendMessage("Targeted player does not have a home with that name.");
                    else
                    {
                        pd.sendMessage(targetPlayerData.getHomeLocationString(args[2]));
                    }
                }
                else
                {
                    OfflinePlayer targetOfflinePlayer = hs.getServer().getOfflinePlayer(hs.getUUIDfromPlayerListFile(args[1]));
                    if (targetOfflinePlayer == null) pd.sendMessage("No player with that name exists.");
                    else if (!targetOfflinePlayer.hasPlayedBefore())
                        pd.sendMessage("That player has never played on this server before.");
                    else
                    {
                        PlayerData targetPlayerData = hs.getPlayerData((Player) targetOfflinePlayer);
                        if (!targetPlayerData.hasHome(args[2]))
                            pd.sendMessage("Targeted player does not have a location with that name.");
                        else
                        {
                            pd.sendMessage(targetPlayerData.getHomeLocationString(args[2]));
                        }
                    }
                }
            }
        }
        else
        {
            NoPermission(pd);
        }
    }

    private void resetHeartStone(CommandSender sender, String[] args, PlayerData pd)
    {
        if (!sender.hasPermission("hearthstone.reset"))
        {
            NoPermission(pd);
        }
        else if (args.length == 2)
        {
            Player targetPlayer = hs.getServer().getPlayer(args[1]);
            if (targetPlayer != null)
            {
                PlayerData targetPlayerData = hs.getPlayerData(targetPlayer);
                targetPlayerData.resetCooldown();
                pd.sendMessage(args[1] + "\'s cooldowns have been reset.");
                targetPlayerData.sendMessage("Your cooldowns have been reset.");
            }
            else
            {
                pd.sendMessage("No online player named '" + args[1] + "' could be found.");
            }
        }
        else
        {
            pd.sendMessage("Set Syntax : /hs reset [player name]");
        }
    }

    private void defaultHeartStone(String[] args, Player player, PlayerData pd)
    {
        boolean override = checkOverride(args);
        if (args.length == 1 || (args.length == 2 && override))
        {
            String homeName = args[0].toLowerCase();
            if (homeName.indexOf(":") > 0)//check position off.
            {
                if (player.hasPermission("hearthstone.use.other"))
                {
                    String[] input = homeName.split(":");
                    Player targetPlayer = hs.getServer().getPlayer(input[0]);
                    if (targetPlayer != null)
                    {
                        OtherPlayerData(pd, input, targetPlayer, override);
                    }
                    else
                    {
                        OtherOfflinePlayerData(pd, input, override);
                    }
                }
                else
                {
                    NoPermission(pd);
                }
            }
            else if (pd.hasHome(homeName))
            {
                pd.teleportPlayerLocation(homeName, pd.getHomeLocation(homeName), Cooldown.USAGE, override);
            }
            else // error
            {
                pd.sendMessage("You have no HearthStone with that name.");
            }
        }
        else if (pd.hasHome() == 1)
        {
            pd.teleportPlayerLocation(args[0], pd.getHomeLocation(), Cooldown.USAGE, override);
        }
        else if (pd.hasHome() > 1) //error
        {
            pd.sendMessage("Give a HearthStone name as parameter.");
        }
        else//error
        {
            pd.sendMessage("You have no HearthStone set. View /hs help for extra information.");
        }
    }

    private void OtherPlayerData (PlayerData pd, String[] input, Player targetPlayer, Boolean override)
    {
        PlayerData targetPlayerData = hs.getPlayerData(targetPlayer);
        if (input.length == 1)
        {
            OtherPlayerHomes(targetPlayerData, pd, input[0]);
        }
        else if (!targetPlayerData.hasHome(input[1]))
        {
            pd.sendMessage("Targeted player does not have a home with that name.");
        }
        else
        {
            pd.teleportPlayerLocation(input[1], targetPlayerData.getHomeLocation(input[1]), Cooldown.OTHER, override);
        }
    }
    
    private void OtherOfflinePlayerData (PlayerData pd, String[] input, Boolean override)
    {
        try
        {
            UUID uuid = hs.getUUIDfromPlayerListFile(input[0]);
            if (uuid == null)
            {
                pd.sendMessage("Player name does not match any known playernames.");
                return;
            }
            OfflinePlayer targetOfflinePlayer = hs.getServer().getOfflinePlayer(uuid);
            if (targetOfflinePlayer == null)
            {
                pd.sendMessage("No player with that name exists.");
            }
            else if (!targetOfflinePlayer.hasPlayedBefore())
            {
                pd.sendMessage("That player has never played on this server before.");
            }
            else
            {
                PlayerData targetPlayerData = new PlayerData(targetOfflinePlayer, hs);
                if (input.length == 1)
                {
                    OtherPlayerHomes(targetPlayerData, pd, input[0]);
                }
                else if (!targetPlayerData.hasHome(input[1]))
                {
                    pd.sendMessage("Targeted player does not have a location with that name.");
                }
                else
                {
                    pd.teleportPlayerLocation(input[1], targetPlayerData.getHomeLocation(input[1]), Cooldown.OTHER, override);
                }
            }
        }
        catch(IllegalArgumentException ex)
        {
            pd.sendMessage("Error getting player data. Is the name correct?");
            Logger.getAnonymousLogger().log(Level.SEVERE, "IllegalArgumentException : Error getting UUID of a offline player", ex);
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
    {
        if (!(sender instanceof Player))
        {
            sender.sendMessage("the console can't use any HearthStone commands");
            return false;
        }

        Player player = (Player) sender;
        PlayerData pd = hs.getPlayerData(player);
        if (!sender.hasPermission("hearthstone.use"))
        {
            NoPermission(pd);
            return false;
        }

        if (args.length == 0)
        {
            if (pd.hasHome() > 0)
            {
                PlayerHomes(pd);
                return true;
            }

            pd.sendMessage("You have no HearthStone set. View /hs help for extra information.");
            return true;
        }

        if (args.length >= 1)
        {
            switch (args[0].toLowerCase())
            {
                case "set":
                    //Set new Hearthstone
                    setNewHeartStone(sender, args, pd);
                    break;
                case "delete"://Deletes current hearthstone
                case "del":  //Alias for delete
                    deleteHearthStone(args, pd);
                    break;
                case "accept":
                    //accepts heartstone
                    acceptHeartStone(args, player, pd);
                    break;
                case "decline":
                    declineHeartStone(player, pd);
                    break;
                case "help":
                case "h":
                    //View info about hearthstone
                    pd.sendMessage("- /hs - To see your current HearthStone(hs) locations");
                    pd.sendMessage("- /hs set [name] - To set a HearthStone location with a name");
                    pd.sendMessage("- /hs delete [name] - To delete a HearthStone location by name");
                    pd.sendMessage("- /hs invite [player] [name] - To invite a player to your HS location by player name and HS name");
                    pd.sendMessage("- /hs accept - To accept a HearthStone invite");
                    pd.sendMessage("- /hs decline - To decline a HearthStone invite");
                    pd.sendMessage("- /hs info - Information about the creator of the HearthStone plugin and its features");
                    break;
                case "invite":
                case "inv":
                    inviteHeartStone(sender, args, player, pd);
                    break;
                case "request":
                case "req":
                    //TODO: Send request to player to go to his home
                    //pd.sendMessage("Request Syntax : /hs request [Player name]");
                    pd.sendMessage("Not in use.");
                    break;
                case "info":
                    pd.sendMessage("This plugin [HearthStone] is created by Myron Antonissen for the DayLightCraft server.");
                    pd.sendMessage("HearthStone is a 'home' plugin to save locations and to be able to return to them, while also being able to invite other players to the location.");
                    break;
                case "reset":
                    //Resets the cooldowns of a player
                    resetHeartStone(sender, args, pd);
                    break;
                case "locate":
                    //Get the location of a set HS of a player
                    locateHeartStone(args, player, pd);
                    break;
                default:
                    defaultHeartStone(args, player, pd);
                    break;
            }
        }
        return true;
    }

    private void SendInvite(Player pTarget, Player player, PlayerData pd, String name)
    {
        PlayerData targetpd = hs.getPlayerData(pTarget);

        PacketPlayOutChat packet = new PacketPlayOutChat(
                IChatBaseComponent.ChatSerializer.a("[\"\",{\"text\":\"[Accept]\",\"color\":\"green\",\"bold\":true,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/hs accept\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"\",\"extra\":[{\"text\":\"Accept HearthStone invite.\",\"color\":\"green\"}]}}},{\"text\":\" or \",\"color\":\"none\",\"bold\":false},{\"text\":\"[Decline]\",\"color\":\"red\",\"bold\":true,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/hs decline\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"\",\"extra\":[{\"text\":\"Decline HearthStone invite.\",\"color\":\"red\"}]}}}]"),
                ChatMessageType.SYSTEM,
                UUID.randomUUID()
                );
        targetpd.sendMessage(player.getName() + " invited you to his HearthStone '" + name + "' will expire in " + hs.getInviteTimeoutSec() + " sec. click a option below:");
        targetpd.sendChatPacket(packet);
        hs.addInvite(targetpd, pd, name);
    }

    private void OtherPlayerHomes(PlayerData tpd, PlayerData pd, String playername)
    {
        String ClickableHearthStoneList = "\"\"";
        //displays list of homes and help
        for (String s : tpd.getHomes().keySet())
        {
            ClickableHearthStoneList = ClickableHearthStoneList + ",{\"text\":\"" + s + " \",\"color\":\"green\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/hs " + playername + ":" + s + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"\",\"extra\":[{\"text\":\"Use HearthStone to " + s + " from " + playername + ".\"}]}}}";
        }
        PacketPlayOutChat packet = new PacketPlayOutChat(
                IChatBaseComponent.ChatSerializer.a("[" + ClickableHearthStoneList + "]"),
                ChatMessageType.SYSTEM,
                UUID.randomUUID()
        );
        pd.sendMessage(playername + "'s current HearthStone's are:");
        pd.sendChatPacket(packet);
    }

    private void PlayerHomes(PlayerData pd)
    {
        String ClickableHearthStoneList = "\"\"";
        //displays list of homes and help
        for (String s : pd.getHomes().keySet())
        {
            ClickableHearthStoneList = ClickableHearthStoneList + ",{\"text\":\"" + s + " \",\"color\":\"green\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/hs " + s + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"\",\"extra\":[{\"text\":\"Use HearthStone to " + s + ".\"}]}}}";
        }
        PacketPlayOutChat packet = new PacketPlayOutChat(
                IChatBaseComponent.ChatSerializer.a("[" + ClickableHearthStoneList + "]"),
                ChatMessageType.SYSTEM,
                UUID.randomUUID()
        );
        pd.sendMessage("Your current HearthStone's are:");
        pd.sendChatPacket(packet);
    }

    /**
     * Called upon when the Player does not have the permission to use a command.
     *
     * @pd is needed to send the player the message with layout template.
     */
    private void NoPermission(PlayerData pd)
    {
        pd.sendMessage("You have no permission to use this HearthStone command.");
    }

    private boolean checkOverride(String[] args)
    {
        return Arrays.stream(args).anyMatch(arg -> arg.contains("!"));
    }

}
