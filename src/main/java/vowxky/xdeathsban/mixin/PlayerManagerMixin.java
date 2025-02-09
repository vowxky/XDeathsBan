package vowxky.xdeathsban.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vowxky.xdeathsban.handler.JsonHandler;

import java.net.SocketAddress;

/**
 * This class was created by Vowxky.
 * All rights reserved to the developer.
 */

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Inject(method = "checkCanJoin", at = @At("HEAD"), cancellable = true)
    private void canPlayerLogin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir) {
        String playerUUID = profile.getId().toString();

        if (!JsonHandler.isBanned(playerUUID)) {
            return;
        }

        if (!JsonHandler.isBanTemporary()) {
            cir.setReturnValue(Text.of("§cYou have been permanently banned for exceeding the death limit."));
            return;
        }

        long remainingSeconds = JsonHandler.getRemainingBanTicks(playerUUID) / 20;

        cir.setReturnValue(Text.of(String.format(
                "§cYou are temporarily banned. Remaining time: %02d:%02d:%02d",
                remainingSeconds / 3600, (remainingSeconds % 3600) / 60, remainingSeconds % 60
        )));
    }
}