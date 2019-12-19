package jose.coronel.cl.battleship.game.board;

import java.util.Arrays;

public class Game {

    private static final int[] LENGTH_SHIPS = {1, 1, 1, 1, 2, 2, 2, 3, 3, 4};
    private static final int NUMBER_OF_SHIPS = 10;
    
    public Ship[] ships;
    public BoardGame boardGameAlexa;
    public BoardGame boardGamePlayer;


    public Game() {
    }

    public void createNewGame() {
        ships = new Ship[NUMBER_OF_SHIPS];
        for (int i = 0; i < NUMBER_OF_SHIPS; i++) {
            ships[i] = new Ship(i, LENGTH_SHIPS[i]);
        }

        boardGameAlexa = new BoardGame();
        boardGameAlexa.createNewBoardGame();

        boardGamePlayer = new BoardGame();
        boardGamePlayer.createNewBoardGame();
    }


    public int numberOfPendingShipsToConfigure() {
        int total = 10;
        for (Ship ship : ships) {
            if (ship.hasAnAssignedZone() && ship.hasAnAssignedDirection()) {
                total--;
            }
        }
        return total;
    }


    public Ship getShipByIndifier(int identifier){
        for (int i = 0; i < NUMBER_OF_SHIPS; i++) {
            Ship ship = ships[i];
            if(ship.getIdentifier() == identifier){
                return ship;
            }
        }
        return null;
    }


    public Ship[] getShips() {
        return ships;
    }

    public void setShips(Ship[] ships) {
        this.ships = ships;
    }

    public BoardGame getBoardGameAlexa() {
        return boardGameAlexa;
    }

    public void setBoardGameAlexa(BoardGame boardGameAlexa) {
        this.boardGameAlexa = boardGameAlexa;
    }

    public BoardGame getBoardGamePlayer() {
        return boardGamePlayer;
    }

    public void setBoardGamePlayer(BoardGame boardGamePlayer) {
        this.boardGamePlayer = boardGamePlayer;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Game{");
        sb.append("ships=").append(Arrays.toString(ships));
        sb.append(", boardGameAlexa=").append(boardGameAlexa);
        sb.append(", boardGamePlayer=").append(boardGamePlayer);
        sb.append('}');
        return sb.toString();
    }
}