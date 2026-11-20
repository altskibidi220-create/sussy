package dev.soulsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Small toolbox for particles, sounds, targeting and attributes. */
public final class Fx {
    private Fx() {}

    // ---------- sound / particle primitives ----------
    public static void sound(Location l, Sound s, float v, float pitch) {
        l.getWorld().playSound(l, s, SoundCategory.PLAYERS, v, pitch);
    }
    public static void dust(Location l, Color c, float size, int count, double spread) {
        l.getWorld().spawnParticle(Particle.DUST, l, count, spread, spread, spread, 0, new Particle.DustOptions(c, size));
    }
    public static void spawn(Location l, Particle p, int count, double spread, double speed) {
        l.getWorld().spawnParticle(p, l, count, spread, spread, spread, speed);
    }
    public static void block(Location l, Material m, int count) {
        l.getWorld().spawnParticle(Particle.BLOCK, l, count, 0.2, 0.2, 0.2, 0, m.createBlockData());
    }

    // ---------- shapes ----------
    public static void ring(Location c, double r, int pts, Consumer<Location> f) {
        for (int i = 0; i < pts; i++) {
            double a = 2 * Math.PI * i / pts;
            f.accept(c.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r));
        }
    }
    public static void sphere(Location c, double r, int pts, Consumer<Location> f) {
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < pts; i++) {
            double y = 1 - (i / (double) Math.max(1, pts - 1)) * 2;
            double rad = Math.sqrt(Math.max(0, 1 - y * y));
            double th = golden * i;
            f.accept(c.clone().add(Math.cos(th) * rad * r, y * r, Math.sin(th) * rad * r));
        }
    }
    public static void line(Location a, Location b, double step, Consumer<Location> f) {
        Vector d = b.toVector().subtract(a.toVector());
        double len = d.length();
        if (len < 0.01) return;
        d.normalize().multiply(step);
        Location cur = a.clone();
        for (double t = 0; t < len; t += step) { f.accept(cur.clone()); cur.add(d); }
    }
    /** Expanding ring from the centre outwards over `ticks` ticks. */
    public static void nova(Location c, double maxR, int ticks, Consumer<Location> f) {
        repeat(ticks, 1, i -> {
            double r = maxR * (i + 1) / ticks;
            ring(c, r, (int) (12 + r * 10), f);
        });
    }
    /** Spiral wrapped around the segment a->b (negative turns = opposite direction). */
    public static void helix(Location a, Location b, double radius, double turns, int points, Consumer<Location> f) {
        Vector dir = b.toVector().subtract(a.toVector());
        double len = dir.length();
        if (len < 0.01) return;
        dir.normalize();
        Vector ref = Math.abs(dir.getY()) < 0.95 ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
        Vector u = ref.crossProduct(dir).normalize();
        Vector v = dir.clone().crossProduct(u).normalize();
        for (int i = 0; i < points; i++) {
            double t = i / (double) points, ang = t * turns * 2 * Math.PI;
            Vector off = u.clone().multiply(Math.cos(ang) * radius).add(v.clone().multiply(Math.sin(ang) * radius));
            f.accept(a.clone().add(dir.clone().multiply(t * len)).add(off));
        }
    }
    /** Cracks spreading outward along the ground from c. */
    public static void cracks(Location c, double r, int arms, Consumer<Location> f) {
        double[] start = new double[arms];
        for (int k = 0; k < arms; k++) start[k] = Math.random() * Math.PI * 2;
        repeat(8, 1, i -> {
            for (int k = 0; k < arms; k++) {
                double a = start[k] + k * 2 * Math.PI / arms;
                for (double d = r * i / 8.0 + 0.3; d < r * (i + 1) / 8.0 + 0.3; d += 0.35) {
                    double wob = Math.sin(d * 3 + k) * 0.18;
                    f.accept(c.clone().add(Math.cos(a) * d - Math.sin(a) * wob, 0.05, Math.sin(a) * d + Math.cos(a) * wob));
                }
            }
        });
    }
    public static void repeat(int times, int period, IntConsumer f) {
        new BukkitRunnable() {
            int i = 0;
            @Override public void run() {
                if (i >= times) { cancel(); return; }
                f.accept(i++);
            }
        }.runTaskTimer(SoulPlugin.get(), 0, period);
    }
    public static void later(int ticks, Runnable r) {
        Bukkit.getScheduler().runTaskLater(SoulPlugin.get(), r, ticks);
    }

    // ---------- targeting ----------
    public static boolean valid(LivingEntity le, Player self) {
        if (le.equals(self) || le instanceof ArmorStand || le.isDead() || !le.isValid()) return false;
        return !(le instanceof Player pl && (pl.getGameMode() == GameMode.SPECTATOR || pl.getGameMode() == GameMode.CREATIVE));
    }
    public static List<LivingEntity> around(Location c, double r, Player self) {
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : c.getWorld().getNearbyEntities(c, r, r, r)) {
            if (e instanceof LivingEntity le && valid(le, self) && e.getLocation().distanceSquared(c) <= r * r) out.add(le);
        }
        return out;
    }
    public static double rayDist(Player p, double range) {
        Location eye = p.getEyeLocation();
        RayTraceResult r = p.getWorld().rayTraceBlocks(eye, eye.getDirection(), range, FluidCollisionMode.NEVER, true);
        return r == null ? range : r.getHitPosition().distance(eye.toVector());
    }
    public static Location targetLoc(Player p, double range) {
        Location eye = p.getEyeLocation();
        return eye.clone().add(eye.getDirection().multiply(rayDist(p, range)));
    }
    /** Living entities along the player's view ray, nearest first. */
    public static List<LivingEntity> onRay(Player p, double range, double width) {
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection();
        double max = rayDist(p, range);
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : p.getWorld().getNearbyEntities(eye, range, range, range)) {
            if (!(e instanceof LivingEntity le) || !valid(le, p)) continue;
            Vector to = le.getBoundingBox().getCenter().subtract(eye.toVector());
            double proj = to.dot(dir);
            if (proj < 0 || proj > max) continue;
            double dist = to.clone().subtract(dir.clone().multiply(proj)).length();
            if (dist <= width + le.getWidth() / 2) out.add(le);
        }
        out.sort(Comparator.comparingDouble(x -> x.getLocation().distanceSquared(eye)));
        return out;
    }
    public static Location muzzle(Player p) {
        return p.getEyeLocation().add(0, -0.25, 0).add(p.getEyeLocation().getDirection().multiply(0.6));
    }
    public static Vector flatDir(Player p) {
        Vector d = p.getLocation().getDirection().setY(0);
        return d.lengthSquared() < 0.001 ? new Vector(0, 0, 1) : d.normalize();
    }
    public static void push(LivingEntity t, Vector from, double strength, double up) {
        Vector v = t.getLocation().toVector().subtract(from).setY(0);
        if (v.lengthSquared() > 0.01) v.normalize().multiply(strength);
        t.setVelocity(v.setY(up));
    }

    // ---------- effects / attributes ----------
    public static void effect(LivingEntity e, PotionEffectType t, int ticks, int amp) {
        e.addPotionEffect(new PotionEffect(t, ticks, amp));
    }
    public static void passiveEffect(Player p, PotionEffectType t, int ticks, int amp) {
        p.addPotionEffect(new PotionEffect(t, ticks, amp, true, false, false));
    }
    public static void heal(LivingEntity e, double amount) {
        AttributeInstance max = e.getAttribute(Attribute.MAX_HEALTH);
        double m = max == null ? 20 : max.getValue();
        e.setHealth(Math.min(m, e.getHealth() + amount));
    }
    public static void attr(Player p, Attribute a, String name, double v, AttributeModifier.Operation op) {
        AttributeInstance in = p.getAttribute(a);
        if (in == null) return;
        NamespacedKey key = new NamespacedKey(SoulPlugin.get(), "soul_" + name);
        for (AttributeModifier m : new ArrayList<>(in.getModifiers())) if (m.getKey().equals(key)) in.removeModifier(m);
        in.addModifier(new AttributeModifier(key, v, op, EquipmentSlotGroup.ANY));
    }

    // ---------- shared flair ----------
    /** Played on every successful ability cast. */
    public static void cast(Player p, Color c) {
        Location l = p.getLocation();
        sound(l, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.4f);
        sound(l, Sound.ENTITY_EVOKER_CAST_SPELL, 0.5f, 1.6f);
        repeat(10, 1, i -> ring(p.getLocation().add(0, i * 0.2, 0), 0.9 - i * 0.03, 14, q -> dust(q, c, 1.2f, 1, 0)));
    }
    /** Played when a soul is absorbed (right-click item, GUI equip). */
    public static void absorb(Player p, Soul s) {
        sound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.5f);
        sound(p.getLocation(), Sound.ENTITY_ALLAY_ITEM_GIVEN, 1f, 0.8f);
        repeat(24, 1, i -> {
            double r = 3.0 * (1 - i / 24.0);
            for (int k = 0; k < 3; k++) {
                double a = i * 0.5 + k * 2.094;
                Location q = p.getLocation().add(Math.cos(a) * r, 0.2 + i * 0.07, Math.sin(a) * r);
                dust(q, s.color, 1.4f, 2, 0.05);
                spawn(q, Particle.END_ROD, 1, 0.02, 0.01);
            }
        });
        later(24, () -> {
            Location c = p.getLocation().add(0, 1, 0);
            sphere(c, 1.3, 45, q -> dust(q, s.color, 1.5f, 1, 0));
            spawn(c, Particle.TOTEM_OF_UNDYING, 30, 0.4, 0.3);
            sound(c, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            sound(c, Sound.ITEM_TOTEM_USE, 0.4f, 1.5f);
            p.sendActionBar(Component.text("Soul absorbed: ", NamedTextColor.GRAY).append(Component.text(s.name, s.text)));
        });
    }
}
