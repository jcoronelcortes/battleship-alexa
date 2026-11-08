package jose.coronel.cl.battleship.game.board;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import static org.slf4j.LoggerFactory.getLogger;

public class BoardGame {

    private static Logger log = getLogger(BoardGame.class);

    private Zone[][] zones;
    private int points;

    public static final int NUMBER_OF_ROWS = 9;
    public static final int NUMBER_OF_COLUMNS = 9;
    private final int TOTAL_POINTS_TO_WIN = 17;

    private List<Attack> nextAttack;
    private List<Attack> impacts;
    private Attack impact;

    private int[][] pdfMatrix = {{0},{0}};

    private boolean destroyerNotDestroyed;
    private boolean submarineNotDestroyed;
    private boolean cruiserNotDestroyed;
    private boolean battleshipNotDestroyed;
    private boolean aircraftCarrierNotDestroyed;

    public BoardGame() {}

    public void createNewBoardGame() {
        destroyerNotDestroyed = true;
        submarineNotDestroyed = true;
        cruiserNotDestroyed = true;
        battleshipNotDestroyed = true;
        aircraftCarrierNotDestroyed = true;
        nextAttack = new ArrayList<>();
        impacts = new ArrayList<>();
        impact = new Attack(-1, -1);
        zones = new Zone[NUMBER_OF_ROWS][NUMBER_OF_COLUMNS];

        for (int fila = 0; fila < zones.length; fila++) {
            for (int columna = 0; columna < zones[fila].length; columna++) {
                Zone zone = new Zone();
                zone.createNewZone();
                zones[fila][columna] = zone;
            }
        }
    }


    public void recordImpact(int row, int column) {
        zones[row][column].recordImpact();
        points = points + 1;
        Attack attack = new Attack(row, column);
        impacts.add(attack);
        impact = attack;
    }


    public void recordFailedImpact(int row, int column) {
        zones[row][column].recordFailedImpact();
    }

    public boolean wasAttacked(int row, int column){
        return !zones[row][column].pendingToAttack();
    }

    public boolean validateProbabilityDensityFunctionZone(int row, int column){
        return zones[row][column].validateProbabilityDensityFunctionStatus();
    }

    public boolean validateImpactStatusForPDF(int row, int column){
        return zones[row][column].validateImpactStatus();
    }
    public boolean hasShip(int row, int column){
        return zones[row][column].isHasShip();
    }

    public int getImpactedShipIdentifier(int row, int column){
        return zones[row][column].getIdentifyShip();
    }

    public boolean alreadyLostTheGame() {
        return (points >= TOTAL_POINTS_TO_WIN) ? true : false;
    }

    public void addShip(Ship ship) {
        int row = ship.getRow();
        int column = ship.getColumn();
        int length = ship.getLength();
        int direction = ship.getDirection();
        int shipIdentifier = ship.getIdentifier();
        
        // 0 : Horizontal
        // 1 : Vertical
        if (direction == 0) {
            for (int i = column; i < column+length; i++) {
                zones[row][i].setHasShip(true);
                zones[row][i].setShipLength(length);
                zones[row][i].setIdentifyShip(shipIdentifier);
            }
        }
        else if (direction == 1) {
            for (int i = row; i < row+length; i++) {
                zones[i][column].setHasShip(true);
                zones[i][column].setShipLength(length);
                zones[i][column].setIdentifyShip(shipIdentifier);
            }
        }
    }


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

    /**
     * Medium mode: takes the next shot from the candidate list.
     * After two or more hits in a line it prefers candidates that continue that line.
     * @return the next shot, or null when there are no candidates left
     */
    public Attack removeNextAttack() {
        pruneAttackedNextAttacks();
        log.info("Get next position to attack. Number of candidates : {}", nextAttack.size());
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
        Attack chosen = pool.get(new Random().nextInt(pool.size()));
        nextAttack.remove(chosen);
        return chosen;
    }

    /** Removes candidates that were already attacked, and duplicates. */
    public void pruneAttackedNextAttacks() {
        List<Attack> unique = new ArrayList<>(new LinkedHashSet<>(nextAttack));
        unique.removeIf(attack -> wasAttacked(attack.getRow(), attack.getColumn()));
        nextAttack = unique;
    }

    public void addNewNextAttacks(List<Attack> newNextAttacks) {
        nextAttack.addAll(newNextAttacks);
    }

    public void setNextAttack(List<Attack> nextAttacks) {
        if (nextAttacks != null){
            this.nextAttack = nextAttacks;
        }
    }

    public void cleanNextAttacks(){
        nextAttack.clear();
    }

    public List<Attack> getImpacts() {
        return impacts;
    }

    public void setImpacts(List<Attack> impacts) {
        if (impacts != null){
            this.impacts = impacts;
        }
    }

