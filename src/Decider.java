import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Brian on 1/17/2017.
 */
public class Decider {
    private static final int SIMULATION_COUNT = 1000000;
    private static final int threadCount = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

    private final Map<Scenario, HashMap<Decision, Double>> expectedValues = new HashMap<>();

    private int playerValue;
    private int dealerValue;
    private int deckCount;
    private double penetrationValue;
    private boolean isSoft;
    private boolean isPair;

    // returns a random hand with the given value with the given softness
    // NOTE: the caller must verify that this hand is valid for a given shoe
    // (e.g., generateHand(21, false) may give [3, 3, 3, 3, 3, 6] which has too many 3's for one deck
    private List<Card> generateHandWithValue(final int value, final boolean isSoft) throws Exception {
        //final int threadCount = Runtime.getRuntime().availableProcessors();
        //final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        Callable<List<Card>> task = new Callable<List<Card>>() {
            @Override
            public List<Card> call() throws Exception {
                Player player = new Player();
                //System.out.println("beginning loop. player value: " + player.getHandValue());

                while (player.getHandValue() != value || player.handIsSoft() != isSoft) {
                    final Card randomCard = Card.getRandom();
                    player.addCard(randomCard);
                    //System.out.println("added card " + randomCard + ". new value: " + player.getHandValue());

                    if (player.getHandValue() > 21) {
                        //System.out.println("over 21. resetting.");
                        player = new Player(); // very, very dirty
                    }
                }

                return player.getCards();
            }
        };

        List<Callable<List<Card>>> taskList = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            taskList.add(task);
        }

        List<Card> result = executor.invokeAny(taskList);
        return result;
    }

    /*
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

    */

    public static void main(String[] args) throws Exception {
        //System.out.println("generating a hard 16...");
        final long startTime = System.nanoTime();
        Decider decider = new Decider();

        for (int i = 0; i < 1000000; i++) {
            List<Card> hand = decider.generateHandWithValue(16, false);
            System.out.print("Computing hand " + i + " of 1000000...\r");
        }

        /*StringBuilder result = new StringBuilder();

        for (Card card : hand) {
            result.append(card.toString() + " ");
        }*/

        //System.out.println("result: " + result);
        final long endTime = System.nanoTime();
        System.out.print("finished generating 1M hands. avg computation time per 1K hands: " + (endTime - startTime)/1000000000 + "ms\r");
    }
}
