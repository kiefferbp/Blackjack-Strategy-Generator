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

        shuffle();

        // remove cards up to the cut card, which removes [100(1 - |penetrationValue|)]% of the deck
        final int numCardsToRemove = (int) Math.floor((1 - penetrationValue) * deckCount * Deck.CARDS_PER_DECK);
        for (int i = 0; i < numCardsToRemove; i++) {
            removeTopCard();
        }

        // create a copy of the shoe
        originalShoe.addAll(shoe);
    }

    public Shoe shuffle() {
        Collections.shuffle(shoe);
        return this;
    }

    public Card removeTopCard() {
        return shoe.pop();
    }

    public Card peekTopCard() {
        return shoe.peek();
    }

    public boolean removeCard(Card card) {
        final boolean cardWasRemoved = shoe.remove(card);
        shuffle();

        return cardWasRemoved;
    }
}
