package me.shadowsmp;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class HeartManager implements Listener {

    private static final double MAX_HEARTS = 20.0;

    private final ShadowSmpCore plugin;
    private final NamespacedKey heartKey;

    public HeartManager(ShadowSmpCore plugin) {
        this.plugin = plugin;
        this.heartKey = new NamespacedKey(plugin, "heart_item");

        registerRecipe();
    }

    public ItemStack createHeart() {
        ItemStack heart = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = heart.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "❤ Heart");

            meta.getPersistentDataContainer().set(
                    heartKey,
                    PersistentDataType.BYTE,
                    (byte) 1
            );

            heart.setItemMeta(meta);
        }

        return heart;
    }

    private void registerRecipe() {
        NamespacedKey recipeKey =
                new NamespacedKey(plugin, "heart_recipe");

        ShapedRecipe recipe =
                new ShapedRecipe(recipeKey, createHeart());

        recipe.shape(
                "DND",
                "NIN",
                "DND"
        );

        recipe.setIngredient('D', Material.DIAMOND_BLOCK);
        recipe.setIngredient('N', Material.NETHER_STAR);
        recipe.setIngredient('I', Material.NETHERITE_INGOT);

        plugin.getServer().addRecipe(recipe);
    }

    @EventHandler
    public void onHeartUse(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) {
            return;
        }

        ItemStack item = event.getItem();

        if (!isHeart(item)) {
            return;
        }

        Player player = event.getPlayer();

        var attribute =
                player.getAttribute(Attribute.MAX_HEALTH);

        if (attribute == null) {
            return;
        }

        double currentHearts =
                attribute.getBaseValue() / 2.0;

        if (currentHearts >= MAX_HEARTS) {
            player.sendMessage(
                    ChatColor.RED +
                    "❤ You already have 20 hearts!"
            );

            event.setCancelled(true);
            return;
        }

        attribute.setBaseValue(
                Math.min(
                        attribute.getBaseValue() + 2.0,
                        MAX_HEARTS * 2.0
                )
        );

        removeOneHeart(
                item,
                event.getHand(),
                player
        );

        player.sendMessage(
                ChatColor.GREEN +
                "❤ You gained 1 heart!"
        );

        event.setCancelled(true);
    }

    private void removeOneHeart(
            ItemStack item,
            EquipmentSlot hand,
            Player player
    ) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            return;
        }

        if (hand == EquipmentSlot.HAND) {
            player.getInventory().setItemInMainHand(null);
        } else if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(null);
        }
    }

    @EventHandler
    public void onHeartDespawn(ItemDespawnEvent event) {
        Item item = event.getEntity();

        if (isHeart(item.getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHeartDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item item)) {
            return;
        }

        if (!isHeart(item.getItemStack())) {
            return;
        }

        switch (event.getCause()) {
            case FIRE:
            case FIRE_TICK:
            case LAVA:
            case HOT_FLOOR:
            case CACTUS:
            case EXPLOSION:
            case BLOCK_EXPLOSION:
                event.setCancelled(true);
                break;
            default:
                break;
        }
    }

    @EventHandler
    public void onHeartDrop(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();

        if (isHeart(item.getItemStack())) {
            item.setCanMobPickup(false);
        }
    }

    @EventHandler
    public void onHeartContainerPickup(
            InventoryPickupItemEvent event) {

        Item item = event.getItem();

        if (isHeart(item.getItemStack())) {
            event.setCancelled(true);
        }
    }

    public boolean isHeart(ItemStack item) {
        if (item == null ||
                item.getType() != Material.NETHER_STAR) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return false;
        }

        Byte value =
                meta.getPersistentDataContainer().get(
                        heartKey,
                        PersistentDataType.BYTE
                );

        return value != null &&
                value == (byte) 1;
    }
    }
