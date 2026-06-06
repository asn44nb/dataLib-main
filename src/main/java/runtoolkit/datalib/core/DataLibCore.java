package runtoolkit.datalib.core;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import runtoolkit.datalib.command.DataLibCommand;
import runtoolkit.datalib.config.DataLibConfig;
import runtoolkit.datalib.engine.DataLibEngine;

public class DataLibCore implements ModInitializer {

    public static final String MOD_ID = "datalib-core";
    public static final String NAMESPACE = "datalib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static DataLibCore instance;
    private DataLibConfig config;
    private DataLibEngine engine;

    public static DataLibCore getInstance() {
        return instance;
    }

    public DataLibConfig getConfig() {
        return config;
    }

    public DataLibEngine getEngine() {
        return engine;
    }

    @Override
    public void onInitialize() {
        instance = this;
        config = new DataLibConfig();
        config.load();
        engine = new DataLibEngine();

        LOGGER.info("[DataLib] Core initialised (v6.0.0 for 1.21.8).");

        DataLibCommand.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("[DataLib] Server started — datapack engine ready.");
            engine.onServerStarted(server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("[DataLib] Server stopping.");
            engine.onServerStopping(server);
        });
    }
}
