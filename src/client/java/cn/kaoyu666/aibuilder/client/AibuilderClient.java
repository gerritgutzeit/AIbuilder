package cn.kaoyu666.aibuilder.client;

import cn.kaoyu666.aibuilder.client.config.AIModelConfig;
import cn.kaoyu666.aibuilder.client.mode.BuilderMode;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.loader.api.FabricLoader;

public class AibuilderClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 初始化配置管理器
        AIModelConfig.init(FabricLoader.getInstance().getConfigDir());

        // 注册客户端命令 /aibuilder（1.16.x 使用 v1 DISPATCHER）
        ClientCommandManager.DISPATCHER.register(
                ClientCommandManager.literal("aibuilder")
                        .executes(context -> {
                            BuilderMode.getInstance().toggle();
                            return 1;
                        })
        );
    }
}
