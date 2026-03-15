package hu.jgj52.huTiersPlaceholders;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import hu.jgj52.database.PostgreSQL;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import static hu.jgj52.database.Database.postgres;
import static hu.jgj52.huTiersPlaceholders.HuTiersPlaceholders.plugin;

public class Placeholder extends PlaceholderExpansion {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();
    @Override
    public @NotNull String getIdentifier() {
        return "hutiers";
    }

    @Override
    public @NotNull String getAuthor() {
        return "JGJ52";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        return getPlayer(player).getOrDefault(params, "");
    }

    private List<Map<String, Object>> rows = new ArrayList<>();
    private final Map<UUID, Map<String, String>> cache = new HashMap<>();
    private boolean update = true;

    public void update() {
        Bukkit.getScheduler().runTask(plugin, () -> update = true);
    }

    public Placeholder() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (update) {
                update = false;
                postgres.from("players").execute().thenAccept(result -> {
                    if (!result.hasError()) {
                        Bukkit.getScheduler().runTask(plugin, () -> rows = List.copyOf(result.data));
                        new Thread(() -> {
                            try {
                                HttpRequest request = HttpRequest.newBuilder().uri(new URI("https://api.hutiers.hu/v2/overall/0/-1")).GET().build();
                                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                                JsonArray arr = gson.fromJson(response.body(), JsonArray.class);
                                Map<String, Map<String, Object>> rows = new HashMap<>();
                                for (Map<String, Object> row : result.data) {
                                    rows.put(row.get("uuid").toString(), row);
                                }
                                for (JsonElement element : arr) {
                                    JsonObject player = element.getAsJsonObject();
                                    String place = player.get("place").getAsString();
                                    String points = player.get("points").getAsString();
                                    Map<String, Object> c = rows.get(player.get("uuid").getAsString());
                                    UUID uuid = UUID.fromString(player.get("uuid").getAsString());
                                    if (c != null && !place.equals(Objects.toString(c.get("place"), "")) && !points.equals(Objects.toString(c.get("points"), ""))) {
                                        Map<String, Object> data = new HashMap<>();
                                        data.put("place", place);
                                        data.put("points", points);
                                        if (!place.isEmpty()) data.put("place_formatted", "&7Helyezés: &f#" + place);
                                        if (!points.isEmpty()) data.put("points_formatted", "&7Pontok: &f" + points);
                                        postgres.from("players").eq("uuid", uuid).update(data).thenAccept(r -> Bukkit.getScheduler().runTask(plugin, this::update));
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                        cache.clear();
                    }
                });
            }
        }, 0L, 20L);
    }

    private Map<String, String> getPlayer(Player player) {
        UUID u = player.getUniqueId();
        Map<String, String> cached = cache.get(u);
        if (cached != null) return cached;
        for (Map<String, Object> row : rows) {
            UUID uuid = UUID.fromString(row.get("uuid").toString());
            if (uuid.equals(u)) {
                Map<String, String> data = new HashMap<>();
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    if (!entry.getKey().equals("uuid")) {
                        data.put(entry.getKey(), entry.getValue().toString());
                    }
                }
                cache.put(u, data);
                return data;
            }
        }
        return Map.of();
    }
}
