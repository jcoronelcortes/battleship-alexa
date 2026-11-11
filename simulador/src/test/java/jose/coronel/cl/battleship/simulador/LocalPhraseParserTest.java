package jose.coronel.cl.battleship.simulador;

import jose.coronel.cl.battleship.simulador.LocalPhraseParser.Command;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phrases typed or dictated in local mode, turned into the same intents as the Spanish model. */
class LocalPhraseParserTest {

    @Test
    void normalizeRemovesAccentsPunctuationAndWakeWord() {
        assertEquals("disparo a c cinco", LocalPhraseParser.normalize("  Alexa, ¡Disparo a C cinco!  "));
        assertEquals("modo basico", LocalPhraseParser.normalize("Modo BÁSICO."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abre batalla naval", "Alexa, abre Batalla Naval", "abrir batalla naval."})
    void launchPhrases(String text) {
        assertTrue(LocalPhraseParser.isLaunchPhrase(text));
    }

    @Test
    void otherPhrasesAreNotLaunch() {
        assertFalse(LocalPhraseParser.isLaunchPhrase("comenzar juego"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"comenzar juego", "Nueva partida", "empezar juego"})
    void startGame(String text) {
        assertEquals(new Command("ComenzarJuegoIntent", Map.of()), LocalPhraseParser.parse(text));
    }

    @ParameterizedTest
    @CsvSource({
            "modo básico, básico",
            "modo fácil, básico",
            "nivel medio, medio",
            "modo de juego avanzado, avanzado",
            "modo difícil, avanzado"})
    void gameLevel(String text, String level) {
        assertEquals(new Command("ModoDeJuegoIntent", Map.of("nivel", level)), LocalPhraseParser.parse(text));
    }

    @ParameterizedTest
    @CsvSource({
            "disparo a C cinco, C, 5",
            "ataca la ce cinco, C, 5",
            "disparar a A uno, A, 1",
            "dispara a hache ocho, H, 8",
            "fila efe columna nueve, F, 9",
            "ataque en la fila B columna 3, B, 3",
            "I9, I, 9",
            "de dos, D, 2",
            "ge siete por favor, G, 7"})
    void playerAttack(String text, String row, String column) {
        assertEquals(new Command("AtaqueJugadorIntent", Map.of("Fila", row, "Columna", column)),
                LocalPhraseParser.parse(text));
    }

    @ParameterizedTest
    @CsvSource({
            "al agua, AlAguaAlexaIntent",
            "Agua, AlAguaAlexaIntent",
            "tocado, ImpactoAlexaIntent",
            "impacto, ImpactoAlexaIntent",
            "ayuda, AMAZON.HelpIntent",
            "ayúdame, AMAZON.HelpIntent",
            "me rindo, AlexaGanaEljuegoIntent",
            "tú ganas, AlexaGanaEljuegoIntent"})
    void answersAndCommands(String text, String intent) {
        assertEquals(new Command(intent, Map.of()), LocalPhraseParser.parse(text));
    }

    @ParameterizedTest
    @ValueSource(strings = {"hundido", "hundiste mi barco"})
    void sunkWithoutShipName(String text) {
        assertEquals(new Command("DestruidoIntent", Map.of()), LocalPhraseParser.parse(text));
    }

    @ParameterizedTest
    @CsvSource({
            "hundiste mi crucero, crucero",
            "hundiste el submarino, submarino",
            "hundido portaaviones, portaaviones",
            "Hundiste mi destructor, destructor"})
    void sunkWithShipName(String text, String ship) {
        assertEquals(new Command("DestruidoIntent", Map.of("barco", ship)), LocalPhraseParser.parse(text));
    }

    @ParameterizedTest
    @ValueSource(strings = {"una frase desconocida", "disparo a C diez", "disparo a J cinco", ""})
    void unknownPhrasesExplainWhatIsRecognized(String text) {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> LocalPhraseParser.parse(text));
        assertTrue(error.getMessage().contains("comenzar juego"));
    }
}
