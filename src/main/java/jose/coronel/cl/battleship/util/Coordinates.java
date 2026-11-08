package jose.coronel.cl.battleship.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * Board coordinates as Alexa hears them: rows A–I, columns 1–9 (digits or words in English/Spanish).
 * Internally rows and columns are 0–8.
 */
public final class Coordinates {

    public static final int SIZE = 9;
    private static final String ROWS = "ABCDEFGHI";

    private static final Map<String, Integer> COLUMN_WORDS = Map.ofEntries(
            Map.entry("uno", 1), Map.entry("one", 1),
            Map.entry("dos", 2), Map.entry("two", 2),
            Map.entry("tres", 3), Map.entry("three", 3),
            Map.entry("cuatro", 4), Map.entry("four", 4),
            Map.entry("cinco", 5), Map.entry("five", 5),
            Map.entry("seis", 6), Map.entry("six", 6),
            Map.entry("siete", 7), Map.entry("seven", 7),
            Map.entry("ocho", 8), Map.entry("eight", 8),
            Map.entry("nueve", 9), Map.entry("nine", 9));

    private Coordinates() {
    }

    /**
     * "C", "c", "C." → 2. Spanish speakers often hear "I" as "Y", so "Y" is row I.
     * @return 0–8, or -1 if it is not a row
     */
    public static int rowFromSpoken(String spoken) {
        if (spoken == null || spoken.isBlank()) {
            return -1;
        }
        char letter = Character.toUpperCase(spoken.trim().charAt(0));
        if (letter == 'Y') {
            return 8;
        }
        return ROWS.indexOf(letter);
    }

    /**
     * "5", "cinco", "five" → 4.
     * @return 0–8, or -1 if it is not a column
     */
    public static int columnFromSpoken(String spoken) {
        if (spoken == null) {
            return -1;
        }
        String text = Normalizer.normalize(spoken, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .trim().toLowerCase(Locale.ROOT);
        Integer number = COLUMN_WORDS.get(text);
        if (number == null && text.matches("[1-9]")) {
            number = Integer.parseInt(text);
        }
        return number == null ? -1 : number - 1;
    }

    /** 2 → "C". */
    public static String rowLetter(int row) {
        return row >= 0 && row < SIZE ? String.valueOf(ROWS.charAt(row)) : "";
    }

    public static boolean isInside(int row, int column) {
        return row >= 0 && row < SIZE && column >= 0 && column < SIZE;
    }

}
