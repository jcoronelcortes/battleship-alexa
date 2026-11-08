package jose.coronel.cl.battleship.game.board;

/** One cell of a board. Getters and setters are used to keep the game in the Alexa session (JSON). */
public class Zone {

    public static final int PENDING_ATTACK = 0;
    public static final int IMPACT = 1;
    public static final int FAIL = 2;

    private int identifyShip;
    private boolean hasShip;
    private int status = PENDING_ATTACK;
    private int shipLength = -1;

    public boolean pendingToAttack() {
        return status == PENDING_ATTACK;
    }

    /** A ship could still be here: not attacked yet, or already hit. */
    public boolean canHoldShip() {
        return status == PENDING_ATTACK || status == IMPACT;
    }

    public boolean impacted() {
        return status == IMPACT;
    }

    public void recordImpact() {
        status = IMPACT;
    }

    public void recordFailedImpact() {
        status = FAIL;
    }

    public void placeShip(int identifier, int length) {
        hasShip = true;
        identifyShip = identifier;
        shipLength = length;
    }

    public int getIdentifyShip() {
        return identifyShip;
    }

    public void setIdentifyShip(int identifyShip) {
        this.identifyShip = identifyShip;
    }

    public boolean isHasShip() {
        return hasShip;
    }

    public void setHasShip(boolean hasShip) {
        this.hasShip = hasShip;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getShipLength() {
        return shipLength;
    }

    public void setShipLength(int shipLength) {
        this.shipLength = shipLength;
    }

}
