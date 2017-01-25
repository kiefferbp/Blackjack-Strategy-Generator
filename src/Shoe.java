import java.util.*;

/**
 * Created by Brian on 1/17/2017.
 */
public class Shoe {
    private Stack<Card> shoe = new Stack<>();
    private final Stack<Card> originalShoe = new Stack<>();

    // generates a shoe with the given parameters
    Shoe(int deckCount, double penetrationValue) {
        for (int i = 0; i < deckCount; i++) {
            final Stack<Card> deck = Deck.getUnshuffledDeck();
            shoe.addAll(deck);
        }

        Collections.shuffle(shoe);

        // remove cards up to the cut card, which removes [100(1 - |penetrationValue|)]% of the deck
        final int numCardsToRemove = (int) Math.floor((1 - penetrationValue) * deckCount * Deck.CARDS_PER_DECK);
        for (int i = 0; i < numCardsToRemove; i++) {
            removeTopCard();
        }

        // create a copy of the shoe
        originalShoe.addAll(shoe);
    }

    public Shoe reset() {
        shoe = originalShoe;

        return this;
    }

    public Card removeTopCard() {
        return shoe.pop();
    }

    public Card peekTopCard() {
        return shoe.peek();
    }

    public boolean removeCard(Card card) {
        return shoe.remove(card);
    }
}
