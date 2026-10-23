package fr.minibosstimer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Configuration : fichier config/miniboss_timer.json */
public final class Config {
    public static final class Data {
        /** Temps de respawn par défaut (minutes). */
        public int defaultRespawnMinutes = 15;
        /** Mini-boss connus : nom (ou morceau de nom) -> minutes (0 = valeur par défaut). */
        public Map<String, Integer> bosses = new LinkedHashMap<>();
        /** Si vrai : toute entité avec un nom perso et beaucoup de PV est considérée comme un mini-boss. */
        public boolean useHealthHeuristic = true;
        public double minBossMaxHealth = 100.0;
        /** Distance max (blocs) entre toi et le mini-boss mort pour que ça compte. */
        public double maxKillDistance = 128.0;
        /** Nom de serveur forcé (vide = automatique : nom dans ta liste de serveurs). */
        public String manualServerName = "";
        /** Regex retirée des noms de boss (ex: "\\[Lv\\.? ?\\d+\\]"). Vide = rien. */
        public String nameCleanupRegex = "";
        /** Regex de messages de chat annonçant une mort de boss. Le groupe 1 = nom du boss. */
        public List<String> chatKillPatterns = new ArrayList<>();
        public boolean showHud = true;
        public int hudX = 6;
        public int hudY = 6;
        /** Taille de l'affichage (1.0 = normal). Change avec /mbt size. */
        public float hudScale = 1.0F;
        public int hudMaxLines = 12;
        /** Nombre de caractères gardés pour le code du sous-serveur (ex: 578). */
        public int serverCodeLength = 3;
        /** Détection automatique de la mort des boss. Désactivée : tu utilises /mbt start <boss> <serveur>. */
        public boolean autoDetectKills = false;
        /** Alerte X secondes avant le respawn (0 = désactivé). */
        public int preAlertSeconds = 30;
        /** Durée d'affichage du "PRÊT" avant de retirer le timer. */
        public int keepAfterReadySeconds = 120;
        public boolean playSound = true;
        public boolean debug = false;
        /** Regex : une icône du menu /voyager dont le texte (nom + description) correspond est un serveur. */
        public String navItemRegex = "(?i)spawn";
        /** Optionnel : regex pour extraire le nom du serveur (groupe 1). Vide = première ligne du nom de l'item. */
        public String navServerNameRegex = "";
    }

    public static Data data = new Data();
    private static Pattern cleanupPattern = null;
    private static Pattern navItemPattern = null;
    private static Pattern navNamePattern = null;
    private static List<Pattern> killPatterns = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("miniboss_timer.json");
    }

    public static void load() {
        Path f = file();
        try {
            if (Files.exists(f)) {
                Data d = GSON.fromJson(Files.readString(f), Data.class);
                if (d != null) data = d;
            }
        } catch (Exception e) {
            MiniBossTimerMod.LOGGER.error("Impossible de lire la config, valeurs par defaut utilisees", e);
        }
        if (data.bosses == null) data.bosses = new LinkedHashMap<>();
        if (data.chatKillPatterns == null) data.chatKillPatterns = new ArrayList<>();
        if (data.manualServerName == null) data.manualServerName = "";
        if (data.nameCleanupRegex == null) data.nameCleanupRegex = "";
        if (data.navItemRegex == null) data.navItemRegex = "(?i)spawn";
        if (data.navServerNameRegex == null) data.navServerNameRegex = "";
        if (data.defaultRespawnMinutes <= 0) data.defaultRespawnMinutes = 15;
        if (data.hudScale < 0.5F || data.hudScale > 4.0F) data.hudScale = 1.0F;
        if (data.hudMaxLines <= 0) data.hudMaxLines = 12;
        if (data.serverCodeLength <= 0) data.serverCodeLength = 3;
        rebuildPatterns();
        save(); // écrit le fichier avec toutes les options visibles
    }

    public static void save() {
        try {
            Files.writeString(file(), GSON.toJson(data));
        } catch (IOException e) {
            MiniBossTimerMod.LOGGER.error("Impossible d'ecrire la config", e);
        }
    }

    private static void rebuildPatterns() {
        cleanupPattern = null;
        if (!data.nameCleanupRegex.isBlank()) {
            try {
                cleanupPattern = Pattern.compile(data.nameCleanupRegex);
            } catch (PatternSyntaxException e) {
                MiniBossTimerMod.LOGGER.warn("nameCleanupRegex invalide : {}", e.getMessage());
            }
        }
        navItemPattern = compileOrNull(data.navItemRegex, "navItemRegex");
        navNamePattern = compileOrNull(data.navServerNameRegex, "navServerNameRegex");
        List<Pattern> list = new ArrayList<>();
        for (String s : data.chatKillPatterns) {
            try {
                list.add(Pattern.compile(s));
            } catch (PatternSyntaxException e) {
                MiniBossTimerMod.LOGGER.warn("chatKillPatterns invalide '{}' : {}", s, e.getMessage());
            }
        }
        killPatterns = list;
    }

    private static Pattern compileOrNull(String regex, String label) {
        if (regex == null || regex.isBlank()) return null;
        try {
            return Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            MiniBossTimerMod.LOGGER.warn("{} invalide : {}", label, e.getMessage());
            return null;
        }
    }

    public static Pattern navItemPattern() {
        return navItemPattern;
    }

    public static Pattern navNamePattern() {
        return navNamePattern;
    }

    /** Retire seulement les codes couleur et les espaces en trop (pour les textes d'infobulle). */
    public static String strip(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("\u00a7.", "").replaceAll("\\s+", " ").trim();
    }

    public static List<Pattern> killPatterns() {
        return killPatterns;
    }

    /** Retire les codes couleur § et nettoie le nom. */
    public static String cleanName(String raw) {
        if (raw == null) return "";
        String s = raw.replaceAll("\u00a7.", "");
        if (cleanupPattern != null) s = cleanupPattern.matcher(s).replaceAll("");
        return s.replaceAll("\\s+", " ").trim();
    }

    /** Trouve l'entrée de la liste qui correspond au nom (comparaison "contient", insensible à la casse). */
    private static Map.Entry<String, Integer> match(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Integer> e : data.bosses.entrySet()) {
            if (!e.getKey().isBlank() && lower.contains(e.getKey().toLowerCase(Locale.ROOT))) return e;
        }
        return null;
    }

    public static int minutesFor(String name) {
        Map.Entry<String, Integer> e = match(name);
        if (e != null && e.getValue() != null && e.getValue() > 0) return e.getValue();
        return data.defaultRespawnMinutes;
    }

    public static boolean isMiniBoss(String name, float maxHealth, boolean hasCustomName) {
        if (name.isBlank()) return false;
        if (match(name) != null) return true;
        return data.useHealthHeuristic && hasCustomName && maxHealth >= data.minBossMaxHealth;
    }
}
