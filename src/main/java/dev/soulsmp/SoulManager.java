package dev.soulsmp;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public final class SoulManager {
    private static final Key ICON_FONT = Key.key("soulsmp", "icons");
    private static final List<Attribute> MODIFIED = List.of(Attribute.MAX_HEALTH, Attribute.ATTACK_DAMAGE,
        Attribute.MOVEMENT_SPEED, Attribute.ARMOR, Attribute.KNOCKBACK_RESISTANCE, Attribute.ATTACK_SPEED);

    private final SoulPlugin plugin;
    final NamespacedKey soulKey, rolledKey;
    private final Map<UUID, long[]> cooldowns = new HashMap<>();
    private final Map<UUID, Roulette> roulettes = new HashMap<>();

    public SoulManager(SoulPlugin plugin) {
        this.plugin = plugin;
        this.soulKey = new NamespacedKey(plugin, "soul");
        this.rolledKey = new NamespacedKey(plugin, "rolled");
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::hud, 20, 4);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                Soul s = getSoul(p);
                if (s != null) s.passive.onTick.accept(p);
            }
        }, 40, 40);
    }

    public void shutdown() {
        for (Roulette r : new ArrayList<>(roulettes.values())) r.finish(true);
        roulettes.clear();
    }

    // ---------------- data ----------------
    public Soul getSoul(Player p) {
        return SoulRegistry.get(p.getPersistentDataContainer().get(soulKey, PersistentDataType.STRING));
    }
    public boolean hasRolled(Player p) { return p.getPersistentDataContainer().has(rolledKey, PersistentDataType.BYTE); }
    public void markRolled(Player p) { p.getPersistentDataContainer().set(rolledKey, PersistentDataType.BYTE, (byte) 1); }

    public void setSoul(Player p, Soul s, boolean fx) {
        Soul old = getSoul(p);
        if (old != null) old.passive.onRemove.accept(p);
        clearModifiers(p);
        cooldowns.remove(p.getUniqueId());
        if (s == null) {
            p.getPersistentDataContainer().remove(soulKey);
            return;
        }
        p.getPersistentDataContainer().set(soulKey, PersistentDataType.STRING, s.id);
        s.passive.onApply.accept(p);
        s.passive.onTick.accept(p);
        if (fx) Fx.absorb(p, s);
    }

    private void clearModifiers(Player p) {
        String ns = plugin.getName().toLowerCase(Locale.ROOT);
        for (Attribute a : MODIFIED) {
            AttributeInstance in = p.getAttribute(a);
            if (in == null) continue;
            for (AttributeModifier m : new ArrayList<>(in.getModifiers())) {
                if (m.getKey().getNamespace().equals(ns) && m.getKey().getKey().startsWith("soul_")) in.removeModifier(m);
            }
        }
        AttributeInstance mh = p.getAttribute(Attribute.MAX_HEALTH);
        if (mh != null && p.getHealth() > mh.getValue()) p.setHealth(mh.getValue());
    }

    // ---------------- abilities ----------------
    public void resetCooldowns(Player p) { cooldowns.remove(p.getUniqueId()); }

    /** slot 0 = F (offhand), slot 1 = Shift+F */
    public void cast(Player p, int slot) {
        Soul s = getSoul(p);
        if (s == null || isRolling(p)) return;
        Ability a = slot == 0 ? s.primary : s.secondary;
        long[] cd = cooldowns.computeIfAbsent(p.getUniqueId(), k -> new long[2]);
        long now = System.currentTimeMillis();
        if (cd[slot] > now) {
            Fx.sound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
            return;
        }
        boolean ok;
        try { ok = a.action().run(p); }
        catch (Exception ex) { plugin.getLogger().warning("Ability " + a.name() + " failed: " + ex); return; }
        if (ok) {
            cd[slot] = now + s.cooldown(slot) * 1000L;
            Fx.cast(p, s.color);
        }
    }

    // ---------------- roulette ----------------
    public boolean isRolling(Player p) { return roulettes.containsKey(p.getUniqueId()); }
    Roulette rouletteOf(Player p) { return roulettes.get(p.getUniqueId()); }
    void forget(Player p) { roulettes.remove(p.getUniqueId()); }

    public void startRoulette(Player p, Soul forced) {
        if (isRolling(p)) return;
        Soul chosen = forced != null ? forced : SoulRegistry.random();
        markRolled(p);
        if (getSoul(p) != null) setSoul(p, null, false);
        Roulette r = new Roulette(plugin, this, p, chosen);
        roulettes.put(p.getUniqueId(), r);
        r.start();
    }

    // ---------------- HUD (action bar) ----------------
    private void hud() {
        boolean icon = plugin.getConfig().getBoolean("hud-icon-font", true);
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Soul s = getSoul(p);
            if (s == null || isRolling(p)) continue;
            long[] cd = cooldowns.getOrDefault(p.getUniqueId(), new long[2]);
            Component bar = Component.empty();
            if (icon) bar = bar.append(Component.text(String.valueOf(s.glyph)).font(ICON_FONT).color(NamedTextColor.WHITE)).append(Component.text(" "));
            else bar = bar.append(Component.text("✦ ", s.text));
            bar = bar.append(Component.text(s.name.toUpperCase(), s.text, TextDecoration.BOLD))
                .append(Component.text("  |  ", NamedTextColor.DARK_GRAY))
                .append(slot("F", s.primary, cd[0], now))
                .append(Component.text("  |  ", NamedTextColor.DARK_GRAY))
                .append(slot("Shift+F", s.secondary, cd[1], now));
            p.sendActionBar(bar);
        }
    }

    private Component slot(String key, Ability a, long expiry, long now) {
        Component state = expiry <= now
            ? Component.text("READY", NamedTextColor.GREEN)
            : Component.text(String.format(Locale.ROOT, "%.1fs", (expiry - now) / 1000.0), NamedTextColor.RED);
        return Component.text("[" + key + "] ", NamedTextColor.GRAY)
            .append(Component.text(a.name() + " ", NamedTextColor.WHITE))
            .append(state);
    }
}
