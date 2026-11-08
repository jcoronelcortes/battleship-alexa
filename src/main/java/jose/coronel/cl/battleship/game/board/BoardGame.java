package jose.coronel.cl.battleship.game.board;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * A 9x9 board. Alexa's board holds her ships; the player's board records Alexa's shots
 * and the AI memory (hits, candidate shots, sunk ships).
 * Getters and setters are used to keep the game in the Alexa session (JSON).
 */
public class BoardGame {

    public static final int NUMBER_OF_ROWS = 9;
    public static final int NUMBER_OF_COLUMNS = 9;

    private Zone[][] zones;
    private int points;
    /** Candidate shots for the medium/advanced AI. */
    private List<Attack> nextAttack = new ArrayList<>();
    /** Hits on the ship Alexa is currently hunting (cleared when it sinks). */
    private List<Attack> impacts = new ArrayList<>();
    /** Last hit. */
    private Attack impact = new Attack(-1, -1);
    private Set<ShipType> sunkShips = EnumSet.noneOf(ShipType.class);

    public BoardGame() {
    }

    public static BoardGame empty() {
        BoardGame board = new BoardGame();
        board.zones = new Zone[NUMBER_OF_ROWS][NUMBER_OF_COLUMNS];
        for (int row = 0; row < NUMBER_OF_ROWS; row++) {
            for (int column = 0; column < NUMBER_OF_COLUMNS; column++) {
                board.zones[row][column] = new Zone();
            }
        }
        return board;
    }

    // ---------- shots ----------

    public void recordImpact(int row, int column) {
        zones[row][column].recordImpact();
        points++;
        Attack attack = new Attack(row, column);
        impacts.add(attack);
        impact = attack;
    }

    public void recordFailedImpact(int row, int column) {
        zones[row][column].recordFailedImpact();
    }

    public boolean wasAttacked(int row, int column) {
        return !zones[row][column].pendingToAttack();
    }

    public boolean isImpact(int row, int column) {
        return zones[row][column].impacted();
    }

    public boolean isSunk(int row, int column) {
        return zones[row][column].sunk();
    }

    /** An afloat ship could be here: not attacked yet, or a hit that is not part of a sunk ship. */
    public boolean canHoldShip(int row, int column) {
        return zones[row][column].canHoldShip();
    }

    /** All 17 ship cells were hit. */
    public boolean allShipsHit() {
        return points >= ShipType.TOTAL_CELLS;
    }

    // ---------- ships ----------

    public void addShip(Ship ship) {
        for (int i = 0; i < ship.getLength(); i++) {
            int row = ship.getDirection() == Ship.VERTICAL ? ship.getRow() + i : ship.getRow();
            int column = ship.getDirection() == Ship.HORIZONTAL ? ship.getColumn() + i : ship.getColumn();
            zones[row][column].placeShip(ship.getIdentifier(), ship.getLength());
        }
    }

    public boolean hasShip(int row, int column) {
        return zones[row][column].isHasShip();
    }

    public int shipIdentifierAt(int row, int column) {
        return zones[row][column].getIdentifyShip();
    }

    /**
     * The player says Alexa sank a ship with her shot on [row, column].
     * @param type the ship, or null when unknown (only the hit is recorded)
     * @return how many of the player's ships are still afloat
     */
    public int markSunk(ShipType type, int row, int column) {
        recordImpact(row, column);
        ShipType sunk = type != null ? type : guessSunkShip(row, column).orElse(null);
        if (sunk != null) {
            sunkShips.add(sunk);
            markShipCellsSunk(sunk.length(), row, column);
        }
        nextAttack.clear();
        impacts.clear();
        return pendingShips();
    }

    /**
     * Finds the line of hits that was the sunk ship (it contains the final shot) and marks it SUNK,
     * so those cells stop counting as possible ships and hits on other ships are kept.
     * Only when exactly one line fits: then it is certainly right. With touching ships several lines
     * may fit; guessing could hide a ship that is still afloat, so the hits are left as they are.
     */
    private void markShipCellsSunk(int length, int row, int column) {
        int[] found = null;
        for (int[] d : new int[][]{{0, 1}, {1, 0}}) {
            for (int offset = 0; offset < length; offset++) {
                int startRow = row - d[0] * offset;
                int startColumn = column - d[1] * offset;
                if (lineOfHits(startRow, startColumn, d, length)) {
                    if (found != null) {
                        return; // ambiguous
                    }
                    found = new int[]{startRow, startColumn, d[0], d[1]};
                }
            }
        }
        if (found != null) {
            for (int i = 0; i < length; i++) {
                zones[found[0] + found[2] * i][found[1] + found[3] * i].recordSunk();
            }
        }
    }

    /** Number of consecutive hits on afloat ships through [row, column] along a direction. */
    private int hitRun(int row, int column, int[] d) {
        int run = 1;
        for (int sign : new int[]{1, -1}) {
            int r = row + sign * d[0];
            int c = column + sign * d[1];
            while (inside(r, c) && zones[r][c].impacted()) {
                run++;
                r += sign * d[0];
                c += sign * d[1];
            }
        }
        return run;
    }

