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

    public static void shutDownThreads() {
        executor.shutdownNow();
    }

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

        // if the dealer has an Ace or a 10-valued card, the card at the top of the shoe cannot give him Blackjack
        if ((scenario.dealerCard.equals(Card.ACE) && shoe.peekTopCard().getValue() == 10) ||
                (scenario.dealerCard.getValue() == 10 && shoe.peekTopCard().equals(Card.ACE))) {
            // retry
            return getShoePlayerHandPair(deckCount, penetrationValue, scenario);
        }
        return new Object[]{shoe, playerCards};
    }

    // simulates and returns the PlayResult that occurs when standing under a scenario
    private PlayResult simulateStanding(Player player, Player dealer, boolean dealerHitsSoft17) throws Exception {
        // play out the dealer's hand
        while (dealer.getHandValue() < 17 || (dealer.getHandValue() == 17 && dealer.handIsSoft() && dealerHitsSoft17)) {
            dealer.hit();
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

    Map<Scenario, Double> expectedHitMap = new HashMap<>(); // for memoization
    private double getExpectedHitValue(Scenario scenario, boolean dealerHitsSoft17) throws Exception {
        if (expectedHitMap.get(scenario) != null) {
            return expectedHitMap.get(scenario);
        }

        // base case: the player is guaranteed to bust if he hits a hard 21
        if (scenario.playerValue == 21 && !scenario.isPlayerSoft) {
            expectedHitMap.put(scenario, -1.0);
            return -1.0;
        }

        double unitsWon = 0;
        for (int i = 0; i < SIMULATION_COUNT; i++) {
            // generate a random shoe and player hand under this scenario
            final Object[] shoePlayerHandPair = getShoePlayerHandPair(deckCount, penetrationValue, scenario);
            final Shoe shoe = (Shoe) shoePlayerHandPair[0];
            final Player player = new Player(shoe);
            final Player dealer = new Player(shoe);
            final List<Card> playerHand = (List<Card>) shoePlayerHandPair[1];
            dealer.addCard(scenario.dealerCard);
            player.addAllCards(playerHand);

            // take a hit
            player.hit();

            // did we bust?
            if (player.getHandValue() > 21) {
                unitsWon -= 1;
            } else { // inductive step: get the expected value of the best strategy with the new hand
                Scenario newScenario = buildScenario(player, dealer);
                unitsWon += computeBestScenarioResult(newScenario, dealerHitsSoft17).getValue();
            }
        }

        final double expectedWinnings = unitsWon / SIMULATION_COUNT;
        expectedHitMap.put(scenario, expectedWinnings);
        return expectedWinnings;
    }

    Map<Scenario, Double> expectedStandMap = new HashMap<>(); // for memoization
    private double getExpectedStandValue(Scenario scenario, boolean dealerHitsSoft17) throws Exception {
        if (expectedStandMap.get(scenario) != null) {
            return expectedStandMap.get(scenario);
        }

        double unitsWon = 0;
        // debug
        int wins = 0;
        int pushes = 0;
        int losses = 0;
        for (int i = 0; i < SIMULATION_COUNT; i++) {
            // generate a random shoe and player hand under this scenario
            final Object[] shoePlayerHandPair = getShoePlayerHandPair(deckCount, penetrationValue, scenario);
            final Shoe shoe = (Shoe) shoePlayerHandPair[0];
            final Player player = new Player(shoe);
            final Player dealer = new Player(shoe);
            final List<Card> playerHand = (List<Card>) shoePlayerHandPair[1];
            dealer.addCard(scenario.dealerCard);
            player.addAllCards(playerHand);

            // play this scenario out
            final PlayResult result = simulateStanding(player, dealer, dealerHitsSoft17);

            if (result.equals(PlayResult.WIN)) {
                //System.out.println("I won");
                unitsWon += 1;
                wins += 1;
            }

            if (result.equals(PlayResult.LOSE)) {
                //System.out.println("I lost");
                unitsWon -= 1;
                losses += 1;
            }

            if (result.equals(PlayResult.PUSH)) {
                //System.out.println("I pushed");
                pushes += 1;
            }

            System.out.println("wins: " + wins + ", pushes: " + pushes + ", losses: " + losses);
        }

        final double expectedWinnings = unitsWon / SIMULATION_COUNT;

        System.out.println("standing result (" + scenario + "): " + expectedWinnings);
        expectedStandMap.put(scenario, expectedWinnings);
        return expectedWinnings;
    }

    public DecisionValuePair computeBestScenarioResult(Scenario scenario, boolean dealerHitsSoft17) throws Exception {
        final double expectedHitValue = getExpectedHitValue(scenario, dealerHitsSoft17);
        final double expectedStandValue = getExpectedStandValue(scenario, dealerHitsSoft17);

        if (expectedHitValue > expectedStandValue) {
            return new DecisionValuePair(Decision.HIT, expectedHitValue);
        }

        return new DecisionValuePair(Decision.STAND, expectedStandValue);
    }

    private Map<Scenario, Map<Decision, Double>> computeExpectedValues() {
        return null;
    }

    public static void main(String[] args) throws Exception {
        Decider d = new Decider();
        Scenario scenario = new Scenario();
        scenario.playerValue = 21;
        scenario.isPlayerSoft = false;
        scenario.isPair = false;
        scenario.dealerCard = Card.TWO;

        DecisionValuePair p = d.computeBestScenarioResult(scenario, false);
        if (p.getDecision().equals(Decision.HIT)) {
            System.out.println(scenario + " best strategy: HIT (" + p.getValue() + ")");
        } else {
            System.out.println(scenario + " best strategy: STAND (" + p.getValue() + ")");
        }

        Decider.shutDownThreads();
    }
}
