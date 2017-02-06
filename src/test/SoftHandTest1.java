package test;

import main.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Brian on 2/5/2017.
 */
@RunWith(Parameterized.class)
public class SoftHandTest1 {
    private static final int SIMULATION_COUNT = 5000000;
    private static final Rule r = new RuleBuilder()
            .setDeckCount(1000) // essentially infinite
            .setPenetrationValue(1.0)
            .setDealerHitsSoft17(false)
            .setMaxSplitHands(4)
            .build();
    private static final Decider d = new Decider(r, SIMULATION_COUNT);

    @Parameterized.Parameters
    // I would add more test cases here, but two cases causes this to exceed 50 minutes (the limit for Travis CI jobs).
    // As a result, I am splitting up the soft test cases into multiple cases and running each class as a job.
    public static Iterable<?> data() {
        // source: http://wizardofodds.com/games/blackjack/appendix/1/
        final Map<Scenario, Map<Decision, Double>> paramMap = new HashMap<>();

        // soft 18 V 6: standing = 0.283444, hitting = 0.190753, doubling = 0.381506, splitting = Integer.MIN_VALUE
        final Scenario s18v6MapScenario = new ScenarioBuilder()
                .setPlayerValue(18)
                .setDealerCard(Card.SIX)
                .setSoftFlag(true)
                .setPairFlag(false)
                .build();
        final Map<Decision, Double> s18v6Map = new HashMap<>();
        s18v6Map.put(Decision.STAND, 0.283444);
        s18v6Map.put(Decision.HIT, 0.190753);
        s18v6Map.put(Decision.DOUBLE, 0.381506);
        s18v6Map.put(Decision.SPLIT, (double) Integer.MIN_VALUE);
        paramMap.put(s18v6MapScenario, s18v6Map);

        return paramMap.entrySet();
    }

    @Parameterized.Parameter
    public Map.Entry<Scenario, Map<Decision, Double>> param;

    @Test
    public void testDecision() throws Exception {
        TestUtils.testDecision(d, param);
    }
}
