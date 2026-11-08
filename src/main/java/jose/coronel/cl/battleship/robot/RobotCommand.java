package jose.coronel.cl.battleship.robot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A message for the EV3 robot, published as JSON on AWS IoT.
 * The field names and order are a contract with battleship-ev3/robot.py: do not rename them.
 */
@JsonPropertyOrder({"type", "command", "attackResult", "shipDestroyed", "row", "column", "alexaRow", "alexaColumn", "logZones"})
public class RobotCommand {

    // Commands
    public static final String START_GAME = "start_game";
    public static final String GAME_MODE_CONFIGURED = "game_mode_configured";
    public static final String PLAYER_ATTACK = "player_attack";
    public static final String FAIL_ATTACK = "fail_attack";
    public static final String ALEXA_IMPACT = "alexa_impact";
    public static final String ALEXA_SHIP_DESTROYED = "alexa_ship_destroyed";
    public static final String FINISH_GAME = "finish_game";
    public static final String TEST_MODE = "test_mode";

    // attackResult values
    public static final int IMPACT = 1;
    public static final int SHIP_DESTROYED = 2;
    public static final int FAIL = 3;
    public static final int PLAYER_WON = 4;
    public static final int DUPLICATE_ATTACK = 5;

    private final String type = "command";
    private String command;
    private int attackResult;
    private String shipDestroyed = "NO";
    private String row;
    private String column;
    private String alexaRow;
    private String alexaColumn;
    private String logZones;

    public RobotCommand() {
    }

    private RobotCommand(String command) {
        this.command = command;
    }

    public static RobotCommand of(String command) {
        return new RobotCommand(command);
    }

    /** Player's shot on Alexa's board; row and column are 0–8. */
    public static RobotCommand playerAttack(int row, int column) {
        RobotCommand attack = new RobotCommand(PLAYER_ATTACK);
        attack.row = String.valueOf(row);
        attack.column = String.valueOf(column);
        return attack;
    }

    public static RobotCommand testMode(String logZones) {
        RobotCommand test = new RobotCommand(TEST_MODE);
        test.logZones = logZones;
        return test;
    }

    @JsonIgnore
    public boolean isPlayerWon() {
        return attackResult == PLAYER_WON;
    }

    public String getType() {
        return type;
    }

    public String getCommand() {
        return command;
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

    public String getColumn() {
        return column;
    }

    public String getAlexaRow() {
        return alexaRow;
    }

    public String getAlexaColumn() {
        return alexaColumn;
    }

    /** Alexa's counter-attack, told to the robot for logging; row letter and column 1–9. */
    public void setAlexaAttack(String rowLetter, int columnNumber) {
        this.alexaRow = rowLetter;
        this.alexaColumn = String.valueOf(columnNumber);
    }

    public String getLogZones() {
        return logZones;
    }

}
