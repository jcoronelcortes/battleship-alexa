package jose.coronel.cl.battleship.util;

public enum AlexaSpeechTextEnglish {

    IMPACT_1("<amazon:emotion name=\"excited\" intensity=\"high\">You have impacted a ship!. </amazon:emotion><say-as interpret-as=\"interjection\">well done</say-as>"),
    IMPACT_2("<amazon:emotion name=\"disappointed\" intensity=\"high\">Your shot damaged my ship.</amazon:emotion> <say-as interpret-as=\"interjection\">no</say-as>"),
    IMPACT_3("<amazon:emotion name=\"disappointed\" intensity=\"low\">Impact, but it was a small damage.</amazon:emotion>"),
    IMPACT_4("<amazon:emotion name=\"disappointed\" intensity=\"medium\">You impacted a ship.</amazon:emotion> <amazon:emotion name=\"excited\" intensity=\"high\">You are a lucky person!.</amazon:emotion>");

    AlexaSpeechTextEnglish(String speechText){
        this.speechText = speechText;
    }

    private final String speechText;

    public String getSpeechText() {
        return speechText;
    }
}
