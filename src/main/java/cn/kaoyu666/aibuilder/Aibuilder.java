package cn.kaoyu666.aibuilder;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Aibuilder implements ModInitializer {
    public static final String MOD_ID = "aibuilder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("AIbuilder mod initialized");
    }
}
