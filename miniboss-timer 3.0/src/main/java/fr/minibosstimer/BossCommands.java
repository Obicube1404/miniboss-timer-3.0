package fr.minibosstimer;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Commandes client : /mbt ... (ne sont jamais envoyées au serveur). */
public final class BossCommands {
    private BossCommands() {}

    private interface BossAction {
        void run(FabricClientCommandSource source, String boss, String code);
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("mbt")
                        .executes(ctx -> help(ctx.getSource()))

                        // --- timers ---
                        .then(bossServerCommand("start", BossCommands::doStart))
                        .then(bossServerCommand("rst", BossCommands::doRestart))
                        .then(bossServerCommand("restart", BossCommands::doRestart))
                        .then(bossServerCommand("del", BossCommands::doDelete))
                        .then(ClientCommands.literal("list")
                                .executes(ctx -> list(ctx.getSource())))
                        .then(ClientCommands.literal("clear")
                                .executes(ctx -> {
                                    TimerManager.clear();
                                    ok(ctx.getSource(), "Tous les timers ont ete supprimes.");
                                    return 1;
                                }))

                        // --- affichage ---
                        .then(ClientCommands.literal("size")
                                .executes(ctx -> {
                                    ok(ctx.getSource(), "Taille : " + fmt(Config.data.hudScale)
                                            + "  (/mbt size + | - | 0.5 a 4)");
                                    return 1;
                                })
                                .then(ClientCommands.literal("+")
                                        .executes(ctx -> setScale(ctx.getSource(), Config.data.hudScale + 0.25F)))
                                .then(ClientCommands.literal("-")
                                        .executes(ctx -> setScale(ctx.getSource(), Config.data.hudScale - 0.25F)))
                                .then(ClientCommands.argument("valeur", FloatArgumentType.floatArg(0.5F, 4.0F))
                                        .executes(ctx -> setScale(ctx.getSource(),
                                                FloatArgumentType.getFloat(ctx, "valeur")))))
                        .then(ClientCommands.literal("pos")
                                .then(ClientCommands.argument("x", IntegerArgumentType.integer(0, 4000))
                                        .then(ClientCommands.argument("y", IntegerArgumentType.integer(0, 4000))
                                                .executes(ctx -> {
                                                    Config.data.hudX = IntegerArgumentType.getInteger(ctx, "x");
                                                    Config.data.hudY = IntegerArgumentType.getInteger(ctx, "y");
                                                    Config.save();
                                                    ok(ctx.getSource(), "Position : " + Config.data.hudX
                                                            + ", " + Config.data.hudY);
                                                    return 1;
                                                }))))
                        .then(ClientCommands.literal("hud")
                                .executes(ctx -> {
                                    Config.data.showHud = !Config.data.showHud;
                                    Config.save();
                                    ok(ctx.getSource(), "Affichage : " + (Config.data.showHud ? "active" : "desactive"));
                                    return 1;
                                }))

