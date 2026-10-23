package fr.minibosstimer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Détecte la mort d'un mini-boss :
 *  1) via l'entité qui disparaît du monde alors qu'elle est morte (méthode principale) ;
 *  2) via un message de chat annonçant la mort (optionnel, voir chatKillPatterns dans la config).
 *
 * Le timer démarre que ce soit toi ou un autre joueur qui ait tué le boss : le respawn est le même.
 */
public final class KillDetector {
    private KillDetector() {}

    public static void register() {
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> onUnload(entity));
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) onChat(message.getString());
        });
    }

    private static void onUnload(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (entity instanceof Player) return;
        if (!(entity instanceof LivingEntity)) return;

        LivingEntity living = (LivingEntity) entity;
        if (!living.isDeadOrDying()) return;

        String name = Config.cleanName(entity.getName().getString());
        float maxHp = living.getMaxHealth();
        boolean named = entity.hasCustomName();
        double dist = entity.distanceTo(mc.player);

        if (Config.data.debug && (named || maxHp >= 50.0F)) {
            Msg.chat(Msg.t("[debug] mort : \"" + name + "\" | PV max " + Math.round(maxHp)
                    + " | nom perso : " + named + " | " + Math.round(dist) + " blocs",
                    ChatFormatting.DARK_GRAY));
        }

        if (!Config.data.autoDetectKills) return;
        if (dist > Config.data.maxKillDistance) return;
        if (!Config.isMiniBoss(name, maxHp, named)) return;
        handleKill(name, ServerResolver.current(), false);
    }

    private static void onChat(String text) {
        if (!Config.data.autoDetectKills) return;
        for (Pattern p : Config.killPatterns()) {
            Matcher m = p.matcher(text);
            if (m.find()) {
                String boss = "Mini-boss";
                if (m.groupCount() >= 1 && m.group(1) != null) boss = Config.cleanName(m.group(1));
                handleKill(boss, ServerResolver.current(), false);
                return;
            }
        }
    }

    /** Démarre le timer et affiche le message "tué sur X, respawn dans Y". */
    public static void handleKill(String bossName, String server, boolean force) {
        if (bossName == null || bossName.isBlank()) return;
        int minutes = Config.minutesFor(bossName);

        TimerManager.Timer t = TimerManager.start(bossName, server, minutes, force);
        if (t == null) return; // doublon

        Msg.chat(Msg.prefix()
                .append(Msg.t(bossName, ChatFormatting.YELLOW))
                .append(Msg.t(" tue sur ", ChatFormatting.GRAY))
                .append(Msg.t(server, ChatFormatting.AQUA))
                .append(Msg.t(" - respawn dans ", ChatFormatting.GRAY))
                .append(Msg.t(Msg.duration(t.respawnAt - t.startedAt), ChatFormatting.GREEN))
                .append(Msg.t(" (a " + Msg.clock(t.respawnAt) + ")", ChatFormatting.DARK_GRAY)));
    }
}
