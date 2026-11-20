package dev.soulsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SoulCommand implements TabExecutor {
    private final SoulManager m;
    public SoulCommand(SoulManager m) { this.m = m; }

    private static void msg(CommandSender s, String text, NamedTextColor c) { s.sendMessage(Component.text(text, c)); }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {
        if (!sender.hasPermission("soulsmp.admin")) { msg(sender, "No permission.", NamedTextColor.RED); return true; }
        if (a.length == 0) {
            if (sender instanceof Player p) SoulGui.open(p); else msg(sender, "Usage: /soul <set|give|clear|roll|cooldown> ...", NamedTextColor.GRAY);
            return true;
        }
        String sub = a[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) {
            SoulPlugin.get().reloadConfig();
            msg(sender, "SoulSMP config reloaded (cooldowns updated).", NamedTextColor.GREEN);
            return true;
        }
        Player target = a.length >= 2 ? Bukkit.getPlayerExact(a[1]) : (sender instanceof Player p ? p : null);
        if (target == null) { msg(sender, "Player not found.", NamedTextColor.RED); return true; }
        switch (sub) {
            case "set", "give" -> {
                Soul s = a.length >= 3 ? SoulRegistry.get(a[2]) : null;
                if (s == null) { msg(sender, "Usage: /soul " + sub + " <player> <soul>", NamedTextColor.RED); return true; }
                if (sub.equals("set")) m.setSoul(target, s, true); else target.getInventory().addItem(s.item());
                msg(sender, "Done: " + s.name + " -> " + target.getName(), NamedTextColor.GREEN);
            }
            case "clear" -> { m.setSoul(target, null, false); msg(sender, "Cleared " + target.getName() + "'s soul.", NamedTextColor.GREEN); }
            case "roll" -> { m.startRoulette(target, null); msg(sender, "Rolling for " + target.getName() + "...", NamedTextColor.GREEN); }
            case "cooldown" -> { m.resetCooldowns(target); msg(sender, "Cooldowns reset.", NamedTextColor.GREEN); }
            default -> msg(sender, "Usage: /soul [set|give|clear|roll|cooldown] <player> [soul]", NamedTextColor.RED);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) out.addAll(List.of("set", "give", "clear", "roll", "cooldown", "reload"));
        else if (a.length == 2) Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        else if (a.length == 3 && (a[0].equalsIgnoreCase("set") || a[0].equalsIgnoreCase("give")))
            SoulRegistry.all().forEach(s -> out.add(s.id));
        String last = a[a.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
