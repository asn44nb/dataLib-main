package runtoolkit.datalib.core;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataLibCore implements ModInitializer {

    public static final String MOD_ID = "datalib-core";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[DataLib] Core initialised.");

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("[DataLib] Server started — datapack engine ready.");
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("[DataLib] Server stopping.");
        });
    }
}
