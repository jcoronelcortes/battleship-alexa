package jose.coronel.cl.battleship.handlers;

import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.game.board.Attack;
import jose.coronel.cl.battleship.game.board.Game;

import java.util.Optional;

/**
 * Base for the player's answers to Alexa's shot ("you fail", "impact", "sunk").
 * Each shot is answered once: a repeated "impact" no longer counts twice.
 */
public abstract class AlexaShotAnswerHandler extends ActiveGameHandler {

    protected abstract Optional<Response> handle(Turn turn, Game game, Attack alexaShot);

    @Override
    protected final Optional<Response> handle(Turn turn, Game game) {
        Optional<Attack> shot = turn.session().pendingAlexaShot();
        if (shot.isEmpty()) {
            return turn.ask(turn.text("noAlexaShotSpeechText"));
        }
        turn.session().clearPendingAlexaShot();
        return handle(turn, game, shot.get());
    }

}
