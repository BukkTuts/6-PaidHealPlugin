package com.bukkthat.paidhealplugin;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main HealPlugin class that handles all of the
 * plugin functionality.  It extends JavaPlugin
 * because it's a Bukkit plugin, but it does not
 * implement Listener because it does not have
 * any EventHandlers within it.
 * 
 * @author gomeow
 * @author Tzeentchful
 */
public class PaidHealPlugin extends JavaPlugin {
	
	/* This holds the link to vault so we don't need to grab it every time we want access to vault.
	 * It's to null initially as we set it in our onEnable.
	 * It's also given a static modifier so that it can be accessed by external classes (not covered in this tutorial).
	 */
	public static Economy economy = null;
	
	/* This is our global variable to hold how much the /heal command will cost.
	 * It's Given a static modifier so it can be accessed by external classes (not covered in this tutorial).
	 * It's also given the final modifier to make it a constant.
	 */
	public static final double healPrice = 5.0D;
	
	/**
	 * This method is called when the server enables the plugin.
	 */
	public void onEnable() {
		// Call the setupEconomy method which will check if vault is enabled on the server or not.
		// It will return true if vault is enabled and false if it's not.
		if(!setupEconomy()){
			// In the case that vault is not enabled, disable the plugin.
			this.setEnabled(false);
		}
	}

    /* This is the method invoked when a command that we've registered in our plugin.yml
     * is typed by a Player, or the console.
     * <p>
     * The first parameter, the CommandSender, is the person who typed the command.
     * This person could be the console, or it could be a Player.  We should always
     * make sure that the sender is a Player before we cast to a Player.
     * <p>
     * The second parameter, the Command, is the command that they used.  The Command
     * can be used to determine the command that they typed by using Command#getName().
     * In our case, since our Command variable is called cmd, our method invocation would
     * look like cmd.getName().  You can compare the name using String's #equalIgnoreCase
     * to see if they typed a specific command.  We don't do that in this example because
     * we only have one command registered, so there's no need to check the name.
     * <p>
     * The third parameter, the String that we call the label, is the word that the user
     * literally typed to use our command.  If the server is using Bukkit aliasing it is
     * possible for this word to not be our command, so it should only be used when giving
     * a response to the user.  In other words, instead of telling them to type "/heal", we
     * should tell them to type "/"+label as to respect whatever bukkit.yml aliases that the
     * server owner has set up.
     * <p>
     * The fourth parameter, the String array, is any words that they typed after the command,
     * separated by spaces.  That means if they typed "/heal a lot of people", the String array
     * would contain "a", "lot", "of", "people".  To combine the words into a single String we
     * can use org.apache.commons.lang.StringUtils.join with the appropriate arguments.
     * <p>
     * The return value determines whether or not the usage message that we put in our plugin.yml
     * is displayed to the user.  If we return true then the message isn't shown.  If we
     * return false then the usage message is sent to the CommandSender.
     */
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Check to see if they only typed "/heal", with nothing after "heal".
        if(args.length == 0) {
            // The CommandSender could be the console instead of a Player.
            // We need to make sure they're a Player if we're going to heal them.
            if(sender instanceof Player) {
                // We can now safely cast the CommandSender to a Player
                Player player = (Player) sender;
                // Get the player's balance and store it
                double playerBalance = economy.getBalance(player.getName());
                // Permissions check, does the player have the heal.self node?
                if(player.hasPermission("heal.self")) {
                	// Check if they have enough money to use the command
                	if(playerBalance >= healPrice) {
                		// Set their health back to the maximum
                		player.setHealth(player.getMaxHealth());
                		// Feed them to 20/20
                		player.setFoodLevel(20);
                		// Send them a chat message
                		player.sendMessage(ChatColor.GREEN + "You have been fed and healed!");
                	} else {
                		// If they don't have enough money, tell them
                		player.sendMessage(ChatColor.RED + "You do not have enough money to use this command!");
                		player.sendMessage(ChatColor.GREEN + "You require " + ChatColor.GOLD + economy.format(healPrice - playerBalance));
                	}
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to do that!");
                }
            } else {
                // If they're not a Player(the console, in other words), we should tell them that they can't
                // use the command.
                sender.sendMessage(ChatColor.RED + "Only a player has health!");
            }
            // Check if they typed a word after "/heal".
        } else if(args.length == 1) {
        	// Permissions check, does the player have the heal.others node?
        	if(sender.hasPermission("heal.others")) {
        		// Check if our sender is a player and check if they have enough money
        		if(sender instanceof Player && economy.getBalance(((Player) sender).getName()) >= healPrice) {
        			// Get the player using the username supplied in the first argument
        			Player target = Bukkit.getPlayer(args[0]);
        			// Make sure the player is online.
        			// If they're offline, Bukkit.getPlayer will return null.
        			if(target == null) {
        				sender.sendMessage(ChatColor.RED + "That player is not online!");
        			} else {
        				// Set their health back to the maximum
        				target.setHealth(target.getMaxHealth());
        				// Feed them to 20/20
        				target.setFoodLevel(20);
        				sender.sendMessage(ChatColor.GREEN + target.getName() + " was healed and fed!");
        				target.sendMessage(ChatColor.GREEN + "You were healed and fed!");
        			}
        		}else{
        			// If they don't have enough money, tell them
        			sender.sendMessage(ChatColor.RED + "You do not have enough money to use this command!");
            		sender.sendMessage(ChatColor.GREEN + "You require " + ChatColor.GOLD + economy.format(healPrice - economy.getBalance(((Player) sender).getName())));
        		}
        	} else {
        		// If they don't have the required permission, tell them.
        		sender.sendMessage(ChatColor.RED + "You do not have permission to do that!");
            }
        } else {
            // Return false, this will output the usage message from the plugin.yml file
            return false;
        }
        // By default we should always return true.  If we return false the usage message from
        // our plugin.yml is sent to the CommandSender.
        return true;
    }
    
    /**
     * This method comes from vault's bukkit dev page http://dev.bukkit.org/server-mods/vault/#w-linking-vault
     * This method will attempt to get the economy provider from vault and set our economy variable.
     * 
     * @return If it was successful in getting the economy provider from vault.
     */
    private boolean setupEconomy() {
    	// Gets the service provider for vault
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        // Check that we were successful in getting the provider
        if (economyProvider != null) {
        	// Set our static economy variable
            economy = economyProvider.getProvider();
        }
        //Return if we where successful or not
        return (economy != null);
    }

}
