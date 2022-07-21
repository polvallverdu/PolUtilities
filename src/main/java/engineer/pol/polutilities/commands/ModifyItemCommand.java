package engineer.pol.polutilities.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.command.argument.NbtElementArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Text.Serializer;

import java.util.ArrayList;
import java.util.List;

public class ModifyItemCommand {

    public enum ModifyItemOperation {
        SET,
        ADD,
    }

    public enum ModifyItemParameter {
        NAME(false),
        LORE(true),
        NBT(true),
        CUSTOMMODELDATA(true),;

        private final boolean isNbtElement;

        ModifyItemParameter(boolean isNbtElement) {
            this.isNbtElement = isNbtElement;
        }

        public boolean isNbtElement() {
            return isNbtElement;
        }
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> literalBuilderModifyItem = CommandManager.literal("modifyitem").requires(source -> source.hasPermissionLevel(2));
        LiteralArgumentBuilder<ServerCommandSource> literalBuilderHasItem = CommandManager.literal("hasitem").requires(source -> source.hasPermissionLevel(2));

        var lastArgument = CommandManager.argument("nbtvalue", NbtElementArgumentType.nbtElement());

        for (ModifyItemOperation operation : ModifyItemOperation.values()) {
            LiteralArgumentBuilder<ServerCommandSource> operationBuilder = CommandManager.literal(operation.name().toLowerCase());
            for (ModifyItemParameter parameter : ModifyItemParameter.values()) {
                if (parameter.isNbtElement()) {
                    operationBuilder.then(CommandManager.literal(parameter.name().toLowerCase())
                            .then(CommandManager.argument("value", NbtElementArgumentType.nbtElement())
                                    .executes(context -> modifyItem(context, operation, parameter))));
                } else {
                    operationBuilder.then(CommandManager.literal(parameter.name().toLowerCase())
                            .then(CommandManager.argument("value", StringArgumentType.string())
                                    .executes(context -> modifyItem(context, operation, parameter))));
                }
            }
            lastArgument.then(operationBuilder);
        }

