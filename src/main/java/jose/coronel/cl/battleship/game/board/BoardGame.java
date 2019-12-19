package jose.coronel.cl.battleship.game.board;

import java.util.Arrays;

public class BoardGame {

    private Zone[][] zones;
    private int points;

    public static final int NUMBER_OF_ROWS = 9;
    public static final int NUMBER_OF_COLUMNS = 9;
    private final int TOTAL_POINTS_TO_WIN = 20;


    public BoardGame() {}

    public void createNewBoardGame() {
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
    }


    public void recordFailedImpact(int row, int column) {
        zones[row][column].recordFailedImpact();
    }

    public boolean wasAttacked(int row, int column){
        return !zones[row][column].pendingToAttack();
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