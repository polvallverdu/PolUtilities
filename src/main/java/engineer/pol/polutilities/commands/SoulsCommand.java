package engineer.pol.polutilities.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class SoulsCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> literalBuilder = CommandManager.literal("soul").requires(source -> source.hasPermissionLevel(2));

        literalBuilder.then(CommandManager.argument("player", EntityArgumentType.player())
            .then(CommandManager.argument("dimension", StringArgumentType.string())
                .executes(commandContext -> souls(commandContext))));

        dispatcher.register(literalBuilder);
    }

    private static int souls(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var entity = EntityArgumentType.getPlayer(context, "player");
        var dimension = StringArgumentType.getString(context, "dimension");
        var source = context.getSource();
        
        try {
            if(dimension.toString().equals("heaven")){
                ItemStack item = entity.getOffHandStack();
                Identifier identifier = Registry.ITEM.getId(item.getItem());
    
                if(identifier.getPath().equals("flower_collector")) {
                    int souls = item.getOrCreateNbt().getInt("souls");
                    int maxSouls = item.getOrCreateNbt().getInt("maxsouls");
                    if(souls < 0 || souls >= maxSouls) return 0;

                    entity.getWorld().spawnParticles(ParticleTypes.SOUL, entity.getPos().x, 
                        entity.getPos().y, entity.getPos().z, 10, 0.5, 1, 0.5, 0);
                    entity.getWorld().spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getPos().x, 
                        entity.getPos().y, entity.getPos().z, 10, 0.5, 1, 0.5, 0);

                    item.getOrCreateNbt().putInt("souls", souls + 1);
                    item.setCustomName(Text.of("§fRecolector de Almas: " + (souls+1) + "㍰ ≣ " + maxSouls));
                    return 1;
                }
            }

            for(ItemStack item : entity.getArmorItems()) {
                Identifier identifier = Registry.ITEM.getId(item.getItem());
                if(identifier.getPath().equals("soul_collector_chestplate")) {
                    int souls = item.getOrCreateNbt().getInt("souls_"+dimension);
                    if(souls < 0 || souls >= 64) return 0;

                    entity.getWorld().spawnParticles(ParticleTypes.SOUL, entity.getPos().x, 
                        entity.getPos().y, entity.getPos().z, 10, 0.5, 1, 0.5, 0);
                    entity.getWorld().spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getPos().x, 
                        entity.getPos().y, entity.getPos().z, 10, 0.5, 1, 0.5, 0);

                    item.getOrCreateNbt().putInt("souls_"+dimension, souls + 1);
                    item.setCustomName(Text.of("§fRecolector de Almas: " + "§4" + 
                        item.getOrCreateNbt().getInt("souls_nether") + "§f㍨ ║§2" +
                        item.getOrCreateNbt().getInt("souls_overworld") + "§f㍩ ║§9" +
                        item.getOrCreateNbt().getInt("souls_heaven") + "§f㍰ "));
                    return 1;
                }
            }

            entity.currentScreenHandler.sendContentUpdates();
            entity.playerScreenHandler.onContentChanged(entity.getInventory());
        } catch (Exception e) {
            source.sendError(Text.of("Error: " + e.getMessage()));
            return 0;
        }
        return 1;
    }
}
