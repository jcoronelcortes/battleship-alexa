package jose.coronel.cl.battleship.game;

import jose.coronel.cl.battleship.game.ai.ParityStrategy;
import jose.coronel.cl.battleship.game.ai.ProbabilityDensityFunctionStrategy;
import jose.coronel.cl.battleship.game.board.Attack;
import jose.coronel.cl.battleship.game.board.BoardGame;
import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.game.board.Ship;
import jose.coronel.cl.battleship.handlers.ev3.directives.SendCommandDirective;
import jose.coronel.cl.battleship.util.PositionConvertion;
import org.slf4j.Logger;

import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

public class BattleshipGame {

    private static Logger log = getLogger(BattleshipGame.class);

    public Game startGame(){
        Game game = new Game();
        game.createNewGame();

        int shipNumber = 0;
        Random random = new Random();

        log.info("Creating new ships for Alexa board");
        while (game.numberOfPendingShipsToConfigure() > 0) {
            for (Ship ship : game.getShips()) {
                int row = random.nextInt(9);
                int column = random.nextInt(9);
                int direction = random.nextInt(1);

                while (isTheAttackInvalid(row, column, direction, game, shipNumber)) {
                    row = random.nextInt(9);
                    column = random.nextInt(9);
                    direction = random.nextInt(1);
                }

                game.getShips()[shipNumber].defineZone(row, column);
                game.getShips()[shipNumber].setDirection(direction);
                game.getBoardGameAlexa().addShip(game.getShips()[shipNumber]);
                log.info("Ship number {} created", shipNumber);

                shipNumber++;
            }
        }
        return game;
    }



