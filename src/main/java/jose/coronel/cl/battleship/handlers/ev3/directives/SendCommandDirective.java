package jose.coronel.cl.battleship.handlers.ev3.directives;

public class SendCommandDirective {

    private String type;
    private String command;
    /*
     * 1 : Impact
     * 2 : Ship destroyed
     * 3 : Fail
     * 4 : The player has won
     */
    private int attackResult;
    private String shipDestroyed = "NO";
    private String row;
    private String column;
    private String logZones;


    public SendCommandDirective() {
    }

    public SendCommandDirective(String tipo, String command) {
        this.type = tipo;
        this.command = command;
    }

    public SendCommandDirective(String tipo, String command, String logZones) {
        this.type = tipo;
        this.command = command;
        this.logZones = logZones;
    }

    public SendCommandDirective(String type, String command, String row, String column) {
        this.type = type;
        this.command = command;
        this.row = row;
        this.column = column;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public int getAttackResult() {
        return attackResult;
    }

    public void setAttackResult(int attackResult) {
        this.attackResult = attackResult;
    }

    public String getShipDestroyed() {
        return shipDestroyed;
    }

    public void setShipDestroyed(String shipDestroyed) {
        this.shipDestroyed = shipDestroyed;
    }

    public String getRow() {
        return row;
    }

    public void setRow(String row) {
        this.row = row;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getLogZones() {
        return logZones;
    }

    public void setLogZones(String logZones) {
        this.logZones = logZones;
    }
}
