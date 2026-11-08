package jose.coronel.cl.battleship.game.ai;

import jose.coronel.cl.battleship.game.board.Attack;
import jose.coronel.cl.battleship.game.board.BoardGame;
import org.slf4j.Logger;

import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

public class ProbabilityDensityFunctionStrategy {

    private static Logger log = getLogger(ProbabilityDensityFunctionStrategy.class);

    public static Attack generateNewAttackFromPdfOnHuntMode(BoardGame playerBoardGame){
        log.debug("Calculate 'Probability Density Function'");
        int[][] pdfMatrixCalculated = generatePdfHuntMode(playerBoardGame);
        playerBoardGame.setPdfMatrix(pdfMatrixCalculated);
        printPdfMatrix(pdfMatrixCalculated);
        int max = 0;
        log.debug("Get max higher probability of attack from PDF");
        Map<Integer, List<Attack>> attacks = new HashMap<>();
        for(int row = 0; row < BoardGame.NUMBER_OF_ROWS; row++){
            for(int column = 0; column < BoardGame.NUMBER_OF_COLUMNS; column++){
                if(!playerBoardGame.wasAttacked(row, column)){
                    int pdfValue = pdfMatrixCalculated[row][column];
                    if(!attacks.containsKey(pdfValue)){
                        attacks.put(pdfValue, new ArrayList<>());
                    }
                    max = addPossibleAttackPosition(row, column, pdfMatrixCalculated, max, attacks);
                }
            }
        }
        log.debug("Max higher probability : {}", max);
        List<Attack> attackList = attacks.get(max);
        int attack = new Random().nextInt(attackList.size());
        return attackList.get(attack);
    }

    private static int[][] generatePdfHuntMode(BoardGame playerBoardGame){
        log.debug("Calculate PDF on Hunt mode");

        int[][] pdfMatrix = new int[BoardGame.NUMBER_OF_ROWS][BoardGame.NUMBER_OF_COLUMNS];

        if(playerBoardGame.validateShipLengthTwoNotDestroyed()){
            log.debug("Calculate PDF for Ship length 2");
            pdfMatrix = calculatePDF(playerBoardGame, pdfMatrix, 2);
        }
        if(playerBoardGame.validateShipLengthThreeNotDestroyed()){
            log.debug("Calculate PDF for Ship length 3");
            pdfMatrix = calculatePDF(playerBoardGame, pdfMatrix, 3);
        }
        if(playerBoardGame.validateShipLengthFourNotDestroyed()){
            log.debug("Calculate PDF for Ship length 4");
            pdfMatrix = calculatePDF(playerBoardGame, pdfMatrix, 4);
        }
        if(playerBoardGame.validateShipLengthFiveNotDestroyed()){
            log.debug("Calculate PDF for Ship length 5");
            pdfMatrix = calculatePDF(playerBoardGame, pdfMatrix, 5);
        }
        return pdfMatrix;
    }


    private static int[][] calculatePDF(BoardGame playerBoardGame, int[][] pdfMatriz, int shipLength){
        log.debug("Calculate PDF on Column orientation");
        for(int row = 0; row <= (BoardGame.NUMBER_OF_ROWS - shipLength); row++){
            for(int column = 0; column < BoardGame.NUMBER_OF_COLUMNS; column++){
                boolean posiblePositions = false;
                for (int position = row; position < (row + shipLength); position++){
                    if(playerBoardGame.validateProbabilityDensityFunctionZone(position, column)){
                        posiblePositions = true;
                    }
                    else{
                        posiblePositions = false;
                        break;
                    }
                }
                if(posiblePositions){
                    log.debug("Possible position for Rows from '{}' to '{}' and Column '{}'", row, (row + shipLength), column);
                    for (int position = row; position < (row + shipLength); position++){
                        pdfMatriz[position][column] = pdfMatriz[position][column] + 1;
                    }
                }
            }
        }
        log.debug("Calculate PDF on Row orientation");
        for(int row = 0; row < BoardGame.NUMBER_OF_ROWS; row++){
            for(int column = 0; column <= (BoardGame.NUMBER_OF_COLUMNS - shipLength); column++){
                boolean posiblePositions = false;
                for (int position = column; position < (column + shipLength); position++){
                    if(playerBoardGame.validateProbabilityDensityFunctionZone(row, position)){
                        posiblePositions = true;
                    }
                    else{
                        posiblePositions = false;
                        break;
                    }
                }
                if(posiblePositions){
                    log.debug("Possible position for Row '{}' and Columns from '{}' to '{}'", row, column, (column + shipLength));
                    for (int position = column; position < (column + shipLength); position++){
                        pdfMatriz[row][position] = pdfMatriz[row][position] + 1;
                    }
                }
            }
        }
        return pdfMatriz;
    }


