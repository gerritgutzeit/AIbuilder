package cn.kaoyu666.aibuilder;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Aibuilder implements ModInitializer {
    public static final String MOD_ID = "aibuilder";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("AIbuilder mod initialized");
    }
}
