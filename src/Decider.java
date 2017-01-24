import java.util.HashMap;
import java.util.Map;

/**
 * Created by Brian on 1/17/2017.
 */
public class Decider {
    private static final int SIMULATION_COUNT = 1000000;
    private final Map<Scenario, HashMap<Decision, Double>> expectedValues = new HashMap<>();

    private int playerValue;
    private int dealerValue;
    private int deckCount;
    private double penetrationValue;
    private boolean isSoft;
    private boolean isPair;

    private Shoe buildShoe() {
        return new ShoeBuilder()
                .setPlayerValue(playerValue)
                .setDealerValue(dealerValue)
                .setDeckCount(deckCount)
                .setPenetrationValue(penetrationValue)
                .setSoftFlag(isSoft)
                .setPairFlag(isPair)
                .build();
    }

    // returns the PlayResult of what happens if the player stands under this scenario
    private PlayResult performStandSimulation(Scenario scenario) {
        final int playerValue = scenario.playerValue;
        final Shoe shoe = buildShoe();
        final Card holeCard = shoe.removeCard();
        int currentDealerValue = scenario.dealerValue + holeCard.getValue();

        if (currentDealerValue == 21) { // dealer blackjack
            if (scenario.isPlayerSoft && scenario.playerValue == 21) { // player also has one
                return PlayResult.PUSH;
            }

            return PlayResult.LOSE;
        }

        if (decision == decision.STAND) {
            // play out the dealer's hand

        }
    }

    // we have the following scenarios that build upon previous ones:
    // (1) hard 21, 20, ..., 11 --- non-pairs (as hard 11+ hands remain hard)
    // (2) soft A-10, A-9, ..., A-2 (when hand becomes hard, it becomes a hard 14+)
    // (3) hard 10, 9, ..., 2 --- non-pairs (hitting results in scenario 1 or 2)
    // (4) pairs (choices depend on scenarios 1-3)
    private Map<Decision, Double> computeScenarioExpectedValues(Scenario scenario) {
        // base case: hard 21
        if (scenario.playerValue == 21 && !scenario.isPlayerSoft) {
            final Map<Decision, Double> expectedMap = new HashMap<>();
            expectedMap.put(Decision.HIT, -1); // hitting always loses
            expectedMap.put(Decision.STAND, 1 - 0.0736); // from https://www.lolblackjack.com/blackjack/probability-odds/
            expectedMap.put(Decision.DOUBLE, -2);
            expectedMap.put(Decision.SPLIT, Double.MIN_VALUE);
            return expectedMap;
        }

        // scenario 1: hard 20, 19, ..., 11
        if (scenario.playerValue >= 11 && !scenario.isPlayerSoft) {

        }
    }

    Decider(int playerValue, int dealerValue, int deckCount, double penetrationValue, boolean isSoft, boolean isPair) {
        // set member variables
        this.playerValue = playerValue;
        this.dealerValue = dealerValue;
        this.deckCount = deckCount;
        this.penetrationValue = penetrationValue;
        this.isSoft = isSoft;
        this.isPair = isPair;

        // base case: hard 21
        for (int dealerVal = 2; dealerVal <= 11; dealerVal++) {
            final Scenario hard21Scenario = new Scenario();
            hard21Scenario.playerValue = 21;
            hard21Scenario.dealerValue = dealerVal;
            hard21Scenario.isPlayerSoft = false;
            hard21Scenario.isDealerSoft = (dealerVal == 11);

            // simulate this scenario
            for (int i = 0; i < SIMULATION_COUNT; i++) {
                final Shoe shoe = buildShoe();
            }
        }
    }
}
