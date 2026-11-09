package jose.coronel.cl.battleship.simulador;

import jose.coronel.cl.battleship.BatallaNavalAlexaSkill;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** The skill code running inside the simulator (no AWS, no internet). */
public class LocalSkillGateway implements SkillGateway {

    private final BatallaNavalAlexaSkill skill = new BatallaNavalAlexaSkill();

    @Override
    public String send(String requestJson) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            skill.handleRequest(new ByteArrayInputStream(requestJson.getBytes(StandardCharsets.UTF_8)), out, null);
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
