package jose.coronel.cl.battleship.util;

import jose.coronel.cl.battleship.game.board.Game;

import java.text.Normalizer;
import java.util.Locale;

public class GameModeUtil {

    /** Maps a spoken level (English or Spanish, with or without accents) to a game mode. */
    public static int getGameMode(String level){
        if (level == null) {
            return Game.BASIC_MODE;
        }
        String value = normalize(level);
        switch (value) {
            case "MEDIUM":
            case "MEDIO":
            case "NORMAL":
            case "INTERMEDIO":
            case "INTERMEDIATE":
                return Game.MEDIUM_MODE;
            case "ADVANCE":
            case "ADVANCED":
            case "AVANZADO":
            case "HARD":
            case "DIFICIL":
            case "EXPERT":
            case "EXPERTO":
                return Game.ADVANCE_MODE;
            default:
                return Game.BASIC_MODE;
        }
    }

    /** Uppercase without accents: "básico" → "BASICO". */
    public static String normalize(String text){
        String noAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.trim().toUpperCase(Locale.ROOT);
    }

}
