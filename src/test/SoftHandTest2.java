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
public class SoftHandTest2 {
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

        // soft 17 V 10: standing = -0.419721, hitting = -0.196867, doubling = -0.458316, splitting = Integer.MIN_VALUE
        final Scenario s17v10Scenario = new ScenarioBuilder()
                .setPlayerValue(17)
                .setDealerCard(Card.TEN)
                .setSoftFlag(true)
                .setPairFlag(false)
                .build();
        final Map<Decision, Double> s17v10Map = new HashMap<>();
        s17v10Map.put(Decision.STAND, -0.419721);
        s17v10Map.put(Decision.HIT, -0.196867);
        s17v10Map.put(Decision.DOUBLE, -0.458316);
        s17v10Map.put(Decision.SPLIT, (double) Integer.MIN_VALUE);
        paramMap.put(s17v10Scenario, s17v10Map);

        return paramMap.entrySet();
    }

    @Parameterized.Parameter
    public Map.Entry<Scenario, Map<Decision, Double>> param;

    @Test
    public void testDecision() throws Exception {
        TestUtils.testDecision(d, param);
    }
}
