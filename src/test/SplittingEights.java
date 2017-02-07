package test;

import main.*;
import org.junit.Test;

import static test.TestUtils.assertTrue;

/**
 * Created by Brian on 2/7/2017.
 */
public class SplittingEights {
    private static final int SIMULATION_COUNT = 1000000;
    private static final Rule r = new RuleBuilder()
            .setDeckCount(1000) // essentially infinite
            .setPenetrationValue(1.0)
            .setDealerHitsSoft17(false)
            .setCanSurrender(false)
            .setMaxSplitHands(4)
            .build();
    private static final Decider d = new Decider(r, SIMULATION_COUNT);

    @Test
    public void testSplittingEights() throws Exception {
        try {
            for (Card dealerCard : Card.values()) {
                final Scenario p8svDealerCardScenario = new ScenarioBuilder()
                        .setPlayerValue(16)
                        .setDealerCard(dealerCard)
                        .setSoftFlag(false)
                        .setPairFlag(true)
                        .build();
                final Pair<Decision, Double> p = d.computeBestScenarioResult(p8svDealerCardScenario, true, true);
                final Decision bestDecision = p.get(Decision.class);

                assertTrue("We should split 8's versus " + dealerCard, bestDecision.equals(Decision.SPLIT));
            }

            System.out.println("We should always split 8's - passed");
        } catch (AssertionError e) {
            System.out.println("We should always split 8's - FAILED!");
            throw e;
        }
    }
}
