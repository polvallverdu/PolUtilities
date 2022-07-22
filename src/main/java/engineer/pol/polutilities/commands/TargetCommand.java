package engineer.pol.polutilities.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.atomic.AtomicInteger;

public class TargetCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> literalBuilder = CommandManager.literal("target").requires(source -> source.hasPermissionLevel(2));

        literalBuilder
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("range", IntegerArgumentType.integer(0))
                                .then(CommandManager.argument("target", EntityArgumentType.entity())
                                        .executes(TargetCommand::setTarget))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("range", IntegerArgumentType.integer(0))
                                .executes(TargetCommand::removeTarget)));

        dispatcher.register(literalBuilder);
    }

    private static int setTarget(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var range = IntegerArgumentType.getInteger(context, "range");
        var target = EntityArgumentType.getEntity(context, "target");

        AtomicInteger count = new AtomicInteger();

        if (!(target instanceof LivingEntity)) {
            context.getSource().sendError(Text.of("Target must be a living entity"));
            return count.get();
        }

        Vec3d sourcePos = context.getSource().getPosition();
        Box box = new Box(sourcePos.add(-range, -range, -range), sourcePos.add(range, range, range));
        context.getSource().getWorld().getOtherEntities(target, box, (e) -> e instanceof PathAwareEntity).forEach(e -> {
            ((PathAwareEntity) e).setTarget((LivingEntity) target);
            count.getAndIncrement();
        });

        context.getSource().sendFeedback(Text.of("Updated target on " + count.get() + " entities."), false);
        return count.get();
    }

    private static int removeTarget(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var range = IntegerArgumentType.getInteger(context, "range");

        AtomicInteger count = new AtomicInteger();

        Vec3d sourcePos = context.getSource().getPosition();
        Box box = new Box(sourcePos.add(-range, -range, -range), sourcePos.add(range, range, range));
        context.getSource().getWorld().getOtherEntities(null, box, (e) -> e instanceof PathAwareEntity).forEach(e -> {
            ((PathAwareEntity) e).setTarget(null);
            count.getAndIncrement();
        });

        context.getSource().sendFeedback(Text.of("Target removed successfully from " + count.get() + " entities."), false);
        return count.get();
    }
}
