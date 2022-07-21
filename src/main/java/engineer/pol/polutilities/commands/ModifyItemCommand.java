package engineer.pol.polutilities.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class ModifyItemCommand {

    public enum ModifyItemOperation {
        SET,
        ADD,
    }

    public enum ModifyItemParameter {
        NAME,
        LORE,
        NBT,
        CUSTOMMODELDATA,
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> literalBuilder = CommandManager.literal("modifytime").requires(source -> source.hasPermissionLevel(2));

        var lastArgument = CommandManager.argument("nbtvalue", StringArgumentType.string());

        literalBuilder
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(registryAccess))
                                .then(CommandManager.argument("nbt", StringArgumentType.string())
                                        .then(lastArgument))));

        for (ModifyItemOperation operation : ModifyItemOperation.values()) {
            LiteralArgumentBuilder<ServerCommandSource> operationBuilder = CommandManager.literal(operation.name());
            for (ModifyItemParameter parameter : ModifyItemParameter.values()) {
                operationBuilder.then(CommandManager.argument(parameter.name(), IdentifierArgumentType.identifier())
                        .then(CommandManager.argument("value", StringArgumentType.string())
                                .executes(context -> modifyItem(context, operation, parameter))));
            }
            lastArgument.then(operationBuilder);
        }

        dispatcher.register(literalBuilder);
    }

    private static int modifyItem(CommandContext<ServerCommandSource> context, ModifyItemOperation operation, ModifyItemParameter parameter) throws CommandSyntaxException {
        var item = ItemStackArgumentType.getItemStackArgument(context, "item");
        var nbt = StringArgumentType.getString(context, "nbt");
        var nbtValue = StringArgumentType.getString(context, "nbtvalue");
        var player = EntityArgumentType.getPlayer(context, "player");
        var value = StringArgumentType.getString(context, "value");

        // TODO: Aqui va lo tuyo Nexxxxxxxxxxxxxxxxxxxxx

        return 1;
    }

}
