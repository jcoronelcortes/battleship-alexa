package jose.coronel.cl.battleship.game;

import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.game.board.Attack;
import jose.coronel.cl.battleship.game.board.ShipType;
import jose.coronel.cl.battleship.session.GameSession;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Alexa plays full games against random player boards in each difficulty level.
 * The game is serialized to JSON between every step, like the real Alexa session does.
 */
class AlexaAiSimulationTest {

    private static final int SIZE = 9;
    private static final int GAMES = 300;
    private static final int[] LENGTHS = {2, 3, 3, 4, 5};
    private static final int TOTAL_SHIP_CELLS = 17;

    private final Random random = new Random(2026);

    @Test
    void basicModeFinishesEveryGameWithoutRepeatingShots() {
        double average = averageShots(Game.BASIC_MODE);
        System.out.printf("BASIC    average shots to win: %.1f%n", average);
    }

    @Test
    void mediumModeFinishesEveryGameWithoutRepeatingShots() {
        double average = averageShots(Game.MEDIUM_MODE);
        System.out.printf("MEDIUM   average shots to win: %.1f%n", average);
    }

    @Test
    void advancedModeFinishesEveryGameWithoutRepeatingShots() {
        double average = averageShots(Game.ADVANCE_MODE);
        System.out.printf("ADVANCED average shots to win: %.1f%n", average);
    }

    @Test
    void harderLevelsNeedFewerShots() {
        double basic = averageShots(Game.BASIC_MODE);
        double medium = averageShots(Game.MEDIUM_MODE);
        double advanced = averageShots(Game.ADVANCE_MODE);
        assertTrue(medium < basic, "medium (" + medium + ") should beat basic (" + basic + ")");
        assertTrue(advanced < medium, "advanced (" + advanced + ") should beat medium (" + medium + ")");
    }

    /**
     * Benchmark, only on request:  mvn test -Dtest=AlexaAiSimulationTest#benchmark -Dai.benchmark=true
     * Same boards for every level (seeded), for players who name the sunk ship and players who only say "sunk".
     */
    @Test
    void benchmark() {
        Assumptions.assumeTrue(Boolean.getBoolean("ai.benchmark"));
        int games = Integer.getInteger("ai.games", 3000);
        for (boolean named : new boolean[]{true, false}) {
            for (int mode : Integer.getInteger("ai.mode", -1) >= 0 ? new int[]{Integer.getInteger("ai.mode")} : new int[]{Game.BASIC_MODE, Game.MEDIUM_MODE, Game.ADVANCE_MODE}) {
                Random boards = new Random(42);
                int[] shots = new int[games];
                for (int i = 0; i < games; i++) {
                    shots[i] = playGame(mode, randomPlayerBoard(boards), named);
                }
                Arrays.sort(shots);
                System.out.printf("BENCH %-8s %-14s mean %.2f  median %d  p90 %d  best %d  worst %d%n",
                        new String[]{"BASIC", "MEDIUM", "ADVANCED"}[mode], named ? "names ship" : "says 'sunk'",
                        Arrays.stream(shots).average().orElse(0), shots[games / 2], shots[games * 9 / 10],
                        shots[0], shots[games - 1]);
            }
        }
    }

    private double averageShots(int mode) {
        long total = 0;
        for (int i = 0; i < GAMES; i++) {
            total += playGame(mode, randomPlayerBoard(random), true);
        }
        return (double) total / GAMES;
    }

    /** @return number of shots Alexa needed to sink the whole fleet */
    private static int playGame(int mode, int[][] ships, boolean nameSunkShip) {
        int[] cellsLeft = LENGTHS.clone();
        boolean[][] attacked = new boolean[SIZE][SIZE];
        int hits = 0;

        BattleshipGame engine = new BattleshipGame();
        Game game = engine.startGame();
        game.setGameMode(mode);

        for (int shot = 1; shot <= SIZE * SIZE; shot++) {
            game = roundTrip(game);
            Attack attack = engine.generateAlexaAttack(game);
            int row = attack.getRow();
            int column = attack.getColumn();
            assertFalse(attacked[row][column],
                    "mode " + mode + " repeated shot [" + row + "," + column + "] on shot " + shot);
            attacked[row][column] = true;

            game = roundTrip(game);
            int ship = ships[row][column];
            if (ship < 0) {
                // Player says "you fail"
                engine.recordAlexaMiss(game, row, column);
            } else {
                hits++;
                cellsLeft[ship]--;
                if (cellsLeft[ship] == 0) {
                    // Player says "destroyed <ship>"
                    engine.recordAlexaSunk(game, nameSunkShip ? ShipType.values()[ship] : null, row, column);
                } else {
                    // Player says "impact"
                    engine.recordAlexaHit(game, row, column);
                }
                if (hits == TOTAL_SHIP_CELLS) {
                    return shot;
                }
            }
        }
        return fail("mode " + mode + " did not sink the fleet in " + SIZE * SIZE + " shots");
    }

    private static Game roundTrip(Game game) {
        return GameSession.fromJson(GameSession.toJson(game));
    }

    /** Random non-overlapping placement. Cell value is the ship index, or -1 for water. */
    private static int[][] randomPlayerBoard(Random random) {
        int[][] board = new int[SIZE][SIZE];
        for (int[] row : board) {
            java.util.Arrays.fill(row, -1);
        }
        for (int ship = 0; ship < LENGTHS.length; ship++) {
            int length = LENGTHS[ship];
            while (true) {
                boolean horizontal = random.nextBoolean();
                int row = random.nextInt(horizontal ? SIZE : SIZE - length + 1);
                int column = random.nextInt(horizontal ? SIZE - length + 1 : SIZE);
                boolean free = true;
                for (int i = 0; i < length; i++) {
                    if (board[horizontal ? row : row + i][horizontal ? column + i : column] >= 0) {
                        free = false;
                        break;
                    }
                }
                if (free) {
                    for (int i = 0; i < length; i++) {
                        board[horizontal ? row : row + i][horizontal ? column + i : column] = ship;
                    }
                    break;
                }
            }
        }
        return board;
    }

}
