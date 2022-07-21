package engineer.pol.polutilities.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

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



        for (ModifyItemOperation operation : ModifyItemOperation.values()) {
            LiteralArgumentBuilder<ServerCommandSource> operationBuilder = CommandManager.literal(operation.name().toLowerCase());
            for (ModifyItemParameter parameter : ModifyItemParameter.values()) {
                operationBuilder.then(CommandManager.argument(parameter.name().toLowerCase(), IdentifierArgumentType.identifier())
                        .then(CommandManager.argument("value", StringArgumentType.string())
                                .executes(context -> modifyItem(context, operation, parameter))));
            }
            lastArgument.then(operationBuilder);
        }

        literalBuilder
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(registryAccess))
                                .then(CommandManager.argument("nbt", StringArgumentType.string())
                                        .then(lastArgument))));

        dispatcher.register(literalBuilder);
    }

    private static int modifyItem(CommandContext<ServerCommandSource> context, ModifyItemOperation operation, ModifyItemParameter parameter) throws CommandSyntaxException {
        var item = ItemStackArgumentType.getItemStackArgument(context, "item");
        var nbt = StringArgumentType.getString(context, "nbt");
        var nbtValue = StringArgumentType.getString(context, "nbtvalue");
        var player = EntityArgumentType.getPlayer(context, "player");
        var value = StringArgumentType.getString(context, "value");

        for (ItemStack playerItem : getAllInventory(player)) {
            if (!item.test(playerItem)) continue;

            switch (parameter) {
                case NAME:
                    if(operation == ModifyItemOperation.ADD)
                        playerItem.setCustomName(Text.of(playerItem.getName()+value));
                    else if(operation == ModifyItemOperation.SET)
                        playerItem.setCustomName(Text.of(value));
                    break;
                case LORE:
                    playerItem.getOrCreateSubNbt("Lore"); // TODO
                    break;
                case NBT:
                    playerItem.getOrCreateNbt().putBoolean(value, true);
                    break;
                case CUSTOMMODELDATA:
                    playerItem.getOrCreateNbt().getCompound("tag").putInt(
                            "CustomModelData", Integer.parseInt(value));
                    break;
            }
        }

        return 1;
    }

    private static List<ItemStack> getAllInventory(ServerPlayerEntity player) {
        List<ItemStack> items = new ArrayList<>();
        items.addAll(player.getInventory().main);
        items.addAll(player.getInventory().armor);
        items.addAll(player.getInventory().offHand);

        return items;
    }

}
