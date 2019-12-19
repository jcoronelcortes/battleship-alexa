package jose.coronel.cl.battleship.util;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class AttackBoardTime {

    private static Logger log = getLogger(AttackBoardTime.class);

    private static final int MIN_COLUMN_NUMBER = 0;
    private static final int MAX_COLUMN_NUMBER = 8;
    private static final int MAX_ROW_NUMBER = 8;
    private static final int DISTANCE_TIME_FACTOR = 700;
    private static final int MOVEMENT_FACTOR = 1200;
    private static final int SHOOT_TIME = 4000;
    private static final int RETURN_VELOCITY_TIME = 4000;
    private static final int SHOOT_SOUND_TIME = 3680;
    private static final int MAX_BREAK_TIME = 10000;


    public static String getSsmlBreaksOnMillisecondsBeforeToAttack(int row, int column){
        int totalTime = getTotalMovementTime(row, column);
        log.info("Zone : [{} , {}] - Time before to attack : {}", row, column, totalTime);
        return getTotalPauseInMilliseconds(totalTime, false);
    }


    public static String getSsmlBreaksOnMillisecondsAfterToAttack(int row, int column){
        int totalTime = getTotalMovementTime(row, column) + SHOOT_TIME - RETURN_VELOCITY_TIME;
        log.info("Zone : [{} , {}] - Time after to attack : {}", row, column, totalTime);
        return getTotalPauseInMilliseconds(totalTime, true);
    }


    private static int getTotalMovementTime(int row, int column){
        int columnTime = (MAX_COLUMN_NUMBER - column) * DISTANCE_TIME_FACTOR;
        int rowTime = (MAX_ROW_NUMBER - row) * DISTANCE_TIME_FACTOR;
        int totalTime = columnTime + rowTime;
        if(row > MIN_COLUMN_NUMBER && column < MAX_COLUMN_NUMBER){
            totalTime = totalTime +  MOVEMENT_FACTOR;
        }
        return totalTime;
    }


    private static String getTotalPauseInMilliseconds(int totalPause, boolean withSound) {
        if(withSound){
            totalPause = totalPause - SHOOT_SOUND_TIME;
        }
        if(totalPause < 0) {
            return "";
        }
        else{
            StringBuilder breaks = new StringBuilder();
            int greaterThanTen = totalPause / MAX_BREAK_TIME;
            int lessThanTen = totalPause % MAX_BREAK_TIME;
            log.info("greaterThanTen : {} - lessThanTen : {}", greaterThanTen, lessThanTen);
            for(int i = greaterThanTen; i > 0; i--){
                int value = MAX_BREAK_TIME;
                breaks.append("<break time=\"" + value + "ms\"/>");
            }
            if(lessThanTen > 0){
                breaks.append("<break time=\"" + lessThanTen + "ms\"/>");
            }
            return breaks.toString();
        }
    }

}
