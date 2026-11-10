package forge.neo.adventure;

import forge.neo.NeoShortcuts;
import forge.neo.match.NeoMatchUI;
import forge.neo.ui.CardZoom;
import forge.neo.ui.PauseMenu;
import forge.neo.ui.SettingsPanel;
import forge.neo.ui.TableKeys;
import forge.neo.ui.TableScreen;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * el teclado y el menu de pausa del duelo del Adventure, como en una
 * partida normal de NeoForge.
 *
 * <p>En NeoForge eso lo pone la ventana principal ({@code NeoApp}), que aqui no
 * existe: sin esto, Escape no hacia nada (reportado jugando). Copia su
 * comportamiento: Escape cierra primero la carta ampliada, luego vuelve al
 * dialogo apartado y si no abre la pausa; y los atajos de partida de
 * {@link NeoShortcuts}, con la tecla que tengas configurada.
 *
 * <p>La pausa es la de {@code PauseMenu.forAdventure}: <b>sin "Reiniciar"</b>
 * (repetir el duelo seria volver a barajar contra el mismo enemigo) y con
 * "Salir" diciendo lo que hace aqui — rendirse y volver al mapa — tambien en la
 * pregunta de confirmacion, que es donde antes seguia hablando de la pantalla
 * de inicio.
 *
 * <p>Y se abre ademas desde un <b>boton</b> de la mesa
 * ({@code TableScreen.enablePauseButton}), no solo con Escape: aqui no hay
 * tutorial que lo ensenye.
 */
public final class DuelControls {

    private DuelControls() {
    }

    private static final Set<NeoShortcuts.Action> HELD = EnumSet.noneOf(NeoShortcuts.Action.class);

    /** La mesa y la partida del duelo en curso. Solo desde el hilo de JavaFX. */
    private static TableScreen currentTable;
    private static NeoMatchUI currentUi;
    /** La escena que ya tiene puesta la guardia de los botones. */
    private static Scene guarded;

