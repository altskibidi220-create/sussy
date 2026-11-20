package dev.soulsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class SoulListener implements Listener {
    private final SoulPlugin plugin;
    private final SoulManager m;

    public SoulListener(SoulPlugin plugin, SoulManager m) { this.plugin = plugin; this.m = m; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String url = plugin.getConfig().getString("resource-pack.url", "");
        if (url != null && !url.isBlank()) {
            String sha = plugin.getConfig().getString("resource-pack.sha1", "");
            if (sha != null && !sha.isBlank()) p.setResourcePack(url, sha); else p.setResourcePack(url);
        }
        Soul s = m.getSoul(p);
        if (s != null) { m.setSoul(p, s, false); return; }
        boolean reroll = plugin.getConfig().getBoolean("reroll-on-death", true);
        if (!m.hasRolled(p) || reroll) {
            Fx.later(60, () -> { if (p.isOnline() && m.getSoul(p) == null && !m.isRolling(p)) m.startRoulette(p, null); });
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Roulette r = m.rouletteOf(e.getPlayer());
        if (r != null) r.finish(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        Roulette r = m.rouletteOf(p);
        if (r != null) r.finish(true);
        Soul s = m.getSoul(p);
        if (s == null) return;
        m.setSoul(p, null, false);
        p.sendMessage(Component.text("Your ", NamedTextColor.GRAY)
            .append(Component.text(s.name + " Soul", s.text))
            .append(Component.text(" shattered as you died.", NamedTextColor.GRAY)));
        Fx.sound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
        Fx.sphere(p.getLocation().add(0, 1, 0), 1.2, 40, q -> Fx.dust(q, s.color, 1.5f, 1, 0));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getConfig().getBoolean("reroll-on-death", true)) return;
        Fx.later(40, () -> { if (p.isOnline() && !p.isDead() && m.getSoul(p) == null && !m.isRolling(p)) m.startRoulette(p, null); });
    }

    /** F = ability 1, Shift+F = ability 2. Normal hand swapping is disabled while you hold a soul. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (m.getSoul(p) == null && !m.isRolling(p)) return;
        e.setCancelled(true);
        if (m.isRolling(p)) return;
        m.cast(p, p.isSneaking() ? 1 : 0);
    }

    /** Right-clicking a soul item absorbs it. */
    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !e.getAction().isRightClick()) return;
        ItemStack it = e.getItem();
        if (it == null || !it.hasItemMeta()) return;
        String id = it.getItemMeta().getPersistentDataContainer().get(plugin.itemKey(), PersistentDataType.STRING);
        Soul s = SoulRegistry.get(id);
        if (s == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (m.isRolling(p)) return;
        if (m.getSoul(p) != null && !p.hasPermission("soulsmp.admin")) {
            p.sendActionBar(Component.text("You already have a soul. You must die to lose it.", NamedTextColor.RED));
            return;
        }
        it.setAmount(it.getAmount() - 1);
        m.setSoul(p, s, true);
        p.sendMessage(Component.text("You absorbed the ", NamedTextColor.GRAY).append(Component.text(s.name + " Soul", s.text)).append(Component.text(".", NamedTextColor.GRAY)));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!m.isRolling(e.getPlayer())) return;
        Location from = e.getFrom(), to = e.getTo();
        if (to == null) return;
        if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
            Location fixed = from.clone();
            fixed.setY(to.getY());
            fixed.setYaw(to.getYaw());
            fixed.setPitch(to.getPitch());
            e.setTo(fixed);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Soul s = m.getSoul(p);
        if (s == null || !s.passive.immune.contains(e.getCause())) return;
        e.setCancelled(true);
        if (e.getCause() == EntityDamageEvent.DamageCause.FIRE || e.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) p.setFireTicks(0);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHurtBy(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Soul s = m.getSoul(p);
        if (s == null || s.passive.onHurt == null) return;
        Entity d = e.getDamager();
        if (d instanceof Projectile pr && pr.getShooter() instanceof Entity sh) d = sh;
        if (d instanceof LivingEntity le && !le.equals(p)) s.passive.onHurt.accept(p, le);
    }
}
