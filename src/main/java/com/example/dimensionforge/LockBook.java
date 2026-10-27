package com.example.dimensionforge;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/** Builds and recognises the numbered "Lock" books. */

public final class LockBook {

    private LockBook() {}

    private static NamespacedKey numberKey(DimensionForge plugin) {
        return new NamespacedKey(plugin, "lock_number");
    }

    public static ItemStack create(DimensionForge plugin, int number) {
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) item.getItemMeta();

        meta.title(Component.text("Lock " + number));
        meta.author(Component.text("DimensionForge"));
        // Tattered books can't be copied, so players can't duplicate the key.
        meta.setGeneration(BookMeta.Generation.TATTERED);

      Component clickable = Component.text("Unlock Lock " + number,
                        NamedTextColor.RED, TextDecoration.BOLD, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.runCommand("/lock" + number))
                .hoverEvent(HoverEvent.showText(Component.text("Click to unlock", NamedTextColor.GRAY)));

        // Books have no centre alignment, so pad with blank lines (vertical)
        // and spaces (horizontal) to put the text in the middle of the page.
        Component page = Component.text("\n\n\n\n\n\n")
                .append(Component.text("     "))
                .append(clickable);
        meta.addPages(page);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(plugin.getLockKey(), PersistentDataType.BYTE, (byte) 1); // marker (also used by older books)
      pdc.set(numberKey(plugin), PersistentDataType.INTEGER, number);
        item.setItemMeta(meta);
        return item;
    }

    /** Returns the lock number of a book (1, 2, 3...), or 0 if the item is not a lock book. */
    public static int getNumber(DimensionForge plugin, ItemStack item) {
        if (item == null || item.getType() != Material.WRITTEN_BOOK || !item.hasItemMeta()) return 0;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Integer n = pdc.get(numberKey(plugin), PersistentDataType.INTEGER);
        if (n != null) return n;
        // Books made before numbering was added are Lock 1.
        return pdc.has(plugin.getLockKey(), PersistentDataType.BYTE) ? 1 : 0;
    }

      /** The hand holding the given lock's book, or null if the player isn't holding it. */
    public static EquipmentSlot heldSlot(DimensionForge plugin, Player player, int number) {
        if (getNumber(plugin, player.getInventory().getItemInMainHand()) == number) return EquipmentSlot.HAND;
        if (getNumber(plugin, player.getInventory().getItemInOffHand()) == number) return EquipmentSlot.OFF_HAND;
        return null;
    }
}

      
