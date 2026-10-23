package fr.minibosstimer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/** Gère la liste des timers de respawn (sauvegardée sur disque : survit à un redémarrage du jeu). */
public final class TimerManager {

    public static final class Timer {
        public String boss;
        public String server;
        public long startedAt;
        public long respawnAt;
        public boolean preAlerted;
        public boolean notified;

        String key() {
            return key(server, boss);
        }

        static String key(String server, String boss) {
            return server.toLowerCase(Locale.ROOT) + "|" + boss.toLowerCase(Locale.ROOT);
        }
    }

    private static final class State {
        List<Timer> timers = new ArrayList<>();
    }

    private static final List<Timer> TIMERS = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private TimerManager() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("miniboss_timer_state.json");
    }

    public static synchronized void load() {
        TIMERS.clear();
        Path f = file();
        if (!Files.exists(f)) return;
        try {
            State s = GSON.fromJson(Files.readString(f), State.class);
            if (s == null || s.timers == null) return;
            long now = System.currentTimeMillis();
            for (Timer t : s.timers) {
                if (t == null || t.boss == null || t.server == null) continue;
                if (now > t.respawnAt) {
                    // déjà expiré pendant que le jeu était fermé : on ne le garde pas
                    continue;
                }
                TIMERS.add(t);
            }
        } catch (Exception e) {
            MiniBossTimerMod.LOGGER.error("Impossible de lire les timers sauvegardes", e);
        }
    }

    private static void save() {
        try {
            State s = new State();
            s.timers = new ArrayList<>(TIMERS);
            Files.writeString(file(), GSON.toJson(s));
        } catch (IOException e) {
            MiniBossTimerMod.LOGGER.error("Impossible d'ecrire les timers", e);
        }
    }

    /**
     * Démarre (ou relance) un timer.
     * @param force si faux, ignore un doublon démarré il y a moins de 30 s (évite les doubles détections)
     * @return le timer créé, ou null si ignoré comme doublon
     */
    public static synchronized Timer start(String boss, String server, int minutes, boolean force) {
        long now = System.currentTimeMillis();
        String key = Timer.key(server, boss);
        for (Iterator<Timer> it = TIMERS.iterator(); it.hasNext(); ) {
            Timer t = it.next();
            if (t.key().equals(key)) {
                if (!force && now - t.startedAt < 30_000L) return null;
                it.remove();
            }
        }
        Timer t = new Timer();
        t.boss = boss;
        t.server = server;
        t.startedAt = now;
        t.respawnAt = now + minutes * 60_000L;
        TIMERS.add(t);
        save();
        return t;
    }

    /** Retrouve le timer d'un boss sur un serveur (insensible à la casse). */
    public static synchronized Timer find(String boss, String server) {
        String key = Timer.key(server, boss);
        for (Timer t : TIMERS) {
            if (t.key().equals(key)) return t;
        }
        return null;
    }

    /** Relance le timer à son temps complet. Retourne null si aucun timer n'existe pour ce boss/serveur. */
    public static synchronized Timer restart(String boss, String server, int minutes) {
        Timer t = find(boss, server);
        if (t == null) return null;
        long now = System.currentTimeMillis();
        t.startedAt = now;
        t.respawnAt = now + minutes * 60_000L;
        t.preAlerted = false;
        t.notified = false;
        save();
        return t;
    }

    public static synchronized boolean remove(String boss, String server) {
        Timer t = find(boss, server);
        if (t == null) return false;
        TIMERS.remove(t);
        save();
        return true;
    }

    public static synchronized List<Timer> snapshot() {
        List<Timer> copy = new ArrayList<>(TIMERS);
        copy.sort(Comparator.comparingLong(t -> t.respawnAt));
        return copy;
    }

    public static synchronized void clear() {
        TIMERS.clear();
        save();
    }

    /** Appelé à chaque tick client : déclenche les alertes et nettoie les vieux timers. */
    public static synchronized void tick() {
        if (TIMERS.isEmpty()) return;
        if (Minecraft.getInstance().player == null) return; // pas en jeu : on attend

        long now = System.currentTimeMillis();
        long keepMs = Config.data.keepAfterReadySeconds * 1000L;
        boolean dirty = false;

        for (Iterator<Timer> it = TIMERS.iterator(); it.hasNext(); ) {
            Timer t = it.next();
            long remaining = t.respawnAt - now;

            if (!t.notified && remaining <= -keepMs) {
                it.remove(); // expiré depuis trop longtemps, on ne spamme pas
                dirty = true;
                continue;
            }
            if (!t.notified && remaining <= 0) {
                t.notified = true;
                dirty = true;
                Msg.chat(Msg.prefix()
                        .append(Msg.t(t.boss, ChatFormatting.GREEN))
                        .append(Msg.t(" a respawn sur ", ChatFormatting.GRAY))
                        .append(Msg.t(t.server, ChatFormatting.AQUA))
                        .append(Msg.t(" !", ChatFormatting.GRAY)));
                Msg.ping();
            } else if (!t.notified && !t.preAlerted
                    && Config.data.preAlertSeconds > 0
                    && remaining <= Config.data.preAlertSeconds * 1000L) {
                t.preAlerted = true;
                dirty = true;
                Msg.chat(Msg.prefix()
                        .append(Msg.t(t.boss, ChatFormatting.YELLOW))
                        .append(Msg.t(" respawn dans ", ChatFormatting.GRAY))
                        .append(Msg.t(Msg.duration(remaining), ChatFormatting.GREEN))
                        .append(Msg.t(" sur ", ChatFormatting.GRAY))
                        .append(Msg.t(t.server, ChatFormatting.AQUA)));
            }
            if (t.notified && now - t.respawnAt > keepMs) {
                it.remove();
                dirty = true;
            }
        }
        if (dirty) save();
    }
}
