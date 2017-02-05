package test;

import main.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Brian on 2/4/2017.
 */
@RunWith(Parameterized.class)
public class DeciderTest {
    private static final double ERROR_MARGIN = 0.002;
    private static final Decider d = new Decider(400, 1.0);

    @Parameterized.Parameters
    public static Iterable<?> data() {
        // source: http://wizardofodds.com/games/blackjack/appendix/1/
        final Map<Scenario, Map<Decision, Double>> paramMap = new HashMap<>();

        // hard 21 V A: standing = 0.922194, hitting = -1, doubling = -2, splitting = Integer.MIN_VALUE
        final Scenario h21vAMapScenario = new ScenarioBuilder()
                .setPlayerValue(21)
                .setDealerCard(Card.ACE)
                .setSoftFlag(false)
                .setPairFlag(false)
                .build();
        final Map<Decision, Double> h21vAMap = new HashMap<>();
        h21vAMap.put(Decision.STAND, 0.922194);
        h21vAMap.put(Decision.HIT, -1.0);
        h21vAMap.put(Decision.DOUBLE, -2.0);
        h21vAMap.put(Decision.SPLIT, (double) Integer.MIN_VALUE);
        paramMap.put(h21vAMapScenario, h21vAMap);

        return paramMap.entrySet();
    }

    @Parameterized.Parameter
    public Map.Entry<Scenario, Map<Decision, Double>> param;

    private static boolean approximatelyEqual(double a, double b) {
        return (Math.abs(a - b) < ERROR_MARGIN);
    }

    private static void assertTrue(String description, boolean bool) {
        try {
            org.junit.Assert.assertTrue(bool);
            System.out.println(description + " - passed");
        } catch (AssertionError e) {
            System.err.println(description + " - FAILED!");
            throw e;
        }
    }

    private static Pair<Decision, Double> maxOverMap(Map<Decision, Double> map) {
        Decision bestDecision = null;
        double bestValue = Integer.MIN_VALUE;

        for (Map.Entry<Decision, Double> entry : map.entrySet()) {
            final Decision entryDecision = entry.getKey();
            final double entryValue = entry.getValue();

            if (entryValue > bestValue) {
                bestDecision = entryDecision;
                bestValue = entryValue;
            }
        }

        return new Pair<>(bestDecision, bestValue);
    }

    @Test
    public void testDecision() throws Exception {
        final Scenario scenario = param.getKey();
        final Map<Decision, Double> decisionMap = param.getValue();
        final Decision bestDecision = maxOverMap(decisionMap).get(Decision.class);
        final double bestValue = maxOverMap(decisionMap).get(Double.class);

        final Pair<Decision, Double> p = d.computeBestScenarioResult(scenario, true);
        assertTrue("We should " + bestDecision + " on " + scenario, bestDecision.equals(p.get(Decision.class)));
        assertTrue("The expected value of " + scenario + " is about " + bestValue, approximatelyEqual(p.get(Double.class), bestValue));
    }
}