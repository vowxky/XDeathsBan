package vowxky.xdeathsban;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import vowxky.xdeathsban.command.XDeathsBanCommand;
import vowxky.xdeathsban.event.AfterDeathEvent;
import vowxky.xdeathsban.handler.JsonHandler;

public class Xdeathsban implements DedicatedServerModInitializer{

    @Override
    public void onInitializeServer() {
        JsonHandler.initialize();
        ServerLivingEntityEvents.AFTER_DEATH.register(new AfterDeathEvent());
        ServerTickEvents.END_SERVER_TICK.register(server -> {JsonHandler.ticksPlayersBan();});
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> XDeathsBanCommand.register(dispatcher));
    }
}
