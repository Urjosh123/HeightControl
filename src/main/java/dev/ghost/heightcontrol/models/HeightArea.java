package dev.ghost.heightcontrol.models;

import org.bukkit.Location;
import org.bukkit.World;

public class HeightArea {

    private final String name;
    private final String worldName;
    private final int minX, minZ, maxX, maxZ;
    private int maxBuildHeight;

    public HeightArea(String name, String worldName, int minX, int minZ, int maxX, int maxZ, int maxBuildHeight) {
        this.name = name;
        this.worldName = worldName;
        this.minX = Math.min(minX, maxX);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxZ = Math.max(minZ, maxZ);
        this.maxBuildHeight = maxBuildHeight;
    }

    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public int getMinX() { return minX; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxZ() { return maxZ; }
    public int getMaxBuildHeight() { return maxBuildHeight; }
    public void setMaxBuildHeight(int height) { this.maxBuildHeight = height; }

    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equals(worldName)) return false;
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public int getSizeX() { return maxX - minX + 1; }
    public int getSizeZ() { return maxZ - minZ + 1; }

    // Serialize to config string: worldName,minX,minZ,maxX,maxZ,height
    public String serialize() {
        return worldName + "," + minX + "," + minZ + "," + maxX + "," + maxZ + "," + maxBuildHeight;
    }

    public static HeightArea deserialize(String name, String data) {
        String[] parts = data.split(",");
        if (parts.length < 6) return null;
        try {
            return new HeightArea(
                name,
                parts[0],
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]),
                Integer.parseInt(parts[4]),
                Integer.parseInt(parts[5])
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
