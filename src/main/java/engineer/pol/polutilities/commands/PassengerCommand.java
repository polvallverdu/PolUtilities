package engineer.pol.polutilities.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class PassengerCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> literalBuilder = CommandManager.literal("passenger").requires(source -> source.hasPermissionLevel(2));

        literalBuilder.then(CommandManager.argument("entity", EntityArgumentType.entity())
            .then(CommandManager.argument("target", EntityArgumentType.entity())
        .executes(commandContext -> passenger(commandContext))));

        dispatcher.register(literalBuilder);
    }

    private static int passenger(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var entity = EntityArgumentType.getEntity(context, "entity");
        var target = EntityArgumentType.getEntity(context, "target");
        var source = context.getSource();
        
        try {
            if(target.hasPassengers()) target.removeAllPassengers();
            if(entity.hasPassengers()) target.removeAllPassengers();
            entity.startRiding(target, true);
            
            if(target instanceof ServerPlayerEntity player)
                player.networkHandler.sendPacket(new EntityPassengersSetS2CPacket(entity));
        } catch (Exception e) {
            source.sendError(Text.of("Error: " + e.getMessage()));
            return 0;
        }

        source.sendFeedback(Text.of("Passenger applied successfully"), false);
        return 1;
    }
}
