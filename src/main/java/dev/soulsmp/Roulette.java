package dev.soulsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.time.Duration;
import java.util.List;

/**
 * Soul-selection animation: every soul orbits the player, the ring spins down,
 * the chosen soul stops at the pointer, the others collapse, the winner rises and bursts.
 */
final class Roulette extends BukkitRunnable {
    private static final int SPIN = 130, REVEAL = 60;
    private static final double TAU = Math.PI * 2, RADIUS = 2.4;

    private final SoulPlugin plugin;
    private final SoulManager mgr;
    private final Player p;
    private final Soul chosen;
    private final List<Soul> souls = SoulRegistry.all();
    private final int n = souls.size(), idx;
    private final ItemDisplay[] orbs = new ItemDisplay[n];
    private final double pointer, total;
    private int t = 0, lastSector = 0;
    private boolean prevInvuln, awarded, finished;
    private Location chosenStart;

    Roulette(SoulPlugin plugin, SoulManager mgr, Player p, Soul chosen) {
        this.plugin = plugin; this.mgr = mgr; this.p = p; this.chosen = chosen;
        this.idx = souls.indexOf(chosen);
        Vector dir = p.getLocation().getDirection();
        this.pointer = Math.atan2(dir.getZ(), dir.getX());
        double diff = (((pointer - TAU * idx / n) % TAU) + TAU) % TAU;
        this.total = diff + TAU * 3;
    }

    void start() {
        prevInvuln = p.isInvulnerable();
        p.setInvulnerable(true);
        Location c = center();
        for (int i = 0; i < n; i++) {
            final Soul s = souls.get(i);
            orbs[i] = c.getWorld().spawn(c, ItemDisplay.class, d -> {
                d.setItemStack(s.item());
                d.setBillboard(Display.Billboard.CENTER);
                d.setTeleportDuration(2);
                d.setInterpolationDuration(3);
                d.setBrightness(new Display.Brightness(15, 15));
                d.setPersistent(false);
                d.setTransformation(scale(0.01f));
            });
        }
        p.showTitle(Title.title(
            Component.text("✦ SOUL AWAKENING ✦", NamedTextColor.WHITE, TextDecoration.BOLD),
            Component.text("Fate is choosing...", NamedTextColor.GRAY),
            Title.Times.times(Duration.ofMillis(400), Duration.ofSeconds(5), Duration.ofMillis(400))));
        Fx.sound(c, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 0.7f);
        Fx.sound(c, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 0.8f);
        runTaskTimer(plugin, 0, 1);
    }

    private Location center() { return p.getLocation().add(0, 1.0, 0); }

