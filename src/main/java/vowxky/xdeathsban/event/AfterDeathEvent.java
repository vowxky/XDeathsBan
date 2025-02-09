package vowxky.xdeathsban.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import vowxky.xdeathsban.handler.JsonHandler;

/**
 * This class was created by Vowxky.
 * All rights reserved to the developer.
 */

public class AfterDeathEvent implements ServerLivingEntityEvents.AfterDeath {
    @Override
    public void afterDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        if (JsonHandler.getOnlyPvPDeaths() && !(source.getAttacker() instanceof PlayerEntity)) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        String playerUUID = player.getUuidAsString();
        String playerName = player.getName().getString();
        long serverTicks = server.getTicks();

        if (JsonHandler.registerDeath(playerUUID, serverTicks, playerName)) {
            player.networkHandler.disconnect(Text.of("§cYou have been banned from the server"));
        }
    }
}
