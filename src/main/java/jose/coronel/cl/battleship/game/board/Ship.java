package jose.coronel.cl.battleship.game.board;

/** One of Alexa's ships. Getters and setters are used to keep the game in the Alexa session (JSON). */
public class Ship {

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    private int identifier;
    private int row = -1;
    private int column = -1;
    private int length;
    private int direction = HORIZONTAL;
    private String typeOfShip;
    private int lifePoints;

    public Ship() {
    }

    public Ship(ShipType type, int row, int column, int direction) {
        this.identifier = type.ordinal();
        this.length = type.length();
        this.lifePoints = type.length();
        this.typeOfShip = type.label();
        this.row = row;
        this.column = column;
        this.direction = direction;
    }

    /** Records a hit. @return true when this hit sank the ship */
    public boolean hit() {
        lifePoints--;
        return lifePoints <= 0;
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

}