    private static int addPossibleAttackPosition(int rowImpact, int columnImpact, int[][] pdfMatrixCalculated, int max, Map<Integer, List<Attack>> attacks) {
        int pdfValue;
        pdfValue = pdfMatrixCalculated[rowImpact][columnImpact];
        if(pdfValue > max){
            max = pdfValue;
        }
        if(attacks.containsKey(pdfValue)){
            attacks.get(pdfValue).add(new Attack(rowImpact, columnImpact));
        }
        else {
            List<Attack> attackList = new ArrayList<>();
            attackList.add(new Attack(rowImpact, columnImpact));
            attacks.put(pdfValue, attackList);
        }
        return max;
    }


    public static void generateNewAttackFromPdfOnDestroyMode(BoardGame playerBoardGame){
        int[][] pdfMatrix = new int[BoardGame.NUMBER_OF_ROWS][BoardGame.NUMBER_OF_COLUMNS];
        Attack impact = playerBoardGame.getImpact();
        log.debug("Last impact : {}", impact);
        int maxShipLength = 0;
        if(playerBoardGame.validateShipLengthTwoNotDestroyed()){
            log.debug("Calculate pdf for Ship with length 2");
            pdfMatrix = calculatePdfByImpactOnDestroyMode(playerBoardGame, pdfMatrix, impact.getRow(),
                    impact.getColumn(), 2);
            maxShipLength = 2;
        }
        if(playerBoardGame.validateShipLengthThreeNotDestroyed()){
            log.debug("Calculate pdf for Ship with length 3");
            pdfMatrix = calculatePdfByImpactOnDestroyMode(playerBoardGame, pdfMatrix, impact.getRow(),
                    impact.getColumn(), 3);
            maxShipLength = 3;
        }
        if(playerBoardGame.validateShipLengthFourNotDestroyed()){
            log.debug("Calculate pdf for Ship with length 4");
            pdfMatrix = calculatePdfByImpactOnDestroyMode(playerBoardGame, pdfMatrix, impact.getRow(),
                    impact.getColumn(), 4);
            maxShipLength = 4;
        }
        if(playerBoardGame.validateShipLengthFiveNotDestroyed()){
            log.debug("Calculate pdf for Ship with length 5");
            pdfMatrix = calculatePdfByImpactOnDestroyMode(playerBoardGame, pdfMatrix, impact.getRow(),
                    impact.getColumn(), 5);
            maxShipLength = 5 ;
        }

        printPdfMatrix(pdfMatrix);

        log.debug("List of pending attacks : {}", playerBoardGame.getNextAttack());
        List<Attack> newAttacks = new ArrayList<>();


        //validate up
        int rowPosition = -1;
        int minRow = impact.getRow() - (maxShipLength - 1);
        if(minRow < 0){
            minRow = 0;
        }
        for(int i = impact.getRow(); i >= minRow; i--){
            if(!playerBoardGame.validateImpactStatusForPDF(i, impact.getColumn())){
                if(!playerBoardGame.wasAttacked(i, impact.getColumn())){
                    rowPosition = i;
                    break;
                }
                else {
                    rowPosition = -1;
                    break;
                }
            }
        }
        if(rowPosition != -1 && pdfMatrix[rowPosition][impact.getColumn()] > 0){
            newAttacks.add(new Attack(rowPosition, impact.getColumn()));
            log.debug("Add new attack [{}, {}]", rowPosition, impact.getColumn());
        }


        //Validate right
        int columnPosition = -1;
        int maxColumn = impact.getColumn() + (maxShipLength -1);
        if(maxColumn > 8){
            maxColumn = 8;
        }
        for (int i = impact.getColumn(); i <= maxColumn; i++) {
            if (!playerBoardGame.validateImpactStatusForPDF(impact.getRow(), i)) {
                if (!playerBoardGame.wasAttacked(impact.getRow(), i)) {
                    columnPosition = i;
                    break;
                } else {
                    columnPosition = -1;
                    break;
                }
            }
        }
        if(columnPosition != -1 && pdfMatrix[impact.getRow()][columnPosition] > 0){
            newAttacks.add(new Attack(impact.getRow(), columnPosition));
            log.debug("Add new attack [{}, {}]", impact.getRow(), columnPosition);
        }


        //Validate down
        rowPosition = -1;
        int maxRow = impact.getRow() + (maxShipLength -1);
        if(maxRow > 8){
            maxRow = 8;
        }
        for (int i = impact.getRow(); i <= maxRow; i++) {
            if (!playerBoardGame.validateImpactStatusForPDF(i, impact.getColumn())) {
                if (!playerBoardGame.wasAttacked(i, impact.getColumn())) {
                    rowPosition = i;
                    break;
                } else {
                    rowPosition = -1;
                    break;
                }
            }
        }
        if(rowPosition != -1 && pdfMatrix[rowPosition][impact.getColumn()] > 0){
            newAttacks.add(new Attack(rowPosition, impact.getColumn()));
            log.debug("Add new attack [{}, {}]", rowPosition, impact.getColumn());
        }


        //Validate left
        columnPosition = -1;
        int minColumn = impact.getColumn() - (maxShipLength -1);
        if(minColumn < 0){
            minColumn = 0;
        }
        for(int i = impact.getColumn(); i >= minColumn; i--){
            if(!playerBoardGame.validateImpactStatusForPDF(impact.getRow(), i)){
                if(!playerBoardGame.wasAttacked(impact.getRow(), i)){
                    columnPosition = i;
                    break;
                }
                else {
                    columnPosition = -1;
                    break;
                }
            }
        }

        if(columnPosition != -1 && pdfMatrix[impact.getRow()][columnPosition] > 0){
            newAttacks.add(new Attack(impact.getRow(), columnPosition));
            log.debug("Add new attack [{}, {}]", impact.getRow(), columnPosition);
        }

        //order the list by priority
        if(!newAttacks.isEmpty()){
            Map<Integer, Set<Attack>> attacks = new HashMap<>();
            for(Attack attack : newAttacks) {
                int pdfValue = pdfMatrix[attack.getRow()][attack.getColumn()];
                if(pdfValue > 0){
                    log.debug("PDF value : {} - Attack : {}", pdfValue, attack);
                    if(!attacks.containsKey(pdfValue)){
                        attacks.put(pdfValue, new HashSet<>());
                    }
                    attacks.get(pdfValue).add(attack);
                }
            }
            for(Attack attack : playerBoardGame.getNextAttack()) {
                int pdfValue = pdfMatrix[attack.getRow()][attack.getColumn()];
                log.debug("PDF value : {} - Attack : {}", pdfValue, attack);
                if(!attacks.containsKey(pdfValue)){
                    attacks.put(pdfValue, new HashSet<>());
                }
                attacks.get(pdfValue).add(attack);
            }

            playerBoardGame.cleanNextAttacks();

            List list = new ArrayList(attacks.keySet());
            Collections.sort(list, Collections.reverseOrder());
            Set<Integer> keys = new LinkedHashSet(list);
            for (Integer key : keys){
                Set<Attack> attackList = attacks.get(key);
                log.debug("Key : {} - Attacks : {}", key, attackList);
                playerBoardGame.getNextAttack().addAll(attackList);
            }
        }
        log.debug("New Attack list in order of priority : {}", playerBoardGame.getNextAttack());
    }



