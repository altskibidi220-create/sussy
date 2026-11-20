package dev.soulsmp;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SoulPlugin extends JavaPlugin {
    private static SoulPlugin instance;
    private SoulManager manager;
    private NamespacedKey itemKey;

    public static SoulPlugin get() { return instance; }
    public NamespacedKey itemKey() { return itemKey; }
    public SoulManager manager() { return manager; }

    @Override public void onEnable() {
        instance = this;
        saveDefaultConfig();
        itemKey = new NamespacedKey(this, "soul_item");
        manager = new SoulManager(this);
        Bukkit.getPluginManager().registerEvents(new SoulListener(this, manager), this);
        Bukkit.getPluginManager().registerEvents(new SoulGui(manager), this);
        PluginCommand cmd = getCommand("soul");
        if (cmd != null) {
            SoulCommand sc = new SoulCommand(manager);
            cmd.setExecutor(sc);
            cmd.setTabCompleter(sc);
        }
        manager.start();
        // players already online (e.g. /reload)
        for (var p : Bukkit.getOnlinePlayers()) {
            Soul s = manager.getSoul(p);
            if (s != null) manager.setSoul(p, s, false);
        }
        getLogger().info("SoulSMP enabled with " + SoulRegistry.all().size() + " souls.");
    }

    @Override public void onDisable() {
        if (manager != null) manager.shutdown();
    }
}
