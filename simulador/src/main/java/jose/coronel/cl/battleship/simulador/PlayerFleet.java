package jose.coronel.cl.battleship.simulador;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The player's ships on the bottom board, placed by hand (clicks on the page) or at random.
 * Used to draw the ships and to answer Alexa's shots automatically.
 */
public class PlayerFleet {

    static final String[] NAMES = {"destructor", "submarino", "crucero", "acorazado", "portaaviones"};
    static final int[] LENGTHS = {2, 3, 3, 4, 5};
    private static final int SIZE = 9;

    /** Where a ship is: its first cell and direction. {@code ship} is the index in NAMES. */
    public record Placement(int ship, int row, int column, boolean horizontal) {

        public String name() {
            return 0 <= ship && ship < NAMES.length ? NAMES[ship] : "?";
        }

        public int length() {
            return 0 <= ship && ship < LENGTHS.length ? LENGTHS[ship] : 0;
        }
    }

    private final List<Placement> placements;
    /** Ship index per cell, -1 for water. */
    private final int[][] cells;

    private PlayerFleet(List<Placement> placements, int[][] cells) {
        this.placements = placements;
        this.cells = cells;
    }

    /**
     * Fleet placed by the player.
     * @throws IllegalArgumentException (message in Spanish, shown on the page) if it is not a valid fleet
     */
    public static PlayerFleet of(List<Placement> placements) {
        if (placements == null || placements.size() != NAMES.length) {
            throw new IllegalArgumentException("Ubica los 5 barcos antes de guardar la flota.");
        }
        int[][] cells = emptyCells();
        boolean[] seen = new boolean[NAMES.length];
        for (Placement p : placements) {
            if (p.ship() < 0 || p.ship() >= NAMES.length || seen[p.ship()]) {
                throw new IllegalArgumentException("Cada barco debe aparecer una sola vez.");
            }
            seen[p.ship()] = true;
            for (int i = 0; i < p.length(); i++) {
                int r = p.horizontal() ? p.row() : p.row() + i;
                int c = p.horizontal() ? p.column() + i : p.column();
                if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) {
                    throw new IllegalArgumentException("El " + p.name() + " se sale del tablero.");
                }
                if (cells[r][c] >= 0) {
                    throw new IllegalArgumentException("El " + p.name() + " se superpone con el " + NAMES[cells[r][c]] + ".");
                }
                cells[r][c] = p.ship();
            }
        }
        List<Placement> sorted = new ArrayList<>(placements);
        sorted.sort(Comparator.comparingInt(Placement::ship));
        return new PlayerFleet(List.copyOf(sorted), cells);
    }

    public static PlayerFleet random(Random random) {
        List<Placement> placements = new ArrayList<>();
        int[][] cells = emptyCells();
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
                    placements.add(new Placement(ship, row, column, horizontal));
                    break;
                }
            }
        }
        return new PlayerFleet(List.copyOf(placements), cells);
    }

    private static int[][] emptyCells() {
        int[][] cells = new int[SIZE][SIZE];
        for (int[] row : cells) {
            Arrays.fill(row, -1);
        }
        return cells;
    }

    public int[][] cells() {
        return cells;
    }

    public List<Placement> placements() {
        return placements;
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
