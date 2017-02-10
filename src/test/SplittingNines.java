package test;

import main.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static test.TestUtils.assertTrue;

/**
 * Created by Brian on 2/7/2017.
 */
public class SplittingNines {
    private static final int SIMULATION_COUNT = 1000000;
    private static final Rule r = new RuleBuilder()
            .setDeckCount(1000) // essentially infinite
            .setPenetrationValue(1.0)
            .setDealerHitsSoft17(false)
            .setCanSurrender(true)
            .setMaxSplitHands(4)
            .build();
    private static final Decider d = new Decider(r, SIMULATION_COUNT);

    // we should stand with a pair of 9's vs 7, T, A and split o/w
    private static final List<Card> standAgainstCards = Arrays.asList(Card.SEVEN, Card.TEN, Card.ACE);

    @Test
    public void testSplittingNines() throws Exception {
        try {
            // iterating through Card.values() will consider 10/J/Q/K as four separate cases --- not necessary
            for (int dealerCardValue = Card.TWO.getValue(); dealerCardValue <= Card.ACE.getValue(); dealerCardValue++) {
                final Card dealerCard = Card.getCardWithValue(dealerCardValue);
                final Scenario p8svDealerCardScenario = new ScenarioBuilder()
                        .setPlayerValue(18)
                        .setDealerCard(dealerCard)
                        .setSoftFlag(false)
                        .setPairFlag(true)
                        .build();
                final Pair<Decision, Double> p = d.computeBestScenarioResult(p8svDealerCardScenario);
                final Decision bestDecision = p.get(Decision.class);

                if (standAgainstCards.contains(dealerCard)) {
                    assertTrue("We should stand on a pair of 9's versus " + dealerCard, bestDecision.equals(Decision.STAND));
                } else {
                    assertTrue("We should split 9's versus " + dealerCard, bestDecision.equals(Decision.SPLIT));
                }
            }

            System.out.println("We should split 9's except versus 7, T, A - passed");
        } catch (AssertionError e) {
            System.out.println("We should split 9's except versus 7, T, A - FAILED!");
            throw e;
        }
    }
}
