package engineer.pol.polutilities;

import engineer.pol.polutilities.commands.ModifyItemCommand;
import engineer.pol.polutilities.commands.MotionCommand;
import engineer.pol.polutilities.commands.TargetCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class PolUtilities implements ModInitializer {
    @Override
    public void onInitialize() {
        registerCommands();
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MotionCommand.register(dispatcher, registryAccess, environment);
            ModifyItemCommand.register(dispatcher, registryAccess, environment);
            TargetCommand.register(dispatcher, registryAccess, environment);
        });
    }
}
