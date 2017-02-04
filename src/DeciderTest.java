import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Brian on 2/4/2017.
 */
public class DeciderTest {
    private static final double ERROR_MARGIN = 0.002;
    private static final Decider d = new Decider(400, 1.0);

    private boolean approximatelyEqual(double a, double b) {
        return (Math.abs(a - b) < ERROR_MARGIN);
    }

    @Test
    public void testDecision() throws Exception {
        final Scenario scenario = new Scenario();
        scenario.playerValue = 16;
        scenario.isPlayerSoft = false;
        scenario.isPair = true;
        scenario.dealerCard = Card.ACE;

        final Pair<Decision, Double> p = d.computeBestScenarioResult(scenario, true);
        final Decision bestDecision = p.get(Decision.class);
        final double bestResult = p.get(Double.class);

        assertTrue("We should split two 8's vs an Ace", bestDecision.equals(Decision.SPLIT));
        assertTrue("The expected value of two 8's vs an Ace should be about -0.372535", approximatelyEqual(bestResult, -0.372535));
    }
}