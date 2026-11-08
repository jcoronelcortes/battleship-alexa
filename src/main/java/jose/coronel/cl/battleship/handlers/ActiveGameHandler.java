package jose.coronel.cl.battleship.handlers;

import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.game.board.Game;

import java.util.Optional;

/** Base for intents that need a started game. Without one, Alexa asks the player to start it. */
public abstract class ActiveGameHandler extends GameIntentHandler {

    protected abstract Optional<Response> handle(Turn turn, Game game);

    @Override
    protected final Optional<Response> handle(Turn turn) {
        Optional<Game> game = turn.session().game();
        if (game.isEmpty()) {
            return turn.ask(turn.text("noGameSpeechText"));
        }
        return handle(turn, game.get());
    }

}
