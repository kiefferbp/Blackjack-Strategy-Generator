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
public class HardHandTest {
    private static final int SIMULATION_COUNT = 5000000;
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

        // hard 13 V 3: standing = -0.252250, hitting = -0.291210, doubling = -0.582420, splitting = Integer.MIN_VALUE
        final Scenario h13v3Scenario = new ScenarioBuilder()
                .setPlayerValue(13)
                .setDealerCard(Card.THREE)
                .setSoftFlag(false)
                .setPairFlag(false)
                .build();
        final Map<Decision, Double> h13v3Map = new HashMap<>();
        h13v3Map.put(Decision.STAND, -0.252250);
        h13v3Map.put(Decision.HIT, -0.291210);
        h13v3Map.put(Decision.DOUBLE, -0.582420);
        h13v3Map.put(Decision.SPLIT, (double) Integer.MIN_VALUE);
        paramMap.put(h13v3Scenario, h13v3Map);

        return paramMap.entrySet();
    }

    @Parameterized.Parameter
    public Map.Entry<Scenario, Map<Decision, Double>> param;

    @Test
    public void testDecision() throws Exception {
        TestUtils.testDecision(d, param);
    }
}