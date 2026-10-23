package fr.minibosstimer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

/** Détermine "sur quel serveur" tu te trouves. */
public final class ServerResolver {
    /** Serveur détecté automatiquement via le menu /voyager (vide = pas détecté). */
    static volatile String detected = "";

    private ServerResolver() {}

    public static String current() {
        // 1) serveur détecté après un /voyager
        String d = detected;
        if (d != null && !d.isBlank()) return d;

        // 2) nom forcé avec /mbt server <nom>
        String manual = Config.data.manualServerName;
        if (manual != null && !manual.isBlank()) return manual;

        // 3) nom du serveur dans ta liste multijoueur (sinon son adresse IP)
        ServerData sd = Minecraft.getInstance().getCurrentServer();
        if (sd == null) return "Solo";
        if (sd.name != null && !sd.name.isBlank()) return sd.name;
        if (sd.ip != null && !sd.ip.isBlank()) return sd.ip;
        return "Inconnu";
    }

    public static boolean isDetected() {
        return detected != null && !detected.isBlank();
    }
}
