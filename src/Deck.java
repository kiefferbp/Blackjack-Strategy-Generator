import java.util.*;

/**
 * Created by Brian on 1/17/2017.
 */
public class Deck {
    private static final int CARDS_PER_DECK = 52;
    private static final int MAX_CARDS_PER_RANK = 4;
    private final Stack<Card> deck = new Stack<>();

    // creates a shuffled deck of playing cards
    Deck() {
        final List<Card> tempDeck = new ArrayList<>(); // holds an initially unshuffled deck

        // push four of each of 2, 3, ..., 10, J/Q/K, A in order to the deck
        for (int i = 0; i < MAX_CARDS_PER_RANK; i++) {
            for (Card card : Card.values()) {
                tempDeck.add(card);
            }
        }

        // shuffle the list and init the card stack with it
        Collections.shuffle(tempDeck);
        for (Card card : tempDeck) {
            deck.push(card);
        }
    }

    public Stack<Card> getDeck() {
        return deck;
    }
}
