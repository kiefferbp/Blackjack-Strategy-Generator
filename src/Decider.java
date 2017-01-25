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

    private int deckCount = 4;
    private double penetrationValue = 1.0;



    // returns a random hand with the given value with the given softness
    // NOTE: the caller must verify that this hand is valid for a given shoe.
    // (e.g., generateHand(21, false) may give [3, 3, 3, 3, 3, 6] which has too many 3's for one deck)
    private List<Card> generateHandWithValue(final int value, final boolean isSoft) throws Exception {
        Callable<List<Card>> task = new Callable<List<Card>>() {
            @Override
            public List<Card> call() throws Exception {
                Player player = new Player();

                while (player.getHandValue() != value || player.handIsSoft() != isSoft) {
                    final Card randomCard = Card.getRandom();
                    player.addCard(randomCard);

                    if (player.getHandValue() > 21) {
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

    private Scenario buildScenario(Player player, Player dealer) {
        Scenario scenario = new Scenario();
        scenario.playerValue = player.getHandValue();
        scenario.dealerCard = Card.getCardWithValue(dealer.getHandValue());
        scenario.isPlayerSoft = player.handIsSoft();
        scenario.isPair = false; // for now

        return scenario;
    }

    // returns an Object array with two elements: the modified shoe, and the player hand
    private Object[] getShoePlayerHandPair(int deckCount, double penetrationValue, Scenario scenario) throws Exception {
        final Shoe shoe = new Shoe(deckCount, penetrationValue);

        // remove the dealer card from the shoe
        final Card dealerCard = scenario.dealerCard;
        shoe.removeCard(dealerCard);

        // generate a random player hand
        final List<Card> playerCards = generateHandWithValue(scenario.playerValue, scenario.isPlayerSoft);
        final int numOfPlayerCards = playerCards.size();

        // remove each player card if the shoe has the card
        // if the shoe doesn't have the card, we will need to generate another hand
        int numCardsRemoved = 0;
        for (Card playerCard : playerCards) {
            if (!shoe.removeCard(playerCard)) {
                break;
            }

            numCardsRemoved += 1;
        }

        if (numCardsRemoved != numOfPlayerCards) { // a player card wasn't found in the shoe
            // retry, which will most likely generate a valid shoe
            return getShoePlayerHandPair(deckCount, penetrationValue, scenario);
        }

        return new Object[]{shoe, playerCards};
    }

    // simulates and returns the PlayResult that occurs when standing under a scenario
    private PlayResult simulateStanding(Player player, Player dealer, Shoe shoe, boolean dealerHitsSoft17) throws Exception {
        // play out the dealer's hand
        while (dealer.getHandValue() < 17 || (dealer.getHandValue() == 17 && dealer.handIsSoft() && dealerHitsSoft17)) {
            dealer.hit(shoe);
        }

        // determine the result
        final int finalDealerValue = dealer.getHandValue();
        final int playerValue = player.getHandValue();
        if (playerValue > finalDealerValue || finalDealerValue > 21) {
            return PlayResult.WIN;
        } else if (playerValue < dealer.getHandValue()) {
            return PlayResult.LOSE;
        } else {
            return PlayResult.PUSH;
        }
    }

    // for memoization
    Map<Scenario, Double> expectedHitMap = new HashMap<>();
    private double getExpectedHitValue(Player player, Player dealer, Shoe shoe, boolean dealerHitsSoft17) throws Exception {
        Scenario currentScenario = new Scenario();
        currentScenario.playerValue = player.getHandValue();
        currentScenario.dealerCard = Card.getCardWithValue(dealer.getHandValue());
        currentScenario.isPlayerSoft = player.handIsSoft();
        currentScenario.isPair = false; // for now

        // base case: the player is guaranteed to bust if he hits a hard 21
        if (player.getHandValue() == 21 && !player.handIsSoft()) {
            expectedHitMap.put(currentScenario, -1.0);
            return -1.0;
        }

        // take a hit
        player.hit(shoe);

        // did we bust?

        final Player player = new Player();
        final Object[] handShoePair = getShoePlayerHandPair(deckCount, penetrationValue, scenario);
        final Shoe shoe = (Shoe) handShoePair[0];
        final List<Card> playerHand = (List<Card>) handShoePair[1];

    }

    private Map<Scenario, Map<Decision, Double>> computeExpectedValues() {

    }
}
