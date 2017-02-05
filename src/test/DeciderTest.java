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
    private static final double ERROR_MARGIN = 0.005;
    private static final Decider d = new Decider(1000, 1.0);

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

        // hard 16 V 10: standing = -0.540430, hitting = -0.539826, doubling = -1.079653, splitting = Integer.MIN_VALUE
        final Scenario h16v10Scenario = new ScenarioBuilder()
                .setPlayerValue(16)
                .setDealerCard(Card.TEN)
                .setSoftFlag(false)
                .setPairFlag(false)
                .build();
        final Map<Decision, Double> h16v10Map = new HashMap<>();
        h16v10Map.put(Decision.STAND, -0.540430);
        h16v10Map.put(Decision.HIT, -0.539826);
        h16v10Map.put(Decision.DOUBLE, -1.079653);
        h16v10Map.put(Decision.SPLIT, (double) Integer.MIN_VALUE);
        paramMap.put(h16v10Scenario, h16v10Map);

        // hard 11 V 6: standing = -0.153699, hitting = 0.333690, doubling = 0.667380, splitting = Integer.MIN_VALUE
        final Scenario h11v6Scenario = new ScenarioBuilder()
                .setPlayerValue(11)
                .setDealerCard(Card.SIX)
                .setSoftFlag(false)
                .setPairFlag(false)
                .build();
        final Map<Decision, Double> h11v6Map = new HashMap<>();
        h11v6Map.put(Decision.STAND, -0.153699);
        h11v6Map.put(Decision.HIT, 0.333690);
        h11v6Map.put(Decision.DOUBLE, 0.667380);
        h11v6Map.put(Decision.SPLIT, (double) Integer.MIN_VALUE);
        paramMap.put(h11v6Scenario, h11v6Map);

        return paramMap.entrySet();
    }

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

    @Parameterized.Parameter
    public Map.Entry<Scenario, Map<Decision, Double>> param;

    @Test
    public void testDecision() throws Exception {
        final Scenario scenario = param.getKey();
        final Map<Decision, Double> decisionMap = param.getValue();
        final Decision bestDecision = maxOverMap(decisionMap).get(Decision.class);
        final double bestValue = maxOverMap(decisionMap).get(Double.class);

        final Pair<Decision, Double> p = d.computeBestScenarioResult(scenario, true);
        final Map<Decision, Double> scenarioExpectedValues = d.computeExpectedValues(scenario, true);

        for (Map.Entry<Decision, Double> entry : scenarioExpectedValues.entrySet()) {
            final Decision decision = entry.getKey();
            final double decisionValue = entry.getValue();
            final double targetValue = decisionMap.get(decision);

            assertTrue("The expected value of " + decision + " on " + scenario + " is about " + targetValue, approximatelyEqual(decisionValue, targetValue));
        }

        assertTrue("We should " + bestDecision + " on " + scenario, bestDecision.equals(p.get(Decision.class)));
        assertTrue("The expected value of " + scenario + " is about " + bestValue, approximatelyEqual(p.get(Double.class), bestValue));
    }
}