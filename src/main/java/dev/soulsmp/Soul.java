package dev.soulsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class Soul {
    private static int counter = 0;

    public final String id, name, tagline;
    public final char glyph;
    public final Color color;
    public final TextColor text;
    public final Passive passive;
    public final Ability primary, secondary;

    public Soul(String id, String name, int rgb, String tagline, Passive passive, Ability primary, Ability secondary) {
        this.id = id; this.name = name; this.tagline = tagline;
        this.glyph = (char) (0xE000 + counter++);   // must match the resource-pack font order
        this.color = Color.fromRGB(rgb);
        this.text = TextColor.color(rgb);
        this.passive = passive; this.primary = primary; this.secondary = secondary;
    }

    public static Component line(String s, TextColor c) {
        return Component.text(s, c).decoration(TextDecoration.ITALIC, false);
    }

    /** Cooldown in seconds: config.yml (cooldowns.<soul>.primary/secondary) or the built-in default. */
    public int cooldown(int slot) {
        Ability a = slot == 0 ? primary : secondary;
        return Math.max(0, SoulPlugin.get().getConfig().getInt("cooldowns." + id + "." + (slot == 0 ? "primary" : "secondary"), a.cooldown()));
    }

    public Component display() {
        return Component.text(name + " Soul", text, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
    }

    public List<Component> lore() {
        List<Component> l = new ArrayList<>();
        l.add(line(tagline, NamedTextColor.GRAY));
        l.add(Component.empty());
        l.add(line("Passive: " + passive.name, text));
        l.add(line("  " + passive.desc, NamedTextColor.GRAY));
        l.add(line("[F] " + primary.name() + "  (" + cooldown(0) + "s)", text));
        l.add(line("  " + primary.desc(), NamedTextColor.GRAY));
        l.add(line("[Shift+F] " + secondary.name() + "  (" + cooldown(1) + "s)", text));
        l.add(line("  " + secondary.desc(), NamedTextColor.GRAY));
        return l;
    }

    /** The physical soul item (custom texture via the resource pack's item model). */
    public ItemStack item() {
        ItemStack it = new ItemStack(Material.ECHO_SHARD);
        it.editMeta(m -> {
            m.displayName(display());
            List<Component> lore = lore();
            lore.add(Component.empty());
            lore.add(line("Right-click to absorb", NamedTextColor.DARK_GRAY));
            m.lore(lore);
            m.setItemModel(NamespacedKey.fromString("soulsmp:soul_" + id));
            m.getPersistentDataContainer().set(SoulPlugin.get().itemKey(), PersistentDataType.STRING, id);
        });
        return it;
    }
}
