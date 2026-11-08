package jose.coronel.cl.battleship.game;

import jose.coronel.cl.battleship.game.board.BoardGame;
import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.util.GameModeUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;


class BattleshipGameTest {

    /*
    private static Logger log = getLogger(BattleshipGameTest.class);

    String[][] playerBoard;
    private int destroyer;
    private int submarine;
    private int cruiser;
    private int battleship;
    private int aircraftCarrier;


    public void initEach(){
        playerBoard = new String[BoardGame.NUMBER_OF_ROWS][BoardGame.NUMBER_OF_COLUMNS];
        for (int row = 0; row < BoardGame.NUMBER_OF_ROWS; row++) {
            for (int column = 0; column < BoardGame.NUMBER_OF_COLUMNS; column++) {
                playerBoard[row][column] = "";
            }
        }
        destroyer = 2;
        submarine = 3;
        cruiser = 3;
        battleship = 4;
        aircraftCarrier = 5;
    }

    @Test
    public void gameTestNoShipContact(){
        int finalResult = 0;
        int total = 10000;
        for (int i=1; i<= total; i++){
            initEach();
            //Destroyer
            playerBoard[7][1] = "D";
            playerBoard[7][2] = "D";

            //Submarine
            playerBoard[2][4] = "S";
            playerBoard[3][4] = "S";
            playerBoard[4][4] = "S";

            //Cruiser
            playerBoard[2][7] = "C";
            playerBoard[3][7] = "C";
            playerBoard[4][7] = "C";

            //Batleship
            playerBoard[7][4] = "B";
            playerBoard[7][5] = "B";
            playerBoard[7][6] = "B";
            playerBoard[7][7] = "B";

            //Aircraft Carrier
            playerBoard[1][1] = "A";
            playerBoard[2][1] = "A";
            playerBoard[3][1] = "A";
            playerBoard[4][1] = "A";
            playerBoard[5][1] = "A";
            finalResult = finalResult + executeGame();
        }
        log.info("******************************************");
        log.info("******************************************");
        log.info("* FINAL RESULT : " + finalResult/total);
        log.info("******************************************");
        log.info("******************************************");
    }

    @Test
    public void gameTestOneShipContact(){
        int finalResult = 0;
        int total = 10000;
        for (int i=1; i<= total; i++){
            initEach();
            //Destroyer
            playerBoard[7][1] = "D";
            playerBoard[7][2] = "D";

            //Submarine
            playerBoard[2][3] = "S";
            playerBoard[3][3] = "S";
            playerBoard[4][3] = "S";

            //Cruiser
            playerBoard[2][8] = "C";
            playerBoard[3][8] = "C";
            playerBoard[4][8] = "C";

            //Batleship
            playerBoard[5][3] = "B";
            playerBoard[5][4] = "B";
            playerBoard[5][5] = "B";
            playerBoard[5][6] = "B";

            //Aircraft Carrier
            playerBoard[1][1] = "A";
            playerBoard[2][1] = "A";
            playerBoard[3][1] = "A";
            playerBoard[4][1] = "A";
            playerBoard[5][1] = "A";
            finalResult = finalResult + executeGame();
        }
        log.info("******************************************");
        log.info("******************************************");
        log.info("* FINAL RESULT : " + finalResult/total);
        log.info("******************************************");
        log.info("******************************************");
    }

    @Test
    public void gameTestTwoShipContact(){
        int finalResult = 0;
        int total = 10000;
        for (int i=1; i<= total; i++){
            initEach();
            //Destroyer
            playerBoard[7][1] = "D";
            playerBoard[7][2] = "D";

            //Submarine
            playerBoard[1][3] = "S";
            playerBoard[2][3] = "S";
            playerBoard[3][3] = "S";

            //Cruiser
            playerBoard[1][6] = "C";
            playerBoard[2][6] = "C";
            playerBoard[3][6] = "C";

            //Batleship
            playerBoard[4][3] = "B";
            playerBoard[4][4] = "B";
            playerBoard[4][5] = "B";
            playerBoard[4][6] = "B";

            //Aircraft Carrier
            playerBoard[1][1] = "A";
            playerBoard[2][1] = "A";
            playerBoard[3][1] = "A";
            playerBoard[4][1] = "A";
            playerBoard[5][1] = "A";
            finalResult = finalResult + executeGame();
        }
        log.info("******************************************");
        log.info("******************************************");
        log.info("* FINAL RESULT : " + finalResult/total);
        log.info("******************************************");
        log.info("******************************************");
    }

    @Test
    public void gameTesAllShipContact(){
        int finalResult = 0;
        int total = 10000;
        for (int i=1; i<= total; i++){
            initEach();
            //Destroyer
            playerBoard[1][3] = "D";
            playerBoard[2][3] = "D";

            //Submarine
            playerBoard[2][1] = "S";
            playerBoard[3][1] = "S";
            playerBoard[4][1] = "S";

            //Cruiser
            playerBoard[2][6] = "C";
            playerBoard[3][6] = "C";
            playerBoard[4][6] = "C";

            //Batleship
            playerBoard[3][2] = "B";
            playerBoard[3][3] = "B";
            playerBoard[3][4] = "B";
            playerBoard[3][5] = "B";

            //Aircraft Carrier
            playerBoard[4][4] = "A";
            playerBoard[5][4] = "A";
            playerBoard[6][4] = "A";
            playerBoard[7][4] = "A";
            playerBoard[8][4] = "A";
            finalResult = finalResult + executeGame();
        }
        log.info("******************************************");
        log.info("******************************************");
        log.info("* FINAL RESULT : " + finalResult/total);
        log.info("******************************************");
        log.info("******************************************");
    }


    @Test
    public void gameTestFullContact(){
        int finalResult = 0;
        int total = 10000;
        for (int i=1; i<= total; i++){
            initEach();
            //Destroyer
            playerBoard[2][4] = "D";
            playerBoard[3][4] = "D";

            //Submarine
            playerBoard[2][3] = "S";
            playerBoard[3][3] = "S";
            playerBoard[4][3] = "S";

            //Cruiser
            playerBoard[2][5] = "C";
            playerBoard[3][5] = "C";
            playerBoard[4][5] = "C";

            //Batleship
            playerBoard[1][2] = "B";
            playerBoard[1][3] = "B";
            playerBoard[1][4] = "B";
            playerBoard[1][5] = "B";

            //Aircraft Carrier
            playerBoard[2][2] = "A";
            playerBoard[3][2] = "A";
            playerBoard[4][2] = "A";
            playerBoard[5][2] = "A";
            playerBoard[6][2] = "A";
            finalResult = finalResult + executeGame();
        }
        log.info("******************************************");
        log.info("******************************************");
        log.info("* FINAL RESULT : " + finalResult/total);
        log.info("******************************************");
        log.info("******************************************");
    }



    private int executeGame(){
        Game game = new BattleshipGame().startGame();
        game.setGameMode(GameModeUtil.getGameMode("AVANZADO"));

        int totalAttacks = 0;
        while (destroyer + submarine + cruiser + battleship + aircraftCarrier > 0 ){
            totalAttacks = totalAttacks + 1;
            int[] attack = new BattleshipGame().generateAlexaAttack(game);
            String result = playerBoard[attack[0]][attack[1]];
            switch(result) {
                case "D":
                    playerBoard[attack[0]][attack[1]] = "";
                    destroyer = destroyer - 1;
                    if(destroyer > 0){
                        new BattleshipGame().saveImpactOnPlayerBoard(game, attack[0], attack[1]);
                    }
                    else {
                        game.getBoardGamePlayer().updateZonesToDestroyedAndCleanImpacts(game, attack[0], attack[1], "DESTRUCTOR");
                    }
                    break;
                case "S":
                    playerBoard[attack[0]][attack[1]] = "";
                    submarine = submarine - 1;
                    if(submarine > 0){
                        new BattleshipGame().saveImpactOnPlayerBoard(game, attack[0], attack[1]);
                    }
                    else {
                        game.getBoardGamePlayer().updateZonesToDestroyedAndCleanImpacts(game, attack[0], attack[1], "SUBMARINO");
                    }
                    break;
                case "C":
                    playerBoard[attack[0]][attack[1]] = "";
                    cruiser = cruiser - 1;
                    if(cruiser > 0){
                        new BattleshipGame().saveImpactOnPlayerBoard(game, attack[0], attack[1]);
                    }
                    else {
                        game.getBoardGamePlayer().updateZonesToDestroyedAndCleanImpacts(game, attack[0], attack[1], "CRUCERO");
                    }
                    break;
                case "B":
                    playerBoard[attack[0]][attack[1]] = "";
                    battleship = battleship - 1;
                    if(battleship > 0){
                        new BattleshipGame().saveImpactOnPlayerBoard(game, attack[0], attack[1]);
                    }
                    else {
                        game.getBoardGamePlayer().updateZonesToDestroyedAndCleanImpacts(game, attack[0], attack[1], "ACORAZADO");
                    }
                    break;
                case "A":
                    playerBoard[attack[0]][attack[1]] = "";
                    aircraftCarrier = aircraftCarrier - 1;
                    if(aircraftCarrier > 0){
                        new BattleshipGame().saveImpactOnPlayerBoard(game, attack[0], attack[1]);
                    }
                    else {
                        game.getBoardGamePlayer().updateZonesToDestroyedAndCleanImpacts(game, attack[0], attack[1], "PORTAAVIONES");
                    }
                    break;
                default:
                    new BattleshipGame().saveFailImpactOnPlayerBoard(game.getBoardGamePlayer(), String.valueOf(attack[0]), String.valueOf(attack[1]));
            }
        }
        log.info("******************************************");
        log.info("* TOTAL NUMBER OF ATTACKS : " + totalAttacks);
        log.info("******************************************");
        return totalAttacks;
    }
    */


}