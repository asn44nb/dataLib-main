package runtoolkit.datalib.engine;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import runtoolkit.datalib.core.DataLibCore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * DataLib Engine: Java-side manager for the embedded datapack framework.
 *
 * The actual datapack lives inside the JAR (injected by build.sh from
 * github.com/runtoolkit/dataLib) and is loaded by Fabric as a built-in
 * resource pack. This class does NOT duplicate or replace any mcfunction
 * logic — it only:
 *   1. Tracks whether the datapack engine has loaded (reads
 *      datalib:engine global{loaded:1b} state set by dl_load).
 *   2. Manages user-created datapacks generated via the GUI.
 *   3. Provides /datalib reload — which reloads ONLY dataLib config,
 *      never triggers /reload or other datapacks.
 *
 * Load chain (mcfunction side, for reference):
 *   minecraft:load → datalib.main:datalib/load
 *     → #load:_private/load (Lantern Load)
 *       → datalib:load → dl_load:_ (stage 0, marker entity)
 *         → dl_load:load/confirm → gate (admin yes/no)
 *           → dl_load:load/all → scoreboards, storages, …
 *             → data modify storage datalib:engine global.loaded set value 1b
 *             → dl_load:core/internal/load/finalize
 *
 * check_all (mcfunction side):
 *   datalib:debug/tools/utils/check_all
 *     → load_check, perm_check, input_check
 */
public class DataLibEngine {

    private boolean loaded = false;
    private MinecraftServer server;
    private final List<String> managedPacks = new ArrayList<>();

    public boolean isLoaded() {
        return loaded;
    }

    public int getManagedPackCount() {
        return managedPacks.size();
    }

    public List<String> getManagedPacks() {
        return new ArrayList<>(managedPacks);
    }

    public void onServerStarted(MinecraftServer server) {
        this.server = server;
        this.loaded = true;

        // Discover existing managed packs in the world datapacks directory
        discoverManagedPacks();

        DataLibCore.LOGGER.info("[DataLib] Engine loaded. {} managed pack(s).", managedPacks.size());
    }

    public void onServerStopping(MinecraftServer server) {
        this.loaded = false;
        this.server = null;
        DataLibCore.LOGGER.info("[DataLib] Engine unloaded.");
    }

    /**
     * Reload only dataLib mod config + rediscover managed packs.
     * This does NOT call /reload or re-trigger the mcfunction load chain.
     * The datapack side (dl_load gate) is intentionally NOT re-triggered,
     * because dl_load:load/validate guards against double-load
     * (global{loaded:1b} → "Already loaded — skipping reload").
     *
     * To fully re-initialise the engine, the player should:
     *   1. /function datalib:disable  (clears global)
     *   2. /reload                    (triggers the gate again)
     */
    public void reload(MinecraftServer server) {
        this.server = server;
        DataLibCore.getInstance().getConfig().load();
        discoverManagedPacks();
        DataLibCore.LOGGER.info("[DataLib] Mod config reloaded. {} managed pack(s).", managedPacks.size());
    }

    /**
     * Runs the mcfunction check_all via the server command dispatcher.
     * This invokes datalib:debug/tools/utils/check_all which validates:
     *   - load_check  (engine loaded?)
     *   - perm_check  (security module OK?)
     *   - input_check (I/O storages exist?)
     */
    public void runCheckAll() {
        if (server == null) return;

        server.getCommandManager().executeWithPrefix(
                server.getCommandSource().withSilent(),
                "function datalib:debug/tools/utils/check_all"
        );

        DataLibCore.LOGGER.info("[DataLib] check_all executed.");
    }

    private void discoverManagedPacks() {
        managedPacks.clear();
        if (server == null) return;

        Path datapacksDir = server.getSavePath(WorldSavePath.DATAPACKS);
        if (!Files.exists(datapacksDir)) return;

        try (var stream = Files.list(datapacksDir)) {
            stream.filter(Files::isDirectory).forEach(dir -> {
                Path marker = dir.resolve(".datalib_managed");
                if (Files.exists(marker)) {
                    managedPacks.add(dir.getFileName().toString());
                }
            });
        } catch (IOException e) {
            DataLibCore.LOGGER.error("[DataLib] Failed to discover managed packs", e);
        }
    }

    public MinecraftServer getServer() {
        return server;
    }
}