                        // --- réglages ---
                        .then(ClientCommands.literal("minutes")
                                .then(ClientCommands.argument("n", IntegerArgumentType.integer(1, 600))
                                        .executes(ctx -> {
                                            Config.data.defaultRespawnMinutes = IntegerArgumentType.getInteger(ctx, "n");
                                            Config.save();
                                            ok(ctx.getSource(), "Respawn par defaut : "
                                                    + Config.data.defaultRespawnMinutes + " min");
                                            return 1;
                                        })))
                        .then(ClientCommands.literal("add")
                                .then(ClientCommands.argument("nom", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String name = Config.cleanName(StringArgumentType.getString(ctx, "nom"));
                                            Config.data.bosses.put(name, 0);
                                            Config.save();
                                            ok(ctx.getSource(), "Mini-boss ajoute : " + name);
                                            return 1;
                                        })))
                        .then(ClientCommands.literal("remove")
                                .then(ClientCommands.argument("nom", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String name = Config.cleanName(StringArgumentType.getString(ctx, "nom"));
                                            boolean removed = Config.data.bosses.keySet()
                                                    .removeIf(k -> k.equalsIgnoreCase(name));
                                            Config.save();
                                            ok(ctx.getSource(), removed ? "Retire : " + name : "Introuvable : " + name);
                                            return 1;
                                        })))
                        .then(ClientCommands.literal("bosses")
                                .executes(ctx -> bosses(ctx.getSource())))
                        .then(ClientCommands.literal("auto")
                                .executes(ctx -> {
                                    Config.data.autoDetectKills = !Config.data.autoDetectKills;
                                    Config.save();
                                    ok(ctx.getSource(), "Detection automatique des morts : "
                                            + (Config.data.autoDetectKills ? "activee" : "desactivee"));
                                    return 1;
                                }))
                        .then(ClientCommands.literal("server")
                                .executes(ctx -> {
                                    ok(ctx.getSource(), "Serveur (mode auto) : " + ServerResolver.current()
                                            + " - /mbt server <nom> pour forcer, /mbt server auto pour effacer");
                                    return 1;
                                })
                                .then(ClientCommands.argument("nom", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "nom").trim();
                                            if (name.equalsIgnoreCase("auto")) name = "";
                                            Config.data.manualServerName = name;
                                            ServerResolver.detected = "";
                                            Config.save();
                                            ok(ctx.getSource(), "Serveur (mode auto) : " + ServerResolver.current());
                                            return 1;
                                        })))
                        .then(ClientCommands.literal("debug")
                                .executes(ctx -> {
                                    Config.data.debug = !Config.data.debug;
                                    Config.save();
                                    ok(ctx.getSource(), "Debug : " + (Config.data.debug ? "active" : "desactive"));
                                    return 1;
                                }))
                        .then(ClientCommands.literal("reload")
                                .executes(ctx -> {
                                    Config.load();
                                    ok(ctx.getSource(), "Config rechargee.");
                                    return 1;
                                }))
                ));
    }

    // ------------------------------------------------------------------ actions

    private static void doStart(FabricClientCommandSource s, String boss, String code) {
        KillDetector.handleKill(boss, code, true);
    }

    private static void doRestart(FabricClientCommandSource s, String boss, String code) {
        TimerManager.Timer t = TimerManager.restart(boss, code, Config.minutesFor(boss));
        if (t == null) {
            err(s, "Aucun timer pour " + boss + " sur " + code + ". Utilise /mbt start " + boss + " " + code);
            return;
        }
        s.sendFeedback(Msg.prefix()
                .append(Msg.t(t.boss, ChatFormatting.YELLOW))
                .append(Msg.t(" relance sur ", ChatFormatting.GRAY))
                .append(Msg.t(t.server, ChatFormatting.AQUA))
                .append(Msg.t(" - respawn dans ", ChatFormatting.GRAY))
                .append(Msg.t(Msg.duration(t.respawnAt - t.startedAt), ChatFormatting.GREEN))
                .append(Msg.t(" (a " + Msg.clock(t.respawnAt) + ")", ChatFormatting.DARK_GRAY)));
    }

    private static void doDelete(FabricClientCommandSource s, String boss, String code) {
        boolean removed = TimerManager.remove(boss, code);
        ok(s, removed ? "Timer supprime : " + boss + " sur " + code
                      : "Aucun timer pour " + boss + " sur " + code);
    }

    private static int setScale(FabricClientCommandSource s, float value) {
        float v = Math.max(0.5F, Math.min(4.0F, value));
        Config.data.hudScale = Math.round(v * 100.0F) / 100.0F;
        Config.save();
        ok(s, "Taille : " + fmt(Config.data.hudScale));
        return 1;
    }

    // ------------------------------------------------------------------ outils

    /**
     * Construit une commande du type : /mbt <literal> <nom du boss> <code serveur>
     * Le dernier mot est le code du serveur, tout ce qui précède est le nom du boss (peut contenir des espaces).
     */
    private static LiteralArgumentBuilder<FabricClientCommandSource> bossServerCommand(
            String literal, BossAction action) {
        return ClientCommands.literal(literal)
                .executes(ctx -> {
                    usage(ctx.getSource(), literal);
                    return 0;
                })
                .then(ClientCommands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            FabricClientCommandSource s = ctx.getSource();
                            String[] parsed = parseBossAndServer(StringArgumentType.getString(ctx, "args"));
                            if (parsed == null) {
                                usage(s, literal);
                                return 0;
                            }
                            action.run(s, parsed[0], parsed[1]);
                            return 1;
                        }));
    }

    /** "Ace 578" -> ["Ace", "578"] ; "Capitaine Smoker 12a" -> ["Capitaine Smoker", "12A"] ; sinon null. */
    static String[] parseBossAndServer(String raw) {
        String cleaned = raw.trim().replaceAll("\\s+", " ");
        int i = cleaned.lastIndexOf(' ');
        if (i <= 0) return null;
        String boss = Config.cleanName(cleaned.substring(0, i));
        String code = cleaned.substring(i + 1);
        if (boss.isBlank() || !code.matches("[\\p{L}\\p{N}]+")) return null;
        int len = Config.data.serverCodeLength;
        if (code.length() > len) code = code.substring(0, len);
        return new String[] { boss, code.toUpperCase(Locale.ROOT) };
    }

    private static void usage(FabricClientCommandSource s, String literal) {
        err(s, "Usage : /mbt " + literal + " <boss> <serveur>   (exemple : /mbt " + literal + " Ace 578)");
    }

    private static String fmt(float v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static void ok(FabricClientCommandSource s, String text) {
        s.sendFeedback(Msg.prefix().append(Msg.t(text, ChatFormatting.GRAY)));
    }

    private static void err(FabricClientCommandSource s, String text) {
        s.sendFeedback(Msg.prefix().append(Msg.t(text, ChatFormatting.RED)));
    }

    private static int help(FabricClientCommandSource s) {
        s.sendFeedback(Msg.t("=== MiniBoss Timer ===", ChatFormatting.GOLD));
        String[] lines = {
                "/mbt start <boss> <serveur> - lancer le timer (ex: /mbt start Ace 578)",
                "/mbt rst <boss> <serveur> - relancer le timer a zero",
                "/mbt del <boss> <serveur> - supprimer un timer",
                "/mbt list - timers en cours (le plus proche en premier)",
                "/mbt size + | - | <0.5 a 4> - taille de l'affichage",
                "/mbt pos <x> <y> - deplacer l'affichage",
                "/mbt hud - afficher/masquer l'affichage",
                "/mbt minutes <n> - temps de respawn par defaut",
                "/mbt add|remove <boss> | bosses - liste des mini-boss (mode auto)",
                "/mbt auto - detection automatique des morts (desactivee par defaut)",
                "/mbt clear - supprimer tous les timers",
                "/mbt reload - recharger config/miniboss_timer.json"
        };
        for (String l : lines) s.sendFeedback(Msg.t(l, ChatFormatting.GRAY));
        return 1;
    }

    private static int list(FabricClientCommandSource s) {
        List<TimerManager.Timer> timers = TimerManager.snapshot();
        if (timers.isEmpty()) {
            ok(s, "Aucun timer en cours.");
            return 1;
        }
        long now = System.currentTimeMillis();
        s.sendFeedback(Msg.t("=== Timers de respawn ===", ChatFormatting.GOLD));
        for (TimerManager.Timer t : timers) {
            long remaining = t.respawnAt - now;
            s.sendFeedback(Msg.t(t.server + " | ", ChatFormatting.AQUA)
                    .append(Msg.t(t.boss + "  ", ChatFormatting.YELLOW))
                    .append(remaining <= 0
                            ? Msg.t("PRET !", ChatFormatting.GREEN)
                            : Msg.t(Msg.duration(remaining) + " (a " + Msg.clock(t.respawnAt) + ")", ChatFormatting.GRAY)));
        }
        return 1;
    }

    private static int bosses(FabricClientCommandSource s) {
        s.sendFeedback(Msg.t("=== Mini-boss connus (mode auto) ===", ChatFormatting.GOLD));
        if (Config.data.bosses.isEmpty()) {
            s.sendFeedback(Msg.t("(liste vide)", ChatFormatting.GRAY));
        }
        for (Map.Entry<String, Integer> e : Config.data.bosses.entrySet()) {
            int min = (e.getValue() != null && e.getValue() > 0) ? e.getValue() : Config.data.defaultRespawnMinutes;
            s.sendFeedback(Msg.t("- " + e.getKey() + " : " + min + " min", ChatFormatting.GRAY));
        }
        return 1;
    }
}
