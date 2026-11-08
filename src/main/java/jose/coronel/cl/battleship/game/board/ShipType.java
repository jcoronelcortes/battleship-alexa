package jose.coronel.cl.battleship.game.board;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

/** The five ships of each fleet. The order is also each ship's identifier on the board. */
public enum ShipType {

    DESTROYER(2, "Destroyer", "destroyer", "destructor"),
    SUBMARINE(3, "Submarine", "submarine", "submarino"),
    CRUISER(3, "Cruiser", "cruiser", "crucero"),
    BATTLESHIP(4, "Battleship", "battleship", "acorazado"),
    AIRCRAFT_CARRIER(5, "Aircraft Carrier", "aircraft carrier", "portaaviones");

    public static final int TOTAL_CELLS = 17;

    private final int length;
    private final String label;
    private final String[] spokenNames;

    ShipType(int length, String label, String... spokenNames) {
        this.length = length;
        this.label = label;
        this.spokenNames = spokenNames;
    }

    public int length() {
        return length;
    }

    /** English name sent to the robot ("Destroyer"). */
    public String label() {
        return label;
    }

    /** Translation key of the spoken name, e.g. shipNameAircraftCarrier. */
    public String nameKey() {
        return "shipName" + label.replace(" ", "");
    }

    /** "submarino", "SUBMARINE", "Aircraft Carrier" or "porta aviones" → the ship. */
    public static Optional<ShipType> fromSpoken(String spoken) {
        if (spoken == null) {
            return Optional.empty();
        }
        String wanted = simplify(spoken);
        for (ShipType type : values()) {
            if (simplify(type.name()).equals(wanted) || simplify(type.label).equals(wanted)) {
                return Optional.of(type);
            }
            for (String name : type.spokenNames) {
                if (simplify(name).equals(wanted)) {
                    return Optional.of(type);
                }
            }
        }
        return Optional.empty();
    }

    private static String simplify(String text) {
        String noAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

}
