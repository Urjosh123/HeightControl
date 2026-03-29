package dev.ghost.heightcontrol.listeners;

import dev.ghost.heightcontrol.HeightControl;
import dev.ghost.heightcontrol.managers.AreaManager;
import dev.ghost.heightcontrol.managers.SelectionManager;
import dev.ghost.heightcontrol.models.HeightArea;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ShovelListener implements Listener {

    private final HeightControl plugin;
    private final SelectionManager selectionManager;
    private final AreaManager areaManager;

    public ShovelListener(HeightControl plugin) {
        this.plugin = plugin;
        this.selectionManager = plugin.getSelectionManager();
        this.areaManager = plugin.getAreaManager();
    }

    /** Check if the item is our special selection shovel */
    public static boolean isSelectionShovel(ItemStack item, HeightControl plugin) {
        if (item == null || item.getType() != Material.WOODEN_SHOVEL) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(plugin.getShovelKey(), PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isSelectionShovel(item, plugin)) return;

        // Cancel default shovel behaviour
        event.setCancelled(true);

        Block block = event.getClickedBlock();
        if (block == null) return;
        Location loc = block.getLocation();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            selectionManager.setPos1(player.getUniqueId(), loc);
            player.sendMessage(Component.text("✔ Position 1 set: ", NamedTextColor.GOLD)
                    .append(Component.text(formatLoc(loc), NamedTextColor.YELLOW)));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            selectionManager.setPos2(player.getUniqueId(), loc);
            player.sendMessage(Component.text("✔ Position 2 set: ", NamedTextColor.GOLD)
                    .append(Component.text(formatLoc(loc), NamedTextColor.YELLOW)));
        }

        SelectionManager.Selection sel = selectionManager.get(player.getUniqueId());
        if (sel != null && sel.isComplete() && sel.sameWorld()) {
            int sizeX = Math.abs(sel.pos2.getBlockX() - sel.pos1.getBlockX()) + 1;
            int sizeZ = Math.abs(sel.pos2.getBlockZ() - sel.pos1.getBlockZ()) + 1;
            player.sendMessage(Component.text("  Selection: " + sizeX + " × " + sizeZ + " blocks. Use ", NamedTextColor.GRAY)
                    .append(Component.text("/savearea <name>", NamedTextColor.AQUA))
                    .append(Component.text(" to save.", NamedTextColor.GRAY)));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Let operators with bypass permission ignore restrictions
        if (player.hasPermission("heightcontrol.bypass")) return;

        Block block = event.getBlockPlaced();
        Location loc = block.getLocation();

        HeightArea area = areaManager.getAreaAt(loc);
        if (area == null) return;

        // Block Y is 0-based; maxBuildHeight is the exclusive upper limit (like vanilla)
        if (loc.getBlockY() >= area.getMaxBuildHeight()) {
            event.setCancelled(true);
            player.sendMessage(Component.text("✘ Build height in area ", NamedTextColor.RED)
                    .append(Component.text("[" + area.getName() + "]", NamedTextColor.GOLD))
                    .append(Component.text(" is limited to Y " + area.getMaxBuildHeight() + ".", NamedTextColor.RED)));
        }
    }

    private String formatLoc(Location loc) {
        return "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }
}
