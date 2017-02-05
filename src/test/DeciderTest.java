package test;

import main.*;
import org.junit.Test;

/**
 * Created by Brian on 2/4/2017.
 */
public class DeciderTest {
    private static final double ERROR_MARGIN = 0.002;
    private static final Decider d = new Decider(400, 1.0);

    private static boolean approximatelyEqual(double a, double b) {
        return (Math.abs(a - b) < ERROR_MARGIN);
    }

    private static void assertTrue(String description, boolean bool) {
        try {
            org.junit.Assert.assertTrue(bool);
            System.out.println(description + " - passed");
        } catch (AssertionError e) {
            System.err.println(description + " - failed");
            throw e;
        }
    }

    @Test
    public void testDecision() throws Exception {
        final Scenario scenario = new Scenario();
        scenario.playerValue = 16;
        scenario.isPlayerSoft = false;
        scenario.isPair = true;
        scenario.dealerCard = Card.ACE;

        Pair<Decision, Double> p = d.computeBestScenarioResult(scenario, true);
        Decision bestDecision = p.get(Decision.class);
        double bestResult = p.get(Double.class);
        assertTrue("We should split two 8's vs an Ace", bestDecision.equals(Decision.SPLIT));
        assertTrue("The expected value of two 8's vs an Ace should be about -0.372535", approximatelyEqual(bestResult, -0.372535));

        scenario.playerValue = 21;
        scenario.isPlayerSoft = false;
        scenario.isPair = false;
        scenario.dealerCard = Card.ACE;
        p = d.computeBestScenarioResult(scenario, true);
        bestDecision = p.get(Decision.class);
        bestResult = p.get(Double.class);
        assertTrue("We should stand on a hard 21 vs an Ace", bestDecision.equals(Decision.STAND));
        assertTrue("The expected value of two 8's vs an Ace should be about 0.922194", approximatelyEqual(bestResult, 0.922194));
    }
}