package jose.coronel.cl.battleship.game.board;

import java.util.Arrays;

public class Game {


    public static final int BASIC_MODE = 0;
    public static final int MEDIUM_MODE = 1;
    public static final int ADVANCE_MODE = 2;
    private static final int[] LENGTH_SHIPS = {2, 3, 3, 4, 5};
    private static final String[] TYPE_OF_SHIPS = {"Destroyer", "Submarine", "Cruiser", "Battleship", "Aircraft Carrier"};
    private static final int NUMBER_OF_SHIPS = 5;
    

    private Ship[] ships;
    private BoardGame boardGameAlexa;
    private BoardGame boardGamePlayer;
    private int gameMode;

    public Game() {
    }

    public void createNewGame() {
        ships = new Ship[NUMBER_OF_SHIPS];
        for (int i = 0; i < NUMBER_OF_SHIPS; i++) {
            ships[i] = new Ship(i, LENGTH_SHIPS[i], TYPE_OF_SHIPS[i]);
        }

        boardGameAlexa = new BoardGame();
        boardGameAlexa.createNewBoardGame();

        boardGamePlayer = new BoardGame();
        boardGamePlayer.createNewBoardGame();
    }


    public int numberOfPendingShipsToConfigure() {
        int total = NUMBER_OF_SHIPS;
        for (Ship ship : ships) {
            if (ship.hasAnAssignedZone() && ship.hasAnAssignedDirection()) {
                total--;
            }
        }
        return total;
    }


    public Ship getShipByIdentifier(int identifier){
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

    public int getGameMode() {
        return gameMode;
    }

    public void setGameMode(int gameMode) {
        this.gameMode = gameMode;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Game{");
        sb.append("ships=").append(Arrays.toString(ships));
        sb.append(", boardGameAlexa=").append(boardGameAlexa);
        sb.append(", boardGamePlayer=").append(boardGamePlayer);
        sb.append(", gameMode=").append(gameMode);
        sb.append('}');
        return sb.toString();
    }
}