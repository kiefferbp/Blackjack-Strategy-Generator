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
public class SoftHandTest {
    private static final int SIMULATION_COUNT = 1000000;
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

        // soft 19 V 2: standing = 0.386305, hitting = 0.123958, doubling = 0.241855, splitting = Integer.MIN_VALUE
        final Scenario s19v2Scenario = new ScenarioBuilder()
                .setPlayerValue(19)
                .setDealerCard(Card.TWO)
                .setSoftFlag(true)
                .setPairFlag(false)
                .build();
        final Map<Decision, Double> s19v2Map = new HashMap<>();
        s19v2Map.put(Decision.STAND, 0.386305);
        s19v2Map.put(Decision.HIT, 0.123958);
        s19v2Map.put(Decision.DOUBLE, 0.241855);
        s19v2Map.put(Decision.SPLIT, (double) Integer.MIN_VALUE);
        paramMap.put(s19v2Scenario, s19v2Map);

        return paramMap.entrySet();
    }

    @Parameterized.Parameter
    public Map.Entry<Scenario, Map<Decision, Double>> param;

    @Test
    public void testDecision() throws Exception {
        TestUtils.testDecision(d, param);
    }
}
