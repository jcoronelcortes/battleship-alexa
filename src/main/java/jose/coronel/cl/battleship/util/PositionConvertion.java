package jose.coronel.cl.battleship.util;

public class PositionConvertion {

    public static int convertRowLetterToNumber(String rowLetter) {
        int position = -1;
        switch (rowLetter.toUpperCase())  {
            case "A":
                position = 0;
                break;
            case "B":
                position = 1;
                break;
            case "C":
                position = 2;
                break;
            case "D":
                position = 3;
                break;
            case "E":
                position = 4;
                break;
            case "F":
                position = 5;
                break;
            case "G":
                position = 6;
                break;
            case "H":
                position = 7;
                break;
            case "I":
                position = 8;
                break;
            case "Y":
                position = 8;
                break;
        }
        return position;
    }


    public static String convertNumberToRowLetter(int row) {
        String position = "";
        switch (row)  {
            case 0:
                position = "A";
                break;
            case 1:
                position = "B";
                break;
            case 2:
                position = "C";
                break;
            case 3:
                position = "D";
                break;
            case 4:
                position = "E";
                break;
            case 5:
                position = "F";
                break;
            case 6:
                position = "G";
                break;
            case 7:
                position = "H";
                break;
            case 8:
                position = "I";
        }
        return position;
    }


    public static int convertTextToNumber(String row){
        int position = 0;
        switch (row.toUpperCase())  {
            case "1":
                position = 1;
                break;
            case "UNO":
                position = 1;
                break;
            case "ONE":
                position = 1;
                break;
            case "2":
                position = 2;
                break;
            case "DOS":
                position = 2;
                break;
            case "TWO":
                position = 2;
                break;
            case "3":
                position = 3;
                break;
            case "TRES":
                position = 3;
                break;
            case "THREE":
                position = 3;
                break;
            case "4":
                position = 4;
                break;
            case "CUATRO":
                position = 4;
                break;
            case "FOUR":
                position = 4;
                break;
            case "5":
                position = 5;
                break;
            case "CINCO":
                position = 5;
                break;
            case "FIVE":
                position = 5;
                break;
            case "6":
                position = 6;
                break;
            case "SEIS":
                position = 6;
                break;
            case "SIX":
                position = 6;
                break;
            case "7":
                position = 7;
                break;
            case "SIETE":
                position = 7;
                break;
            case "SEVEN":
                position = 7;
                break;
            case "8":
                position = 8;
                break;
            case "OCHO":
                position = 8;
                break;
            case "EIGHT":
                position = 8;
                break;
            case "9":
                position = 9;
                break;
            case "NUEVE":
                position = 9;
                break;
            case "NINE":
                position = 9;
                break;
        }
        return position;
    }

}
