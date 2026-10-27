package com.example.dimensionforge;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class DimensionCommand implements TabExecutor {

    private static final Pattern VALID_NAME = Pattern.compile("^[a-z0-9_\\-]{1,32}$");
    private static final String ADMIN = "dimensionforge.admin";

    private final DimensionForge plugin;

    public DimensionCommand(DimensionForge plugin) {
        this.plugin = plugin;

    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            usage(sender);
            return true;
        }

  switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(sender, args);
            case "tp" -> teleport(sender, args);
            case "list" -> list(sender);
            case "unload" -> unload(sender, args, false);
            case "delete" -> unload(sender, args, true);
            case "givebook" -> giveBook(sender, args);
            case "setlock1" -> setLock(sender);
            default -> usage(sender);
        }
        return true;
    }

    // ---------------------------------------------------------------- create

    private void create(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) { noPerm(sender); return; }
        if (args.length < 2) {
            msg(sender, "Usage: /dimension create <name> [normal|nether|end] [normal|flat|void] [seed]", NamedTextColor.YELLOW);
            return;
        }
      
String name = args[1].toLowerCase(Locale.ROOT);
        if (!VALID_NAME.matcher(name).matches()) {
            msg(sender, "Names may only use a-z, 0-9, '_' and '-' (max 32 characters).", NamedTextColor.RED);
            return;
        }
        if (Bukkit.getWorld(name) != null || new File(Bukkit.getWorldContainer(), name).exists()) {
            msg(sender, "A world or folder called '" + name + "' already exists.", NamedTextColor.RED);
            return;
        }

      World.Environment env = World.Environment.NORMAL;
        if (args.length >= 3) {
            switch (args[2].toLowerCase(Locale.ROOT)) {
                case "normal" -> env = World.Environment.NORMAL;
                case "nether" -> env = World.Environment.NETHER;
                case "end" -> env = World.Environment.THE_END;
                default -> { msg(sender, "Environment must be normal, nether or end.", NamedTextColor.RED); return; }
            }
        }

  DimensionForge.Terrain terrain = DimensionForge.Terrain.NORMAL;
        if (args.length >= 4) {
            try {
                terrain = DimensionForge.Terrain.valueOf(args[3].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                msg(sender, "Terrain must be normal, flat or void.", NamedTextColor.RED);
                return;
            }
        }

        long seed = new Random().nextLong();
        if (args.length >= 5) {
            try {
                seed = Long.parseLong(args[4]);
            } catch (NumberFormatException e) {
                seed = args[4].hashCode();
            }
        }

msg(sender, "Creating dimension '" + name + "'... the server may lag for a moment.", NamedTextColor.GRAY);
        World world = plugin.createWorld(name, env, terrain, seed);
        if (world == null) {
            msg(sender, "Failed to create the dimension. Check the console for errors.", NamedTextColor.RED);
            return;
        }
        plugin.saveWorld(name, env, terrain, seed);
        msg(sender, "Dimension '" + name + "' created. Use /dimension tp " + name + " to visit it.", NamedTextColor.GREEN);
    }

    // -------------------------------------------------------------------- tp
private void teleport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dimensionforge.use")) { noPerm(sender); return; }
        if (args.length < 2) {
            msg(sender, "Usage: /dimension tp <name> [player]", NamedTextColor.YELLOW);
            return;
        }

        World world = Bukkit.getWorld(args[1].toLowerCase(Locale.ROOT));
        if (world == null) {
            msg(sender, "No loaded dimension called '" + args[1] + "'.", NamedTextColor.RED);
            return;
        }

  Player target;
        if (args.length >= 3) {
            if (!sender.hasPermission(ADMIN)) { noPerm(sender); return; }
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                msg(sender, "Player '" + args[2] + "' is not online.", NamedTextColor.RED);
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            msg(sender, "Specify a player: /dimension tp <name> <player>", NamedTextColor.RED);
            return;
        }

  target.teleportAsync(world.getSpawnLocation()).thenAccept(ok -> {
            if (!ok) msg(sender, "Teleport failed.", NamedTextColor.RED);
        });
        msg(sender, "Teleporting " + target.getName() + " to '" + world.getName() + "'.", NamedTextColor.GREEN);
    }

    // ------------------------------------------------------------------ list

private void list(CommandSender sender) {
        if (!sender.hasPermission("dimensionforge.use")) { noPerm(sender); return; }
        msg(sender, "Loaded dimensions:", NamedTextColor.GOLD);
        for (World w : Bukkit.getWorlds()) {
            String tag = plugin.isManaged(w.getName()) ? " (DimensionForge)" : "";
            msg(sender, " - " + w.getName() + " [" + w.getEnvironment().name() + "] "
                    + w.getPlayers().size() + " player(s)" + tag, NamedTextColor.WHITE);
        }
    }

    // ------------------------------------------------------- unload / delete
