package fr.minibosstimer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Petits utilitaires : messages de chat, son, formatage du temps. */
final class Msg {
    private static final DateTimeFormatter CLOCK =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private Msg() {}

    static MutableComponent t(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color);
    }

    static MutableComponent prefix() {
        return t("[MiniBoss] ", ChatFormatting.GOLD);
    }

    static void chat(Component c) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.sendSystemMessage(c);
    }

    static void ping() {
        if (!Config.data.playSound) return;
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
    }

    /** 754000 ms -> "12:34" (ou "1:02:03" si plus d'une heure). */
    static String duration(long ms) {
        long s = Math.max(0L, (ms + 999L) / 1000L);
        long h = s / 3600L;
        long m = (s % 3600L) / 60L;
        long sec = s % 60L;
        if (h > 0) return String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec);
        return String.format(Locale.ROOT, "%02d:%02d", m, sec);
    }

    static String clock(long epochMs) {
        return CLOCK.format(Instant.ofEpochMilli(epochMs));
    }
}