    private static int[][] calculatePdfByImpactOnDestroyMode(BoardGame playerBoardGame, int[][] pdfMatrix,
                                                             int impactRow, int impactColumn, int shipLength) {

        log.debug("Row left");
        int endPosition = impactColumn - (shipLength - 1);
        boolean posiblePositions;
        if(endPosition > -1){
            posiblePositions = false;
            for(int column = impactColumn; column >= endPosition; column--){
                if(playerBoardGame.validateProbabilityDensityFunctionZone(impactRow, column)) {
                    posiblePositions = true;
                }
                else{
                    posiblePositions = false;
                    break;
                }
            }
            if(posiblePositions){
                int deltaImpact = 0;
                for(int column = impactColumn; column >= endPosition; column--){
                    if (playerBoardGame.validateImpactStatusForPDF(impactRow, column)){
                        pdfMatrix[impactRow][column] = 0;
                        deltaImpact = deltaImpact + 1;
                    }
                    else {
                        pdfMatrix[impactRow][column] = pdfMatrix[impactRow][column] + 1;
                    }
                }
                boolean stop = true;
                int reverseColumn = impactColumn + 1;
                if(reverseColumn < 9){
                    do{
                        if (playerBoardGame.validateImpactStatusForPDF(impactRow, reverseColumn)){
                            deltaImpact = deltaImpact + 1;
                            reverseColumn = reverseColumn + 1;
                        }
                        else{
                            stop = false;
                        }
                        if(reverseColumn > 8){
                            stop = false;
                        }
                    }while (stop);
                }
                if(deltaImpact > 1){
                    for(int column = impactColumn; column >= endPosition; column--){
                        pdfMatrix[impactRow][column] = pdfMatrix[impactRow][column]* deltaImpact;
                    }
                }
            }
        }

        log.debug("Row right");
        endPosition = impactColumn + (shipLength - 1);
        if(endPosition < 9){
            posiblePositions = false;
            for(int column = impactColumn; column <= endPosition; column++){
                if(playerBoardGame.validateProbabilityDensityFunctionZone(impactRow, column)) {
                    posiblePositions = true;
                }
                else{
                    posiblePositions = false;
                    break;
                }
            }
            if(posiblePositions){
                int deltaImpact = 0;
                for(int column = impactColumn; column <= endPosition; column++){
                    if (playerBoardGame.validateImpactStatusForPDF(impactRow, column)){
                        pdfMatrix[impactRow][column] = 0;
                        deltaImpact = deltaImpact + 1;
                    }
                    else {
                        pdfMatrix[impactRow][column] = pdfMatrix[impactRow][column] + 1;
                    }
                }
                boolean stop = true;
                int reverseColumn = impactColumn - 1;
                if(reverseColumn > -1){
                    do{
                        if (playerBoardGame.validateImpactStatusForPDF(impactRow, reverseColumn)){
                            deltaImpact = deltaImpact + 1;
                            reverseColumn = reverseColumn - 1;
                        }
                        else{
                            stop = false;
                        }
                        if(reverseColumn < 0){
                            stop = false;
                        }
                    }while (stop);
                }
                do{
                    if (playerBoardGame.validateImpactStatusForPDF(impactRow, reverseColumn)){
                        deltaImpact = deltaImpact + 1;
                        reverseColumn = reverseColumn - 1;
                    }
                    else{
                        stop = false;
                    }
                }while (stop);
                if(deltaImpact > 1){
                    for(int column = impactColumn; column <= endPosition; column++){
                        pdfMatrix[impactRow][column] = pdfMatrix[impactRow][column] * deltaImpact;
                    }
                }
            }
        }

        log.debug("Column up");
        endPosition = impactRow - (shipLength - 1);
        if(endPosition > -1){
            posiblePositions = false;
            for(int row = impactRow; row >= endPosition; row--){
                if(playerBoardGame.validateProbabilityDensityFunctionZone(row, impactColumn)) {
                    posiblePositions = true;
                }
                else{
                    posiblePositions = false;
                    break;
                }
            }
            if(posiblePositions){
                int deltaImpact = 0;
                for(int row = impactRow; row >= endPosition; row--){
                    if (playerBoardGame.validateImpactStatusForPDF(row, impactColumn)){
                        pdfMatrix[row][impactColumn] =  0;
                        deltaImpact = deltaImpact + 1;
                    }
                    else {
                        pdfMatrix[row][impactColumn] = pdfMatrix[row][impactColumn] + 1;
                    }
                }
                boolean stop = true;
                int reverseRow = impactRow + 1;
                if(reverseRow < 9){
                    do{
                        if (playerBoardGame.validateImpactStatusForPDF(reverseRow, impactColumn)){
                            deltaImpact = deltaImpact + 1;
                            reverseRow = reverseRow + 1;
                        }
                        else{
                            stop = false;
                        }
                        if(reverseRow > 8){
                            stop = false;
                        }
                    }while (stop);
                }
                if(deltaImpact > 1){
                    for(int row = impactRow; row >= endPosition; row--){
                        pdfMatrix[row][impactColumn] = pdfMatrix[row][impactColumn] * deltaImpact;
                    }
                }
            }
        }

        log.debug("Column down");
        endPosition = impactRow + (shipLength - 1);
        if(endPosition < 9){
            posiblePositions = false;
            for(int row = impactRow; row <= endPosition; row++){
                if(playerBoardGame.validateProbabilityDensityFunctionZone(row, impactColumn)) {
                    posiblePositions = true;
                }
                else{
                    posiblePositions = false;
                    break;
                }
            }
            if(posiblePositions){
                int deltaImpact = 0;
                for(int row = impactRow; row <= endPosition; row++){
                    if (playerBoardGame.validateImpactStatusForPDF(row, impactColumn)){
                        pdfMatrix[row][impactColumn] =  0;
                        deltaImpact = deltaImpact + 1;
                    }
                    else {
                        pdfMatrix[row][impactColumn] = pdfMatrix[row][impactColumn] + 1;
                    }
                }
                boolean stop = true;
                int reverseRow = impactRow - 1;
                if(reverseRow > -1){
                    do{
                        if (playerBoardGame.validateImpactStatusForPDF(reverseRow, impactColumn)){
                            deltaImpact = deltaImpact + 1;
                            reverseRow = reverseRow - 1;
                        }
                        else{
                            stop = false;
                        }
                        if(reverseRow < 0){
                            stop = false;
                        }
                    }while (stop);
                }
                if(deltaImpact > 1){
                    for(int row = impactRow; row <= endPosition; row++){
                        pdfMatrix[row][impactColumn] = pdfMatrix[row][impactColumn] * deltaImpact;
                    }
                }
            }
        }
        return pdfMatrix;
    }


    public static void printPdfMatrix(int[][] pdfMatrixCalculated){
        log.debug("********************************");
        log.debug("* Probability Density Function");
        log.debug("********************************");
        int size = pdfMatrixCalculated.length;
        for(int row=0; row < size; row++){
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i <  7 * size ; i++){
                sb.append("-");
            }
            sb.append("-");
            log.debug(sb.toString());

            sb = new StringBuilder();
            for(int i = 1; i <= pdfMatrixCalculated[row].length; i++){
                sb.append(String.format("| %4d ", pdfMatrixCalculated[row][i - 1]));
            }
            sb.append("-");
            log.debug(sb.toString());

            if(row == size -  1){
                sb = new StringBuilder();

                for(int i = 0; i <  7 * size ; i++){
                    sb.append("-");
                }
                sb.append("-");
                log.debug(sb.toString());
            }
        }
    }

}
