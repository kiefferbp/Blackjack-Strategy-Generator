import java.util.ArrayList;
import java.util.List;

/**
 * Created by Brian on 1/18/2017.
 * Handles both the player and dealer
 */
public class Player {
    private List<Card> cards = new ArrayList<>();
    private int aceCount = 0;

    public Card addCard(Card card) {
        cards.add(card);

        if (card.equals(Card.ACE)) {
            aceCount += 1;
        }

        return card;
    }

    private int getHandValueWithoutAces() {
        int valueWithNoAces = 0;
        for (Card card : cards) {
            if (!card.equals(Card.ACE)) {
                valueWithNoAces += card.getValue();
            }
        }

        return valueWithNoAces;
    }

    public boolean handIsSoft() {
        // compute the value of the dealer's hand without any aces
        final int valueWithNoAces = getHandValueWithoutAces();

        if (valueWithNoAces >= 11) {
            return false; // as any ace in this scenario must count as a 1
        }

        // Explanation: the hand is soft if at least one ace can count as an 11 (e.g., an ace can contribute 10 more points).
        // Since the value of a hand must be <= 21, this can only occur if the total when counting aces as 1's is <= 11.
        return (valueWithNoAces + aceCount <= 11);
    }

    public Card hit(Shoe shoe) {
        Card cardObtained = shoe.removeCard();
        addCard(cardObtained);

        return cardObtained;
    }

    public int getHandValue() {
        // begin considering the value of the cards that are not aces
        int handValue = getHandValueWithoutAces();

        if (handIsSoft()) {
            // there is at least one ace in the hand, and only one ace can contribute 11 points
            handValue += 11;

            if (aceCount > 1) {
                // all other aces count as 1 point
                handValue += aceCount - 1;
            }
        } else {
            // all aces must count as 1 point
            handValue += aceCount;
        }

        return handValue;
    }
}
