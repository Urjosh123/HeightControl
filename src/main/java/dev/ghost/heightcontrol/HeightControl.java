package dev.ghost.heightcontrol;

import dev.ghost.heightcontrol.commands.CommandHandler;
import dev.ghost.heightcontrol.listeners.ShovelListener;
import dev.ghost.heightcontrol.managers.AreaManager;
import dev.ghost.heightcontrol.managers.SelectionManager;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class HeightControl extends JavaPlugin {

    private AreaManager areaManager;
    private SelectionManager selectionManager;
    private NamespacedKey shovelKey;

    @Override
    public void onEnable() {
        shovelKey = new NamespacedKey(this, "selection_shovel");

        areaManager = new AreaManager(this);
        selectionManager = new SelectionManager();

        // Register listeners
        getServer().getPluginManager().registerEvents(new ShovelListener(this), this);

        // Register commands
        CommandHandler handler = new CommandHandler(this);
        for (String cmd : new String[]{"shovel", "savearea", "setheight", "listheights", "deletearea", "areainfo"}) {
            getCommand(cmd).setExecutor(handler);
            getCommand(cmd).setTabCompleter(handler);
        }

        getLogger().info("HeightControl enabled — " + areaManager.getAllAreas().size() + " area(s) loaded.");
    }

    @Override
    public void onDisable() {
        if (areaManager != null) areaManager.saveAreas();
        getLogger().info("HeightControl disabled.");
    }

    public AreaManager getAreaManager() { return areaManager; }
    public SelectionManager getSelectionManager() { return selectionManager; }
    public NamespacedKey getShovelKey() { return shovelKey; }
}
