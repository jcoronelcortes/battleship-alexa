package jose.coronel.cl.battleship.game.board;

public class Zone {

    public static final int ATTACKED = 0;
    public static final int IMPACT = 1;
    public static final int FAIL = 2;


    private int identifyShip;
    private boolean hasShip;
    private int status;
    private int shipLength;
    

    public Zone() {}

    public void createNewZone() {
        identifyShip = 0;
        status = 0;
        hasShip = false;
        shipLength = -1;
    }


    public boolean pendingToAttack() {
        return (status == ATTACKED) ? true : false;
    }


    public void recordImpact() {
        this.status = IMPACT;
    }


    public void recordFailedImpact() {
        this.status = FAIL;
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


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Zone{");
        sb.append("identifyShip=").append(identifyShip);
        sb.append(", hasShip=").append(hasShip);
        sb.append(", status=").append(status);
        sb.append(", shipLength=").append(shipLength);
        sb.append('}');
        return sb.toString();
    }
}