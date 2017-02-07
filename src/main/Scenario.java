package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by Brian on 1/17/2017.
 */

public class Scenario {
    public int playerValue;
    public Card dealerCard;
    public boolean isPlayerSoft;
    public boolean isPair;

    public static List<Scenario> getAll() {
        final List<Scenario> result = new ArrayList<>();

        for (Card dealerCard : Card.values()) {
            final ScenarioBuilder builder = new ScenarioBuilder()
                    .setDealerCard(dealerCard)
                    .setPairFlag(false)
                    .setSoftFlag(false);

            // hard 5-21 (non-pairs)
            for (int playerValue = 5; playerValue <= 21; playerValue++) {
                builder.setPlayerValue(playerValue);
                result.add(builder.build());
            }

            // soft 13-21 (non-pairs)
            builder.setSoftFlag(true);
            for (int playerValue = 13; playerValue <= 21; playerValue++) {
                builder.setPlayerValue(playerValue);
                result.add(builder.build());
            }

            // 2-2, 3-3, ..., 10-10 pairs
            builder.setSoftFlag(false);
            builder.setPairFlag(true);
            for (int cardValue = 2; cardValue <= 10; cardValue++) {
                builder.setPlayerValue(cardValue);
                result.add(builder.build());
            }

            // add pair of Aces
            result.add(builder.setSoftFlag(true).setPlayerValue(12).build());
        }

        return result;
    }

    public static List<Scenario> getAllWithDealerCardValue(int targetValue) {
        final List<Scenario> allScenarios = getAll();
        final List<Scenario> result = new ArrayList<>();

        for (Scenario scenario : allScenarios) {
            if (scenario.dealerCard.getValue() == targetValue) {
                result.add(scenario);
            }
        }

        return result;
    }

    public static List<Scenario> getAllSoftHands() {
        final List<Scenario> allScenarios = getAll();
        final List<Scenario> result = new ArrayList<>();

        for (Scenario scenario : allScenarios) {
            if (scenario.isPlayerSoft) {
                result.add(scenario);
            }
        }

        return result;
    }

    public static List<Scenario> getAllPairs() {
        final List<Scenario> allScenarios = getAll();
        final List<Scenario> result = new ArrayList<>();

        for (Scenario scenario : allScenarios) {
            if (scenario.isPair) {
                result.add(scenario);
            }
        }

        return result;
    }

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
            playerDescription = "Pair of " + playerCard + "s";
        } else {
            final String playerHandType = (isPlayerSoft ? "Soft" : "Hard");
            playerDescription = playerHandType + " " + playerValue;
        }
        result.append(playerDescription);

        // append the dealer hand description
        result.append(" versus " + dealerCard);

        return result.toString();
    }
}
