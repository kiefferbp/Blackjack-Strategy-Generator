package test;

import main.Decider;
import main.Decision;
import main.Pair;
import main.Scenario;

import java.util.Map;

/**
 * Created by Brian on 2/5/2017.
 */
public class TestUtils {
    private static final double ERROR_MARGIN = 0.005;
    private static final int SIMULATION_COUNT = 5000000;
    private static final Decider d = new Decider(1000, 1.0, SIMULATION_COUNT);

    private TestUtils() {}

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

    public static void testDecision(Map.Entry<Scenario, Map<Decision, Double>> param) throws Exception {
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
