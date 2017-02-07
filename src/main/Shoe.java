package main;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Brian on 1/17/2017.
 */
public class Shoe {
    public static final int CARDS_PER_DECK = 52;
    public static final int CARDS_PER_RANK = 4;

    private final Map<Card, Integer> shoeComposition = new HashMap<>();
    private int cardsInShoe;
    private int deckCount;
    private double penetrationValue;

    // generates a shoe with the given parameters
    Shoe(int deckCount, double penetrationValue) {
        this.deckCount = deckCount;
        this.penetrationValue = penetrationValue;
        buildShoe();
    }

    private void buildShoe() {
        cardsInShoe = deckCount * CARDS_PER_DECK;

        // initialize the deck composition map
        for (Card card : Card.values()) {
            // there are four of each card (2, 3, ..., 10/J/Q/K, A) in a deck
            shoeComposition.put(card, CARDS_PER_RANK * deckCount);
        }

        // remove cards up to the cut card, which removes [100(1 - |penetrationValue|)]% of the deck
        final int numCardsToRemove = (int) Math.floor((1 - penetrationValue) * deckCount * CARDS_PER_DECK);
        for (int i = 0; i < numCardsToRemove; i++) {
            removeTopCard();
        }
    }

    public Shoe rebuildShoe() {
        buildShoe();
        return this;
    }

    public Card removeCard(Card card) {
        final int currentCount = shoeComposition.get(card);
        if (currentCount == 0) {
            throw new IllegalStateException("Card does not exist in the shoe");
        } else {
            shoeComposition.put(card, currentCount - 1);
            cardsInShoe -= 1;
        }

        return card;
    }

    public Card removeTopCard() {
        final int cumulativeTarget = ThreadLocalRandom.current().nextInt(1, cardsInShoe + 1);
        int currentCumulative = 0;
        for (Card card : Card.values()) {
            final int cardCount = shoeComposition.get(card);
            currentCumulative += cardCount;

            if (currentCumulative >= cumulativeTarget) {
                return removeCard(card);
            }
        }

        throw new IllegalStateException("Shoe is empty");
    }

    public Card removeCardWithMaxValue(int maxValue) {
        final List<Card> cardsWithinRange = new ArrayList<>();
        int cardCountWithinRange = 0;

        for (Card card : Card.values()) {
            if (card.getValue() <= maxValue || card.equals(Card.ACE)) {
                cardsWithinRange.add(card);
                cardCountWithinRange += shoeComposition.get(card);
            }
        }

        // continue in a similar fashion to removeTopCard()
        final int cumulativeTarget = ThreadLocalRandom.current().nextInt(1, cardCountWithinRange + 1);
        int currentCumulative = 0;
        for (Card card : cardsWithinRange) {
            final int cardCount = shoeComposition.get(card);
            currentCumulative += cardCount;

            if (currentCumulative >= cumulativeTarget) {
                return removeCard(card);
            }
        }

        throw new IllegalStateException("Shoe does not contain card within range");
    }

    public Card putCardBack(Card card) {
        final int currentCount = shoeComposition.get(card);
        if (currentCount >= Deck.CARDS_PER_RANK * deckCount) {
            throw new IllegalStateException("Max cards of this type are already in the deck");
        } else {
            shoeComposition.put(card, currentCount + 1);
            cardsInShoe += 1;
        }

        return card;
    }
}
