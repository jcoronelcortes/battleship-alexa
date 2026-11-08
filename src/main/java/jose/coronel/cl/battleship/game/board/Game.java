package jose.coronel.cl.battleship.game.board;

import java.util.Random;

/** A full game: Alexa's fleet and board, the player's board and the difficulty level. */
public class Game {

    public static final int BASIC_MODE = 0;
    public static final int MEDIUM_MODE = 1;
    public static final int ADVANCE_MODE = 2;

    private Ship[] ships;
    private BoardGame boardGameAlexa;
    private BoardGame boardGamePlayer;
    private int gameMode = BASIC_MODE;

    public Game() {
    }

    /** Empty boards and no ships placed yet. */
    public static Game empty() {
        Game game = new Game();
        game.ships = new Ship[ShipType.values().length];
        game.boardGameAlexa = BoardGame.empty();
        game.boardGamePlayer = BoardGame.empty();
        return game;
    }

    /** New game with Alexa's five ships placed at random, horizontal or vertical, without overlapping. */
    public static Game newGame(Random random) {
        Game game = empty();
        for (ShipType type : ShipType.values()) {
            Ship ship;
            do {
                int direction = random.nextInt(2);
                int row = random.nextInt(BoardGame.NUMBER_OF_ROWS);
                int column = random.nextInt(BoardGame.NUMBER_OF_COLUMNS);
                ship = new Ship(type, row, column, direction);
            } while (!game.fits(ship));
            game.ships[type.ordinal()] = ship;
            game.boardGameAlexa.addShip(ship);
        }
        return game;
    }

    private boolean fits(Ship ship) {
        for (int i = 0; i < ship.getLength(); i++) {
            int row = ship.getDirection() == Ship.VERTICAL ? ship.getRow() + i : ship.getRow();
            int column = ship.getDirection() == Ship.HORIZONTAL ? ship.getColumn() + i : ship.getColumn();
            if (row >= BoardGame.NUMBER_OF_ROWS || column >= BoardGame.NUMBER_OF_COLUMNS
                    || boardGameAlexa.hasShip(row, column)) {
                return false;
            }
        }
        return true;
    }

    public Ship shipByIdentifier(int identifier) {
        for (Ship ship : ships) {
            if (ship != null && ship.getIdentifier() == identifier) {
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

}
