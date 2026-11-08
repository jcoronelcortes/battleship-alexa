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

    /** A ship could be here: the cell was not attacked yet, or it was a hit. */
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
        if (type != null) {
            sunkShips.add(type);
        }
        recordImpact(row, column);
        nextAttack.clear();
        impacts.clear();
        return pendingShips();
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
     * When the player only says "sunk", guess the ship from the hits since the last sunk ship
     * (plus the final shot): the afloat ship whose length is closest.
     */
    public Optional<ShipType> guessSunkShip() {
        int length = impacts.size() + 1;
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

    /** Board as text for debug logs: 0 pending, I impact, F fail. */
    public String toDebugString() {
        StringBuilder text = new StringBuilder();
        for (Zone[] row : zones) {
            for (Zone zone : row) {
                text.append(zone.impacted() ? " I" : zone.pendingToAttack() ? " 0" : " F");
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
