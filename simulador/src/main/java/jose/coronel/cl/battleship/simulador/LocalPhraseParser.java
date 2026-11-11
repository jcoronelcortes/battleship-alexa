package jose.coronel.cl.battleship.simulador;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LocalPhraseParser {

    record Command(String intent, Map<String, String> slots) {
    }

    private static final Pattern LEVEL = Pattern.compile("\\b(?:modo(?: de juego)?|nivel)\\s+(basico|facil|medio|avanzado|dificil)\\b");
    private static final Pattern COORDINATE = Pattern.compile("\\b(a|b|be|c|ce|d|de|e|f|efe|g|ge|h|hache|i)\\s*(uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|[1-9])\\b");
    private static final Map<String, String> ROWS = Map.ofEntries(
            Map.entry("a", "A"), Map.entry("b", "B"), Map.entry("be", "B"),
            Map.entry("c", "C"), Map.entry("ce", "C"), Map.entry("d", "D"),
            Map.entry("de", "D"), Map.entry("e", "E"), Map.entry("f", "F"),
            Map.entry("efe", "F"), Map.entry("g", "G"), Map.entry("ge", "G"),
            Map.entry("h", "H"), Map.entry("hache", "H"), Map.entry("i", "I"));
    private static final Map<String, String> COLUMNS = Map.ofEntries(
            Map.entry("uno", "1"), Map.entry("dos", "2"), Map.entry("tres", "3"),
            Map.entry("cuatro", "4"), Map.entry("cinco", "5"), Map.entry("seis", "6"),
            Map.entry("siete", "7"), Map.entry("ocho", "8"), Map.entry("nueve", "9"));
    private static final Set<String> START_PHRASES = Set.of(
            "comenzar juego", "comienza juego", "comenzar partida", "comenzar una partida",
            "empezar juego", "empieza juego", "empezar partida", "iniciar juego",
            "inicia juego", "nueva partida", "nuevo juego");
    private static final Map<String, String> SHIPS = Map.of(
            "destructor", "destructor", "submarino", "submarino", "crucero", "crucero",
            "acorazado", "acorazado", "portaaviones", "portaaviones");

    private LocalPhraseParser() {
    }

    static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[¿?¡!,.;:]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .replaceFirst("^alexa\\s+", "");
    }

    static boolean isLaunchPhrase(String text) {
        String phrase = normalize(text);
        return Set.of("abre batalla naval", "abrir batalla naval").contains(phrase);
    }

    static Command parse(String text) {
        String phrase = normalize(text);
        if (START_PHRASES.contains(phrase)) {
            return command("ComenzarJuegoIntent");
        }

        Matcher level = LEVEL.matcher(phrase);
        if (level.find()) {
            String value = switch (level.group(1)) {
                case "basico", "facil" -> "básico";
                case "medio" -> "medio";
                default -> "avanzado";
            };
            return new Command("ModoDeJuegoIntent", Map.of("nivel", value));
        }

        Matcher coordinate = COORDINATE.matcher(phrase.replaceAll(
                "\\b(disparar|dispara|disparo|atacar|ataca|ataque|fila|columna|casilla|la|por favor)\\b", " "));
        if (coordinate.find()) {
            String column = COLUMNS.getOrDefault(coordinate.group(2), coordinate.group(2));
            return new Command("AtaqueJugadorIntent",
                    Map.of("Fila", ROWS.get(coordinate.group(1)), "Columna", column));
        }

        if (Set.of("al agua", "agua").contains(phrase)) {
            return command("AlAguaAlexaIntent");
        }
        if (Set.of("tocado", "impacto").contains(phrase)) {
            return command("ImpactoAlexaIntent");
        }
        if (Set.of("hundido", "hundiste mi barco").contains(phrase)
                || phrase.startsWith("hundiste mi ") || phrase.startsWith("hundiste el ")
                || phrase.startsWith("hundido ")) {
            String ship = SHIPS.keySet().stream().filter(phrase::endsWith).findFirst().orElse(null);
            return ship == null
                    ? command("DestruidoIntent")
                    : new Command("DestruidoIntent", Map.of("barco", SHIPS.get(ship)));
        }
        if (Set.of("ayuda", "ayudame").contains(phrase)) {
            return command("AMAZON.HelpIntent");
        }
        if (Set.of("tu ganas", "me rindo", "rendirme", "rendirse").contains(phrase)) {
            return command("AlexaGanaEljuegoIntent");
        }

        throw new IllegalArgumentException("No reconocí esa instrucción. Prueba «comenzar juego», «modo avanzado», "
                + "«disparo a C cinco», «al agua», «tocado» o «hundiste mi submarino».");
    }

    private static Command command(String intent) {
        return new Command(intent, Map.of());
    }
}
