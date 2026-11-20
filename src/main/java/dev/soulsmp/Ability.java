package dev.soulsmp;

import org.bukkit.entity.Player;

/** One castable ability. Action returns true if it was actually used (cooldown starts). */
public record Ability(String name, String desc, int cooldown, Action action) {
    @FunctionalInterface
    public interface Action { boolean run(Player p); }
}
