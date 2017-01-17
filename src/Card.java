/**
 * Created by Brian on 1/17/2017.
 */
public enum Card {
    TWO (2),
    THREE (3),
    FOUR (4),
    FIVE (5),
    SIX (6),
    SEVEN (7),
    EIGHT (8),
    NINE (9),
    TEN (10),
    KING (10),
    QUEEN (10),
    JACK (10),
    ACE (11); // assuming soft hand

    private int value;

    Card(int value) {
       this.value = value;
    }

    public int getValue() {
        return value;
    }
}
