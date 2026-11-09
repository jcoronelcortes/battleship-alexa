package jose.coronel.cl.battleship.simulador;

/** Sends an Alexa request (JSON, exactly as Alexa would) to the skill and returns its JSON response. */
public interface SkillGateway {

    String send(String requestJson);

}
