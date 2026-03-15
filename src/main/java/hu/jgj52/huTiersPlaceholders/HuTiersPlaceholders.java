package hu.jgj52.huTiersPlaceholders;

import hu.jgj52.huTiers.PlayerChangeEvent;
import hu.jgj52.huTiersMessenger.Messenger;
import org.bukkit.plugin.java.JavaPlugin;

public final class HuTiersPlaceholders extends JavaPlugin {
    public static HuTiersPlaceholders plugin;
    public static Placeholder placeholder;

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        placeholder = new Placeholder();
        placeholder.register();
        Messenger.listen("placeholders", message -> placeholder.update());
        getServer().getPluginManager().registerEvents(new JoinListener(), this);
        PlayerChangeEvent.register((player, hplayer) -> {
            if (player.getPlayer() != null) placeholder.update();
        });
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
