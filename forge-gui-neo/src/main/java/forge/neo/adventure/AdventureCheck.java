package forge.neo.adventure;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * La Aventura sin ventana: lo que se rompe EN SILENCIO al rebasar con Forge.
 *
 * <p>Para meter nuestros combates y nuestro editor en el Adventure se copian dos
 * clases de Forge ({@code DuelScene} y {@code DeckEditScene}) con un bloque
 * anyadido, y la copia va antes en el classpath. Si Card-Forge cambia el
 * original, el rebase no da ningun conflicto -nuestra copia no es su fichero- y
 * seguiriamos jugando con su version VIEJA sin que nada fallara. Por eso aqui se
 * guarda la huella del original que se copio, y en cuanto cambie esto se pone en
 * rojo: hay que volver a copiar su version nueva y reponer el bloque NEOFORGE.
 *
 * <p>Comprueba ademas que las copias son las que se cargan (el orden del
 * classpath) y que el lanzador del Adventure esta.
 *
 * <p>{@code java -cp ... forge.neo.adventure.AdventureCheck}, desde
 * {@code forge-gui-neo} (lo lanza {@code tools/comprobar-todo.py}).
 */
public final class AdventureCheck {

    private AdventureCheck() {
    }

    private static int ok;
    private static int bad;

    public static void main(final String[] args) throws Exception {
        System.out.println("== Aventura: las copias de Forge siguen al dia ==");
        // The Forsaken Realms fusion: no shadow copies. TFR's own scenes call the ExternalTable hooks.
        check("the Adventure launcher (forge.app.Main) is on the classpath",
                resource("forge/app/Main.class") != null);
        check("the ExternalTable hooks are on the classpath",
                resource("forge/adventure/scene/ExternalTable.class") != null);
        check("TFR's DuelScene offers the duel to ExternalTable.duel",
                mobileSource("DuelScene").contains("externalTable.play("));
        check("TFR's DeckEditScene offers the deck to ExternalTable.deckEditor",
                mobileSource("DeckEditScene").contains("externalEditor.open("));
        check("AdventureNeoMain sets both hooks",
                neoSource("forge/neo/adventure/AdventureNeoMain.java").contains("ExternalTable.duel =")
                        && neoSource("forge/neo/adventure/AdventureNeoMain.java").contains("ExternalTable.deckEditor ="));
        duelHasAWayOut();
        ledgerKeepsOrder();
        System.out.println();
        System.out.println(ok + " comprobaciones OK, " + bad + " fallos");
        System.exit(bad == 0 ? 0 : 1);
    }

    /**
     * Del duelo de la Aventura se tiene que poder salir <b>viendolo</b>.
     *
     * <p>Aqui no hay tutorial que ensenye Escape — se entra desde el mapa de
     * Forge, en otro proceso — asi que la unica salida tiene que estar a la
     * vista. Reportado en Reddit el 22-09-2026: <i>"I don't seem to find a
     * concede button in the battle screen for adventure mode"</i>.
     *
     * <p>Se comprueba leyendo el codigo, como el resto de este fichero: montar
     * una escena de JavaFX sin ventana para mirar un boton costaria mas que lo
     * que protege, y lo que se rompe en silencio es que alguien quite la
     * llamada al reordenar la pantalla.
     */
    private static void duelHasAWayOut() {
        final String duel = neoSource("forge/neo/adventure/DuelControls.java");
        check("el duelo enciende el boton de la pausa (no solo Escape)",
                duel.contains("enablePauseButton("));
        check("y abre el menu de la Aventura: sin Reiniciar, y salir es rendirse",
                duel.contains("PauseMenu.forAdventure("));
        for (final String key : new String[] {"table.menu", "adventure.concede",
                "adventure.concede.ask", "adventure.concede.detail", "adventure.concede.yes"}) {
            final List<String> missing = languagesWithout(key);
            check("\"" + key + "\" esta en los diez idiomas"
                    + (missing.isEmpty() ? "" : " -> falta en " + missing),
                    missing.isEmpty());
        }
    }

    /**
     * El registro de "cuando conseguiste cada carta" ({@link AcquiredLedger}).
     *
     * <p>Lo que se rompe sin avisar es lo de la carga: si cargar una partida
     * contara como conseguir, TODA la coleccion saldria "nueva" cada vez, y el
     * orden no serviria de nada — sin un solo error.
     */
    private static void ledgerKeepsOrder() {
        final AcquiredLedger.Book book = new AcquiredLedger.Book();
        book.update(java.util.Map.of("sol ring", 1, "island", 10), true, 100);
        check("registro: lo que habia al cargar no tiene hora",
                book.stampOf("sol ring") == 0 && book.stampOf("island") == 0);
        book.update(java.util.Map.of("sol ring", 1, "island", 10, "counterspell", 1), false, 200);
        check("registro: una carta nueva lleva su hora", book.stampOf("counterspell") == 200);
        book.update(java.util.Map.of("sol ring", 2, "island", 10, "counterspell", 1), false, 300);
        check("registro: otra copia de una que ya tenias tambien cuenta",
                book.stampOf("sol ring") == 300 && book.stampOf("counterspell") == 200);
        book.update(java.util.Map.of("sol ring", 2, "island", 10), false, 400);
        book.update(java.util.Map.of("sol ring", 2, "island", 10, "counterspell", 1), false, 500);
        check("registro: vendida y vuelta a conseguir, es nueva", book.stampOf("counterspell") == 500);
        book.update(java.util.Map.of("sol ring", 5, "island", 30, "counterspell", 3), true, 600);
        check("registro: cargar otra partida no pone horas",
                book.stampOf("sol ring") == 300 && book.stampOf("island") == 0);
        for (final String key : new String[] {"deck.newest", "deck.newest.tip"}) {
            final List<String> missing = languagesWithout(key);
            check("\"" + key + "\" esta en los diez idiomas"
                    + (missing.isEmpty() ? "" : " -> falta en " + missing), missing.isEmpty());
        }
    }

    /** Los idiomas a los que les falta esa clave. */
    private static List<String> languagesWithout(final String key) {
        final List<String> missing = new ArrayList<>();
        final File dir = new File("src/main/resources/forge/neo/lang");
        final File[] files = dir.listFiles((d, n) -> n.startsWith("neo-") && n.endsWith(".properties"));
        if (files == null || files.length == 0) {
            return List.of("(no esta " + dir + ")");
        }
        for (final File f : files) {
            try {
                final Properties p = new Properties();
                try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                    p.load(r);
                }
                final String value = p.getProperty(key);
                if (value == null || value.isBlank()) {
                    missing.add(f.getName());
                }
            } catch (final Exception e) {
                missing.add(f.getName() + " (" + e + ")");
            }
        }
        return missing;
    }

    /** Un fichero de codigo NUESTRO, leido tal cual. */
    private static String neoSource(final String path) {
        try {
            return Files.readString(new File("src/main/java/" + path).toPath());
        } catch (final Exception e) {
            return "";
        }
    }

    /** A scene of TFR's Adventure, read as is. Empty when the source is not next to this module. */
    private static String mobileSource(final String name) {
        try {
            return Files.readString(new File("../forge-gui-mobile/src/forge/adventure/scene/" + name + ".java").toPath());
        } catch (final Exception e) {
            return "";
        }
    }

    private static URL resource(final String path) {
        return AdventureCheck.class.getClassLoader().getResource(path);
    }

    private static void check(final String what, final boolean pass) {
        if (pass) {
            ok++;
            System.out.println("  [OK] " + what);
        } else {
            bad++;
            System.out.println("  [MAL] " + what);
        }
    }
}
