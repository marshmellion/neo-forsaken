package forge.neo.adventure;

import forge.interfaces.IDevModeCheats;
import forge.interfaces.IGameController;
import forge.localinstance.properties.ForgePreferences;
import forge.neo.match.NeoMatchUI;
import forge.neo.ui.TableScreen;
import forge.neo.ui.UiScale;
import forge.util.ThreadUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * The Forsaken Realms fusion: Forge's Dev Mode for a duel on NeoForge's table.
 *
 * <p>Every cheat of Forge's own dev menu (IDevModeCheats: the mobile VDevMenu and the desktop Dev
 * menu), reached three ways: the "Dev" button beside the log button, F2, and the pause menu. Only
 * when Dev Mode is on (the Adventure's Settings).
 *
 * <p>Easier to use than a flat menu: the cheats are grouped, a filter box has the focus when the panel
 * opens (type a few letters, Enter runs the first match), the two switches show their state and
 * leave the panel open, and the last cheat used is offered again at the top.
 *
 * <p>A cheat asks its questions through the match UI, so it runs on the game thread, as VDevMenu
 * does, never on JavaFX's.
 */
public final class DevPanel {

    private DevPanel() {
    }

    /** One cheat. A switch has {@code state}; the others close the panel when used. */
    private static final class Cheat {
        final String group;
        final String name;
        final Consumer<IDevModeCheats> action;
        final BooleanSupplier state;
        final Consumer<Boolean> set;

        Cheat(final String group, final String key, final String fallback, final Consumer<IDevModeCheats> action) {
            this.group = group;
            this.name = text(key, fallback);
            this.action = action;
            this.state = null;
            this.set = null;
        }

        Cheat(final String group, final String key, final String fallback, final BooleanSupplier state,
              final Consumer<Boolean> set) {
            this.group = group;
            this.name = text(key, fallback);
            this.action = null;
            this.state = state;
            this.set = set;
        }
    }

    /** The last cheat used, by name: offered again at the top of the panel. */
    private static String lastUsed;

    /** The duel in progress, for the test hooks. JavaFX thread only. */
    private static TableScreen currentTable;
    private static NeoMatchUI currentUi;

    static void remember(final TableScreen table, final NeoMatchUI ui) {
        currentTable = table;
        currentUi = ui;
    }

    /** Dev Mode is on and this duel has a controller to cheat through. */
    static boolean available(final NeoMatchUI ui) {
        return ForgePreferences.DEV_MODE && ui != null;
    }

    private static List<Cheat> cheats(final IGameController gc) {
        final List<Cheat> c = new ArrayList<>();
        final String cards = "Cards";
        c.add(new Cheat(cards, "lblCardToHand", "Add Card to Hand", IDevModeCheats::addCardToHand));
        c.add(new Cheat(cards, "lblCardToBattlefield", "Add Card to Battlefield", IDevModeCheats::addCardToBattlefield));
        c.add(new Cheat(cards, "lblTokenToBattlefield", "Add Token to Battlefield", IDevModeCheats::addTokenToBattlefield));
        c.add(new Cheat(cards, "lblCardToLibrary", "Add Card to Library", IDevModeCheats::addCardToLibrary));
        c.add(new Cheat(cards, "lblCardToGraveyard", "Add Card to Graveyard", IDevModeCheats::addCardToGraveyard));
        c.add(new Cheat(cards, "lblCardToExile", "Add Card to Exile", IDevModeCheats::addCardToExile));
        c.add(new Cheat(cards, "lblRepeatAddCard", "Repeat Last Add Card", IDevModeCheats::repeatLastAddition));
        c.add(new Cheat(cards, "lblTutor", "Tutor for Card", IDevModeCheats::tutorForCard));
        c.add(new Cheat(cards, "lblCastSpellOrPlayLand", "Cast Spell/Play Land", IDevModeCheats::castASpell));
        final String board = "Board";
        c.add(new Cheat(board, "lblGenerateMana", "Generate Mana", IDevModeCheats::generateMana));
        c.add(new Cheat(board, "lblTapPermanent", "Tap Permanents", IDevModeCheats::tapPermanents));
        c.add(new Cheat(board, "lblUntapPermanent", "Untap Permanents", IDevModeCheats::untapPermanents));
        c.add(new Cheat(board, "lblAddCounterPermanent", "Add Counters to Card", IDevModeCheats::addCountersToPermanent));
        c.add(new Cheat(board, "lblSubCounterPermanent", "Sub Counters from Card", IDevModeCheats::removeCountersFromPermanent));
        c.add(new Cheat(board, "lblExileFromPlay", "Exile Card from Play", IDevModeCheats::exileCardsFromBattlefield));
        c.add(new Cheat(board, "lblExileFromHand", "Exile Card from Hand", IDevModeCheats::exileCardsFromHand));
        c.add(new Cheat(board, "lblRemoveFromGame", "Remove Card from Game", IDevModeCheats::removeCardsFromGame));
        final String game = "Game";
        c.add(new Cheat(game, "lblSetLife", "Set Player Life", IDevModeCheats::setPlayerLife));
        c.add(new Cheat(game, "lblWinGame", "Win Game", IDevModeCheats::winGame));
        c.add(new Cheat(game, "lblDumpGame", "Save Game State", IDevModeCheats::dumpGameState));
        c.add(new Cheat(game, "lblSetupGame", "Load Game State", IDevModeCheats::setupGameState));
        c.add(new Cheat(game, "lblAskAI", "Ask AI for suggestion", ch -> ch.askAI(false)));
        c.add(new Cheat(game, "lblAskSimulationAI", "Ask Simulation AI for suggestion", ch -> ch.askAI(true)));
        c.add(new Cheat(game, "lblRiggedRoll", "Rigged Planar Roll", IDevModeCheats::riggedPlanarRoll));
        c.add(new Cheat(game, "lblWalkTo", "Planeswalk to", IDevModeCheats::planeswalkTo));
        final String switches = "Switches";
        c.add(new Cheat(switches, "lblUnlimitedLands", "Play Unlimited Lands", gc::canPlayUnlimitedLands,
                on -> gc.cheat().setCanPlayUnlimitedLands(on)));
        c.add(new Cheat(switches, "lblViewAll", "View All Cards", gc::mayLookAtAllCards,
                on -> gc.cheat().setViewAllCards(on)));
        return c;
    }

    /** From the JavaFX thread: the panel, over the table. */
    static void open(final TableScreen table, final NeoMatchUI ui) {
        final IGameController gc = ui.getGameController();
        if (gc == null) {
            NeoDuelBridge.log("dev mode: this duel has no controller yet");
            return;
        }
        final List<Cheat> all = cheats(gc);

        final VBox box = new VBox(10);
        box.getStyleClass().addAll("dialog", "pause-menu");
        box.setPadding(new Insets(20, 26, 20, 26));
        box.setAlignment(Pos.TOP_CENTER);
        box.setMaxWidth(Region.USE_PREF_SIZE);
        box.setMaxHeight(Region.USE_PREF_SIZE);

        final Label title = new Label("Dev mode");
        title.getStyleClass().add("dialog-title");
        final Label hint = new Label("Type to filter, Enter runs the first match. F2 or Esc closes.");
        hint.getStyleClass().add("home-subtitle");

        final TextField filter = new TextField();
        filter.setPromptText("Filter");
        filter.setMaxWidth(Double.MAX_VALUE);

        final VBox list = new VBox(8);
        final ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportWidth(UiScale.px(640));
        scroll.setPrefViewportHeight(UiScale.px(400));
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        final Runnable[] rebuild = new Runnable[1];
        final Button[] first = new Button[1];
        rebuild[0] = () -> {
            list.getChildren().clear();
            first[0] = null;
            final String q = filter.getText() == null ? "" : filter.getText().trim().toLowerCase(Locale.ROOT);
            if (q.isEmpty() && lastUsed != null) {
                for (final Cheat ch : all) {
                    if (ch.name.equals(lastUsed)) {
                        list.getChildren().add(section("Again"));
                        list.getChildren().add(tiles(List.of(button(ch, table, gc, rebuild[0]))));
                    }
                }
            }
            String group = null;
            List<Button> row = new ArrayList<>();
            for (final Cheat ch : all) {
                if (!q.isEmpty() && !ch.name.toLowerCase(Locale.ROOT).contains(q)) {
                    continue;
                }
                if (!ch.group.equals(group)) {
                    if (!row.isEmpty()) {
                        list.getChildren().add(tiles(row));
                        row = new ArrayList<>();
                    }
                    group = ch.group;
                    list.getChildren().add(section(group));
                }
                final Button b = button(ch, table, gc, rebuild[0]);
                if (first[0] == null) {
                    first[0] = b;
                }
                row.add(b);
            }
            if (!row.isEmpty()) {
                list.getChildren().add(tiles(row));
            }
            if (first[0] == null) {
                list.getChildren().add(section("Nothing matches \"" + q + "\""));
            }
        };
        filter.textProperty().addListener((o, a, b) -> rebuild[0].run());
        filter.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER && first[0] != null) {
                first[0].fire();
                ev.consume();
            }
        });
        rebuild[0].run();

        final Button close = new Button("Close");
        close.getStyleClass().addAll("btn-primary", "pause-item");
        close.setOnAction(e -> table.getMenuOverlay().hide());

        box.getChildren().addAll(title, hint, filter, scroll, close);
        table.getMenuOverlay().setOnBackgroundClick(table.getMenuOverlay()::hide);
        table.getMenuOverlay().show(box);
        filter.requestFocus();
    }

    private static Label section(final String text) {
        final Label l = new Label(text);
        l.getStyleClass().add("home-subtitle");
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private static TilePane tiles(final List<Button> buttons) {
        final TilePane t = new TilePane(6, 6);
        t.setPrefColumns(3);
        t.setPrefTileWidth(UiScale.px(204));
        t.getChildren().addAll(buttons);
        return t;
    }

    private static Button button(final Cheat ch, final TableScreen table, final IGameController gc,
                                 final Runnable rebuild) {
        final boolean isSwitch = ch.state != null;
        final Button b = new Button(isSwitch ? ch.name + ": " + (ch.state.getAsBoolean() ? "ON" : "off") : ch.name);
        b.getStyleClass().addAll(isSwitch && ch.state.getAsBoolean() ? "btn-primary" : "btn-secondary", "pause-item");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> {
            if (isSwitch) {
                final boolean on = !ch.state.getAsBoolean();
                NeoDuelBridge.log("dev mode: " + ch.name + " -> " + (on ? "on" : "off"));
                try {
                    ch.set.accept(on);
                } catch (final RuntimeException ex) {
                    NeoDuelBridge.log("dev mode: " + ch.name + " failed: " + ex);
                }
                rebuild.run(); // a switch leaves the panel open, with its new state
                return;
            }
            table.getMenuOverlay().hide();
            run(ch, gc);
        });
        return b;
    }

    private static void run(final Cheat ch, final IGameController gc) {
        lastUsed = ch.name;
        NeoDuelBridge.log("dev mode: " + ch.name);
        ThreadUtil.invokeInGameThread(() -> {
            try {
                ch.action.accept(gc.cheat());
            } catch (final RuntimeException ex) {
                NeoDuelBridge.log("dev mode: " + ch.name + " failed: " + ex);
                ex.printStackTrace();
            }
        });
    }

    private static String text(final String key, final String fallback) {
        try {
            return forge.util.Localizer.getInstance().getMessageorUseDefault(key, fallback);
        } catch (final RuntimeException e) {
            return fallback;
        }
    }

    /** Tests only (-Dneo.adventure.devOpen=true). */
    static void openForTest() {
        if (currentTable != null && available(currentUi)) {
            open(currentTable, currentUi);
        } else {
            NeoDuelBridge.log("dev mode: not available (Dev Mode off?)");
        }
    }

    /** Tests only (-Dneo.adventure.devRun=part of a cheat's name). */
    static void runForTest(final String part) {
        if (currentTable == null || !available(currentUi) || currentUi.getGameController() == null) {
            NeoDuelBridge.log("dev mode: not available (Dev Mode off?)");
            return;
        }
        for (final Cheat ch : cheats(currentUi.getGameController())) {
            if (ch.action != null && ch.name.toLowerCase(Locale.ROOT).contains(part.toLowerCase(Locale.ROOT))) {
                run(ch, currentUi.getGameController());
                return;
            }
        }
        NeoDuelBridge.log("dev mode: no cheat named like " + part);
    }
}
