# NeoForge fusion (branch `neoforge-fusion`)

The Forsaken Realms played with [NeoForge](https://github.com/AdrianLopez98/NeoForge)'s table and deck builder.
NeoForge is one extra Maven module, `forge-gui-neo` (JavaFX), on top of the Forge engine; it was at 1.0.50
(`58c9544`), built against upstream `7d0acc8aa92` - the same engine commit as TFR v1.28, so nothing had to be merged.

## How it fits together

- NeoForge's menu starts the Adventure in its own process (`forge.neo.adventure.AdventureNeoMain` ->
  `forge.app.Main`). The world map, towns, shops, quests and every TFR system are TFR's own libGDX code, unchanged.
- `forge.adventure.scene.ExternalTable` (new, in forge-gui-mobile) holds two hooks, null in a stock TFR build:
  - `duel`: `DuelScene.enter()` offers it the match once decks, items, blessings, life and TFR's rules are applied.
    NeoForge plays it on its table, then runs TFR's `GameEnd()` and exit, so rewards, ante, streaks, Notoriety and the
    logs are TFR's. `DuelHeartbeat` runs as in a normal duel.
  - `deckEditor`: `DeckEditScene.enter()` offers it the player's own decks. Event decks keep TFR's editor.
- `AdventureNeoMain` sets both hooks. Upstream NeoForge instead shadowed `DuelScene` and `DeckEditScene` with patched
  copies placed first on the classpath; those copies are gone here, because TFR edits both files often and a stale
  copy would silently drop TFR's changes.
- Stay on TFR's own table: watched fights (guards, Deck Tester), agent-piloted duels, and everything when the game is
  started the usual way (the hooks are null). `-Dneo.adventure.bridge=false` / `-Dneo.adventure.editor=false` turn the
  NeoForge table / editor off inside NeoForge.

## Build and run

```
mvn -B package -DskipTests -pl forge-gui-neo -am
cd forge-gui-neo
java -Dfile.encoding=UTF-8 -Xmx2g -Dneo.dataDir=<a folder for saves and settings> \
  --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED \
  --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.math=ALL-UNNAMED \
  --add-opens=java.base/java.net=ALL-UNNAMED \
  -cp "target/classes;target/lib/*" forge.neo.NeoMain ui
```
(`:` instead of `;` on macOS and Linux.) Then Adventure from NeoForge's menu. `forge.neo.adventure.AdventureCheck`,
run from `forge-gui-neo`, checks the wiring (19 checks).

## Tested (2026-11-10, Linux, Xvfb, JDK 21)

- Builds; AdventureCheck 19 of 19.
- `-Dneo.adventure.selftest=anteduel`: new TFR world, a duel against Goblin Worker (Apprentice) on NeoForge's table,
  back through `GameEnd()` - ante loss, win streak, win ratio, Notoriety and ReloadJab lines all written.
- `-Dneo.adventure.selftest=editor`: NeoForge's deck builder opens on the TFR collection and saves the deck back.

## The Ante Re-roll

`NeoMatchUI.revealAnteCards` is TFR's Ante Re-roll on NeoForge's table: the reveal, then "Keep / Re-roll" with the cost
from the plane's tuning (`anteRerollBaseShardCost`, `anteRerollEscalationRate`, difficulty-scaled), the free daily
re-roll of the Law of Stakes, and both the purse and the in-match shard count charged. It mirrors
`MatchController.revealAnteCards`; a change to the rules there has to be made in both. Seen in the self-test
(`-Dneo.adventure.anteTest=true` skips the reveal so the offer shows unattended): the offer at 10 Shards, logged as
`[TFR-AnteReroll] (NeoForge table)`. Clicking Re-roll and paying was not exercised.

## Not carried into a NeoForge duel (TFR features that live in TFR's own match screen)

- The boss intro and win-streak notice before the duel (written to the log only), and TFR's boss music: NeoForge mutes
  Forge's music in the Adventure process and plays its own.
- The 50x speed button and other additions to `MatchController` / `VPlayerPanel` / `VPrompt`.
- `[TFR-Duel]` logs `life=? enemyLife=?`: the game object is already gone when `GameEnd()` runs.

## Not tested

- A human playing: the world map into a duel and back by hand, bosses, dungeons, the Arena, Inn events, best of three.
- Windows and macOS (upstream NeoForge's Adventure is Windows-only), packaging, Android.
- Custom TFR cards and mechanics (Empower, Incubate, Recruit) as NeoForge draws them.
- Saves: the NeoForge profile keeps its own data folder; a TFR save copied into it should load, not verified.
