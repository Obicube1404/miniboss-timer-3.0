package fr.minibosstimer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Point d'entrée du mod (côté client uniquement). */
public class MiniBossTimerMod implements ClientModInitializer {
    public static final String MOD_ID = "miniboss_timer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        Config.load();
        TimerManager.load();
        KillDetector.register();
        NavigationTracker.register();
        TimerHud.register();
        BossCommands.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            TimerManager.tick();
            NavigationTracker.tick();
        });
        LOGGER.info("MiniBoss Timer charge.");
    }
}