        literalBuilderModifyItem
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(registryAccess))
                                .then(CommandManager.argument("count", IntegerArgumentType.integer(0))
                                    .then(CommandManager.argument("nbt", StringArgumentType.string())
                                            .then(lastArgument)))));

        literalBuilderHasItem
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(registryAccess))
                                .then(CommandManager.argument("count", IntegerArgumentType.integer(0))
                                    .then(CommandManager.argument("nbt", StringArgumentType.string())
                                            .then(CommandManager.argument("nbtvalue", NbtElementArgumentType.nbtElement()).executes(ModifyItemCommand::hasItem))))));

        dispatcher.register(literalBuilderModifyItem);
        dispatcher.register(literalBuilderHasItem);
    }

    private static int modifyItem(CommandContext<ServerCommandSource> context, ModifyItemOperation operation, ModifyItemParameter parameter) throws CommandSyntaxException {
        var item = ItemStackArgumentType.getItemStackArgument(context, "item");
        var count = IntegerArgumentType.getInteger(context, "count");
        var nbt = StringArgumentType.getString(context, "nbt");
        var nbtValue = NbtElementArgumentType.getNbtElement(context, "nbtvalue");
        var player = EntityArgumentType.getPlayer(context, "player");
        var v = parameter.isNbtElement ? NbtElementArgumentType.getNbtElement(context, "value") : StringArgumentType.getString(context, "value");

        String valueString = v instanceof String ? (String) v : "";
        NbtElement valueNbt = v instanceof NbtElement ? (NbtElement) v : null;

        for (ItemStack playerItem : getAllInventory(player)) {
            if (isSimilar(item.createStack(count, false), playerItem, nbt, nbtValue)) continue;

            switch (parameter) {
                case NAME:
                    if (operation == ModifyItemOperation.ADD) {
                        playerItem.setCustomName(Text.of(playerItem.getName() + valueString));
                    } else if (operation == ModifyItemOperation.SET) {
                        playerItem.setCustomName(Text.of(valueString));
                    }
                    break;
                case LORE:
                    NbtCompound nbtCompound = playerItem.getOrCreateSubNbt("display");

                    if (operation == ModifyItemOperation.SET) {
                        nbtCompound.put("Lore", valueNbt);
                    } else if (operation == ModifyItemOperation.ADD) {
                        NbtList loreList = nbtCompound.getList("Lore",8);
                        if (valueNbt.getType() == 9) {
                            NbtList list = ((NbtList) valueNbt);
                            loreList.addAll(list);
                        } else {
                            loreList.add(valueNbt);
                        }
                    }
                    break;
                case NBT:
                    if (valueNbt.getType() != 10) break;
                    playerItem.getOrCreateNbt().copyFrom((NbtCompound) valueNbt);
                    break;
                case CUSTOMMODELDATA:
                    playerItem.getOrCreateNbt().getCompound("tag").putInt(
                            "CustomModelData", Integer.parseInt(valueString));
                    break;
            }
            return 1;
        }

        return 1;
    }

    private static int hasItem(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var item = ItemStackArgumentType.getItemStackArgument(context, "item");
        var count = IntegerArgumentType.getInteger(context, "count");
        var nbt = StringArgumentType.getString(context, "nbt");
        var nbtValue = NbtElementArgumentType.getNbtElement(context, "nbtvalue");
        var player = EntityArgumentType.getPlayer(context, "player");

        for (ItemStack playerItem : getAllInventory(player)) {
            if (isSimilar(item.createStack(count, false), playerItem, nbt, nbtValue)) {
                context.getSource().sendFeedback(Text.literal("true"), false);
                return 1;
            }
        }

        context.getSource().sendError(Text.literal("false"));
        return 1;
    }

    private static List<ItemStack> getAllInventory(ServerPlayerEntity player) {
        List<ItemStack> items = new ArrayList<>();
        items.addAll(player.getInventory().main);
        items.addAll(player.getInventory().armor);
        items.addAll(player.getInventory().offHand);

        return items;
    }

    private static boolean isSimilar(ItemStack ref, ItemStack item, String tag, NbtElement value) {
        return ref.getCount() == item.getCount() && hasSameMaterial(ref, item) && hasCustomModelData(ref, item) && !hasTag(item, tag, value) && hasSimilarEnchantments(ref, item);
    }

    private static boolean hasSimilarEnchantments(ItemStack ref, ItemStack item) {
        NbtList enchantmentsRef = ref.getEnchantments();
        NbtList enchantmentsItem = item.getEnchantments();
        if (enchantmentsRef.size() != enchantmentsItem.size()) {
            return false;
        }

        for (int i = 0; i < enchantmentsRef.size(); i++) {
            NbtCompound enchantmentRef = enchantmentsRef.getCompound(i);
            NbtCompound enchantmentItem = enchantmentsItem.getCompound(i);
            if (!enchantmentRef.getString("id").equals(enchantmentItem.getString("id"))) {
                return false;
            }
            if (enchantmentRef.getInt("lvl") != enchantmentItem.getInt("lvl")) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasSameMaterial(ItemStack ref, ItemStack item) {
        return ref.getRegistryEntry().getKey().equals(item.getRegistryEntry().getKey());
    }

    private static boolean hasCustomModelData(ItemStack ref, ItemStack item) {
        return ref.getOrCreateNbt().getCompound("tag").getInt("CustomModelData") == item.getOrCreateNbt().getCompound("tag").getInt("CustomModelData");
    }

    private static boolean hasTag(ItemStack item, String tag, NbtElement tagValue) {
        if (!item.getOrCreateNbt().getCompound("tag").contains(tag)) return false;

        return item.getOrCreateNbt().getCompound("tag").get(tag).equals(tagValue);
    }

}
