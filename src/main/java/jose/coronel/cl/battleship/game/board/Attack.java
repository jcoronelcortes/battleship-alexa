package jose.coronel.cl.battleship.game.board;

import java.util.Objects;

public class Attack {

    private int column;
    private int row;

    public Attack() {
    }

    public Attack(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Attack)) return false;
        Attack attack = (Attack) o;
        return getColumn() == attack.getColumn() &&
                getRow() == attack.getRow();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getColumn(), getRow());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Attack{");
        sb.append("row=").append(row);
        sb.append(", column=").append(column);
        sb.append('}');
        return sb.toString();
    }
}