    /** Publico solo para {@code forge.neo.match.LeakCheck}, que monta los duelos igual. */
    public static void install(final Scene scene, final TableScreen table, final NeoMatchUI ui) {
        scene.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) {
                if (table.isZoomShowing()) {
                    table.hideZoom();
                } else if (table.getOverlay().isPeeking()) {
                    table.getOverlay().setPeeking(false);
                } else if (table.getMenuOverlay().isShowing()) {
                    table.getMenuOverlay().hide();
                } else {
                    openPause(table, ui);
                }
                ev.consume();
                return;
            }
            if (shortcut(ev, scene, table, ui)) {
                ev.consume();
            }
        });
        scene.setOnKeyReleased(ev -> HELD.clear());
        // Y la guardia del boton con el foco, que es de la mesa y no de esta
        // pantalla: sin ella, despues de clicar UNA vez el boton del registro,
        // cada Espacio lo volvia a abrir en vez de pasar la prioridad.
        // Reportado jugando la Aventura el 20-09-2026 ("this thing starts to
        // show up every time i am pressing space... seems no problem in ascent
        // or classic neo forge"), y el "no pasa en los otros modos" es la
        // pista entera: alli la pone NeoApp, y esta escena es otra.
        //
        // UNA sola vez por escena, leyendo la mesa del duelo de AHORA. La escena
        // es la misma en todos los duelos (NeoWindow la reutiliza) y un filtro
        // no se quita solo: poniendolo en cada duelo con la mesa capturada, cada
        // filtro retenia SU mesa, su partida, sus cartas y sus imagenes para
        // siempre. Medido el 25-09-2026: 4 duelos, 4 TableScreen vivas, y la
        // VRAM de JavaFX subiendo duelo a duelo hasta la pantalla en blanco
        // (itch.io, "10-15 battles"). Ver PrismGuard.
        currentTable = table;
        currentUi = ui;
        if (guarded != scene) {
            guarded = scene;
            TableKeys.guardFocusedButtons(scene, () -> currentTable,
                    ev -> currentTable != null && shortcut(ev, scene, currentTable, currentUi),
                    HELD::clear);
        }
        // Y el boton, porque aqui Escape no lo ensenya nadie: al duelo de la
        // Aventura se llega desde el mapa de Forge, sin pasar por nuestro
        // tutorial. Reportado en Reddit el 22-09-2026: "I don't seem to find a
        // concede button in the battle screen for adventure mode". Estaba, en
        // Escape — pero un control que hay que adivinar no existe.
        table.enablePauseButton(() -> openPause(table, ui));
    }

    private static void openPause(final TableScreen table, final NeoMatchUI ui) {
        if (table.getMenuOverlay().isShowing()) {
            return;
        }
        final PauseMenu menu = PauseMenu.forAdventure(new PauseMenu.Actions() {
            @Override
            public void resume() {
                table.getMenuOverlay().hide();
            }

            @Override
            public void restart() {
                table.getMenuOverlay().hide();
            }

            @Override
            public void quitToMenu() {
                table.getMenuOverlay().hide();
                ui.leaveMatch(NeoMatchUI.Exit.MENU);
            }

            @Override
            public java.util.Map<String, Runnable> devCheats() {
                return DuelControls.devCheats(table, ui);
            }

            // Los eventos de la Aventura van al mejor de tres.
            @Override
            public boolean canConcedeGame() {
                return ui.canConcedeGame();
            }

            @Override
            public void concedeGame() {
                table.getMenuOverlay().hide();
                ui.concedeGame();
            }

            @Override
            public boolean hasSavedOrders() {
                return ui.hasSavedOrders();
            }

            @Override
            public void forgetSavedOrders() {
                ui.forgetSavedOrders();
            }
        }, host(table, ui));
        table.getMenuOverlay().show(menu);
    }

    private static SettingsPanel.Host host(final TableScreen table, final NeoMatchUI ui) {
        return new SettingsPanel.Host() {
            @Override
            public void setAutoPayMana(final boolean on) {
                ui.setAutoPayMana(on);
            }

            @Override
            public void setPauseMode(final int mode) {
                ui.setPauseMode(mode);
            }

            @Override
            public void setAiSpeed(final int hundredths) {
                ui.setAiSpeed(hundredths);
            }

            @Override
            public void applyScale() {
                final Scene s = table.getScene();
                if (s != null && s.getRoot() != null) {
                    s.getRoot().setStyle(forge.neo.ui.UiScale.rootStyle(s.getHeight()));
                }
            }

            @Override
            public boolean isFullScreen() {
                final Scene s = table.getScene();
                return s != null && s.getWindow() instanceof javafx.stage.Stage
                        && ((javafx.stage.Stage) s.getWindow()).isFullScreen();
            }

            @Override
            public void setFullScreen(final boolean on) {
                final Scene s = table.getScene();
                if (s != null && s.getWindow() instanceof javafx.stage.Stage) {
                    ((javafx.stage.Stage) s.getWindow()).setFullScreen(on);
                }
            }

            @Override
            public double getTextZoom() {
                return table.getDetailPanel().getTextZoom();
            }

            @Override
            public void setTextZoom(final double zoom) {
                table.getDetailPanel().setTextZoom(zoom);
            }

            @Override
            public void setDraftRankingVisible(final boolean on) {
            }

            @Override
            public boolean isInMatch() {
                return true;
            }
        };
    }

    /** Los atajos de partida principales, igual que NeoApp.runShortcut. */
    private static boolean shortcut(final KeyEvent ev, final Scene scene, final TableScreen table,
                                    final NeoMatchUI ui) {
        if (ev.getTarget() instanceof javafx.scene.control.TextInputControl
                || scene.getFocusOwner() instanceof javafx.scene.control.TextInputControl) {
            return false;
        }
        final NeoShortcuts.Action action = NeoShortcuts.actionFor(ev);
        if (action == null) {
            return false;
        }
        if (!action.repeats() && !HELD.add(action)) {
            return true;
        }
        final boolean free = !table.isModalShowing();
        switch (action) {
            case PASS_PRIORITY:
                if (free && table.getPromptBanner().acknowledge()) {
                    return true;
                }
                if (table.pressDialogAuto()) {
                    return true;
                }
                return free && table.getActionBar().pressPrimary();
            case PASS_TURN:
                return free && ui.passTurn();
            case ALPHA_STRIKE:
                return free && ui.alphaStrike();
            case ATTACK_NON_TOKENS:
                return free && ui.attackWithNonTokens();
            case ZOOM_CARD: {
                final forge.neo.card.CardNode hovered = CardZoom.hoveredCardNode(table);
                if (hovered != null && hovered.getCard() != null) {
                    table.showZoom(hovered.getCard());
                    return true;
                }
                return false;
            }
            case GAME_LOG:
                table.getLogButton().fire();
                return true;
            case EXPAND_STACK:
                return table.toggleAllStackEntries();
            case STACK_MENU:
                return free && ui.openTopStackMenu();
            // Lo mismo que en NeoApp: en la Aventura no estaban (Discord,
            // 31-10-2026, jugando aqui con un mazo de muchos disparos).
            case AUTO_YIELD_YES:
            case AUTO_YIELD_NO:
            case AUTO_PASS_TOP: {
                if (!free) {
                    return false;
                }
                final String said = action == NeoShortcuts.Action.AUTO_PASS_TOP ? ui.autoPassTop()
                        : ui.autoYieldTop(action == NeoShortcuts.Action.AUTO_YIELD_YES);
                if (said == null) {
                    return false;
                }
                table.getActionBar().setWarning(said, false);
                return true;
            }
            default:
                return false;
        }
    }

    /**
     * The Forsaken Realms fusion: Forge's Dev Mode menu (VDevMenu on TFR's own table) for a duel on this
     * table. Empty unless Dev Mode is on (Adventure settings). The cheats ask their questions through
     * the match UI, so they run on the game thread, never on JavaFX's.
     */
    private static java.util.Map<String, Runnable> devCheats(final TableScreen table, final NeoMatchUI ui) {
        final java.util.Map<String, Runnable> out = new java.util.LinkedHashMap<>();
        if (!forge.localinstance.properties.ForgePreferences.DEV_MODE || ui.getGameController() == null) {
            return out;
        }
        final forge.interfaces.IGameController gc = ui.getGameController();
        addCheat(out, table, "lblSetLife", "Set Player Life", () -> gc.cheat().setPlayerLife());
        addCheat(out, table, "lblWinGame", "Win Game", () -> gc.cheat().winGame());
        addCheat(out, table, "lblGenerateMana", "Generate Mana", () -> gc.cheat().generateMana());
        addCheat(out, table, "lblTutor", "Tutor for Card", () -> gc.cheat().tutorForCard());
        addCheat(out, table, "lblCardToHand", "Add Card to Hand", () -> gc.cheat().addCardToHand());
        addCheat(out, table, "lblCardToBattlefield", "Add Card to Battlefield", () -> gc.cheat().addCardToBattlefield());
        addCheat(out, table, "lblTokenToBattlefield", "Add Token to Battlefield", () -> gc.cheat().addTokenToBattlefield());
        addCheat(out, table, "lblCardToLibrary", "Add Card to Library", () -> gc.cheat().addCardToLibrary());
        addCheat(out, table, "lblCardToGraveyard", "Add Card to Graveyard", () -> gc.cheat().addCardToGraveyard());
        addCheat(out, table, "lblRepeatAddCard", "Repeat Last Add Card", () -> gc.cheat().repeatLastAddition());
        addCheat(out, table, "lblAddCounterPermanent", "Add Counters to Card", () -> gc.cheat().addCountersToPermanent());
        addCheat(out, table, "lblSubCounterPermanent", "Remove Counters from Card", () -> gc.cheat().removeCountersFromPermanent());
        addCheat(out, table, "lblTapPermanent", "Tap Permanents", () -> gc.cheat().tapPermanents());
        addCheat(out, table, "lblUntapPermanent", "Untap Permanents", () -> gc.cheat().untapPermanents());
        addCheat(out, table, "lblExileFromPlay", "Exile Cards from Play", () -> gc.cheat().exileCardsFromBattlefield());
        addCheat(out, table, "lblExileFromHand", "Exile Cards from Hand", () -> gc.cheat().exileCardsFromHand());
        final boolean lands = gc.canPlayUnlimitedLands();
        addCheat(out, table, "lblUnlimitedLands", "Play Unlimited Lands", lands ? ": on" : ": off",
                () -> gc.cheat().setCanPlayUnlimitedLands(!lands));
        final boolean all = gc.mayLookAtAllCards();
        addCheat(out, table, "lblViewAll", "View All Cards", all ? ": on" : ": off",
                () -> gc.cheat().setViewAllCards(!all));
        return out;
    }

    private static void addCheat(final java.util.Map<String, Runnable> out, final TableScreen table,
                                 final String key, final String fallback, final Runnable cheat) {
        addCheat(out, table, key, fallback, "", cheat);
    }

    private static void addCheat(final java.util.Map<String, Runnable> out, final TableScreen table,
                                 final String key, final String fallback, final String suffix, final Runnable cheat) {
        String label;
        try {
            label = forge.util.Localizer.getInstance().getMessageorUseDefault(key, fallback);
        } catch (final RuntimeException e) {
            label = fallback;
        }
        final String name = label + suffix;
        out.put(name, () -> {
            table.getMenuOverlay().hide();
            NeoDuelBridge.log("dev mode: " + name);
            forge.util.ThreadUtil.invokeInGameThread(() -> {
                try {
                    cheat.run();
                } catch (final RuntimeException e) {
                    NeoDuelBridge.log("dev mode: " + name + " failed: " + e);
                }
            });
        });
    }
}
