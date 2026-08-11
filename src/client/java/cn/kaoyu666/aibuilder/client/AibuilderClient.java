package cn.kaoyu666.aibuilder.client;

import cn.kaoyu666.aibuilder.client.config.AIModelConfig;
import cn.kaoyu666.aibuilder.client.mode.BuilderMode;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;

public class AibuilderClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 初始化配置管理器
        AIModelConfig.init(FabricLoader.getInstance().getConfigDir());

        // 注册客户端命令 /aibuilder
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommands.literal("aibuilder")
                            .executes(context -> {
                                BuilderMode.getInstance().toggle();
                                return 1;
                            })
            );
        });
    }
}
