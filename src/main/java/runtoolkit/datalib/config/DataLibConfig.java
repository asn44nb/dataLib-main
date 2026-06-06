package runtoolkit.datalib.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import runtoolkit.datalib.core.DataLibCore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class DataLibConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;
    private final Map<String, String> values = new LinkedHashMap<>();

    public DataLibConfig() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve("datalib.json");
        // defaults
        values.put("auto_reload", "true");
        values.put("debug_mode", "false");
        values.put("engine_tick_interval", "20");
        values.put("show_notifications", "true");
        values.put("datapack_output_dir", "datapacks");
    }

    public void load() {
        if (!Files.exists(configPath)) {
            save();
            return;
        }
        try {
            String json = Files.readString(configPath);
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            if (obj != null) {
                obj.entrySet().forEach(e -> values.put(e.getKey(), e.getValue().getAsString()));
            }
            DataLibCore.LOGGER.info("[DataLib] Config loaded.");
        } catch (IOException e) {
            DataLibCore.LOGGER.error("[DataLib] Failed to load config", e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(values));
            DataLibCore.LOGGER.info("[DataLib] Config saved.");
        } catch (IOException e) {
            DataLibCore.LOGGER.error("[DataLib] Failed to save config", e);
        }
    }

    public String get(String key) {
        return values.getOrDefault(key, null);
    }

    public boolean set(String key, String value) {
        if (!values.containsKey(key)) {
            return false;
        }
        values.put(key, value);
        save();
        return true;
    }

    public Map<String, String> getAll() {
        return new LinkedHashMap<>(values);
    }

    public boolean hasKey(String key) {
        return values.containsKey(key);
    }
}
