package engineer.pol.polutilities.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.apache.logging.log4j.core.tools.picocli.CommandLine;

public class MotionCommand {

    public enum MotionOperation {
        SET,
        ADD,
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> literalBuilder = CommandManager.literal("motion").requires(source -> source.hasPermissionLevel(2));

        for (MotionOperation operation : MotionOperation.values()) {
            literalBuilder.then(CommandManager.literal(operation.name())
                    .then(CommandManager.argument("target", EntityArgumentType.entities())
                            .then(CommandManager.argument("x", DoubleArgumentType.doubleArg())
                                .then(CommandManager.argument("y", DoubleArgumentType.doubleArg())
                                    .then(CommandManager.argument("z", DoubleArgumentType.doubleArg())).executes(context -> motion(context, operation)))))
            );
        }

        dispatcher.register(literalBuilder);
    }

    private static int motion(CommandContext<ServerCommandSource> context, MotionOperation operation) throws CommandSyntaxException {
        var target = EntityArgumentType.getEntities(context, "target");
        var x = DoubleArgumentType.getDouble(context, "x");
        var y = DoubleArgumentType.getDouble(context, "y");
        var z = DoubleArgumentType.getDouble(context, "z");

        for (var entity : target) {
            switch (operation) {
                case SET -> entity.setVelocity(x, y, z);
                case ADD -> entity.setVelocity(entity.getVelocity().add(x, y, z));
            }
        }

        context.getSource().sendFeedback(Text.of("Velocity applied successfully"), false);

        return 1;
    }

}