    private boolean lineOfHits(int row, int column, int[] d, int length) {
        for (int i = 0; i < length; i++) {
            int r = row + d[0] * i;
            int c = column + d[1] * i;
            if (!inside(r, c) || !zones[r][c].impacted()) {
                return false;
            }
        }
        return true;
    }

    private static boolean inside(int row, int column) {
        return row >= 0 && row < NUMBER_OF_ROWS && column >= 0 && column < NUMBER_OF_COLUMNS;
    }

    public boolean isAfloat(ShipType type) {
        return !sunkShips.contains(type);
    }

    /** True if some ship of that length is still afloat. */
    public boolean anyAfloatWithLength(int length) {
        for (ShipType type : ShipType.values()) {
            if (type.length() == length && isAfloat(type)) {
                return true;
            }
        }
        return false;
    }

    public int pendingShips() {
        return ShipType.values().length - sunkShips.size();
    }

    /**
     * When the player only says "sunk" after the final shot on [row, column] (already recorded as a hit):
     * the afloat ship whose length is closest to the longest line of hits through that cell,
     * never longer than that line if a shorter ship fits.
     */
    public Optional<ShipType> guessSunkShip(int row, int column) {
        int line = Math.max(hitRun(row, column, new int[]{0, 1}), hitRun(row, column, new int[]{1, 0}));
        ShipType best = null;
        for (ShipType type : ShipType.values()) {
            if (isAfloat(type) && type.length() <= line && (best == null || type.length() > best.length())) {
                best = type;
            }
        }
        return best != null ? Optional.of(best) : closestAfloat(line);
    }

    private Optional<ShipType> closestAfloat(int length) {
        ShipType closest = null;
        for (ShipType type : ShipType.values()) {
            if (isAfloat(type) && (closest == null
                    || Math.abs(type.length() - length) < Math.abs(closest.length() - length))) {
                closest = type;
            }
        }
        return Optional.ofNullable(closest);
    }

    // ---------- AI candidate shots ----------

    public void addNewNextAttacks(List<Attack> newNextAttacks) {
        nextAttack.addAll(newNextAttacks);
    }

    /** Removes candidates that were already attacked, and duplicates. */
    public void pruneAttackedNextAttacks() {
        List<Attack> unique = new ArrayList<>(new LinkedHashSet<>(nextAttack));
        unique.removeIf(attack -> wasAttacked(attack.getRow(), attack.getColumn()));
        nextAttack = unique;
    }

    /**
     * Medium mode: takes the next shot from the candidate list.
     * After two or more hits in a line it prefers candidates that continue that line.
     * @return the next shot, or null when there are no candidates left
     */
    public Attack removeNextAttack(Random random) {
        pruneAttackedNextAttacks();
        if (nextAttack.isEmpty()) {
            return null;
        }
        List<Attack> inLine = new ArrayList<>();
        if (impacts.size() >= 2) {
            Attack first = impacts.get(0);
            Attack last = impacts.get(impacts.size() - 1);
            boolean sameRow = first.getRow() == last.getRow();
            boolean sameColumn = first.getColumn() == last.getColumn();
            for (Attack attack : nextAttack) {
                if ((sameRow && attack.getRow() == first.getRow())
                        || (sameColumn && attack.getColumn() == first.getColumn())) {
                    inLine.add(attack);
                }
            }
        }
        List<Attack> pool = inLine.isEmpty() ? nextAttack : inLine;
        Attack chosen = pool.get(random.nextInt(pool.size()));
        nextAttack.remove(chosen);
        return chosen;
    }

    /** Board as text for debug logs: 0 pending, I hit, S sunk, F fail. */
    public String toDebugString() {
        StringBuilder text = new StringBuilder();
        for (Zone[] row : zones) {
            for (Zone zone : row) {
                text.append(zone.impacted() ? " I" : zone.sunk() ? " S" : zone.pendingToAttack() ? " 0" : " F");
            }
            text.append('\n');
        }
        return text.toString();
    }

    // ---------- JSON (Alexa session) ----------

    public Zone[][] getZones() {
        return zones;
    }

    public void setZones(Zone[][] zones) {
        this.zones = zones;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public List<Attack> getNextAttack() {
        return nextAttack;
    }

    public void setNextAttack(List<Attack> nextAttack) {
        this.nextAttack = nextAttack == null ? new ArrayList<>() : nextAttack;
    }

    public List<Attack> getImpacts() {
        return impacts;
    }

    public void setImpacts(List<Attack> impacts) {
        this.impacts = impacts == null ? new ArrayList<>() : impacts;
    }

    public Attack getImpact() {
        return impact;
    }

    public void setImpact(Attack impact) {
        this.impact = impact;
    }

    public Set<ShipType> getSunkShips() {
        return sunkShips;
    }

    public void setSunkShips(Set<ShipType> sunkShips) {
        this.sunkShips = sunkShips == null || sunkShips.isEmpty()
                ? EnumSet.noneOf(ShipType.class) : EnumSet.copyOf(sunkShips);
    }

}
