package gg.uhc.unendurablehealing;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Entry extends JavaPlugin {

    protected UnendurableHealing plugin;

    public void onEnable() {
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();

        double percent = config.getDouble("percent max to remove");

        double multiplier = (percent / 100D);

        if (null == plugin) {
            plugin = new UnendurableHealing();
            Bukkit.getPluginManager().registerEvents(plugin, this);
        }

        plugin.setMultiplier(multiplier);
    }
}
