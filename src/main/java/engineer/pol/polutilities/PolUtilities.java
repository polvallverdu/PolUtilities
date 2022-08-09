package engineer.pol.polutilities;

import engineer.pol.polutilities.commands.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public class PolUtilities implements ModInitializer {

    public static MinecraftServer SERVER = null;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> SERVER = server);

        registerCommands();
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MotionCommand.register(dispatcher, registryAccess, environment);
            ModifyItemCommand.register(dispatcher, registryAccess, environment);
            TargetCommand.register(dispatcher, registryAccess, environment);

            CasinoCommands.register(dispatcher, registryAccess, environment);
            PassengerCommand.register(dispatcher, registryAccess, environment);
            SoulsCommand.register(dispatcher, registryAccess, environment);
        });
    }
}
