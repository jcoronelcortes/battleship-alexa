package jose.coronel.cl.battleship.util;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Alexa answers "Invalid SSML Output Speech" when back-to-back breaks add up to more than 10 s.
 * Found in the simulator with shots far from the robot's home position (A1, B3).
 */
class AttackBoardTimeTest {

    private static final Pattern BREAK_RUN = Pattern.compile("(?:\\s*<break time=\"(\\d+)(ms|s)\"/>)+");
    private static final Pattern BREAK = Pattern.compile("<break time=\"(\\d+)(ms|s)\"/>");

    @Test
    void noRunOfBreaksIsLongerThanTenSecondsOnAnyCell() {
        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                check(AttackBoardTime.getSsmlBreaksOnMillisecondsBeforeToAttack(row, column, "apuntando..."), row, column);
                check(AttackBoardTime.getSsmlBreaksOnMillisecondsAfterToAttack(row, column, "apuntando..."), row, column);
            }
        }
    }

    private static void check(String ssml, int row, int column) {
        Matcher run = BREAK_RUN.matcher(ssml);
        while (run.find()) {
            int total = 0;
            Matcher single = BREAK.matcher(run.group());
            while (single.find()) {
                int value = Integer.parseInt(single.group(1));
                total += single.group(2).equals("s") ? value * 1000 : value;
            }
            assertTrue(total <= 10_000, "cell [" + row + "," + column + "] has " + total + " ms of breaks in a row: " + ssml);
        }
    }

}