    private boolean isTheAttackInvalid(int row, int column, int direction, Game game, int cont) {
        int shipLength = game.getShips()[cont].getLength();

        if (direction == 0) {
            log.debug("validate horizontal direction");
            int validate = shipLength + column;
            if (validate > 9) {
                return true;
            }
            else{
                for (int i = column; i < column + shipLength; i++) {
                    if(game.getBoardGameAlexa().hasShip(row, i)) {
                        return true;
                    }
                }
                return false;
            }
        }
        else{
            log.debug("validate vertical direction");
            int validate = shipLength + row;
            if (validate > 9) {
                return true;
            }
            else{
                for (int i = row; i < row + shipLength; i++) {
                    if(game.getBoardGameAlexa().hasShip(i, column)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }



    public SendCommandDirective executePlayerAttack(Game game, String rowRequested, int column) {
        BoardGame boardGameAlexa = game.getBoardGameAlexa();
        SendCommandDirective sendCommandDirective = new SendCommandDirective("command","player_attack");

        rowRequested = rowRequested.toUpperCase();
        int row = PositionConvertion.convertRowLetterToNumber(rowRequested);

        int columnRequested = column;
        column = column - 1;

        log.info("Zone Attack [{}, {}]", row, column);

        sendCommandDirective.setRow(String.valueOf(row));
        sendCommandDirective.setColumn(String.valueOf(column));

        if(!boardGameAlexa.wasAttacked(row, column)){
            if (boardGameAlexa.hasShip(row, column)) {
                log.info("There is a ship in the impacted zone. Recording impact");
                boardGameAlexa.recordImpact(row, column);
                sendCommandDirective.setAttackResult(SendCommandDirective.IMPACT);

                int shipIdentifier = boardGameAlexa.getImpactedShipIdentifier(row, column);
                String shipDestroyed = isShipDestroyed(game, shipIdentifier);
                if(!shipDestroyed.isEmpty()){
                    log.info("Ship destroyed");
                    sendCommandDirective.setAttackResult(SendCommandDirective.SHIP_DESTROYED);
                    sendCommandDirective.setShipDestroyed(shipDestroyed);
                }
            }
            else {
                boardGameAlexa.recordFailedImpact(row, column);
                sendCommandDirective.setAttackResult(SendCommandDirective.FAIL);
                log.info("The played has failed an attack on [{}, {}]", rowRequested, columnRequested);
            }
        }
        else{
            log.error("The zone was already attacked before");
            sendCommandDirective.setAttackResult(SendCommandDirective.DUPLICATE_ATTACK);
        }

        if (boardGameAlexa.alreadyLostTheGame()) {
            sendCommandDirective.setAttackResult(SendCommandDirective.PLAYER_WON);
            log.info("The player won the game!!!");
        }
        return sendCommandDirective;
    }



    private String isShipDestroyed(Game game, int shipIdentifier){
        Ship ship = game.getShipByIdentifier(shipIdentifier);
        ship.reduceOneLifePoint();
        log.info("Validate if the ship was destroyed or not");
        if(ship.statusDestroyed()){
            log.info("The ship destroyed is a {} with identifier {}", ship.getTypeOfShip(), shipIdentifier);
            return ship.getTypeOfShip();
        }
        log.info("The ship was not destroyed");
        return "";
    }


    public int[] generateAlexaAttack(Game game) {
        log.debug("Game mode : {}", game.getGameMode());
        List<Attack> nextAttacks;
        switch(game.getGameMode()) {
            case Game.BASIC_MODE:
                return createRandomAttack(game, false);
            case Game.MEDIUM_MODE:
                game.getBoardGamePlayer().printBoardGame();
                nextAttacks = game.getBoardGamePlayer().getNextAttack();
                if(nextAttacks.isEmpty()){
                    return createRandomAttack(game, true);
                }
                else{
                    return getNextAttack(game.getBoardGamePlayer().removeNextAttack());
                }
            case Game.ADVANCE_MODE:
                if(game.getBoardGamePlayer().getNextAttack().isEmpty()){
                    log.debug("Playing on 'Hunt Mode'");
                    return getNextAttack(ProbabilityDensityFunctionStrategy.generateNewAttackFromPdfOnHuntMode(game.getBoardGamePlayer()));
                }
                else{
                    log.debug("Playing on 'Destroy Mode'");
                    log.debug("Using first attack of next list : {}", game.getBoardGamePlayer().getNextAttack());
                    //Use the first element to attack and remove from the list at the same time
                    return getNextAttack(game.getBoardGamePlayer().getNextAttack().remove(0));
                }
            default:
                return createRandomAttack(game, false);
        }
    }

    private int[] getNextAttack(Attack nextAttack) {
        log.info("Next attack : {}", nextAttack);
        int[] attack = new int[2];
        attack[0] = nextAttack.getRow();
        attack[1] = nextAttack.getColumn();
        return attack;
    }

    private int[] createRandomAttack(Game game, boolean onlyParPosition){
        log.info("Create random attack with only par position : {}", onlyParPosition);
        Random random = new Random();
        int row;
        int column;
        do {
            if (onlyParPosition) {
                row = random.nextInt(9);
                if ((row % 2) == 0) {
                    do {
                        column = random.nextInt(9);
                    }
                    while ((column % 2) != 0);
                } else {
                    do {
                        column = random.nextInt(9);
                    }
                    while ((column % 2) == 0);
                }
            }
            else {
                row = random.nextInt(9);
                column = random.nextInt(9);
            }
        }
        while (game.getBoardGamePlayer().wasAttacked(row, column));
        int[] attack = {row, column};
        return attack;
    }


    public void saveImpactOnPlayerBoard(Game game, int alexaAttackRow, int alexaAttackColumn) {
        log.info("Save impact [{}, {}]", alexaAttackRow, alexaAttackColumn);
        game.getBoardGamePlayer().recordImpact(alexaAttackRow, alexaAttackColumn);

        if(game.getGameMode() == Game.MEDIUM_MODE){
            ParityStrategy.addNewNextAttacks(game.getBoardGamePlayer(), alexaAttackRow, alexaAttackColumn);
        }
        else if(game.getGameMode() == Game.ADVANCE_MODE){
            ProbabilityDensityFunctionStrategy.generateNewAttackFromPdfOnDestroyMode(game.getBoardGamePlayer());
        }
    }


    public void saveFailImpactOnPlayerBoard(BoardGame boardGamePlayer, String row, String column) {
        boardGamePlayer.recordFailedImpact(Integer.parseInt(row), Integer.parseInt(column));
    }


}
