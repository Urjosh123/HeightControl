package dev.ghost.heightcontrol.commands;

import dev.ghost.heightcontrol.HeightControl;
import dev.ghost.heightcontrol.managers.AreaManager;
import dev.ghost.heightcontrol.managers.SelectionManager;
import dev.ghost.heightcontrol.models.HeightArea;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler implements CommandExecutor, TabCompleter {

    private final HeightControl plugin;
    private final AreaManager areaManager;
    private final SelectionManager selectionManager;

    public CommandHandler(HeightControl plugin) {
        this.plugin = plugin;
        this.areaManager = plugin.getAreaManager();
        this.selectionManager = plugin.getSelectionManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();

        switch (name) {
            case "shovel" -> handleShovel(sender);
            case "savearea" -> handleSaveArea(sender, args);
            case "setheight" -> handleSetHeight(sender, args);
            case "listheights" -> handleListHeights(sender);
            case "deletearea" -> handleDeleteArea(sender, args);
            case "areainfo" -> handleAreaInfo(sender, args);
            default -> sender.sendMessage(prefix().append(Component.text("Unknown command.", NamedTextColor.RED)));
        }
        return true;
    }

    // ── /shovel ──────────────────────────────────────────────────────────────

    private void handleShovel(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }
        if (!player.hasPermission("heightcontrol.use")) {
            player.sendMessage(noPerms());
            return;
        }

        ItemStack shovel = new ItemStack(Material.WOODEN_SHOVEL);
        ItemMeta meta = shovel.getItemMeta();
        meta.displayName(Component.text("⛏ Height Selection Tool", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            Component.text("Left-click", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(" → Set Position 1", NamedTextColor.GRAY)),
            Component.text("Right-click", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(" → Set Position 2", NamedTextColor.GRAY)),
            Component.text("Then: /savearea <n>", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(plugin.getShovelKey(), PersistentDataType.BYTE, (byte) 1);
        meta.setUnbreakable(true);
        shovel.setItemMeta(meta);

        player.getInventory().addItem(shovel);
        player.sendMessage(prefix().append(Component.text("Selection shovel given! Left-click = Pos1, Right-click = Pos2.", NamedTextColor.GREEN)));
    }

    // ── /savearea <n> ────────────────────────────────────────────────────

    private void handleSaveArea(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return; }
        if (!player.hasPermission("heightcontrol.use")) { player.sendMessage(noPerms()); return; }

        if (args.length < 1) {
            player.sendMessage(prefix().append(Component.text("Usage: /savearea <n>", NamedTextColor.RED)));
            return;
        }

        String areaName = args[0];
        if (!areaName.matches("[a-zA-Z0-9_\\-]+")) {
            player.sendMessage(prefix().append(Component.text("Area name can only contain letters, numbers, _ and -.", NamedTextColor.RED)));
            return;
        }

        SelectionManager.Selection sel = selectionManager.get(player.getUniqueId());
        if (sel == null || !sel.isComplete()) {
            player.sendMessage(prefix().append(Component.text("You need to select two positions first using ", NamedTextColor.RED))
                .append(Component.text("/shovel", NamedTextColor.GOLD)));
            return;
        }
        if (!sel.sameWorld()) {
            player.sendMessage(prefix().append(Component.text("Both positions must be in the same world.", NamedTextColor.RED)));
            return;
        }

        Location p1 = sel.pos1;
        Location p2 = sel.pos2;
        String worldName = p1.getWorld().getName();

        // Default height = world max build height
        int defaultHeight = p1.getWorld().getMaxHeight();

        HeightArea area = new HeightArea(areaName, worldName,
                p1.getBlockX(), p1.getBlockZ(),
                p2.getBlockX(), p2.getBlockZ(),
                defaultHeight);

        areaManager.addArea(area);
        selectionManager.clear(player.getUniqueId());

        player.sendMessage(prefix()
            .append(Component.text("Area ", NamedTextColor.GREEN))
            .append(Component.text("[" + areaName + "]", NamedTextColor.GOLD))
            .append(Component.text(" saved! (" + area.getSizeX() + "×" + area.getSizeZ() + " blocks). Use ", NamedTextColor.GREEN))
            .append(Component.text("/setheight " + areaName + " <height>", NamedTextColor.AQUA))
            .append(Component.text(" to set its build limit.", NamedTextColor.GREEN)));
    }

    // ── /setheight <area> <height> ───────────────────────────────────────────

    private void handleSetHeight(CommandSender sender, String[] args) {
        if (!sender.hasPermission("heightcontrol.use")) { sender.sendMessage(noPerms()); return; }

        if (args.length < 2) {
            sender.sendMessage(prefix().append(Component.text("Usage: /setheight <area> <height>", NamedTextColor.RED)));
            return;
        }

        String areaName = args[0];
        if (!areaManager.areaExists(areaName)) {
            sender.sendMessage(prefix().append(Component.text("Area ", NamedTextColor.RED))
                .append(Component.text("[" + areaName + "]", NamedTextColor.GOLD))
                .append(Component.text(" does not exist.", NamedTextColor.RED)));
            return;
        }

        int height;
        try {
            height = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix().append(Component.text("Height must be an integer.", NamedTextColor.RED)));
            return;
        }

        if (height < 0) {
            sender.sendMessage(prefix().append(Component.text("Height cannot be negative.", NamedTextColor.RED)));
            return;
        }
        if (height > 4096) {
            sender.sendMessage(prefix().append(Component.text("Height cannot exceed 4096.", NamedTextColor.RED)));
            return;
        }

        areaManager.setHeight(areaName, height);
        sender.sendMessage(prefix()
            .append(Component.text("Build height for ", NamedTextColor.GREEN))
            .append(Component.text("[" + areaName + "]", NamedTextColor.GOLD))
            .append(Component.text(" set to Y " + height + ".", NamedTextColor.GREEN)));
    }

    // ── /listheights ─────────────────────────────────────────────────────────

    private void handleListHeights(CommandSender sender) {
        if (!sender.hasPermission("heightcontrol.use")) { sender.sendMessage(noPerms()); return; }

        Collection<HeightArea> areas = areaManager.getAllAreas();
        if (areas.isEmpty()) {
            sender.sendMessage(prefix().append(Component.text("No areas saved yet.", NamedTextColor.YELLOW)));
            return;
        }

        sender.sendMessage(Component.text("─── Height Areas (" + areas.size() + ") ───", NamedTextColor.GOLD));
        for (HeightArea area : areas) {
            sender.sendMessage(
                Component.text("  " + area.getName(), NamedTextColor.AQUA)
                    .append(Component.text(" | World: ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(area.getWorldName(), NamedTextColor.WHITE))
                    .append(Component.text(" | Size: " + area.getSizeX() + "×" + area.getSizeZ(), NamedTextColor.DARK_GRAY))
                    .append(Component.text(" | Max Y: ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(String.valueOf(area.getMaxBuildHeight()), NamedTextColor.YELLOW))
            );
        }
    }

    // ── /deletearea <area> ───────────────────────────────────────────────────

    private void handleDeleteArea(CommandSender sender, String[] args) {
        if (!sender.hasPermission("heightcontrol.admin")) { sender.sendMessage(noPerms()); return; }

        if (args.length < 1) {
            sender.sendMessage(prefix().append(Component.text("Usage: /deletearea <area>", NamedTextColor.RED)));
            return;
        }

        String areaName = args[0];
        if (areaManager.deleteArea(areaName)) {
            sender.sendMessage(prefix().append(Component.text("Area ", NamedTextColor.GREEN))
                .append(Component.text("[" + areaName + "]", NamedTextColor.GOLD))
                .append(Component.text(" deleted.", NamedTextColor.GREEN)));
        } else {
            sender.sendMessage(prefix().append(Component.text("Area not found.", NamedTextColor.RED)));
        }
    }

    // ── /areainfo <area> ─────────────────────────────────────────────────────

    private void handleAreaInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("heightcontrol.use")) { sender.sendMessage(noPerms()); return; }

        if (args.length < 1) {
            sender.sendMessage(prefix().append(Component.text("Usage: /areainfo <area>", NamedTextColor.RED)));
            return;
        }

        HeightArea area = areaManager.getArea(args[0]);
        if (area == null) {
            sender.sendMessage(prefix().append(Component.text("Area not found.", NamedTextColor.RED)));
            return;
        }

        sender.sendMessage(Component.text("─── Area: " + area.getName() + " ───", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  World: ", NamedTextColor.DARK_GRAY).append(Component.text(area.getWorldName(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Corner 1: ", NamedTextColor.DARK_GRAY).append(Component.text("(" + area.getMinX() + ", " + area.getMinZ() + ")", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Corner 2: ", NamedTextColor.DARK_GRAY).append(Component.text("(" + area.getMaxX() + ", " + area.getMaxZ() + ")", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Size: ", NamedTextColor.DARK_GRAY).append(Component.text(area.getSizeX() + " × " + area.getSizeZ() + " blocks", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Max Build Height: ", NamedTextColor.DARK_GRAY).append(Component.text("Y " + area.getMaxBuildHeight(), NamedTextColor.YELLOW)));
    }

    // ── Tab completion ────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();
        List<String> areas = areaManager.getAllAreas().stream()
                .map(HeightArea::getName).collect(Collectors.toList());

        return switch (name) {
            case "setheight", "deletearea", "areainfo" -> {
                if (args.length == 1) yield filter(areas, args[0]);
                if (args.length == 2 && name.equals("setheight")) {
                    yield filter(List.of("64", "128", "256", "320", "512"), args[1]);
                }
                yield List.of();
            }
            case "savearea" -> args.length == 1 ? List.of("<name>") : List.of();
            default -> List.of();
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Component prefix() {
        return Component.text("[HeightControl] ", NamedTextColor.DARK_AQUA);
    }

    private Component noPerms() {
        return prefix().append(Component.text("You don't have permission.", NamedTextColor.RED));
    }

    private List<String> filter(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
