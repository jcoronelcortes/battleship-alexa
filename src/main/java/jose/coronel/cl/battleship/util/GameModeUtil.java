package jose.coronel.cl.battleship.util;

import jose.coronel.cl.battleship.game.board.Game;

public class GameModeUtil {

    public static int getGameMode(String level){
        if("BASIC".equals(level.toUpperCase()) || "BASICO".equals(level.toUpperCase())){
            return Game.BASIC_MODE;
        }
        else if("MEDIUM".equals(level.toUpperCase()) || "MEDIO".equals(level.toUpperCase())){
            return Game.MEDIUM_MODE;
        }
        else if("ADVANCE".equals(level.toUpperCase()) || "AVANZADO".equals(level.toUpperCase())){
            return Game.ADVANCE_MODE;
        }
        return Game.BASIC_MODE;
    }

}
