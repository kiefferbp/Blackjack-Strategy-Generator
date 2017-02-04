package main;

import java.util.*;

/**
 * Created by Brian on 1/17/2017.
 */
public class Deck {
    public static final int CARDS_PER_DECK = 52;
    public static final int CARDS_PER_RANK = 4;
    private static Stack<Card> unshuffledDeck = null;

    private Deck() {}

    public static Stack<Card> getUnshuffledDeck() {
        if (unshuffledDeck == null) {
            unshuffledDeck = new Stack<>();

            // push four of each of 2, 3, ..., 10, J/Q/K, A in order to the deck
            for (int i = 0; i < CARDS_PER_RANK; i++) {
                for (Card card : Card.values()) {
                    unshuffledDeck.push(card);
                }
            }
        }

        return unshuffledDeck;
    }
}
