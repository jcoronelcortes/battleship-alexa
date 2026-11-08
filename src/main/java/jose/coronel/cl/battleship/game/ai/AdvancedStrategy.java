package jose.coronel.cl.battleship.game.ai;

import jose.coronel.cl.battleship.game.board.Attack;
import jose.coronel.cl.battleship.game.board.BoardGame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Advanced level, probability density:
 * <ul>
 *   <li>Hunt: count how many ways each afloat ship could fit over every cell and shoot the most likely one.</li>
 *   <li>Destroy (after a hit): score the cells around the last hit, giving more weight to lines that
 *       already have several hits, and keep the best candidates ordered by score.</li>
 * </ul>
 * The scoring is the 2020 algorithm, rewritten with direction vectors; AiGoldenMasterTest checks it
 * gives exactly the same numbers.
 */
public class AdvancedStrategy implements AttackStrategy {

    private static final int SIZE = BoardGame.NUMBER_OF_ROWS;
    private static final int[] LENGTHS = {2, 3, 4, 5};
    /** Ship placements from the hit: towards left, right, up, down. */
    private static final int[][] PLACEMENT_DIRECTIONS = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
    /** Candidate cells from the hit: up, right, down, left. */
    private static final int[][] CANDIDATE_DIRECTIONS = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};

    @Override
    public Attack nextShot(BoardGame playerBoard, Random random) {
        playerBoard.pruneAttackedNextAttacks();
        if (!playerBoard.getNextAttack().isEmpty()) {
            return playerBoard.getNextAttack().remove(0);
        }
        return mostLikelyCell(playerBoard, huntProbabilities(playerBoard), random);
    }

    @Override
    public void onHit(BoardGame playerBoard, int row, int column) {
        updateCandidatesAfterHit(playerBoard);
    }

    // ---------- hunt mode ----------

    /** For every cell, in how many ways an afloat ship could cover it. */
    public static int[][] huntProbabilities(BoardGame board) {
        int[][] pdf = new int[SIZE][SIZE];
        for (int length : LENGTHS) {
            if (!board.anyAfloatWithLength(length)) {
                continue;
            }
            for (int[] d : new int[][]{{1, 0}, {0, 1}}) {
                for (int row = 0; row + d[0] * (length - 1) < SIZE; row++) {
                    for (int column = 0; column + d[1] * (length - 1) < SIZE; column++) {
                        if (fits(board, row, column, d, length)) {
                            for (int i = 0; i < length; i++) {
                                pdf[row + d[0] * i][column + d[1] * i]++;
                            }
                        }
                    }
                }
            }
        }
        return pdf;
    }

    private static Attack mostLikelyCell(BoardGame board, int[][] pdf, Random random) {
        int max = 0;
        List<Attack> best = new ArrayList<>();
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                if (board.wasAttacked(row, column)) {
                    continue;
                }
                if (pdf[row][column] > max) {
                    max = pdf[row][column];
                    best.clear();
                }
                if (pdf[row][column] == max) {
                    best.add(new Attack(row, column));
                }
            }
        }
        return best.get(random.nextInt(best.size()));
    }

    // ---------- destroy mode ----------

    /** After a hit: recompute the candidate list around the last hit, best first. */
    static void updateCandidatesAfterHit(BoardGame board) {
        Attack hit = board.getImpact();
        int[][] pdf = new int[SIZE][SIZE];
        int maxShipLength = 0;
        for (int length : LENGTHS) {
            if (board.anyAfloatWithLength(length)) {
                scorePlacementsThroughHit(board, pdf, hit.getRow(), hit.getColumn(), length);
                maxShipLength = length;
            }
        }

        List<Attack> newCandidates = new ArrayList<>();
        for (int[] d : CANDIDATE_DIRECTIONS) {
            Attack candidate = firstFreeCellAfterHits(board, hit, d, maxShipLength);
            if (candidate != null && pdf[candidate.getRow()][candidate.getColumn()] > 0) {
                newCandidates.add(candidate);
            }
        }
        if (newCandidates.isEmpty()) {
            return;
        }

        // New and previous candidates, grouped by score, highest score first
        Map<Integer, Set<Attack>> byScore = new HashMap<>();
        for (Attack attack : newCandidates) {
            byScore.computeIfAbsent(pdf[attack.getRow()][attack.getColumn()], k -> new HashSet<>()).add(attack);
        }
        for (Attack attack : board.getNextAttack()) {
            byScore.computeIfAbsent(pdf[attack.getRow()][attack.getColumn()], k -> new HashSet<>()).add(attack);
        }
        List<Integer> scores = new ArrayList<>(byScore.keySet());
        scores.sort(Collections.reverseOrder());
        board.getNextAttack().clear();
        for (Integer score : scores) {
            board.getNextAttack().addAll(byScore.get(score));
        }
    }

    /**
     * Adds 1 to each cell of every placement of a ship of this length that starts at the hit and
     * extends left, right, up or down. If that line already has several hits (inside the placement
     * or right behind the hit), the placement is multiplied by the number of hits.
     */
    private static void scorePlacementsThroughHit(BoardGame board, int[][] pdf, int row, int column, int length) {
        for (int[] d : PLACEMENT_DIRECTIONS) {
            int endRow = row + d[0] * (length - 1);
            int endColumn = column + d[1] * (length - 1);
            if (!inside(endRow, endColumn) || !fits(board, row, column, d, length)) {
                continue;
            }
            int hits = 0;
            for (int i = 0; i < length; i++) {
                int r = row + d[0] * i;
                int c = column + d[1] * i;
                if (board.isImpact(r, c)) {
                    pdf[r][c] = 0;
                    hits++;
                } else {
                    pdf[r][c]++;
                }
            }
            // Hits right behind the hit, in the opposite direction, belong to the same line
            for (int r = row - d[0], c = column - d[1]; inside(r, c) && board.isImpact(r, c); r -= d[0], c -= d[1]) {
                hits++;
            }
            if (hits > 1) {
                for (int i = 0; i < length; i++) {
                    pdf[row + d[0] * i][column + d[1] * i] *= hits;
                }
            }
        }
    }

    /** Walks from the hit over consecutive hits; returns the first cell that is not a hit if it is still free. */
    private static Attack firstFreeCellAfterHits(BoardGame board, Attack hit, int[] d, int maxShipLength) {
        for (int step = 0; step < maxShipLength; step++) {
            int r = hit.getRow() + d[0] * step;
            int c = hit.getColumn() + d[1] * step;
            if (!inside(r, c)) {
                return null;
            }
            if (!board.isImpact(r, c)) {
                return board.wasAttacked(r, c) ? null : new Attack(r, c);
            }
        }
        return null;
    }

    private static boolean fits(BoardGame board, int row, int column, int[] d, int length) {
        for (int i = 0; i < length; i++) {
            if (!board.canHoldShip(row + d[0] * i, column + d[1] * i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean inside(int row, int column) {
        return row >= 0 && row < SIZE && column >= 0 && column < SIZE;
    }

}
