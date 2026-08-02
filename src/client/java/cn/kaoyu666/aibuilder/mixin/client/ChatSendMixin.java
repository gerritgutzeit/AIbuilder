package cn.kaoyu666.aibuilder.mixin.client;

import cn.kaoyu666.aibuilder.client.mode.BuilderMode;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 拦截聊天消息发送，在 AI Builder 模式下拦截消息发送给 AI
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ChatSendMixin {

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String content, CallbackInfo ci) {
        // 如果消息以 / 开头，视为命令，不拦截
        if (content.startsWith("/")) return;

        if (BuilderMode.getInstance().isActive()) {
            boolean handled = BuilderMode.getInstance().handleChatMessage(content);
            if (handled) {
                ci.cancel(); // 阻止消息发送到服务器
            }
        }
    }
}
