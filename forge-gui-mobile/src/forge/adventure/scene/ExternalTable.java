package forge.adventure.scene;

import forge.game.GameRules;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;

import java.util.List;
import java.util.Set;

/**
 * NeoForge fusion: lets another front end play Adventure duels and edit Adventure decks.
 *
 * <p>Both hooks are null in a stock TFR build, so nothing changes there. The NeoForge module
 * (forge-gui-neo) sets them when it starts the Adventure (AdventureNeoMain). This replaces NeoForge's
 * shadow copies of DuelScene and DeckEditScene, which had to be re-copied whenever the originals changed.
 */
public final class ExternalTable {
    private ExternalTable() {
    }

    public interface Duel {
        /**
         * Called on the libGDX thread from DuelScene.enter(), with the match built and not yet started.
         * Returns true when it took the duel: it then starts the match itself and runs {@code back} on the libGDX
         * thread once the match is over. False leaves the duel to TFR's own table.
         */
        boolean play(HostedMatch match, GameRules rules, Set<GameType> variants, List<RegisteredPlayer> players,
                     RegisteredPlayer human, String enemyName, boolean bossBattle, String introNote, Runnable back);
    }

    public interface DeckEditor {
        /** Called on the libGDX thread from DeckEditScene.enter(). True when it opened its own editor. */
        boolean open(Runnable back);
    }

    public static volatile Duel duel;
    public static volatile DeckEditor deckEditor;
}
