package main;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Brian on 1/17/2017.
 */
public enum Card {
    ACE (11, "Ace"), // assuming soft hand
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
    JACK (10, "Jack");

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
        return values()[ThreadLocalRandom.current().nextInt(0, values().length)];
    }

    public static Card getRandomWithMaxRange(int maxValue) {
        final Card[] cards = Card.values();

        int maxIndex;
        if (maxValue < 10) { // main.Card.TWO through main.Card.NINE
            maxIndex = maxValue - 1; // cards[1] = main.Card.TWO, ..., cards[8] = main.Card.NINE
        } else {
            maxIndex = 12; // include 10/J/Q/K
        }

        return values()[ThreadLocalRandom.current().nextInt(0, maxIndex + 1)];
    }

    @Override
    public String toString() {
        return name;
    }
}
