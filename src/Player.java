import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Brian on 1/18/2017.
 * Handles both the player and dealer
 */
public class Player {
    private List<Card> cards = new ArrayList<>();
    private int aceCount = 0;
    private Shoe shoe;

    // for caching purposes
    private int cachedPlayerValue = 0;
    private boolean cachedPlayerSoftness = false;
    private boolean playerValueIsCached = true;
    private boolean playerSoftnessIsCached = true;

    Player() {} // for construction purposes

    Player(Shoe shoe) {
        this.shoe = shoe;
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

    public List<Card> getCards() {
        return cards;
    }

    public Card addCard(Card card) {
        cards.add(card);

        if (card.equals(Card.ACE)) {
            aceCount += 1;
        }

        return card;
    }

    public List<Card> addAllCards(Collection<? extends Card> cardsToAdd) {
        for (Card cardToAdd : cardsToAdd) {
            addCard(cardToAdd);
        }

        return cards;
    }

    public boolean handIsSoft() {
        if (!playerSoftnessIsCached) {
            // set the softness cached flag
            playerSoftnessIsCached = true;

            // compute the value of the dealer's hand without any aces
            final int valueWithNoAces = getHandValueWithoutAces();

            // Explanation: the hand is soft if at least one ace can count as an 11 (e.g., an ace can contribute 10 more points).
            // If the value of the hand without aces is >= 11, any ace in the hand must count as a 1 and the hand is hard.
            // Also, since the value of a hand must be <= 21 an ace can only contribute 10 more points if the total when
            // counting aces as 1's is <= 11.
            cachedPlayerSoftness = (valueWithNoAces < 11 && aceCount >= 1 && (valueWithNoAces + aceCount <= 11));
        }

        return cachedPlayerSoftness;
    }

    public Card hit() {
        Card cardObtained = shoe.removeTopCard();
        addCard(cardObtained);

        // reset the cached flags
        playerValueIsCached = false;
        playerSoftnessIsCached = false;

        return cardObtained;
    }

    public void resetHand() {
        cards.clear();
        aceCount = 0;
    }

    public int getHandValue() {
        if (!playerValueIsCached) {
            // set the hand value cached flag
            playerValueIsCached = true;

            // begin considering the value of the cards that are not aces
            cachedPlayerValue = getHandValueWithoutAces();

            if (handIsSoft()) {
                // there is at least one ace in the hand, and only one ace can contribute 11 points
                cachedPlayerValue += 11;

                if (aceCount > 1) {
                    // all other aces count as 1 point
                    cachedPlayerValue += aceCount - 1;
                }
            } else {
                // all aces must count as 1 point
                cachedPlayerValue += aceCount;
            }
        }

        return cachedPlayerValue;
    }

    @Override
    public String toString() {
        final String handType = (handIsSoft() ? "Soft" : "Hard");
        return handType + " " + getHandValue();
    }
}
