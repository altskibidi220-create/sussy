package dev.soulsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** All soul definitions: passives + two abilities each. */
final class Souls {
    private Souls() {}

    private static void fail(Player p, String msg) {
        p.sendActionBar(Component.text(msg, NamedTextColor.RED));
        Fx.sound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.6f);
    }

    // =====================================================================================
    static Soul vitality() {
        Color c = Color.fromRGB(0xFF7AA2);
        return new Soul("vitality", "Vitality", 0xFF7AA2, "Overflowing life force.",
            new Passive("Hardy Heart", "+2 max hearts.")
                .apply(p -> Fx.attr(p, Attribute.MAX_HEALTH, "vitality", 4, Operation.ADD_NUMBER)),
            new Ability("Heart Burst", "Heal 3 hearts and gain Regeneration II for 4s.", 30, p -> {
                Fx.heal(p, 6);
                Fx.effect(p, PotionEffectType.REGENERATION, 80, 1);
                Location b = p.getLocation().add(0, 1, 0);
                Fx.sound(b, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.6f);
                Fx.sound(b, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.8f);
                Fx.repeat(20, 1, i -> {
                    double a = i * 0.6;
                    Location q = p.getLocation().add(Math.cos(a) * 0.9, 0.1 + i * 0.1, Math.sin(a) * 0.9);
                    Fx.spawn(q, Particle.HEART, 1, 0.05, 0);
                    Fx.dust(q, c, 1.3f, 3, 0.1);
                });
                return true;
            }),
            new Ability("Life Bulwark", "Gain 4 absorption hearts for 12s and Resistance I for 6s.", 45, p -> {
                Fx.effect(p, PotionEffectType.ABSORPTION, 240, 1);
                Fx.effect(p, PotionEffectType.RESISTANCE, 120, 0);
                Location b = p.getLocation().add(0, 1, 0);
                Fx.sound(b, Sound.ITEM_TOTEM_USE, 0.5f, 1.6f);
                Fx.sound(b, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);
                Fx.sphere(b, 1.3, 50, q -> Fx.dust(q, c, 1.3f, 1, 0));
                Fx.spawn(b, Particle.TOTEM_OF_UNDYING, 25, 0.5, 0.3);
                Fx.repeat(12, 2, i -> Fx.ring(p.getLocation().add(0, i * 0.18, 0), 1.1, 16, q -> Fx.dust(q, Color.WHITE, 1f, 1, 0)));
                return true;
            }));
    }

    // =====================================================================================
    static Soul might() {
        Color c = Color.fromRGB(0xE63946);
        return new Soul("might", "Might", 0xE63946, "Raw, crushing power.",
            new Passive("Brute Force", "+1 attack damage.")
                .apply(p -> Fx.attr(p, Attribute.ATTACK_DAMAGE, "might", 1.0, Operation.ADD_NUMBER)),
            new Ability("Berserk", "Strength II for 6s.", 40, p -> {
                Fx.effect(p, PotionEffectType.STRENGTH, 120, 1);
                Location b = p.getLocation().add(0, 1, 0);
                Fx.sound(b, Sound.ENTITY_RAVAGER_ROAR, 0.9f, 1.1f);
                Fx.sound(b, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.6f);
                Fx.spawn(b, Particle.ANGRY_VILLAGER, 6, 0.5, 0);
                Fx.repeat(25, 1, i -> {
                    double a = i * 0.7;
                    Location q = p.getLocation().add(Math.cos(a) * 0.8, 0.1 + i * 0.08, Math.sin(a) * 0.8);
                    Fx.dust(q, c, 1.6f, 3, 0.1);
                    Fx.spawn(q, Particle.CRIT, 1, 0.1, 0.1);
                });
                return true;
            }),
            new Ability("Titan Slam", "Slam the ground: 3 hearts damage and launch enemies within 5 blocks.", 22, p -> {
                Location b = p.getLocation().add(0, 0.2, 0);
                Fx.sound(b, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);
                Fx.sound(b, Sound.BLOCK_ANVIL_LAND, 1f, 0.6f);
                Fx.spawn(b, Particle.EXPLOSION, 1, 0, 0);
                Fx.nova(b, 5, 8, q -> { Fx.dust(q, c, 1.5f, 1, 0); Fx.block(q, Material.STONE, 2); });
                Fx.cracks(b, 5, 8, q -> { Fx.block(q, Material.COARSE_DIRT, 2); Fx.spawn(q, Particle.SMOKE, 1, 0.05, 0.02); });
                Fx.repeat(8, 1, i -> Fx.ring(b.clone().add(0, i * 0.4, 0), 1.2 - i * 0.08, 12, q -> Fx.dust(q, Color.fromRGB(130, 110, 100), 1.6f, 1, 0.1)));
                for (LivingEntity t : Fx.around(b, 5, p)) {
                    t.damage(6, p);
                    Fx.push(t, b.toVector(), 0.9, 0.55);
                }
                return true;
            }));
    }

    // =====================================================================================
    static Soul swiftness() {
        Color c = Color.fromRGB(0x2EE6C5);
        return new Soul("swiftness", "Swiftness", 0x2EE6C5, "Faster than the wind itself.",
            new Passive("Fleet Foot", "+10% movement speed.")
                .apply(p -> Fx.attr(p, Attribute.MOVEMENT_SPEED, "swiftness", 0.10, Operation.ADD_SCALAR)),
            new Ability("Dash", "Burst forward in the direction you look.", 8, p -> {
                Vector v = p.getLocation().getDirection().multiply(1.6);
                if (v.getY() < 0.2) v.setY(0.2);
                p.setVelocity(v);
                Fx.sound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.5f);
                Fx.sound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.4f);
                Fx.repeat(8, 1, i -> {
                    Location q = p.getLocation().add(0, 0.9, 0);
                    Fx.dust(q, c, 1.6f, 6, 0.25);
                    Fx.spawn(q, Particle.CLOUD, 3, 0.2, 0.02);
                });
                return true;
            }),
            new Ability("Hyper Drive", "Speed III and Jump Boost II for 6s.", 30, p -> {
                Fx.effect(p, PotionEffectType.SPEED, 120, 2);
                Fx.effect(p, PotionEffectType.JUMP_BOOST, 120, 1);
                Fx.sound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 2f);
                Fx.sound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 2f);
                Fx.repeat(120, 1, i -> {
                    Location q = p.getLocation().add(0, 0.1, 0);
                    Fx.dust(q, c, 1.2f, 2, 0.2);
                    if (i % 3 == 0) Fx.spawn(q, Particle.ELECTRIC_SPARK, 2, 0.2, 0.02);
                });
                return true;
            }));
    }

    // =====================================================================================
    static Soul inferno() {
        Color c = Color.fromRGB(0xFF8C1A);
        return new Soul("inferno", "Inferno", 0xFF8C1A, "Born in the flames.",
            new Passive("Molten Skin", "Immune to fire damage. Attackers are ignited for 2s.")
                .immune(DamageCause.FIRE, DamageCause.FIRE_TICK, DamageCause.HOT_FLOOR)
                .hurt((p, a) -> a.setFireTicks(40)),
            new Ability("Fireball", "Launch an exploding fireball.", 12, p -> {
                Fireball fb = p.launchProjectile(Fireball.class, p.getEyeLocation().getDirection().multiply(1.1));
                fb.setYield(1.3f);
                fb.setIsIncendiary(false);
                Fx.sound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.8f);
                Fx.sound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 0.9f);
                new BukkitRunnable() {
                    int n = 0;
                    Location last = fb.getLocation();
                    @Override public void run() {
                        if (!fb.isValid() || n++ > 80) {
                            if (n <= 80) impact(last);
                            cancel();
                            return;
                        }
                        Location q = fb.getLocation();
                        last = q.clone();
                        Fx.spawn(q, Particle.FLAME, 5, 0.2, 0.02);
                        Fx.spawn(q, Particle.SMOKE, 2, 0.15, 0.01);
                        Fx.dust(q, c, 1.8f, 4, 0.25);
                        Fx.dust(q, Color.fromRGB(255, 220, 90), 1.0f, 2, 0.1);
                    }
                    void impact(Location l) {
                        Fx.sphere(l, 2, 60, x -> Fx.dust(x, c, 2f, 1, 0));
                        Fx.spawn(l, Particle.FLAME, 50, 0.9, 0.15);
                        Fx.spawn(l, Particle.LAVA, 15, 0.8, 0);
                        Fx.spawn(l, Particle.EXPLOSION, 1, 0, 0);
                        Fx.nova(l, 3.5, 6, x -> Fx.spawn(x, Particle.FLAME, 1, 0.05, 0.03));
                    }
                }.runTaskTimer(SoulPlugin.get(), 0, 1);
                return true;
            }),
            new Ability("Ring of Fire", "Blast flames outward: 1.5 hearts, ignite and push enemies within 6 blocks.", 35, p -> {
                Location b = p.getLocation().add(0, 0.3, 0);
                Fx.sound(b, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.6f);
                Fx.sound(b, Sound.ITEM_FIRECHARGE_USE, 1f, 0.6f);
                Fx.sound(b, Sound.BLOCK_FIRE_AMBIENT, 1f, 0.8f);
                Fx.spawn(b, Particle.LAVA, 20, 1, 0);
                Fx.nova(b, 6, 10, q -> { Fx.spawn(q, Particle.FLAME, 2, 0.05, 0.02); Fx.dust(q, c, 1.6f, 1, 0); });
                Fx.repeat(14, 1, i -> {
                    double h = Math.min(i, 8) * 0.28;
                    Fx.ring(b, 6, 14, q -> {
                        Fx.spawn(q.clone().add(0, h, 0), Particle.FLAME, 2, 0.08, 0.02);
                        if (i % 3 == 0) Fx.spawn(q.clone().add(0, h, 0), Particle.SMOKE, 1, 0.05, 0.02);
                    });
                    Fx.ring(b.clone().add(0, h * 0.5, 0), 6 - i * 0.35, 18, q -> Fx.dust(q, c, 1.3f, 1, 0));
                });
                for (LivingEntity t : Fx.around(b, 6, p)) {
                    t.damage(3, p);
                    t.setFireTicks(100);
                    Fx.push(t, b.toVector(), 0.8, 0.3);
                }
                return true;
            }));
    }

    // =====================================================================================
    static Soul frost() {
        Color c = Color.fromRGB(0x9BE7FF);
        return new Soul("frost", "Frost", 0x9BE7FF, "Cold as the void between stars.",
            new Passive("Frozen Heart", "Immune to freezing. Attackers are slowed for 3s.")
                .immune(DamageCause.FREEZE)
                .hurt((p, a) -> Fx.effect(a, PotionEffectType.SLOWNESS, 60, 0)),
            new Ability("Frost Nova", "Freeze everything within 6 blocks: 1.5 hearts + Slowness III for 4s.", 28, p -> {
                Location b = p.getLocation().add(0, 0.3, 0);
                Fx.sound(b, Sound.BLOCK_GLASS_BREAK, 1f, 1.5f);
                Fx.sound(b, Sound.BLOCK_POWDER_SNOW_BREAK, 1f, 0.7f);
                Fx.sound(b, Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.8f);
                Fx.nova(b, 6, 10, q -> { Fx.spawn(q, Particle.SNOWFLAKE, 2, 0.05, 0.01); Fx.dust(q, c, 1.4f, 1, 0); });
                Fx.spawn(b.clone().add(0, 1, 0), Particle.SNOWFLAKE, 60, 1.5, 0.1);
                List<Location> spikes = new ArrayList<>();
                for (double r : new double[]{2.0, 3.5, 5.0})
                    Fx.ring(b, r, (int) (r * 4), q -> { if (Math.random() < 0.6) spikes.add(q); });
                Fx.repeat(9, 1, i -> {
                    for (Location sp : spikes) {
                        Location q = sp.clone().add(0, i * 0.22, 0);
                        Fx.spawn(q, Particle.SNOWFLAKE, 1, 0.03, 0);
                        Fx.dust(q, Color.WHITE, 1f, 1, 0.02);
                        Fx.dust(q, c, 1.2f, 1, 0.02);
                    }
                });
                for (LivingEntity t : Fx.around(b, 6, p)) {
                    t.damage(3, p);
                    Fx.effect(t, PotionEffectType.SLOWNESS, 80, 2);
                    t.setFreezeTicks(200);
                }
                return true;
            }),
            new Ability("Ice Lance", "Hitscan shard: 3.5 hearts, Slowness II and freezing.", 14, p -> {
                Location start = Fx.muzzle(p);
                List<LivingEntity> hits = Fx.onRay(p, 30, 0.6);
                Location end;
                if (!hits.isEmpty()) {
                    LivingEntity t = hits.get(0);
                    end = t.getLocation().add(0, t.getHeight() / 2, 0);
                    t.damage(7, p);
                    Fx.effect(t, PotionEffectType.SLOWNESS, 60, 1);
                    t.setFreezeTicks(180);
                } else {
                    end = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(Fx.rayDist(p, 30)));
                }
                Fx.line(start, end, 0.3, q -> { Fx.spawn(q, Particle.SNOWFLAKE, 1, 0.03, 0); Fx.dust(q, c, 1.1f, 1, 0); });
                Fx.helix(start, end, 0.3, Math.max(1, start.distance(end) / 1.5), (int) (start.distance(end) * 8),
                    q -> Fx.dust(q, Color.WHITE, 0.8f, 1, 0));
                Fx.sphere(end, 0.9, 24, q -> Fx.dust(q, c, 1.3f, 1, 0));
                Fx.spawn(end, Particle.SNOWFLAKE, 25, 0.4, 0.05);
                Fx.block(end, Material.ICE, 12);
                Fx.sound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1.8f);
                Fx.sound(end, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 1.5f);
                return true;
            }));
    }

    // =====================================================================================
    static Soul tempest() {
        Color c = Color.fromRGB(0x5B6CFF);
        return new Soul("tempest", "Tempest", 0x5B6CFF, "The storm answers your call.",
            new Passive("Storm Veil", "Immune to lightning. +10% attack speed.")
                .immune(DamageCause.LIGHTNING)
                .apply(p -> Fx.attr(p, Attribute.ATTACK_SPEED, "tempest", 0.10, Operation.ADD_SCALAR)),
            new Ability("Smite", "Call lightning on the targeted spot after a short delay: 4 hearts.", 18, p -> {
                Location t = Fx.targetLoc(p, 30);
                Fx.sound(p.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.5f);
                Fx.repeat(8, 1, i -> Fx.ring(t, 3 - i * 0.3, 20, q -> Fx.dust(q, c, 1.3f, 1, 0)));
                Fx.later(8, () -> {
                    t.getWorld().strikeLightningEffect(t);
                    Fx.sound(t, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.1f);
                    Fx.repeat(6, 1, i -> {
                        for (int y = 0; y < 16; y++) {
                            Location q = t.clone().add(0, y, 0);
                            Fx.dust(q, Color.WHITE, 1.6f, 2, 0.15);
                            Fx.spawn(q, Particle.ELECTRIC_SPARK, 3, 0.25, 0.15);
                        }
                        Fx.ring(t, 0.6 + i * 0.7, 16 + i * 4, q -> Fx.spawn(q, Particle.ELECTRIC_SPARK, 1, 0.05, 0.05));
                    });
                    Fx.cracks(t, 4, 6, q -> { Fx.spawn(q, Particle.ELECTRIC_SPARK, 1, 0.02, 0); Fx.dust(q, c, 1f, 1, 0); });
                    Fx.spawn(t, Particle.EXPLOSION, 1, 0, 0);
                    for (LivingEntity e : Fx.around(t, 3.5, p)) { e.damage(8, p); Fx.effect(e, PotionEffectType.SLOWNESS, 30, 1); }
                });
                return true;
            }),
            new Ability("Chain Lightning", "Zap the targeted enemy; jumps to 4 more nearby for 2.5 hearts each.", 22, p -> {
                List<LivingEntity> first = Fx.onRay(p, 20, 1.0);
                if (first.isEmpty()) { fail(p, "No target in sight"); return false; }
                List<LivingEntity> chain = new ArrayList<>();
                LivingEntity cur = first.get(0);
                chain.add(cur);
                while (chain.size() < 5) {
                    LivingEntity best = null; double bd = 36;
                    for (LivingEntity e : Fx.around(cur.getLocation(), 6, p)) {
                        if (chain.contains(e)) continue;
                        double d = e.getLocation().distanceSquared(cur.getLocation());
                        if (d < bd) { bd = d; best = e; }
                    }
                    if (best == null) break;
                    chain.add(best); cur = best;
                }
                for (int i = 0; i < chain.size(); i++) {
                    final int idx = i;
                    Fx.later(i * 3, () -> {
                        LivingEntity tgt = chain.get(idx);
                        if (!tgt.isValid()) return;
                        Location from = idx == 0 ? Fx.muzzle(p) : chain.get(idx - 1).getLocation().add(0, 1, 0);
                        Location to = tgt.getLocation().add(0, tgt.getHeight() / 2, 0);
                        Fx.line(from, to, 0.3, q -> { Fx.spawn(q, Particle.ELECTRIC_SPARK, 1, 0.05, 0); Fx.dust(q, c, 1f, 1, 0); });
                        Fx.spawn(to, Particle.ELECTRIC_SPARK, 15, 0.3, 0.1);
                        tgt.damage(5, p);
                        Fx.sound(to, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.8f);
                    });
                }
                return true;
            }));
    }

    // =====================================================================================
    static Soul gale() {
        Color c = Color.fromRGB(0xC8F7DC);
        return new Soul("gale", "Gale", 0xC8F7DC, "Light as the open sky.",
            new Passive("Featherfall", "No fall damage. Jump Boost I.")
                .immune(DamageCause.FALL)
                .tick(p -> Fx.passiveEffect(p, PotionEffectType.JUMP_BOOST, 100, 0))
                .remove(p -> p.removePotionEffect(PotionEffectType.JUMP_BOOST)),
            new Ability("Updraft", "Launch skyward and glide down with Slow Falling for 5s.", 18, p -> {
                p.setVelocity(p.getVelocity().setY(1.3));
                Fx.effect(p, PotionEffectType.SLOW_FALLING, 100, 0);
                Fx.sound(p.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1f);
                Fx.spawn(p.getLocation(), Particle.GUST, 1, 0, 0);
                Fx.repeat(15, 1, i -> {
                    Fx.spawn(p.getLocation(), Particle.CLOUD, 6, 0.3, 0.05);
                    Fx.ring(p.getLocation(), 1 + i * 0.05, 12, q -> Fx.dust(q, c, 1.2f, 1, 0));
                });
                return true;
            }),
            new Ability("Cyclone Blast", "Blast a cone of wind: 1 heart and heavy knockback.", 20, p -> {
                Location b = p.getLocation().add(0, 1, 0);
                Vector d = Fx.flatDir(p);
                Vector side = new Vector(-d.getZ(), 0, d.getX());
                Fx.sound(b, Sound.ENTITY_WIND_CHARGE_THROW, 1f, 0.8f);
                Fx.sound(b, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1.2f);
                Fx.repeat(8, 1, i -> {
                    double dist = 1 + i * 0.8;
                    for (int k = -3; k <= 3; k++) {
                        Location q = b.clone().add(d.clone().multiply(dist)).add(side.clone().multiply(k * 0.15 * dist));
                        Fx.spawn(q, Particle.CLOUD, 1, 0.1, 0.02);
                        Fx.dust(q, c, 1.2f, 1, 0);
                    }
                });
                for (LivingEntity t : Fx.around(b, 7, p)) {
                    Vector to = t.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
                    if (to.lengthSquared() < 0.01 || to.clone().normalize().dot(d) < 0.35) continue;
                    t.damage(2, p);
                    t.setVelocity(to.normalize().multiply(1.6).setY(0.5));
                }
                return true;
            }));
    }

    // =====================================================================================
    static Soul terra() {
        Color c = Color.fromRGB(0xA9743F);
        return new Soul("terra", "Terra", 0xA9743F, "Steadfast as the mountain.",
            new Passive("Stoneskin", "+3 armor and +20% knockback resistance.")
                .apply(p -> {
                    Fx.attr(p, Attribute.ARMOR, "terra_armor", 3, Operation.ADD_NUMBER);
                    Fx.attr(p, Attribute.KNOCKBACK_RESISTANCE, "terra_kb", 0.2, Operation.ADD_NUMBER);
                }),
            new Ability("Stone Guard", "Resistance II for 5s (Slowness I while active).", 40, p -> {
                Fx.effect(p, PotionEffectType.RESISTANCE, 100, 1);
                Fx.effect(p, PotionEffectType.SLOWNESS, 100, 0);
                Location b = p.getLocation().add(0, 1, 0);
                Fx.sound(b, Sound.BLOCK_GRAVEL_BREAK, 1f, 0.5f);
                Fx.sound(b, Sound.ENTITY_IRON_GOLEM_DAMAGE, 1f, 0.7f);
                Fx.sound(b, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.6f);
                Fx.sphere(b, 1.4, 60, q -> { Fx.dust(q, c, 1.4f, 1, 0); Fx.block(q, Material.STONE, 1); });
                return true;
            }),
            new Ability("Earthquake", "Shake the earth: 2 hearts, Slowness II and launch enemies within 6 blocks.", 26, p -> {
                Location b = p.getLocation().add(0, 0.2, 0);
                Fx.sound(b, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.6f);
                Fx.sound(b, Sound.BLOCK_GRAVEL_BREAK, 1.5f, 0.5f);
                Fx.nova(b, 6, 10, q -> { Fx.block(q, Material.DIRT, 3); Fx.dust(q, c, 1.4f, 1, 0); });
                Fx.cracks(b, 6, 10, q -> { Fx.block(q, Material.COARSE_DIRT, 3); Fx.spawn(q, Particle.SMOKE, 1, 0.05, 0.03); });
                Fx.repeat(12, 1, i -> {
                    for (int k = 0; k < 6; k++) {
                        double a = Math.random() * Math.PI * 2, r = Math.random() * 6;
                        Fx.block(b.clone().add(Math.cos(a) * r, 0.2 + i * 0.1, Math.sin(a) * r), Material.STONE, 2);
                    }
                });
                for (LivingEntity t : Fx.around(b, 6, p)) {
                    t.damage(4, p);
                    Fx.effect(t, PotionEffectType.SLOWNESS, 60, 1);
                    t.setVelocity(new Vector(0, 0.7, 0));
                }
                return true;
            }));
    }

    // =====================================================================================
    static Soul tide() {
        Color c = Color.fromRGB(0x1E90FF);
        return new Soul("tide", "Tide", 0x1E90FF, "The ocean never forgets.",
            new Passive("Sea's Blessing", "Water Breathing, no drowning, Dolphin's Grace in water.")
                .immune(DamageCause.DROWNING)
                .tick(p -> {
                    Fx.passiveEffect(p, PotionEffectType.WATER_BREATHING, 100, 0);
                    if (p.isInWater()) Fx.passiveEffect(p, PotionEffectType.DOLPHINS_GRACE, 100, 0);
                })
                .remove(p -> { p.removePotionEffect(PotionEffectType.WATER_BREATHING); p.removePotionEffect(PotionEffectType.DOLPHINS_GRACE); }),
            new Ability("Tidal Wave", "Send a wave forward: 2 hearts and heavy knockback.", 20, p -> {
                Location base = p.getLocation().add(0, 0.8, 0);
                Vector d = Fx.flatDir(p);
                Vector side = new Vector(-d.getZ(), 0, d.getX());
                Set<UUID> hit = new HashSet<>();
                Fx.sound(base, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1f, 0.8f);
                Fx.sound(base, Sound.ITEM_TRIDENT_RIPTIDE_3, 1f, 1f);
                Fx.repeat(10, 1, i -> {
                    Location center = base.clone().add(d.clone().multiply(1.5 + i * 1.2));
                    for (double k = -2.5; k <= 2.5; k += 0.5) {
                        Location q = center.clone().add(side.clone().multiply(k)).add(0, Math.abs(Math.sin(k + i)) * 0.5, 0);
                        Fx.spawn(q, Particle.SPLASH, 4, 0.2, 0.1);
                        for (double y = 0.4; y <= 1.6; y += 0.4)
                            Fx.spawn(q.clone().add(0, y, 0), Particle.SPLASH, 1, 0.15, 0.02);
                        Fx.spawn(q, Particle.BUBBLE_POP, 1, 0.2, 0.02);
                        Fx.dust(q, c, 1.4f, 1, 0.1);
                    }
                    for (LivingEntity t : Fx.around(center, 2.6, p)) {
                        if (!hit.add(t.getUniqueId())) continue;
                        t.damage(4, p);
                        t.setVelocity(d.clone().multiply(1.2).setY(0.35));
                    }
                });
                return true;
            }),
            new Ability("Whirlpool", "Create a whirlpool at the target for 2s that drags enemies in, then bursts (1 heart).", 32, p -> {
                Location t = Fx.targetLoc(p, 14);
                Fx.sound(t, Sound.BLOCK_CONDUIT_ACTIVATE, 1.5f, 0.7f);
                Fx.sound(t, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1f, 0.6f);
                Fx.repeat(40, 1, i -> {
                    for (int k = 0; k < 4; k++) {
                        double a = i * 0.5 + k * Math.PI / 2;
                        for (double r = 0.8; r <= 5.5; r += 1.1) {
                            double aa = a + r * 0.6;
                            Location q = t.clone().add(Math.cos(aa) * r, 0.2 + (5.5 - r) * 0.1, Math.sin(aa) * r);
                            Fx.spawn(q, Particle.SPLASH, 2, 0.1, 0.05);
                            Fx.dust(q, c, 1.3f, 1, 0);
                        }
                    }
                    for (LivingEntity e : Fx.around(t, 6, p)) {
                        Vector v = t.toVector().subtract(e.getLocation().toVector()).setY(0);
                        if (v.lengthSquared() > 0.25) e.setVelocity(v.normalize().multiply(0.4).setY(0.08));
                    }
                    if (i == 39) {
                        Fx.spawn(t, Particle.EXPLOSION, 1, 0, 0);
                        Fx.spawn(t.clone().add(0, 1, 0), Particle.SPLASH, 80, 1.5, 0.3);
                        Fx.sound(t, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.5f, 0.5f);
                        for (LivingEntity e : Fx.around(t, 4, p)) { e.damage(2, p); e.setVelocity(new Vector(0, 0.6, 0)); }
                    }
                });
                return true;
            }));
    }

    // =====================================================================================
    static Soul shadow() {
        Color c = Color.fromRGB(0x7A3FD1);
        return new Soul("shadow", "Shadow", 0x7A3FD1, "Unseen. Unheard. Unstoppable.",
            new Passive("Night Eyes", "Permanent Night Vision.")
                .tick(p -> Fx.passiveEffect(p, PotionEffectType.NIGHT_VISION, 260, 0))
                .remove(p -> p.removePotionEffect(PotionEffectType.NIGHT_VISION)),
            new Ability("Shadow Step", "Teleport up to 8 blocks in the direction you look.", 10, p -> {
                Location eye = p.getEyeLocation();
                Vector dir = eye.getDirection();
                double max = Fx.rayDist(p, 9);
                Location dest = null;
                for (double d = max - 0.4; d >= 1.5; d -= 0.5) {
                    Location feet = eye.clone().add(dir.clone().multiply(d)).add(0, -1.62, 0);
                    if (feet.getBlock().isPassable() && feet.clone().add(0, 1, 0).getBlock().isPassable()) { dest = feet; break; }
                }
                if (dest == null) { fail(p, "No room to step"); return false; }
                dest.setYaw(p.getLocation().getYaw());
                dest.setPitch(p.getLocation().getPitch());
                Location from = p.getLocation();
                Fx.sound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.7f);
                Fx.sphere(from.clone().add(0, 1, 0), 0.8, 30, q -> { Fx.dust(q, c, 1.4f, 1, 0); Fx.spawn(q, Particle.SQUID_INK, 1, 0, 0.02); });
                Fx.line(from.clone().add(0, 1, 0), dest.clone().add(0, 1, 0), 0.4, q -> Fx.dust(q, c, 1.1f, 1, 0.05));
                p.teleport(dest);
                p.setFallDistance(0);
                Fx.repeat(10, 2, i -> {
                    for (double y = 0; y <= 1.8; y += 0.3) {
                        Location q = from.clone().add(0, y, 0);
                        Fx.dust(q, Color.fromRGB(30, 10, 50), 1.8f, 3, 0.18);
                        if (i % 2 == 0) Fx.spawn(q, Particle.SQUID_INK, 1, 0.1, 0.01);
                    }
                    Fx.ring(from.clone().add(0, 0.05, 0), 1.2 - i * 0.1, 16, q -> Fx.dust(q, c, 1.2f, 1, 0));
                });
                Location arrive = dest.clone();
                Fx.sound(arrive, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.3f);
                Fx.spawn(arrive.clone().add(0, 1, 0), Particle.PORTAL, 40, 0.4, 0.5);
                Fx.sphere(arrive.clone().add(0, 1, 0), 0.8, 30, q -> Fx.dust(q, c, 1.4f, 1, 0));
                return true;
            }),
            new Ability("Veil of Night", "Invisibility + Speed I for 6s; enemies within 6 blocks are blinded for 2s.", 40, p -> {
                Fx.effect(p, PotionEffectType.INVISIBILITY, 120, 0);
                Fx.effect(p, PotionEffectType.SPEED, 120, 0);
                Location b = p.getLocation().add(0, 1, 0);
                Fx.sound(b, Sound.ENTITY_PHANTOM_SWOOP, 1f, 0.6f);
                Fx.sound(b, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.5f, 1.4f);
                Fx.repeat(20, 1, i -> {
                    double r = 4 - i * 0.18;
                    Fx.ring(p.getLocation().add(0, 0.2 + i * 0.07, 0), Math.max(0.2, r), 18, q -> { Fx.dust(q, c, 1.5f, 1, 0); Fx.spawn(q, Particle.SQUID_INK, 1, 0, 0.01); });
                });
                for (LivingEntity t : Fx.around(b, 6, p)) Fx.effect(t, PotionEffectType.BLINDNESS, 40, 0);
                return true;
            }));
    }

    // =====================================================================================
    static Soul venom() {
        Color c = Color.fromRGB(0x5FD13A);
        return new Soul("venom", "Venom", 0x5FD13A, "A bite that lingers.",
            new Passive("Toxic Blood", "Immune to poison damage. Attackers are poisoned for 3s.")
                .immune(DamageCause.POISON)
                .hurt((p, a) -> Fx.effect(a, PotionEffectType.POISON, 60, 0)),
            new Ability("Toxic Cloud", "Leave a poison cloud at the target for 6s.", 30, p -> {
                Location t = Fx.targetLoc(p, 12);
                Fx.sound(t, Sound.ENTITY_SPLASH_POTION_BREAK, 1f, 0.7f);
                Fx.sound(t, Sound.ENTITY_WITCH_THROW, 1f, 0.8f);
                t.getWorld().spawn(t, AreaEffectCloud.class, a -> {
                    a.setRadius(3.5f);
                    a.setDuration(120);
                    a.setWaitTime(0);
                    a.setReapplicationDelay(20);
                    a.setSource(p);
                    a.setColor(c);
                    a.addCustomEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.POISON, 60, 1), true);
                });
                Fx.repeat(60, 2, i -> {
                    ThreadLocalRandom rnd = ThreadLocalRandom.current();
                    for (int k = 0; k < 8; k++) {
                        double a = rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(rnd.nextDouble()) * 3.5;
                        Location q = t.clone().add(Math.cos(a) * r, 0.2 + rnd.nextDouble() * 0.8, Math.sin(a) * r);
                        Fx.dust(q, c, 1.5f, 1, 0.1);
                    }
                    Fx.spawn(t.clone().add(0, 0.5, 0), Particle.HAPPY_VILLAGER, 2, 1.5, 0);
                });
                return true;
            }),
            new Ability("Venom Bolt", "Spit a bolt: 2 hearts and Poison II for 5s.", 15, p -> {
                Location start = Fx.muzzle(p);
                List<LivingEntity> hits = Fx.onRay(p, 25, 0.6);
                Location end;
                if (!hits.isEmpty()) {
                    LivingEntity t = hits.get(0);
                    end = t.getLocation().add(0, t.getHeight() / 2, 0);
                    t.damage(4, p);
                    Fx.effect(t, PotionEffectType.POISON, 100, 1);
                } else {
                    end = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(Fx.rayDist(p, 25)));
                }
                int[] n = {0};
                Fx.line(start, end, 0.25, q -> {
                    double a = n[0]++ * 0.9;
                    Vector off = new Vector(Math.cos(a) * 0.25, Math.sin(a) * 0.25, 0);
                    Fx.dust(q.clone().add(off), c, 1.2f, 1, 0);
                    Fx.dust(q, Color.fromRGB(0x2B7A1F), 1f, 1, 0);
                });
                Fx.spawn(end, Particle.HAPPY_VILLAGER, 15, 0.4, 0);
                Fx.sound(p.getLocation(), Sound.ENTITY_LLAMA_SPIT, 1f, 0.7f);
                Fx.sound(end, Sound.ENTITY_SLIME_SQUISH, 1f, 0.6f);
                return true;
            }));
    }

    // =====================================================================================
    static Soul radiance() {
        Color c = Color.fromRGB(0xFFD84A);
        return new Soul("radiance", "Radiance", 0xFFD84A, "Light that mends and burns.",
            new Passive("Divine Glow", "Gain Regeneration I while below 35% health.")
                .tick(p -> {
                    double max = p.getAttribute(Attribute.MAX_HEALTH).getValue();
                    if (p.getHealth() <= max * 0.35) Fx.passiveEffect(p, PotionEffectType.REGENERATION, 80, 0);
                }),
            new Ability("Holy Beam", "Piercing beam of light: 3 hearts to everything in a 25-block line.", 14, p -> {
                Location start = Fx.muzzle(p);
                double len = Fx.rayDist(p, 25);
                Location end = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(len));
                Fx.line(start, end, 0.3, q -> { Fx.spawn(q, Particle.END_ROD, 1, 0.04, 0); Fx.dust(q, c, 1.4f, 2, 0.08); });
                int pts = (int) (start.distance(end) * 8);
                double turns = Math.max(1, start.distance(end) / 2);
                Fx.helix(start, end, 0.4, turns, pts, q -> Fx.dust(q, Color.WHITE, 1f, 1, 0));
                Fx.helix(start, end, 0.4, -turns, pts, q -> Fx.spawn(q, Particle.END_ROD, 1, 0, 0));
                Fx.nova(end, 2.5, 6, q -> Fx.spawn(q, Particle.END_ROD, 1, 0.02, 0.02));
                Fx.ring(p.getLocation().add(0, 2.3, 0), 0.5, 16, q -> Fx.dust(q, c, 1.2f, 1, 0));
                for (LivingEntity t : Fx.onRay(p, 25, 0.8)) {
                    t.damage(6, p);
                    Fx.spawn(t.getLocation().add(0, 1, 0), Particle.END_ROD, 15, 0.3, 0.1);
                }
                Fx.spawn(end, Particle.FIREWORK, 15, 0.3, 0.1);
                Fx.sound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 2f);
                Fx.sound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.4f, 1.8f);
                Fx.sound(end, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 2f);
                return true;
            }),
            new Ability("Sanctuary", "Holy zone (5 blocks, 6s): heals players 1 heart/s, cleanses debuffs, burns hostile mobs.", 45, p -> {
                Location c0 = p.getLocation().clone();
                Fx.sound(c0, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.3f);
                Fx.sound(c0, Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
                PotionEffectType[] bad = {PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.SLOWNESS,
                    PotionEffectType.WEAKNESS, PotionEffectType.BLINDNESS, PotionEffectType.NAUSEA, PotionEffectType.HUNGER};
                Fx.repeat(60, 2, i -> {
                    Fx.ring(c0.clone().add(0, 0.1, 0), 5, 40, q -> Fx.dust(q, c, 1.3f, 1, 0));
                    double h = (i % 20) * 0.25;
                    Fx.ring(c0.clone().add(0, h, 0), 5, 24, q -> Fx.spawn(q, Particle.END_ROD, 1, 0, 0));
                    Fx.spawn(c0.clone().add(0, 1, 0), Particle.END_ROD, 2, 0.3, 0.15);
                    if (i % 10 == 0) {
                        Fx.sound(c0, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.6f);
                        for (LivingEntity t : Fx.around(c0, 5, null)) {
                            if (t instanceof Player pl) {
                                Fx.heal(pl, 2);
                                if (i == 0) for (PotionEffectType b : bad) pl.removePotionEffect(b);
                                Fx.spawn(pl.getLocation().add(0, 1.2, 0), Particle.HEART, 2, 0.3, 0);
                            } else if (t instanceof Monster) {
                                t.damage(2, p);
                                Fx.spawn(t.getLocation().add(0, 1, 0), Particle.END_ROD, 8, 0.3, 0.1);
                            }
                        }
                    }
                });
                return true;
            }));
    }
}
