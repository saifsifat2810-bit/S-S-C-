package me.shadowsmp;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class LifestealManager implements Listener, CommandExecutor {

    private static final double STARTING_HEARTS = 10.0;
    private static final double MAX_HEARTS = 20.0;

    private final HeartManager heartManager;

    public LifestealManager(HeartManager heartManager) {
        this.heartManager = heartManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore()) {
            setHearts(player, STARTING_HEARTS);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) {
            return;
        }

        double victimHearts = getHearts(victim);
        double killerHearts = getHearts(killer);

        if (victimHearts <= 1.0) {
            killer.sendMessage(
                    ChatColor.YELLOW +
                    "You did not steal a heart."
            );
            return;
        }

        setHearts(victim, victimHearts - 1.0);

        if (killerHearts >= MAX_HEARTS) {
            event.getDrops().add(
                    heartManager.createHeart()
            );

            killer.sendMessage(
                    ChatColor.YELLOW +
                    "You are already at 20 hearts."
            );

            killer.sendMessage(
                    ChatColor.GREEN +
                    "A Heart dropped!"
            );

            return;
        }

        setHearts(killer, killerHearts + 1.0);

        killer.sendMessage(
                ChatColor.GREEN +
                "You stole 1 heart!"
        );

        victim.sendMessage(
                ChatColor.RED +
                "You lost 1 heart!"
        );
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    ChatColor.RED +
                    "Only players can use this command."
            );
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(
                    ChatColor.RED +
                    "Usage: /withdraw <amount>"
            );
            return true;
        }

        int amount;

        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(
                    ChatColor.RED +
                    "Please enter a valid number."
            );
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(
                    ChatColor.RED +
                    "Amount must be greater than 0."
            );
            return true;
        }

        double currentHearts = getHearts(player);

        if (currentHearts - amount < 1.0) {
            player.sendMessage(
                    ChatColor.RED +
                    "You must keep at least 1 heart."
            );
            return true;
        }

        if (getFreeSlots(player) < amount) {
            player.sendMessage(
                    ChatColor.RED +
                    "Your inventory does not have enough space."
            );

            player.sendMessage(
                    ChatColor.YELLOW +
                    "No hearts were removed."
            );

            return true;
        }

        setHearts(
                player,
                currentHearts - amount
        );

        for (int i = 0; i < amount; i++) {
            player.getInventory().addItem(
                    heartManager.createHeart()
            );
        }

        player.sendMessage(
                ChatColor.GREEN +
                "You withdrew " +
                amount +
                " heart(s)!"
        );

        return true;
    }

    private double getHearts(Player player) {
        var attribute =
                player.getAttribute(Attribute.MAX_HEALTH);

        if (attribute == null) {
            return STARTING_HEARTS;
        }

        return attribute.getBaseValue() / 2.0;
    }

    private void setHearts(
            Player player,
            double hearts
    ) {
        var attribute =
                player.getAttribute(Attribute.MAX_HEALTH);

        if (attribute == null) {
            return;
        }

        hearts = Math.max(
                1.0,
                Math.min(hearts, MAX_HEARTS)
        );

        attribute.setBaseValue(
                hearts * 2.0
        );

        if (player.getHealth() > hearts * 2.0) {
            player.setHealth(hearts * 2.0);
        }
    }

    private int getFreeSlots(Player player) {
        int free = 0;

        for (ItemStack item :
                player.getInventory().getStorageContents()) {

            if (item == null ||
                    item.getType() == Material.AIR) {
                free++;
            }
        }

        return free;
    }
    }
