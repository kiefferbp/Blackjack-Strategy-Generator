import java.util.*;

/**
 * Created by Brian on 1/17/2017.
 */
public class Deck {
    private static final int CARDS_PER_DECK = 52;
    private static final int MAX_CARDS_PER_RANK = 4;
    private final Stack<Card> mCards = new Stack<>();

    // creates a shuffled deck of playing cards
    Deck() {
        final Map<Card, Integer> cardCounts = new HashMap<>(); // maps cards (ignoring suit) to the appearance count

        // this isn't very efficient...
        while (mCards.size() < CARDS_PER_DECK) {
            final Card card = Card.getRandomCard();

            // we only want four cards of each rank in our deck
            // (e.g., a deck can't have five aces)
            if (cardCounts.get(card) == MAX_CARDS_PER_RANK) {
                continue;
            }

            // at this point, we can add the card to the deck
            mCards.push(card);

            // update the number of times this card appears in the deck
            if (cardCounts.containsKey(card)) {
                final int oldCount = cardCounts.get(card);
                cardCounts.put(card, oldCount + 1);
            } else {
                cardCounts.put(card, 1);
            }
        }
    }
}