    private static Transformation scale(float s) {
        return new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(s, s, s), new AxisAngle4f());
    }

    @Override public void run() {
        if (!p.isOnline()) { finish(true); return; }
        Location c = center();
        if (t < SPIN) spin(c); else reveal(c, t - SPIN);
        t++;
    }

    private Location orbPos(Location c, int i, double angle, double r) {
        return c.clone().add(Math.cos(angle) * r, 0.25 * Math.sin(t * 0.12 + i), Math.sin(angle) * r);
    }

    private void spin(Location c) {
        double s = (double) t / SPIN;
        double ease = 1 - Math.pow(1 - s, 3);
        double rot = total * ease;
        double r = RADIUS + Math.max(0, 20 - t) * 0.06;
        for (int i = 0; i < n; i++) {
            double angle = TAU * i / n + rot;
            Location pos = orbPos(c, i, angle, r);
            orbs[i].teleport(pos);
            if (t <= 20) { orbs[i].setInterpolationDelay(0); orbs[i].setTransformation(scale((float) Math.min(0.8, 0.05 + t * 0.05))); }
            Fx.dust(pos, souls.get(i).color, 1f, 2, 0.1);
            if (t % 2 == 0) Fx.spawn(pos, Particle.END_ROD, 1, 0.05, 0.01);
            double diff = Math.abs((((angle - pointer) % TAU) + TAU + Math.PI) % TAU - Math.PI);
            if (diff < 0.22) Fx.dust(pos, souls.get(i).color, 2.2f, 5, 0.2);
        }
        // pointer marker
        Location pp = c.clone().add(Math.cos(pointer) * RADIUS, 0, Math.sin(pointer) * RADIUS);
        Fx.spawn(pp, Particle.END_ROD, 2, 0.05, 0.02);
        for (double y = -0.6; y <= 0.6; y += 0.3) Fx.dust(pp.clone().add(0, y, 0), Color.WHITE, 0.9f, 1, 0);
        // floor ring
        if (t % 3 == 0) Fx.ring(p.getLocation().add(0, 0.05, 0), RADIUS, 36, q -> Fx.dust(q, Color.fromRGB(200, 200, 255), 1f, 1, 0));
        // ticking
        int sector = (int) Math.floor(rot / (TAU / n));
        if (sector != lastSector) {
            lastSector = sector;
            Fx.sound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, (float) (0.6 + s * 1.3));
        }
        if (t == SPIN - 1) Fx.sound(c, Sound.BLOCK_NOTE_BLOCK_BELL, 1.5f, 1.4f);
    }

    private void reveal(Location c, int rt) {
        Location head = c.clone().add(0, 1.5, 0);
        if (rt == 0) {
            chosenStart = orbs[idx].getLocation();
            orbs[idx].setGlowing(true);
            orbs[idx].setGlowColorOverride(chosen.color);
            Fx.sound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.8f);
            Fx.sound(c, Sound.ENTITY_WITHER_SPAWN, 0.4f, 1.6f);
        }
        for (int i = 0; i < n; i++) {
            ItemDisplay o = orbs[i];
            if (o == null || !o.isValid()) continue;
            if (i == idx) {
                double q = Math.min(1, rt / 30.0);
                double e = 1 - Math.pow(1 - q, 3);
                Vector from = chosenStart.toVector(), to = head.toVector();
                Location pos = from.clone().add(to.clone().subtract(from).multiply(e)).toLocation(c.getWorld());
                o.teleport(pos);
                if (rt <= 30) { o.setInterpolationDelay(0); o.setTransformation(scale((float) (0.8 + e * 1.0))); }
                for (int k = 0; k < 3; k++) {
                    double a = rt * 0.4 + k * TAU / 3;
                    Location sp = pos.clone().add(Math.cos(a) * 0.8, Math.sin(rt * 0.3 + k) * 0.3, Math.sin(a) * 0.8);
                    Fx.dust(sp, chosen.color, 1.4f, 2, 0.05);
                    Fx.spawn(sp, Particle.END_ROD, 1, 0.02, 0.01);
                }
            } else {
                double q = Math.min(1, rt / 24.0);
                double angle = TAU * i / n + total + rt * 0.25;
                Location pos = orbPos(c, i, angle, RADIUS * (1 - q)).add(0, q * 1.5, 0);
                o.teleport(pos);
                o.setInterpolationDelay(0);
                o.setTransformation(scale((float) Math.max(0.01, 0.8 * (1 - q))));
                Fx.dust(pos, souls.get(i).color, 1f, 1, 0.1);
                if (rt == 24) {
                    Fx.spawn(pos, Particle.CLOUD, 8, 0.2, 0.05);
                    Fx.sound(pos, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 0.4f, 1.6f);
                    o.remove();
                }
            }
        }
        if (rt == 30) {
            Fx.sphere(head, 1.8, 70, q -> Fx.dust(q, chosen.color, 1.8f, 1, 0));
            Fx.spawn(head, Particle.TOTEM_OF_UNDYING, 50, 0.4, 0.5);
            Fx.spawn(head, Particle.FIREWORK, 30, 0.5, 0.2);
            Fx.spawn(head, Particle.END_ROD, 40, 0.5, 0.3);
            Fx.sound(head, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            Fx.sound(head, Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
            Fx.sound(head, Sound.ITEM_TOTEM_USE, 0.6f, 1.3f);
            Fx.sound(head, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.5f, 1f);
            award();
            p.showTitle(Title.title(
                Component.text(chosen.name.toUpperCase(), chosen.text, TextDecoration.BOLD),
                Component.text("You awakened the " + chosen.name + " Soul", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(800))));
            Bukkit.broadcast(Component.text("✦ ", chosen.text)
                .append(Component.text(p.getName(), NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text(" has awakened the ", NamedTextColor.GRAY))
                .append(Component.text(chosen.name + " Soul", chosen.text, TextDecoration.BOLD))
                .append(Component.text("!", NamedTextColor.GRAY)));
        }
        if (rt >= REVEAL) finish(false);
    }

    private void award() {
        if (awarded) return;
        awarded = true;
        mgr.setSoul(p, chosen, false);
    }

    void finish(boolean abort) {
        if (finished) return;
        finished = true;
        try { cancel(); } catch (IllegalStateException ignored) {}
        for (ItemDisplay o : orbs) if (o != null && o.isValid()) o.remove();
        p.setInvulnerable(prevInvuln);
        if (!awarded) award();
        mgr.forget(p);
    }
}
