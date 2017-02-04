package main;

import java.util.Objects;

/**
 * Created by Brian on 1/17/2017.
 */

public class Scenario {
    public int playerValue;
    public Card dealerCard;
    public boolean isPlayerSoft;
    public boolean isPair;

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Scenario)) {
            return false;
        }

        Scenario other = (Scenario) o;
        return playerValue == other.playerValue &&
                Objects.equals(dealerCard, other.dealerCard) &&
                isPlayerSoft == other.isPlayerSoft &&
                isPair == other.isPair;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerValue, dealerCard, isPlayerSoft, isPair);
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();

        String playerDescription;
        if (isPair) {
            final Card playerCard = Card.getCardWithValue(playerValue / 2);
            playerDescription = "main.Player pair of " + playerCard + "s";
        } else {
            final String playerHandType = (isPlayerSoft ? "Soft" : "Hard");
            playerDescription = "main.Player " + playerHandType + " " + playerValue;
        }
        result.append(playerDescription);

        // append the dealer hand description
        result.append(" against a dealer " + dealerCard);

        return result.toString();
    }
}
