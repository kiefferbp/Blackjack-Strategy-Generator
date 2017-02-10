package test;

import main.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.HashMap;
import java.util.Map;

import static test.TestUtils.approximatelyEqual;
import static test.TestUtils.assertTrue;
import static test.TestUtils.maxOverMap;

/**
 * Created by Brian on 2/5/2017.
 */
@RunWith(Parameterized.class)
public class SurrenderTest {
    private static final int SIMULATION_COUNT = 1000000;
    private static final Rule r = new RuleBuilder()
            .setDeckCount(1000) // essentially infinite
            .setPenetrationValue(1.0)
            .setDealerHitsSoft17(true)
            .setCanSurrender(true)
            .setMaxSplitHands(4)
            .build();
    private static final Decider d = new Decider(r, SIMULATION_COUNT);

    @Parameterized.Parameters
    public static Iterable<?> data() {
        // source: http://wizardofodds.com/games/blackjack/appendix/1/
        final Map<Scenario, Decision> paramMap = new HashMap<>();

        // pair of 8's V A, dealer H17: best move is to surrender
        final Scenario p8svAScenario = new ScenarioBuilder()
                .setPlayerValue(16)
                .setDealerCard(Card.ACE)
                .setSoftFlag(false)
                .setPairFlag(true)
                .build();
        paramMap.put(p8svAScenario, Decision.SURRENDER);

        // hard 16 V A, dealer H17: best move is to surrender
        final Scenario h16svAScenario = new ScenarioBuilder()
                .setPlayerValue(16)
                .setDealerCard(Card.ACE)
                .setSoftFlag(false)
                .setPairFlag(false)
                .build();
        paramMap.put(h16svAScenario, Decision.SURRENDER);

        // hard 17 V A, dealer H17: best move is to surrender
        final Scenario h17svAScenario = new ScenarioBuilder()
                .setPlayerValue(17)
                .setDealerCard(Card.ACE)
                .setSoftFlag(false)
                .setPairFlag(false)
                .build();
        paramMap.put(h17svAScenario, Decision.SURRENDER);

        return paramMap.entrySet();
    }

    @Parameterized.Parameter
    public Map.Entry<Scenario, Decision> param;

    @Test
    public void testDecision() throws Exception {
        final Scenario scenario = param.getKey();
        final Decision bestDecision = param.getValue();

        final Pair<Decision, Double> p = d.computeBestScenarioResult(scenario);

        assertTrue("We should " + bestDecision + " on " + scenario, bestDecision.equals(p.get(Decision.class)));
    }
}
