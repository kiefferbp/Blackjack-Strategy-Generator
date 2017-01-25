/**
 * Created by Brian on 1/17/2017.
 */
public enum Card {
    TWO (2, "Two"),
    THREE (3, "Three"),
    FOUR (4, "Four"),
    FIVE (5, "Five"),
    SIX (6, "Six"),
    SEVEN (7, "Seven"),
    EIGHT (8, "Eight"),
    NINE (9, "Nine"),
    TEN (10, "Ten"),
    KING (10, "King"),
    QUEEN (10, "Queen"),
    JACK (10, "Jack"),
    ACE (11, "Ace"); // assuming soft hand

    private int value;
    private String name;

    Card(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public static Card getCardWithValue(int target) {
        for (Card card : values()) {
            if (card.getValue() == target) {
                return card;
            }
        }

        return null;
    }

    public static Card getRandom() {
        return values()[(int) (Math.random() * values().length)];
    }

    public static Card getRandomWithoutAce() {
        return values()[(int) (Math.random() * (values().length - 1))];
    }

    @Override
    public String toString() {
        return name;
    }
}
