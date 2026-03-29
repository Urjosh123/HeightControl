package dev.ghost.heightcontrol.managers;

import dev.ghost.heightcontrol.HeightControl;
import dev.ghost.heightcontrol.models.HeightArea;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AreaManager {

    private final HeightControl plugin;
    private final Map<String, HeightArea> areas = new LinkedHashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public AreaManager(HeightControl plugin) {
        this.plugin = plugin;
        loadAreas();
    }

    private void loadAreas() {
        dataFile = new File(plugin.getDataFolder(), "areas.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("areas")) {
            for (String key : dataConfig.getConfigurationSection("areas").getKeys(false)) {
                String data = dataConfig.getString("areas." + key);
                if (data != null) {
                    HeightArea area = HeightArea.deserialize(key, data);
                    if (area != null) {
                        areas.put(key.toLowerCase(), area);
                    }
                }
            }
        }
        plugin.getLogger().info("Loaded " + areas.size() + " height area(s).");
    }

    public void saveAreas() {
        dataConfig.set("areas", null); // clear
        for (Map.Entry<String, HeightArea> entry : areas.entrySet()) {
            dataConfig.set("areas." + entry.getKey(), entry.getValue().serialize());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save areas.yml: " + e.getMessage());
        }
    }

    public boolean addArea(HeightArea area) {
        String key = area.getName().toLowerCase();
        areas.put(key, area);
        saveAreas();
        return true;
    }

    public boolean deleteArea(String name) {
        if (areas.remove(name.toLowerCase()) != null) {
            saveAreas();
            return true;
        }
        return false;
    }

    public HeightArea getArea(String name) {
        return areas.get(name.toLowerCase());
    }

    public Collection<HeightArea> getAllAreas() {
        return Collections.unmodifiableCollection(areas.values());
    }

    public boolean areaExists(String name) {
        return areas.containsKey(name.toLowerCase());
    }

    /** Returns the most restrictive area at this location, or null if none */
    public HeightArea getAreaAt(Location loc) {
        HeightArea best = null;
        int bestHeight = Integer.MAX_VALUE;
        for (HeightArea area : areas.values()) {
            if (area.contains(loc)) {
                // If multiple areas overlap, use the most restrictive (lowest max height)
                if (best == null || area.getMaxBuildHeight() < bestHeight) {
                    best = area;
                    bestHeight = area.getMaxBuildHeight();
                }
            }
        }
        return best;
    }

    public void setHeight(String areaName, int height) {
        HeightArea area = getArea(areaName);
        if (area != null) {
            area.setMaxBuildHeight(height);
            saveAreas();
        }
    }
}
