package com.example.dimensionforge;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Runs when a player clicks "Unlock Lock N" in a lock book.
 * For now every lock simply teleports the player to the White Palace.
 */
public final class LockCommand implements CommandExecutor {

  
private final DimensionForge plugin;
    private final int number;

    public LockCommand(DimensionForge plugin, int number) {
        this.plugin = plugin;
        this.number = number;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this.", NamedTextColor.RED));
            return true;
        }


          // The command is only meaningful from the book, so require holding it.
        if (LockBook.heldSlot(plugin, player, number) == null) {
            player.sendMessage(Component.text("You need to be holding the Lock " + number + " book.", NamedTextColor.RED));
            return true;
        }

        Location destination = plugin.getLockLocation();
        if (destination == null) {
            player.sendMessage(Component.text(
                    "The White Palace has no destination yet (or its world isn't loaded). "
                            + "An admin needs to run /dimension setlock1.", NamedTextColor.RED));
            return true;
        }

        player.teleportAsync(destination).thenAccept(ok -> {
            if (ok) {
                Bukkit.getScheduler().runTask(plugin, () -> showWelcome(player));
            }
        });
        return true;
    }

    private void showWelcome(Player player) {
        String text = plugin.getConfig().getString("lock1.title", "Welcome to the White Palace");
        Title title = Title.title(
                Component.text(text, NamedTextColor.WHITE, TextDecoration.BOLD),
                Component.empty(),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofSeconds(1)));
        player.showTitle(title);
    }
}
