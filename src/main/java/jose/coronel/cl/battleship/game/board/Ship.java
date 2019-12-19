package jose.coronel.cl.battleship.game.board;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class Ship {

    private static Logger log = getLogger(Ship.class);

    private int identifier;
    private int row;
    private int column;
    private int length;
    private int direction;
    private String typeOfShip;
    private int lifePoints;

    public static final int UNASSIGNED_DIRECTION = -1;

    public Ship(){}

    public Ship(int identifier, int length) {
        this.identifier = identifier;
        this.length = length;
        this.lifePoints = length;
        this.typeOfShip = typeOfShip(length);
        this.row = -1;
        this.column = -1;
        this.direction = UNASSIGNED_DIRECTION;
        log.info("Creating ship with length : {} - type : {} - identifier : {} ", length, typeOfShip, identifier);
    }

    private String typeOfShip(int length){
        switch (length){
            case 1:
                return "Submarine";
            case 2:
                return "Destroyer";
            case 3:
                return "Cruise";
            default:
                return "Battleship";
        }
    }

    public void reduceOneLifePoint(){
        this.lifePoints = lifePoints -1;
    }

    public boolean statusDestroyed(){
        return (lifePoints <= 0) ? true : false;
    }

    public boolean hasAnAssignedZone() {
        return (row == -1 || column == -1) ? false : true;
    }


    public boolean hasAnAssignedDirection() {
        return (direction == UNASSIGNED_DIRECTION) ? false : true;
    }


    public void defineZone(int row, int column) {
        this.row = row;
        this.column = column;
    }


    public int getIdentifier() {
        return identifier;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public String getTypeOfShip() {
        return typeOfShip;
    }

    public void setTypeOfShip(String typeOfShip) {
        this.typeOfShip = typeOfShip;
    }

    public int getLifePoints() {
        return lifePoints;
    }

    public void setLifePoints(int lifePoints) {
        this.lifePoints = lifePoints;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Ship{");
        sb.append("identifier=").append(identifier);
        sb.append(", row=").append(row);
        sb.append(", column=").append(column);
        sb.append(", length=").append(length);
        sb.append(", direction=").append(direction);
        sb.append(", typeOfShip='").append(typeOfShip).append('\'');
        sb.append(", lifePoints=").append(lifePoints);
        sb.append('}');
        return sb.toString();
    }
}