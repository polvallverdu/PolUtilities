package engineer.pol.polutilities.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.core.tools.picocli.CommandLine;

public class MotionCommand {

    public enum MotionOperation {
        SET,
        ADD,
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> literalBuilder = CommandManager.literal("motion").requires(source -> source.hasPermissionLevel(2));

        for (MotionOperation operation : MotionOperation.values()) {
            literalBuilder.then(CommandManager.literal(operation.name().toLowerCase())
                    .then(CommandManager.argument("target", EntityArgumentType.entities())
                            .then(CommandManager.argument("vel", Vec3ArgumentType.vec3(false)).executes(context -> motion(context, operation))))
            );
        }

        dispatcher.register(literalBuilder);
    }

    private static int motion(CommandContext<ServerCommandSource> context, MotionOperation operation) throws CommandSyntaxException {
        var target = EntityArgumentType.getEntities(context, "target");
        var vel = Vec3ArgumentType.getVec3(context, "vel");

        for (var entity : target) {
            switch (operation) {
                case SET -> setVelocity(entity, vel);
                case ADD -> setVelocity(entity, entity.getVelocity().add(vel));
            }
        }

        context.getSource().sendFeedback(Text.of("Velocity applied successfully"), false);

        return 1;
    }

    private static void setVelocity(Entity entity, Vec3d vec) {
        entity.setVelocity(vec);
        entity.velocityModified = true;
    }

}
