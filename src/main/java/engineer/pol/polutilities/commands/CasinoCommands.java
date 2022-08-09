package engineer.pol.polutilities.commands;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import engineer.pol.polutilities.PolUtilities;
import engineer.pol.polutilities.utils.RandomUtils;
import engineer.pol.polutilities.utils.TaskManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.registry.Registry;

public class CasinoCommands {

    public enum CasinoMachines {
        SLOTS_MACHINE(false);

        private final boolean requireEntity;

        CasinoMachines(boolean requireEntity) {
            this.requireEntity = requireEntity;
        }

        public boolean requireEntity() {
            return requireEntity;
        }
    }

    public enum RouletteAnimation {
        DELITA("spin_dedita", "idle_dedita"),
        EON("spin_eon", "idle_eon"),
        REVIIL("spin_revil", "idle_revil"),
        OTTER("spin_otter", "idle_otter"),;

        private final String spin;
        private final String idle;

        RouletteAnimation(String spin, String idle) {
            this.spin = spin;
            this.idle = idle;
        }

        public String getSpin() {
            return spin;
        }

        public String getIdle() {
            return idle;
        }

        public static RouletteAnimation getFromResult(int res) {
            if (res <= 15) {
                return DELITA;
            } else if (res <= 40) {
                return EON;
            } else if (res <= 70) {
                return REVIIL;
            } else {
                return OTTER;
            }
        }

        public static RouletteAnimation getRandomAnimation() {
            return getFromResult(RandomUtils.getRandomInt(1, 100));
        }
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> literalBuilder = CommandManager.literal("casino_commands").requires(source -> source.hasPermissionLevel(2));

        for(CasinoMachines machine : CasinoMachines.values()) {
            if(machine.requireEntity())
                literalBuilder.then(CommandManager.literal(machine.toString().toLowerCase())
                    .then(CommandManager.argument("entity", EntityArgumentType.entity())
                        .executes(commandContext -> casino(commandContext, machine))));
            else literalBuilder.then(CommandManager.literal(machine.toString().toLowerCase())
                    .executes(commandContext -> casino(commandContext, machine)));
        
        }

        dispatcher.register(literalBuilder);
    }

    private static int casino(CommandContext<ServerCommandSource> context, CasinoMachines machine) throws CommandSyntaxException {
        var entity = EntityArgumentType.getEntity(context, "entity");
        var source = context.getSource();

        RouletteAnimation[] rouletteResults = new RouletteAnimation[]{
                RouletteAnimation.getRandomAnimation(),
                RouletteAnimation.getRandomAnimation(),
                RouletteAnimation.getRandomAnimation()
        };

        try {
            switch(machine){
                case SLOTS_MACHINE:
                    runCommand("tag " + entity.getUuidAsString() + " add running");
                    runCommand("playanimation " + entity.getUuidAsString() + " lever false");
                    for (int i = 0; i < rouletteResults.length; i++) {
                        String cmdBase = "execute at " + entity.getUuidAsString() + " run execute as @e[distance=..2,limit=3,sort=nearest,type=entitycreator:slots_spin,tag=spin_" + i+1 + "] at @s run playanimation @s ";

                        runCommand(cmdBase + rouletteResults[i].getSpin());
                        int tempI = i;
                        TaskManager.INSTANCE.createLaterTask(
                                () -> runCommand(cmdBase + rouletteResults[tempI].getIdle()),
                                Duration.ofMillis((100/20)*1000)
                        );
                    }
                    TaskManager.INSTANCE.createLaterTask(() -> {
                                runCommand("tag " + entity.getUuidAsString() + " remove running");
                                runCommand("playanimation " + entity.getUuidAsString() + " idle false");
                                if (rouletteResults[0] == rouletteResults[1] && rouletteResults[1] == rouletteResults[2]) {
                                    RouletteAnimation result = rouletteResults[0];

                                    ItemStack itemReward = getReward(result);
                                    if (itemReward == null) return;

                                    /*float pitch = entity.getPitch();
                                    float yaw = entity.getYaw();*/

                                    // get location in front of entity
                                    /*double x = entity.getX() + (Math.cos(Math.toRadians(yaw)) * 1.5);
                                    double y = entity.getY() + 1.5;
                                    double z = entity.getZ() + (Math.sin(Math.toRadians(yaw)) * 1.5);*/

                                    // spawn item
                                    String command = "execute at " + entity.getUuidAsString() + " run summon minecraft:item ^ ^1.5 ^1.5 {Item:{id:\"" + Registry.ITEM.getId(itemReward.getItem()).toString() + "\",Count:" + itemReward.getCount() + ",tag:{" + itemReward.getOrCreateNbt().toString() + "}}}";
                                    runCommand(command);
                                }
                            },
                            Duration.ofMillis((120/20)*1000)
                    );

                break;
            }
        } catch (Exception e) {
            source.sendError(Text.of("Error: " + e.getMessage()));
            return 0;
        }
        return 1;
    }

    private static void runCommand(String command) {
        PolUtilities.SERVER.getCommandManager().execute(PolUtilities.SERVER.getCommandSource(), command);
    }

    private static ItemStack getReward(RouletteAnimation result) {
        switch (result) {

        }
        return null;
    }
}
