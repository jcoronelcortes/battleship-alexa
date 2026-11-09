package jose.coronel.cl.battleship.simulador;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

/** The player's ships on the bottom board, placed at random, used to answer Alexa's shots automatically. */
public class PlayerFleet {

    static final String[] NAMES = {"destructor", "submarino", "crucero", "acorazado", "portaaviones"};
    static final int[] LENGTHS = {2, 3, 3, 4, 5};
    private static final int SIZE = 9;

    /** Ship index per cell, -1 for water. */
    private final int[][] cells;

    private PlayerFleet(int[][] cells) {
        this.cells = cells;
    }

    public static PlayerFleet random(Random random) {
        int[][] cells = new int[SIZE][SIZE];
        for (int[] row : cells) {
            Arrays.fill(row, -1);
        }
        for (int ship = 0; ship < LENGTHS.length; ship++) {
            int length = LENGTHS[ship];
            while (true) {
                boolean horizontal = random.nextBoolean();
                int row = random.nextInt(horizontal ? SIZE : SIZE - length + 1);
                int column = random.nextInt(horizontal ? SIZE - length + 1 : SIZE);
                boolean free = true;
                for (int i = 0; i < length; i++) {
                    free &= cells[horizontal ? row : row + i][horizontal ? column + i : column] < 0;
                }
                if (free) {
                    for (int i = 0; i < length; i++) {
                        cells[horizontal ? row : row + i][horizontal ? column + i : column] = ship;
                    }
                    break;
                }
            }
        }
        return new PlayerFleet(cells);
    }

    public int[][] cells() {
        return cells;
    }

    public record Answer(String intent, Map<String, String> slots, String spoken) {
    }

    /**
     * What the player must answer to Alexa's shot on [row, column].
     * @param attacked cells Alexa had already shot before this one
     */
    public Answer answer(int row, int column, boolean[][] attacked) {
        int ship = cells[row][column];
        if (ship < 0) {
            return new Answer("AlAguaAlexaIntent", Map.of(), "al agua");
        }
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (cells[r][c] == ship && !(r == row && c == column) && !attacked[r][c]) {
                    return new Answer("ImpactoAlexaIntent", Map.of(), "tocado");
                }
            }
        }
        return new Answer("DestruidoIntent", Map.of("barco", NAMES[ship]), "hundiste mi " + NAMES[ship]);
    }

}
