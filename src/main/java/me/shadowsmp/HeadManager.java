package me.shadowsmp;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HeadManager implements Listener {

    private final ShadowSmpCore plugin;

    private final NamespacedKey playerHeadKey;
    private final NamespacedKey banHeadKey;
    private final NamespacedKey targetKey;

    private final Map<UUID, Boolean> bannedPlayers =
            new HashMap<>();

    public HeadManager(ShadowSmpCore plugin) {
        this.plugin = plugin;

        playerHeadKey =
                new NamespacedKey(plugin, "player_head");

        banHeadKey =
                new NamespacedKey(plugin, "ban_head");

        targetKey =
                new NamespacedKey(plugin, "target_uuid");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) {
            return;
        }

        double hearts = getHearts(victim);

        if (hearts > 1.0) {
            return;
        }

        ItemStack banHead = createBanHead(victim);

        event.getDrops().add(banHead);

        bannedPlayers.put(
                victim.getUniqueId(),
                true
        );

        killer.sendMessage(
                ChatColor.RED +
                victim.getName() +
                " has been banned."
        );
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {

        Player player = event.getPlayer();

        if (!bannedPlayers.containsKey(
                player.getUniqueId())) {
            return;
        }

        Location spawn =
                player.getServer()
                        .getWorlds()
                        .get(0)
                        .getSpawnLocation();

        event.setRespawnLocation(spawn);

        plugin.getServer().getScheduler().runTask(
                plugin,
                () -> applyBanState(player)
        );
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        if (bannedPlayers.containsKey(
                player.getUniqueId())) {

            applyBanState(player);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {

        Player player = event.getPlayer();

        if (!bannedPlayers.containsKey(
                player.getUniqueId())) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) {
            return;
        }

        if (from.getX() != to.getX()
                || from.getY() != to.getY()
                || from.getZ() != to.getZ()) {

            Location locked = from.clone();

            locked.setYaw(to.getYaw());
            locked.setPitch(to.getPitch());

            event.setTo(locked);
        }
    }

    @EventHandler
    public void onBanHeadPlace(BlockPlaceEvent event) {

        ItemStack item =
                event.getItemInHand();

        if (!isBanHead(item)) {
            return;
        }

        Player reviver =
                event.getPlayer();

        UUID targetUUID =
                getTargetUUID(item);

        if (targetUUID == null) {
            event.setCancelled(true);
            return;
        }

        Player target =
                plugin.getServer()
                        .getPlayer(targetUUID);

        if (target == null) {

            reviver.sendMessage(
                    ChatColor.RED +
                    "That player is offline."
            );

            event.setCancelled(true);
            return;
        }

        bannedPlayers.remove(targetUUID);

        Location reviveLocation =
                event.getBlock()
                        .getLocation()
                        .add(0.5, 1.0, 0.5);

        target.teleport(reviveLocation);

        target.setGameMode(
                GameMode.SURVIVAL
        );

        setHearts(target, 1.0);

        target.setHealth(2.0);

        target.sendMessage(
                ChatColor.GREEN +
                "You have been revived!"
        );

        reviver.sendMessage(
                ChatColor.GREEN +
                "You revived " +
                target.getName() +
                "!"
        );

        event.setCancelled(true);

        ItemStack hand =
                reviver.getInventory()
                        .getItemInMainHand();

        if (hand.getAmount() > 1) {
            hand.setAmount(
                    hand.getAmount() - 1
            );
        } else {
            reviver.getInventory()
                    .setItemInMainHand(null);
        }
    }

    @EventHandler
    public void onHeadDrop(
            PlayerDropItemEvent event) {

        Item item =
                event.getItemDrop();

        if (!isPlayerHead(
                item.getItemStack())) {
            return;
        }

        item.setCanMobPickup(false);
    }

    @EventHandler
    public void onHeadBurn(
            EntityCombustEvent event) {

        Entity entity =
                event.getEntity();

        if (!(entity instanceof Item item)) {
            return;
        }

        if (isPlayerHead(
                item.getItemStack())) {

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHeadDamage(
            EntityDamageEvent event) {

        Entity entity =
                event.getEntity();

        if (!(entity instanceof Item item)) {
            return;
        }

        if (!isPlayerHead(
                item.getItemStack())) {
            return;
        }

        EntityDamageEvent.DamageCause cause =
                event.getCause();

        if (cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.HOT_FLOOR
                || cause == EntityDamageEvent.DamageCause.CACTUS
                || cause == EntityDamageEvent.DamageCause.EXPLOSION
                || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event) {

        ItemStack current =
                event.getCurrentItem();

        ItemStack cursor =
                event.getCursor();

        if (!isPlayerHead(current)
                && !isPlayerHead(cursor)) {
            return;
        }

        if (!(event.getWhoClicked()
                instanceof Player player)) {
            return;
        }

        String type =
                event.getView()
                        .getTopInventory()
                        .getType()
                        .name();

        if (!type.equals("ENDER_CHEST")
                && event.getRawSlot()
                < event.getView()
                        .getTopInventory()
                        .getSize()) {

            event.setCancelled(true);

            player.sendMessage(
                    ChatColor.RED +
                    "Player Heads cannot be stored here."
            );
        }
    }

    @EventHandler
    public void onInventoryDrag(
            InventoryDragEvent event) {

        if (!isPlayerHead(
                event.getOldCursor())) {
            return;
        }

        String type =
                event.getView()
                        .getTopInventory()
                        .getType()
                        .name();

        if (!type.equals("ENDER_CHEST")) {
            event.setCancelled(true);
        }
    }

    private ItemStack createPlayerHead(
            Player player) {

        ItemStack head =
                new ItemStack(Material.PLAYER_HEAD);

        ItemMeta meta =
                head.getItemMeta();

        if (meta == null) {
            return head;
        }

        meta.setDisplayName(
                ChatColor.YELLOW +
                player.getName() +
                "'s Head"
        );

        meta.getPersistentDataContainer().set(
                playerHeadKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        meta.getPersistentDataContainer().set(
                targetKey,
                PersistentDataType.STRING,
                player.getUniqueId().toString()
        );

        head.setItemMeta(meta);

        return head;
    }

    private ItemStack createBanHead(
            Player player) {

        ItemStack head =
                createPlayerHead(player);

        ItemMeta meta =
                head.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(
                    ChatColor.DARK_RED +
                    "Ban Head: " +
                    player.getName()
            );

            meta.getPersistentDataContainer().set(
                    banHeadKey,
                    PersistentDataType.BYTE,
                    (byte) 1
            );

            head.setItemMeta(meta);
        }

        return head;
    }

    private boolean isPlayerHead(
            ItemStack item) {

        if (item == null
                || item.getType()
                != Material.PLAYER_HEAD) {
            return false;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return false;
        }

        Byte value =
                meta.getPersistentDataContainer()
                        .get(
                                playerHeadKey,
                                PersistentDataType.BYTE
                        );

        return value != null
                && value == (byte) 1;
    }

    private boolean isBanHead(
            ItemStack item) {

        if (!isPlayerHead(item)) {
            return false;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return false;
        }

        Byte value =
                meta.getPersistentDataContainer()
                        .get(
                                banHeadKey,
                                PersistentDataType.BYTE
                        );

        return value != null
                && value == (byte) 1;
    }

    private UUID getTargetUUID(
            ItemStack item) {

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return null;
        }

        String value =
                meta.getPersistentDataContainer()
                        .get(
                                targetKey,
                                PersistentDataType.STRING
                        );

        if (value == null) {
            return null;
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private double getHearts(Player player) {

        var attribute =
                player.getAttribute(
                        Attribute.MAX_HEALTH
                );

        if (attribute == null) {
            return 10.0;
        }

        return attribute.getBaseValue() / 2.0;
    }

    private void setHearts(
            Player player,
            double hearts) {

        var attribute =
                player.getAttribute(
                        Attribute.MAX_HEALTH
                );

        if (attribute == null) {
            return;
        }

        hearts = Math.max(
                1.0,
                Math.min(hearts, 20.0)
        );

        attribute.setBaseValue(
                hearts * 2.0
        );

        if (player.getHealth()
                > hearts * 2.0) {

            player.setHealth(
                    hearts * 2.0
            );
        }
    }

    private void applyBanState(
            Player player) {

        player.setGameMode(
                GameMode.SPECTATOR
        );

        player.sendMessage(
                ChatColor.DARK_RED +
                "You are banned until revived."
        );
    }
                          }
