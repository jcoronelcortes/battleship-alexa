package jose.coronel.cl.battleship.game;

import jose.coronel.cl.battleship.game.ai.AdvancedStrategy;
import jose.coronel.cl.battleship.game.board.Attack;
import jose.coronel.cl.battleship.game.board.BoardGame;
import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.game.board.ShipType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Golden master for Alexa's medium and advanced strategies.
 *
 * 300 reproducible game situations (own seeded generator, independent of production code) are
 * replayed through the game engine. For each one we record what the AI computes: the candidate
 * shots list and the hunt-mode probability matrix. The expected file was generated BEFORE the
 * 2026 refactor, so any change in AI behaviour makes this test fail.
 *
 * Regenerate only on purpose: mvn test -Dtest=AiGoldenMasterTest -Dgolden.write=true
 */
class AiGoldenMasterTest {

    private static final Path GOLDEN = Path.of("src/test/resources/golden/ai-golden.txt");
    private static final int SCENARIOS = 300;
    private static final int[] LENGTHS = {2, 3, 3, 4, 5};
    private static final String[] TYPES = {"DESTROYER", "SUBMARINE", "CRUISER", "BATTLESHIP", "AIRCRAFT CARRIER"};

    @Test
    void aiOutputMatchesGoldenMaster() throws IOException {
        List<String> actual = new ArrayList<>();
        for (int seed = 0; seed < SCENARIOS; seed++) {
            List<int[]> shots = scenario(seed);
            actual.add(seed + "|MEDIUM|" + replay(shots, Game.MEDIUM_MODE));
            actual.add(seed + "|ADVANCED|" + replay(shots, Game.ADVANCE_MODE));
        }
        if (Boolean.getBoolean("golden.write")) {
            Files.createDirectories(GOLDEN.getParent());
            Files.write(GOLDEN, actual, StandardCharsets.UTF_8);
        }
        List<String> expected = Files.readAllLines(GOLDEN, StandardCharsets.UTF_8);
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), actual.get(i), "AI behaviour changed in scenario line " + i);
        }
    }

    // ---------- replay through the engine (the only part that may change with the API) ----------

    private static String replay(List<int[]> shots, int mode) {
        BattleshipGame engine = new BattleshipGame();
        Game game = Game.empty();
        game.setGameMode(mode);
        BoardGame board = game.getBoardGamePlayer();

        for (int[] shot : shots) {
            int row = shot[0], column = shot[1], kind = shot[2];
            if (kind == FAIL) {
                engine.recordAlexaMiss(game, row, column);
            } else if (kind == HIT) {
                engine.recordAlexaHit(game, row, column);
            } else {
                engine.recordAlexaSunk(game, ShipType.valueOf(TYPES[shot[3]].replace(' ', '_')), row, column);
            }
        }
        board.pruneAttackedNextAttacks();
        String next = board.getNextAttack().stream()
                .map(a -> a.getRow() + "," + a.getColumn()).collect(Collectors.joining(" "));

        String pdf = "";
        if (mode == Game.ADVANCE_MODE) {
            pdf = Arrays.stream(AdvancedStrategy.huntProbabilities(board)).map(Arrays::toString).collect(Collectors.joining(""));
        }
        return next + "|" + pdf;
    }

    // ---------- scenario generator (independent of production code) ----------

    private static final int FAIL = 0, HIT = 1, SUNK = 2;

    /** Shots on a random fleet: {row, column, FAIL|HIT|SUNK, shipIndex}. */
    private static List<int[]> scenario(int seed) {
        Random random = new Random(seed);
        int[][] ships = new int[9][9];
        for (int[] row : ships) {
            Arrays.fill(row, -1);
        }
        for (int ship = 0; ship < LENGTHS.length; ship++) {
            while (true) {
                boolean horizontal = random.nextBoolean();
                int r = random.nextInt(horizontal ? 9 : 9 - LENGTHS[ship] + 1);
                int c = random.nextInt(horizontal ? 9 - LENGTHS[ship] + 1 : 9);
                boolean free = true;
                for (int i = 0; i < LENGTHS[ship]; i++) {
                    free &= ships[horizontal ? r : r + i][horizontal ? c + i : c] < 0;
                }
                if (free) {
                    for (int i = 0; i < LENGTHS[ship]; i++) {
                        ships[horizontal ? r : r + i][horizontal ? c + i : c] = ship;
                    }
                    break;
                }
            }
        }
        int[] left = LENGTHS.clone();
        boolean[][] shot = new boolean[9][9];
        List<int[]> shots = new ArrayList<>();
        int count = random.nextInt(55);
        int hits = 0;
        for (int i = 0; i < count && hits < 16; i++) {
            int r, c;
            do {
                r = random.nextInt(9);
                c = random.nextInt(9);
            } while (shot[r][c]);
            shot[r][c] = true;
            int ship = ships[r][c];
            if (ship < 0) {
                shots.add(new int[]{r, c, FAIL, -1});
            } else {
                hits++;
                left[ship]--;
                shots.add(new int[]{r, c, left[ship] == 0 ? SUNK : HIT, ship});
            }
        }
        return shots;
    }

    @SuppressWarnings("unused")
    private static String describe(Attack a) {
        return a.getRow() + "," + a.getColumn();
    }

}