    public int updateZonesToDestroyedAndCleanImpacts(Game game, int rowImpact, int columnImpact, String shipType){
        log.info("Save destroy [{}, {}]", rowImpact, columnImpact);
        switch(shipType) {
            case "DESTRUCTOR":
            case "DESTROYER":
                log.info("Set Destroyer to sink status");
                game.getBoardGamePlayer().setDestroyerNotDestroyed(false);
                break;
            case "SUBMARINO":
            case "SUBMARINE":
                log.info("Set Submarine to sink status");
                game.getBoardGamePlayer().setSubmarineNotDestroyed(false);
                break;
            case "CRUCERO":
            case "CRUISER":
                log.info("Set Cruiser to sink status");
                game.getBoardGamePlayer().setCruiserNotDestroyed(false);
                break;
            case "ACORAZADO":
            case "BATTLESHIP":
                log.info("Set Battleship to sink status");
                game.getBoardGamePlayer().setBattleshipNotDestroyed(false);
                break;
            case "PORTAAVIONES":
            case "AIRCRAFT CARRIER":
                log.info("Set Aircraft Carrier to sink status");
                game.getBoardGamePlayer().setAircraftCarrierNotDestroyed(false);
        }
        game.getBoardGamePlayer().recordImpact(rowImpact, columnImpact);

        //Calculate new possibles attacks position
        //ProbabilityDensityFunctionStrategy.generateNewAttackFromPdfOnDestroyMode(game.getBoardGamePlayer());
        game.getBoardGamePlayer().cleanNextAttacks();
        game.getBoardGamePlayer().getImpacts().clear();

        return game.getBoardGamePlayer().numberOfPendingShipsToDestroy();
    }


    public int numberOfPendingShipsToDestroy(){
        int total = 0;
        if(destroyerNotDestroyed){
            total = total + 1;
        }
        if(submarineNotDestroyed){
            total = total + 1;
        }
        if(cruiserNotDestroyed){
            total = total + 1;
        }
        if(battleshipNotDestroyed){
            total = total + 1;
        }
        if(aircraftCarrierNotDestroyed){
            total = total + 1;
        }
        return total;
    }


    public int[][] getPdfMatrix() {
        return pdfMatrix;
    }

    public void setPdfMatrix(int[][] pdfMatrix) {
        this.pdfMatrix = pdfMatrix;
    }

    public Attack getImpact() {
        return impact;
    }

    public void setImpact(Attack impact) {
        this.impact = impact;
    }


    public boolean isDestroyerNotDestroyed() {
        return destroyerNotDestroyed;
    }

    public void setDestroyerNotDestroyed(boolean destroyerNotDestroyed) {
        this.destroyerNotDestroyed = destroyerNotDestroyed;
    }

    public boolean isSubmarineNotDestroyed() {
        return submarineNotDestroyed;
    }

    public void setSubmarineNotDestroyed(boolean submarineNotDestroyed) {
        this.submarineNotDestroyed = submarineNotDestroyed;
    }

    public boolean isCruiserNotDestroyed() {
        return cruiserNotDestroyed;
    }

    public void setCruiserNotDestroyed(boolean cruiserNotDestroyed) {
        this.cruiserNotDestroyed = cruiserNotDestroyed;
    }

    public boolean isBattleshipNotDestroyed() {
        return battleshipNotDestroyed;
    }

    public void setBattleshipNotDestroyed(boolean battleshipNotDestroyed) {
        this.battleshipNotDestroyed = battleshipNotDestroyed;
    }

    public boolean isAircraftCarrierNotDestroyed() {
        return aircraftCarrierNotDestroyed;
    }

    public void setAircraftCarrierNotDestroyed(boolean aircraftCarrierNotDestroyed) {
        this.aircraftCarrierNotDestroyed = aircraftCarrierNotDestroyed;
    }

    public boolean validateShipLengthTwoNotDestroyed() {
        return destroyerNotDestroyed;
    }

    public boolean validateShipLengthThreeNotDestroyed() {
        if(!submarineNotDestroyed && !cruiserNotDestroyed){
            return false;
        }
        else{
            return true;
        }
    }

    public boolean validateShipLengthFourNotDestroyed() {
        return battleshipNotDestroyed;
    }

    public boolean validateShipLengthFiveNotDestroyed() {
        return aircraftCarrierNotDestroyed;
    }


    public void printBoardGame(){
        log.info("********************************");
        log.info("*    BOARD GAME");
        log.info("********************************");
        int size = zones.length;
        for(int row=0; row < size; row++){
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i <  7 * size ; i++){
                sb.append("-");
            }
            sb.append("-");
            log.info(sb.toString());

            sb = new StringBuilder();
            for(int i = 1; i <= zones[row].length; i++){
                switch(zones[row][i - 1].getStatus()) {
                    case 0:
                        sb.append("|   0  ");
                        break;
                    case 1:
                        sb.append("|   I  ");
                        break;
                    case 2:
                        sb.append("|   F  ");
                        break;
                    case 3:
                        sb.append("|   D  ");
                        break;
                }
            }
            sb.append("|");
            log.info(sb.toString());

            if(row == size -  1){
                sb = new StringBuilder();

                for(int i = 0; i <  7 * size ; i++){
                    sb.append("-");
                }
                sb.append("-");
                log.info(sb.toString());
            }
        }
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BoardGame{");
        sb.append("zones=").append(Arrays.toString(zones));
        sb.append(", points=").append(points);
        sb.append(", TOTAL_POINTS_TO_WIN=").append(TOTAL_POINTS_TO_WIN);
        sb.append('}');
        return sb.toString();
    }
}