package dev.ghost.heightcontrol.managers;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {

    public static class Selection {
        public Location pos1; // left click
        public Location pos2; // right click

        public boolean isComplete() {
            return pos1 != null && pos2 != null;
        }

        public boolean sameWorld() {
            return pos1 != null && pos2 != null &&
                   pos1.getWorld().getName().equals(pos2.getWorld().getName());
        }
    }

    private final Map<UUID, Selection> selections = new HashMap<>();

    public Selection getOrCreate(UUID uuid) {
        return selections.computeIfAbsent(uuid, k -> new Selection());
    }

    public Selection get(UUID uuid) {
        return selections.get(uuid);
    }

    public void clear(UUID uuid) {
        selections.remove(uuid);
    }

    public void setPos1(UUID uuid, Location loc) {
        getOrCreate(uuid).pos1 = loc.clone();
    }

    public void setPos2(UUID uuid, Location loc) {
        getOrCreate(uuid).pos2 = loc.clone();
    }
}
