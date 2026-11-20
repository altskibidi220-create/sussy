package dev.soulsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** /soul — admin test GUI. */
public final class SoulGui implements Listener {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 20, 21, 22, 23, 24};
    private static final int REMOVE = 38, ROLL = 40, RESET = 42;

    private final SoulManager m;
    public SoulGui(SoulManager m) { this.m = m; }

    public static final class Holder implements InventoryHolder {
        Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }

    public static void open(Player p) {
        Holder h = new Holder();
        Inventory inv = Bukkit.createInventory(h, 45, Component.text("Souls — Test Menu", NamedTextColor.DARK_PURPLE));
        h.inv = inv;
        ItemStack fill = named(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));
        for (int i = 0; i < 45; i++) inv.setItem(i, fill);
        List<Soul> souls = SoulRegistry.all();
        for (int i = 0; i < souls.size() && i < SLOTS.length; i++) {
            Soul s = souls.get(i);
            ItemStack it = s.item();
            it.editMeta(meta -> {
                List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
                lore.set(lore.size() - 1, Soul.line("Left-click: equip   Right-click: get item", NamedTextColor.YELLOW));
                meta.lore(lore);
            });
            inv.setItem(SLOTS[i], it);
        }
        inv.setItem(REMOVE, named(Material.BARRIER, Soul.line("Remove my soul", NamedTextColor.RED)));
        inv.setItem(ROLL, named(Material.ENDER_EYE, Soul.line("Roll a random soul (animation)", NamedTextColor.LIGHT_PURPLE)));
        inv.setItem(RESET, named(Material.CLOCK, Soul.line("Reset my cooldowns", NamedTextColor.AQUA)));
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.6f, 1.4f);
    }

    private static ItemStack named(Material mat, Component name) {
        ItemStack it = new ItemStack(mat);
        it.editMeta(meta -> meta.displayName(name));
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null || !(e.getClickedInventory().getHolder() instanceof Holder)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.hasPermission("soulsmp.admin")) return;
        int slot = e.getSlot();
        if (slot == REMOVE) { m.setSoul(p, null, false); p.sendActionBar(Component.text("Soul removed.", NamedTextColor.GRAY)); return; }
        if (slot == RESET) { m.resetCooldowns(p); p.sendActionBar(Component.text("Cooldowns reset.", NamedTextColor.AQUA)); return; }
        if (slot == ROLL) { p.closeInventory(); m.startRoulette(p, null); return; }
        List<Soul> souls = SoulRegistry.all();
        for (int i = 0; i < SLOTS.length && i < souls.size(); i++) {
            if (SLOTS[i] != slot) continue;
            Soul s = souls.get(i);
            if (e.getClick().isRightClick()) {
                p.getInventory().addItem(s.item());
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.2f);
            } else {
                m.setSoul(p, s, true);
            }
            return;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }
}
