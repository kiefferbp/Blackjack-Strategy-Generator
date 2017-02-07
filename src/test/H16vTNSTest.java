package test;

import main.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Brian on 2/7/2017.
 */
@RunWith(Parameterized.class)
public class H16vTNSTest {
    private static final int SIMULATION_COUNT = 10000000;
    private static final Rule r = new RuleBuilder()
            .setDeckCount(1000) // essentially infinite
            .setPenetrationValue(1.0)
            .setDealerHitsSoft17(false)
            .setCanSurrender(false)
            .setMaxSplitHands(4)
            .build();
    private static final Decider d = new Decider(r, SIMULATION_COUNT);

    @Parameterized.Parameters
    public static Iterable<?> data() {
        // source: http://wizardofodds.com/games/blackjack/appendix/1/
        final Map<Scenario, Map<Decision, Double>> paramMap = new HashMap<>();

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

        return paramMap.entrySet();
    }

    @Parameterized.Parameter
    public Map.Entry<Scenario, Map<Decision, Double>> param;

    @Test
    public void testDecision() throws Exception {
        TestUtils.testDecision(d, param);
    }
}
