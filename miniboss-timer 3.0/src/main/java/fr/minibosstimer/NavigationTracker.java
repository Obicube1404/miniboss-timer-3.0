package fr.minibosstimer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Détecte le serveur choisi dans le menu /voyager :
 *  - on lit le texte de l'icône survolée (infobulle) ;
 *  - quand tu cliques sur une icône "serveur" (voir navItemRegex), on la garde en attente ;
 *  - dès que tu changes réellement de serveur (nouvelle connexion de jeu), elle devient le serveur actuel.
 */
public final class NavigationTracker {
    private static final long PENDING_MAX_MS = 20_000L;

    private static List<String> lastHover = new ArrayList<>();
    private static long lastHoverAt = 0L;
    private static String lastDebugKey = "";
    private static boolean wasDown = false;

    private static String pending = "";
    private static long pendingAt = 0L;
    private static String announce = null;

    private NavigationTracker() {}

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> onTooltip(lines));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ServerResolver.detected = "");
    }

    /** Appelé depuis le tick client : affiche le message "Serveur : X" quand le joueur est prêt. */
    public static void tick() {
        if (announce != null && Minecraft.getInstance().player != null) {
            Msg.chat(Msg.prefix()
                    .append(Msg.t("Serveur : ", ChatFormatting.GRAY))
                    .append(Msg.t(announce, ChatFormatting.AQUA)));
            announce = null;
        }
    }

    private static void onTooltip(List<Component> lines) {
        List<String> out = new ArrayList<>();
        for (Component c : lines) {
            String s = Config.strip(c.getString());
            if (!s.isBlank()) out.add(s);
        }
        if (out.isEmpty()) return;

        lastHover = out;
        lastHoverAt = System.currentTimeMillis();

        if (Config.data.debug) {
            String key = String.join("|", out);
            if (!key.equals(lastDebugKey)) {
                lastDebugKey = key;
                Msg.chat(Msg.t("[debug] survol : " + String.join("  //  ", out), ChatFormatting.DARK_GRAY));
            }
        }

        // détection du clic gauche (front montant) sur l'icône survolée
        boolean down = Minecraft.getInstance().mouseHandler.isLeftPressed();
        if (down && !wasDown) onClick(out);
        wasDown = down;
    }

    private static void onClick(List<String> hovered) {
        String all = String.join(" ", hovered);
        Pattern item = Config.navItemPattern();
        if (item == null || !item.matcher(all).find()) return;

        String name = hovered.get(0);
        Pattern np = Config.navNamePattern();
        if (np != null) {
            Matcher m = np.matcher(all);
            if (m.find()) {
                String g = (m.groupCount() >= 1 && m.group(1) != null) ? m.group(1) : m.group();
                if (!g.isBlank()) name = Config.strip(g);
            }
        }

        pending = name;
        pendingAt = System.currentTimeMillis();
        if (Config.data.debug) {
            Msg.chat(Msg.t("[debug] clic sur : \"" + name + "\" (en attente du changement de serveur)",
                    ChatFormatting.DARK_GRAY));
        }
    }

    /** Nouvelle connexion de jeu = tu viens de changer de serveur (ou de te connecter). */
    private static void onJoin() {
        if (!Config.data.autoDetectKills) {
            pending = "";
            return;
        }
        long now = System.currentTimeMillis();
        if (!pending.isEmpty() && now - pendingAt < PENDING_MAX_MS) {
            ServerResolver.detected = pending;
            announce = pending;
        }
        pending = "";
    }
}
