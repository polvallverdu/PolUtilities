package engineer.pol.polutilities.utils;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.time.Duration;
import java.util.HashMap;

public class TaskManager {

    public static TaskManager INSTANCE = new TaskManager();

    private HashMap<Long, Runnable> tasks = new HashMap<>();

    private TaskManager() {
        ServerTickEvents.START_SERVER_TICK.register(this::tick);
    }

    private void tick(MinecraftServer server) {
        new HashMap<>(tasks).forEach((key, value) -> {
            if (System.currentTimeMillis() >= key) {
                value.run();
                tasks.remove(key);
            }
        });
    }

    public void createLaterTask(Runnable runnable, Duration delay) {
        tasks.put(System.currentTimeMillis() + delay.toMillis(), runnable);
    }

    public void createLaterTask(Object runnable, Duration ofMillis) {
    }

}
