package com.example.dimensionforge;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Random;

public final class DimensionForge extends JavaPlugin {

    public enum Terrain { NORMAL, FLAT, VOID }

    private NamespacedKey lockKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lockKey = new NamespacedKey(this, "lock_book");

DimensionCommand command = new DimensionCommand(this);
        getCommand("dimension").setExecutor(command);
        getCommand("dimension").setTabCompleter(command);
        for (int i = 1; i <= 3; i++) {
            getCommand("lock" + i).setExecutor(new LockCommand(this, i));
        }

        loadSavedWorlds();
    }

    /** Re-creates every dimension saved in config.yml. */
    private void loadSavedWorlds() {
        ConfigurationSection section = getConfig().getConfigurationSection("worlds");
        if (section == null) return;


      for (String name : section.getKeys(false)) {
            ConfigurationSection w = section.getConfigurationSection(name);
            if (w == null) continue;
            try {
                World.Environment env = World.Environment.valueOf(
                        w.getString("environment", "NORMAL").toUpperCase(Locale.ROOT));
                Terrain terrain = Terrain.valueOf(
                        w.getString("terrain", "NORMAL").toUpperCase(Locale.ROOT));
                long seed = w.getLong("seed", new Random().nextLong());
                if (getServer().getWorld(name) == null) {
                    createWorld(name, env, terrain, seed);
                    getLogger().info("Loaded dimension '" + name + "'.");
                }
            } catch (Exception e) {
                getLogger().warning("Could not load dimension '" + name + "': " + e.getMessage());
            }
        }
    }

      /** Creates (or loads, if the folder already exists) a world and returns it, or null on failure. */
    public World createWorld(String name, World.Environment env, Terrain terrain, long seed) {
        WorldCreator creator = new WorldCreator(name)
                .environment(env)
                .seed(seed);

        switch (terrain) {
            case VOID -> creator.generator(new VoidGenerator());
            case FLAT -> creator.type(WorldType.FLAT);
            case NORMAL -> { /* vanilla generation */ }
        }

        World world = creator.createWorld();

        if (world != null && terrain == Terrain.VOID) {
            // Small spawn platform so players don't fall straight into the void.
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    world.getBlockAt(x, 63, z).setType(Material.STONE);
                }
              
            }
            world.setSpawnLocation(0, 64, 0);
        }
        return world;
    }

    /** Saves a dimension's settings so it is reloaded on the next start. */
    public void saveWorld(String name, World.Environment env, Terrain terrain, long seed) {
        getConfig().set("worlds." + name + ".environment", env.name());
        getConfig().set("worlds." + name + ".terrain", terrain.name());
        getConfig().set("worlds." + name + ".seed", seed);
        saveConfig();
    }

    public void forgetWorld(String name) {
        getConfig().set("worlds." + name, null);
        saveConfig();
    }
  public boolean isManaged(String name) {
        return getConfig().isConfigurationSection("worlds." + name);
    }

    public NamespacedKey getLockKey() {
        return lockKey;
    }

    /** Saves where the Lock 1 book teleports players. */
    public void setLockLocation(Location loc) {
        String p = "lock1.location.";
        getConfig().set(p + "world", loc.getWorld().getName());
        getConfig().set(p + "x", loc.getX());
        getConfig().set(p + "y", loc.getY());
        getConfig().set(p + "z", loc.getZ());
        getConfig().set(p + "yaw", (double) loc.getYaw());
        getConfig().set(p + "pitch", (double) loc.getPitch());
        saveConfig();
    }

    /** Returns the Lock 1 destination, or null if unset or its world isn't loaded. */
public Location getLockLocation() {
        String p = "lock1.location.";
        String worldName = getConfig().getString(p + "world");
        if (worldName == null) return null;
        World world = getServer().getWorld(worldName);
        if (world == null) return null;
        return new Location(world,
                getConfig().getDouble(p + "x"),
                getConfig().getDouble(p + "y"),
                getConfig().getDouble(p + "z"),
                (float) getConfig().getDouble(p + "yaw"),
                (float) getConfig().getDouble(p + "pitch"));
    }
}
  
              
