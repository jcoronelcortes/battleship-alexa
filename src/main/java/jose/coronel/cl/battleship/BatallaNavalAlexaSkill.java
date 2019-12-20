package jose.coronel.cl.battleship;

import com.amazon.ask.Skill;
import com.amazon.ask.SkillStreamHandler;
import com.amazon.ask.Skills;
import jose.coronel.cl.battleship.handlers.common.*;
import jose.coronel.cl.battleship.handlers.ev3.*;
import jose.coronel.cl.battleship.handlers.exception.GenericExceptionHandler;

public class BatallaNavalAlexaSkill extends SkillStreamHandler {

    private static Skill getSkill() {
        return Skills.standard()
                .addRequestHandlers(
                        new LaunchRequestHandler(),
                        new TestModeHandler(),
                        new StartGameHandler(),
                        new PlayerAttackHandler(),
                        new AlexaAttackHandler(),
                        new YouFailAlexaHandler(),
                        new ImpactAlexaHandler(),
                        new AlexaWinsTheGameHandler(),
                        new HelpIntentHandler(),
                        new CancelAndStopIntentHandler(),
                        new SessionEndedRequestHandler(),
                        new FallbackIntentHandler(),
                        new IntentReflectorHandler())
                .addExceptionHandlers(new GenericExceptionHandler())
                .build();
    }

    public BatallaNavalAlexaSkill() {
        super(getSkill());
    }

}