private void unload(CommandSender sender, String[] args, boolean deleteFiles) {
        if (!sender.hasPermission(ADMIN)) { noPerm(sender); return; }
        if (args.length < 2) {
            msg(sender, "Usage: /dimension " + (deleteFiles ? "delete" : "unload") + " <name>", NamedTextColor.YELLOW);
            return;
        }

        String name = args[1].toLowerCase(Locale.ROOT);
        if (!plugin.isManaged(name)) {
            msg(sender, "'" + name + "' is not a DimensionForge dimension, so I won't touch it.", NamedTextColor.RED);
            return;
        }

        World world = Bukkit.getWorld(name);
        File folder = new File(Bukkit.getWorldContainer(), name);

          if (world != null) {
            World fallback = Bukkit.getWorlds().get(0);
            for (Player p : new ArrayList<>(world.getPlayers())) {
                p.teleport(fallback.getSpawnLocation());
                msg(p, "The dimension you were in is being removed.", NamedTextColor.YELLOW);
            }
            folder = world.getWorldFolder();
            if (!Bukkit.unloadWorld(world, !deleteFiles)) {
                msg(sender, "Could not unload '" + name + "'.", NamedTextColor.RED);
                return;
            }
        }

        plugin.forgetWorld(name);

if (deleteFiles) {
            try {
                deleteRecursively(folder.toPath());
                msg(sender, "Dimension '" + name + "' deleted.", NamedTextColor.GREEN);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to delete " + folder + ": " + e.getMessage());
                msg(sender, "Unloaded, but could not delete the world folder. See console.", NamedTextColor.RED);
            }
        } else {
            msg(sender, "Dimension '" + name + "' unloaded and removed from the config. Its files were kept.", NamedTextColor.GREEN);
        }
    }

private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path p : (Iterable<Path>) walk.sorted(Comparator.reverseOrder())::iterator) {
                Files.delete(p);
            }
        }
    }

    // ------------------------------------------------------- Lock 1 book

    private void giveBook(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) { noPerm(sender); return; }

        int number = 1;
        int playerArg = 1;
        if (args.length >= 2 && args[1].matches("[1-3]")) {
            number = Integer.parseInt(args[1]);
            playerArg = 2;
        }

      Player target;
        if (args.length > playerArg) {
            target = Bukkit.getPlayerExact(args[playerArg]);
            if (target == null) {
                msg(sender, "Player '" + args[playerArg] + "' is not online.", NamedTextColor.RED);
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            msg(sender, "Specify a player: /dimension givebook [1-3] <player>", NamedTextColor.RED);
            return;
        }

        var leftover = target.getInventory().addItem(LockBook.create(plugin, number));
        leftover.values().forEach(i -> target.getWorld().dropItemNaturally(target.getLocation(), i));
        msg(sender, "Gave the Lock " + number + " book to " + target.getName() + ".", NamedTextColor.GREEN);
    }


  private void setLock(CommandSender sender) {
        if (!sender.hasPermission(ADMIN)) { noPerm(sender); return; }
        if (!(sender instanceof Player p)) {
            msg(sender, "Run this in-game, standing where Lock 1 should teleport players.", NamedTextColor.RED);
            return;
        }
        plugin.setLockLocation(p.getLocation());
        Location l = p.getLocation();
        msg(sender, String.format(Locale.ROOT, "Lock 1 destination set to %s at %.1f, %.1f, %.1f.",
                l.getWorld().getName(), l.getX(), l.getY(), l.getZ()), NamedTextColor.GREEN);
    }

   // ------------------------------------------------------------ tab complete

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,

                                      @NotNull String label, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(List.of("create", "tp", "list", "unload", "delete", "givebook", "setlock1"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
               if (sub.equals("tp")) {
                Bukkit.getWorlds().forEach(w -> out.add(w.getName()));
            } else if (sub.equals("givebook")) {
                out.addAll(List.of("1", "2", "3"));
                Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
            } else if (sub.equals("unload") || sub.equals("delete")) {
                var section = plugin.getConfig().getConfigurationSection("worlds");
                if (section != null) out.addAll(section.getKeys(false));
            }
        } else if (args[0].equalsIgnoreCase("create")) {
            if (args.length == 3) out.addAll(List.of("normal", "nether", "end"));
            if (args.length == 4) out.addAll(List.of("normal", "flat", "void"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("givebook")) {
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("tp")) {
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        }

      String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }

    // ----------------------------------------------------------------- helpers

    private void usage(CommandSender sender) {
        msg(sender, "DimensionForge commands:", NamedTextColor.GOLD);
        msg(sender, "/dimension create <name> [normal|nether|end] [normal|flat|void] [seed]", NamedTextColor.WHITE);
        msg(sender, "/dimension tp <name> [player]", NamedTextColor.WHITE);
        msg(sender, "/dimension list", NamedTextColor.WHITE);
        msg(sender, "/dimension unload <name>  (keeps files)", NamedTextColor.WHITE);
        msg(sender, "/dimension delete <name>  (removes files)", NamedTextColor.WHITE);

msg(sender, "/dimension givebook [1|2|3] [player]  (gives a lock book; default Lock 1)", NamedTextColor.WHITE);
        msg(sender, "/dimension setlock1  (Lock 1 teleports to where you stand)", NamedTextColor.WHITE);
    }

    private void noPerm(CommandSender sender) {
        msg(sender, "You don't have permission to do that.", NamedTextColor.RED);
    }

    private static void msg(CommandSender to, String text, NamedTextColor color) {
        to.sendMessage(Component.text(text, color));
    }
}
  
