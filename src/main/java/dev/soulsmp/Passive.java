package dev.soulsmp;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class Passive {
    public final String name, desc;
    public Consumer<Player> onApply = p -> {}, onRemove = p -> {}, onTick = p -> {};
    public BiConsumer<Player, LivingEntity> onHurt = null;
    public final Set<DamageCause> immune = EnumSet.noneOf(DamageCause.class);

    public Passive(String name, String desc) { this.name = name; this.desc = desc; }
    public Passive apply(Consumer<Player> c) { onApply = c; return this; }
    public Passive remove(Consumer<Player> c) { onRemove = c; return this; }
    public Passive tick(Consumer<Player> c) { onTick = c; return this; }
    public Passive hurt(BiConsumer<Player, LivingEntity> c) { onHurt = c; return this; }
    public Passive immune(DamageCause... causes) { immune.addAll(java.util.List.of(causes)); return this; }
}
