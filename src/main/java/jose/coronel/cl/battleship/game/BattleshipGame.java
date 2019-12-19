package jose.coronel.cl.battleship.game;

import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.game.board.Ship;
import jose.coronel.cl.battleship.handlers.ev3.directives.SendCommandDirective;
import jose.coronel.cl.battleship.util.PositionConvertion;
import org.slf4j.Logger;

import java.util.Random;

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
            for (Ship ship : game.ships) {
                int row = random.nextInt(9);
                int column = random.nextInt(9);
                int direction = random.nextInt(1);

                while (isTheAttackInvalid(row, column, direction, game, shipNumber)) {
                    row = random.nextInt(9);
                    column = random.nextInt(9);
                    direction = random.nextInt(1);
                }

                game.ships[shipNumber].defineZone(row, column);
                game.ships[shipNumber].setDirection(direction);
                game.boardGameAlexa.addShip(game.ships[shipNumber]);
                log.info("Ship number {} created", shipNumber);

                shipNumber++;
            }
        }
        return game;
    }


    private boolean isTheAttackInvalid(int row, int column, int direction, Game game, int cont) {
        int length = game.ships[cont].getLength();

        if (direction == 0) {
            int validate = length + column;
            if (validate > 9) {
                return true;
            }
        }

        if (direction == 1) {
            int validate = length + row;
            if (validate > 9) {
                return true;
            }
        }

        if (direction == 0) {
            for (int i = column; i < column + length; i++) {
                if(game.boardGameAlexa.hasShip(row, i)) {
                    return true;
                }
            }
        }
        else if (direction == 1){ // Vertical
           for (int i = row; i < row + length; i++) {
                if(game.boardGameAlexa.hasShip(i, column)) {
                    return true;
                }
            }
        }
        return false;
    }



    public SendCommandDirective executePlayerAttack(Game game, String rowRequested, int column) {
        SendCommandDirective sendCommandDirective = new SendCommandDirective("command","player_attack");

        rowRequested = rowRequested.toUpperCase();
        int row = PositionConvertion.convertRowLetterToNumber(rowRequested);

        int columnRequested = column;
        column = column - 1;

        log.info("Zone Attack [{}, {}]", row, column);

        sendCommandDirective.setRow(String.valueOf(row));
        sendCommandDirective.setColumn(String.valueOf(column));

        if(!game.boardGameAlexa.wasAttacked(row, column)){
            if (game.boardGameAlexa.hasShip(row, column)) {
                log.info("There is a ship in the impacted zone. Recording impact");
                game.boardGameAlexa.recordImpact(row, column);
                sendCommandDirective.setAttackResult(1);

                int shipIdentifier = game.boardGameAlexa.getImpactedShipIdentifier(row, column);
                String shipDistroyed = isShipDestroyed(game, shipIdentifier);
                if(!shipDistroyed.isEmpty()){
                    sendCommandDirective.setAttackResult(2);
                    sendCommandDirective.setShipDestroyed(shipDistroyed);
                }
            }
            else {
                game.boardGameAlexa.recordFailedImpact(row, column);
                sendCommandDirective.setAttackResult(3);
                log.info("The played has failed an attack on [{}, {}]", rowRequested, columnRequested);
            }
        }
        else{
            log.error("The zone was already attacked before");
            sendCommandDirective.setAttackResult(5);
        }

        if (game.boardGameAlexa.alreadyLostTheGame()) {
            sendCommandDirective.setAttackResult(4);
            log.info("The player won the game!!!");
        }
        return sendCommandDirective;
    }



    private String isShipDestroyed(Game game, int shipIdentifier){
        Ship ship = game.getShipByIndifier(shipIdentifier);
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
        Random random = new Random();
        int row = random.nextInt(9);
        int column = random.nextInt(9);

        while (game.boardGamePlayer.wasAttacked(row, column)) {
            row = random.nextInt(9);
            column = random.nextInt(9);
        }
        int[] attack = new int[2];
        attack[0] = row;
        attack[1] = column;
        return attack;
    }


    public void saveImpactOnPlayerBoard(Game game, String row, String column) {
        game.boardGamePlayer.recordImpact(Integer.parseInt(row), Integer.parseInt(column));
    }

    public void saveFailImpactOnPlayerBoard(Game game, String row, String column) {
        game.boardGamePlayer.recordFailedImpact(Integer.parseInt(row), Integer.parseInt(column));
    }


}
