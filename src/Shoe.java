import java.util.*;

/**
 * Created by Brian on 1/17/2017.
 */
public class Shoe {
    private final Stack<Card> shoe = new Stack<>();

    // generates a shoe with the given parameters
    // note:
    Shoe(int playerValue, int dealerValue, int deckCount, double penetrationValue, boolean isSoft, boolean isPair) {
        final Card[] cardsToRemove = new Card[3]; // 2 cards for dealer, 1 for player

        // remove 2 cards from the deck that could be the player's
        if (isPair && isSoft) { // two Aces
            cardsToRemove[0] = cardsToRemove[1] = Card.ACE;
        } else if (isPair) { // hard pair
            cardsToRemove[0] = cardsToRemove[1] = Card.getCardWithValue(playerValue / 2);
        } else if (isSoft) { // soft non-pair
            // first card is an ace
            cardsToRemove[0] = Card.ACE;

            // second card is the difference
            cardsToRemove[1] = Card.getCardWithValue(playerValue - Card.ACE.getValue());
        } else { // hard non-pair
            Card firstCard;
            Card secondCard;

            // not efficient, but it should uniformly pick a pair
            final Random rand = new Random();
            do {
                firstCard = Card.getCardWithValue(2 + rand.nextInt(9)); // pick between 2-10
                secondCard = Card.getCardWithValue(2 + rand.nextInt(9));
            } while ((firstCard.getValue() == secondCard.getValue()) || (firstCard.getValue() + secondCard.getValue() != playerValue));

            cardsToRemove[0] = firstCard;
            cardsToRemove[1] = secondCard;
        }

        // remove the dealer card
        cardsToRemove[2] = Card.getCardWithValue(dealerValue);

        // generate the shoe with these cards removed
        final List<Card> shoeList = new ArrayList<>();
        for (int i = 0; i < deckCount; i++) {
            final Stack<Card> deck = new Deck().getUnshuffledDeck();
            shoeList.addAll(deck);
        }
        for (Card cardToRemove : cardsToRemove) {
            shoeList.remove(cardToRemove);
        }

        // shuffle the shoe and add the cards to the shoe stack
        Collections.shuffle(shoeList);
        for (Card card : shoeList) {
            shoe.push(card);
        }
    }
}
