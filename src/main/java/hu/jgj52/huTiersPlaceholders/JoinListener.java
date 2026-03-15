package hu.jgj52.huTiersPlaceholders;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;

import static hu.jgj52.database.Database.postgres;
import static hu.jgj52.huTiersPlaceholders.HuTiersPlaceholders.placeholder;

public class JoinListener implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        postgres.from("players").eq("uuid", event.getPlayer().getUniqueId()).execute().thenAccept(r -> {
            if (r.isEmpty()) {
                Map<String, Object> data = new HashMap<>();
                data.put("uuid", event.getPlayer().getUniqueId());
                postgres.from("players").insert(data);
            }
        });
        placeholder.update();
    }
}
