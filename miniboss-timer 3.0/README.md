# MiniBoss Timer (Minecraft 26.2 / Fabric, client uniquement)

Tu tues un mini-boss, tu tapes une commande avec le nom du boss et le code du sous-serveur,
et le mod affiche un panneau avec tous les timers de respawn, le plus proche en premier.

## Utilisation

    /mbt start Ace 578        -> lance le timer d'Ace sur le serveur 578
    /mbt rst Ace 578          -> relance le timer d'Ace sur 578 (remis a zero)
    /mbt del Ace 578          -> supprime ce timer
    /mbt list                 -> liste des timers (le plus proche en premier)

- Le DERNIER mot est le code du serveur (3 premiers caracteres, lettres ou chiffres, garde en majuscules).
- Tout ce qui est avant est le nom du boss, il peut contenir des espaces : `/mbt start Capitaine Smoker 578`.
- Le panneau affiche `578 | Ace  12:34`, trie du plus proche au plus lointain.
- Alerte dans le chat (et son) 30 s avant le respawn, puis quand le boss est de retour.
- Les timers sont sauvegardes : ils survivent a un redemarrage du jeu.

## Affichage

    /mbt size +     agrandir (par pas de 0.25)
    /mbt size -     reduire
    /mbt size 1.5   taille exacte (0.5 a 4)
    /mbt pos 6 6    deplacer le panneau (x y en pixels depuis le coin haut gauche)
    /mbt hud        afficher / masquer

## Autres commandes

| Commande | Effet |
|---|---|
| `/mbt minutes <n>` | temps de respawn par defaut (15) |
| `/mbt auto` | active/desactive la detection automatique des morts (desactivee par defaut) |
| `/mbt add <boss>` / `remove <boss>` / `bosses` | liste de mini-boss pour le mode auto |
| `/mbt debug` | affiche le nom des entites mortes et le texte des icones survolees |
| `/mbt clear` | supprimer tous les timers |
| `/mbt reload` | recharger `config/miniboss_timer.json` |

Le temps de respawn propre a un boss se regle dans `config/miniboss_timer.json` :
`"bosses": {"Ace": 15, "Amiral": 20}` (0 = temps par defaut).

## Obtenir le .jar sans rien installer (GitHub)

1. Mets tout le contenu de ce dossier dans ton depot GitHub (Add file > Upload files),
   y compris le dossier `.github`. Les fichiers du meme nom sont remplaces.
2. Onglet **Actions** > "Build mod jar" : le build demarre tout seul.
3. Quand il est vert, telecharge l'artifact `miniboss-timer-jar`.
   Le jar du mod est celui qui ne finit pas par `-sources.jar`.
4. Remplace l'ancien jar dans `.minecraft/mods/` (avec Fabric API 26.2).
