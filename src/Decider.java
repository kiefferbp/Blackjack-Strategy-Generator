import java.util.*;
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
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    private int deckCount = 4;
    private double penetrationValue = 1.0;

    public static void shutDownThreads() {
        executor.shutdownNow();
    }

    // returns a random hand with the given value with the given softness
    // NOTE: the caller must verify that this hand is valid for a given shoe.
    // (e.g., generateHand(21, false) may give [3, 3, 3, 3, 3, 6] which has too many 3's for one deck)
    private List<Card> generateHandWithValue(final int targetValue, final boolean targetIsSoft) throws Exception {

        final Callable<List<Card>> task = () -> {
            final Player player = new Player();
            final Callable<Optional<Void>> resetHandTask = () -> {
                player.resetHand();

                if (targetIsSoft) {
                    player.addCard(Card.ACE);
                }

                return Optional.empty();
            };

            if (targetIsSoft) {
                player.addCard(Card.ACE);
            }

            while (player.getHandValue() != targetValue || player.handIsSoft() != targetIsSoft) {
                final int currentHandValue = player.getHandValue();
                final boolean isCurrentlySoft = player.handIsSoft();

                // Explanation: we can build a hand of value |currentHandValue| to one of value |targetValue|
                // by noting that we can usually pick a random card whose value is at most |targetValue| - |currentHandValue|.
                // There are some exceptions to this, however.
                // (i) If |targetValue| < 11 and |targetValue| - |currentHandValue| = 1, building isn't possible
                // (e.g., a hard 9 cannot become a hard 10, as adding an ace to a hard 9 makes a soft 20)
                // (ii) If |isCurrentlySoft| is true and |targetIsSoft| is false, we can take any card that
                // results in either another soft hand or one that tips the new value <= |targetValue|. (see the if statement)
                Card randomCard = null;
                if (targetValue < 11 && targetValue == currentHandValue + 1) { // scenario (i): impossible to build
                    // start over
                    resetHandTask.call();
                    continue;
                } else if (targetValue == currentHandValue + 1) {
                    randomCard = Card.ACE;
                } else if (isCurrentlySoft && !targetIsSoft) { // scenario (ii)
                    if (currentHandValue > targetValue) {
                        // Suppose that you want to generate a hard T from a soft X hand, X > T and T > 11. If you draw a
                        // card with value V such that X + V > 21 (i.e., the hand becomes hard), the resulting
                        // hand is a hard X + V - 10. Thus, X + V - 10 <= T and so V <= T - X + 10.
                        // For example, to generate a hard 13 from a soft 19 we can only randomly pick a card V among those
                        // whose values are <= 13 - 19 + 10 = 4 (Ace, 2, 3, and 4).
                        final int maxRandomCardValue = targetValue - currentHandValue + 10;

                        if (maxRandomCardValue < 1) { // impossible
                            resetHandTask.call();
                            continue;
                        }

                        randomCard = Card.getRandomWithMaxRange(maxRandomCardValue);
                    } else {
                        randomCard = Card.getRandom();
                    }
                } else {
                    final int maxRandomCardValue = targetValue - currentHandValue;
                    randomCard = Card.getRandomWithMaxRange(maxRandomCardValue);
                }

                player.addCard(randomCard);
            }

            return player.getCards();
        };

        final List<Callable<List<Card>>> taskList = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            taskList.add(task);
        }

        final List<Card> result = executor.invokeAny(taskList);
        return result;
    }

    private Scenario buildScenario(Player player, Player dealer) {
        final Scenario scenario = new Scenario();
        scenario.playerValue = player.getHandValue();
        scenario.dealerCard = Card.getCardWithValue(dealer.getHandValue());
        scenario.isPlayerSoft = player.handIsSoft();
        scenario.isPair = false; // for now

        return scenario;
    }

    private boolean isBlackjackPair(Card card1, Card card2) {
        return ((card1.equals(Card.ACE) && card2.getValue() == 10) ||
                (card1.getValue() == 10 && card2.equals(Card.ACE)));
    }

    // returns an Object array with two elements: the modified shoe, and the player hand
    private Object[] getShoePlayerHandPair(int deckCount, double penetrationValue, Scenario scenario) throws Exception {
        final Shoe shoe = new Shoe(deckCount, penetrationValue);

        // remove the dealer card from the shoe
        final Card dealerCard = scenario.dealerCard;
        shoe.removeCard(dealerCard);

        // generate a random player hand
        List<Card> playerCards = generateHandWithValue(scenario.playerValue, scenario.isPlayerSoft);
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
        // if the dealer has an Ace or a 10-valued card, the card at the top of the shoe cannot give him a blackjack
        // if the top card yields a BJ, shuffle the shoe until it no longer does
        final Card dealerCard = dealer.getCards().get(0); // the dealer only has one card right now
        while (isBlackjackPair(dealerCard, shoe.peekTopCard())) {
            shoe.shuffle();
        }

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

    private PlayResult simulateBestPlay(Player player, Player dealer, Shoe shoe, boolean dealerHitsSoft17) throws Exception {
        final Scenario scenario = buildScenario(player, dealer);
        final Decision bestDecision = computeBestScenarioResult(scenario, dealerHitsSoft17).getDecision();

        switch (bestDecision) {
            case HIT:
                player.hit();

                if (player.getHandValue() > 21) {
                    return PlayResult.LOSE;
                } else {
                    return simulateBestPlay(player, dealer, shoe, dealerHitsSoft17);
                }
            case STAND:
                return simulateStanding(player, dealer, shoe, dealerHitsSoft17);
            default:
                return null; // other decisions will come later
        }

    }

    final Map<Scenario, Double> expectedHitMap = new HashMap<>(); // for memoization
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

            if (player.getHandValue() > 21) {
                unitsWon -= 1;
            } else {
                // inductive step: simulate best play on the new hand
                final PlayResult bestPlayResult = simulateBestPlay(player, dealer, shoe, dealerHitsSoft17);
                unitsWon += bestPlayResult.getWinAmount();
            }
        }

        final double expectedWinnings = unitsWon / SIMULATION_COUNT;
        System.out.println("hitting result (" + scenario + "): " + expectedWinnings);
        expectedHitMap.put(scenario, expectedWinnings);
        return expectedWinnings;
    }

    final Map<Scenario, Double> expectedStandMap = new HashMap<>(); // for memoization
    private double getExpectedStandValue(final Scenario scenario, final boolean dealerHitsSoft17) throws Exception {
        if (expectedStandMap.get(scenario) != null) {
            return expectedStandMap.get(scenario);
        }

        final Callable<Double> task = () -> {
            double unitsWon = 0;
            for (int i = 0; i < Math.ceil(SIMULATION_COUNT / threadCount); i++) {
                // generate a random shoe and player hand under this scenario
                final Object[] shoePlayerHandPair = getShoePlayerHandPair(deckCount, penetrationValue, scenario);
                final Shoe shoe = (Shoe) shoePlayerHandPair[0];
                final Player player = new Player(shoe);
                final Player dealer = new Player(shoe);
                final List<Card> playerHand = (List<Card>) shoePlayerHandPair[1];
                dealer.addCard(scenario.dealerCard);
                player.addAllCards(playerHand);

                // play this scenario out
                final PlayResult result = simulateStanding(player, dealer, shoe, dealerHitsSoft17);
                unitsWon += result.getWinAmount();
            }

            return unitsWon;
        };

        final List<Callable<Double>> taskList = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            taskList.add(task);
        }

        double totalUnitsWon = 0;
        for (Future<Double> future : executor.invokeAll(taskList)) {
            totalUnitsWon += future.get();
        }

        final double expectedWinnings = totalUnitsWon / (threadCount * Math.ceil(SIMULATION_COUNT / threadCount));
        System.out.println("standing result (" + scenario + "): " + expectedWinnings);
        expectedStandMap.put(scenario, expectedWinnings);
        return expectedWinnings;
    }

    double getExpectedSplitValue(Scenario scenario, boolean dealerHitsSoft17) throws Exception {
        if (!scenario.isPair) { // can't split
            return Integer.MIN_VALUE;
        }

        double unitsWon = 0;
        for (int i = 0; i < SIMULATION_COUNT; i++) {
            final Shoe shoe = new Shoe(deckCount, penetrationValue);
            final Card playerCard = Card.getCardWithValue(scenario.playerValue / 2);
            final Card dealerCard = scenario.dealerCard;
            shoe.removeCard(playerCard);
            shoe.removeCard(playerCard);
            shoe.removeCard(dealerCard);

            final Player playerHand1 = new Player(shoe);
            final Player playerHand2 = new Player(shoe);
            final Player dealer = new Player(shoe);
            dealer.addCard(dealerCard);
            playerHand1.addCard(playerCard);
            playerHand2.addCard(playerCard);

            // each player hand is required to take a second card
            playerHand1.hit();
            playerHand2.hit();

            final Scenario scenarioHand1 = buildScenario(playerHand1, dealer);
            final Scenario scenarioHand2 = buildScenario(playerHand2, dealer);
            if (playerCard.equals(Card.ACE)) {
                // if we split aces, we cannot take more cards
                final double expectedStandHand1 = getExpectedStandValue(scenarioHand1, dealerHitsSoft17);
                final double expectedStandHand2 = getExpectedStandValue(scenarioHand2, dealerHitsSoft17);
                unitsWon += expectedStandHand1 + expectedStandHand2;
            } else {
                final double expectedBestHand1 = computeBestScenarioResult(scenarioHand1, dealerHitsSoft17).getValue();
                final double expectedBestHand2 = computeBestScenarioResult(scenarioHand2, dealerHitsSoft17).getValue();
                unitsWon += expectedBestHand1 + expectedBestHand2;
            }
        }

        return unitsWon / SIMULATION_COUNT;
    }

    public Map<Decision, Double> computeExpectedValues(Scenario scenario, boolean dealerHitsSoft17) throws Exception {
        final Map<Decision, Double> expectedValueMap = new HashMap<>();
        expectedValueMap.put(Decision.HIT, getExpectedHitValue(scenario, dealerHitsSoft17));
        expectedValueMap.put(Decision.STAND, getExpectedStandValue(scenario, dealerHitsSoft17));
        expectedValueMap.put(Decision.SPLIT, getExpectedSplitValue(scenario, dealerHitsSoft17));

        return expectedValueMap;
    }

    public DecisionValuePair computeBestScenarioResult(Scenario scenario, boolean dealerHitsSoft17) throws Exception {
        final Map<Decision, Double> expectedValueMap = computeExpectedValues(scenario, dealerHitsSoft17);

        Decision bestExpectedDecision = null;
        double bestExpectedValue = Integer.MIN_VALUE;
        for (Map.Entry<Decision, Double> entry : expectedValueMap.entrySet()) {
            final Decision entryDecision = entry.getKey();
            final double entryExpectedValue = entry.getValue();

            if (entryExpectedValue > bestExpectedValue) {
                bestExpectedDecision = entryDecision;
                bestExpectedValue = entryExpectedValue;
            }
        }

        return new DecisionValuePair(bestExpectedDecision, bestExpectedValue);
    }

    public static void main(String[] args) throws Exception {
        final Decider d = new Decider();
        final Scenario scenario = new Scenario();
        scenario.playerValue = 16;
        scenario.isPlayerSoft = false;
        scenario.isPair = true;
        scenario.dealerCard = Card.ACE;

        System.out.println("solving " + scenario);
        final long startTime = System.nanoTime();
        //final Map<Decision, Double> expectedValueMap = d.computeExpectedValues(scenario, false);
        final DecisionValuePair p = d.computeBestScenarioResult(scenario, false);
        System.out.println(scenario + " best strategy: " + p.getDecision() + " (" + p.getValue() + ")");

        /*for (Map.Entry<Decision, Double> entry : expectedValueMap.entrySet()) {
            final Decision entryDecision = entry.getKey();
            final double entryExpectedValue = entry.getValue();

            System.out.println("Expected value of " + entryDecision + ": " + entryExpectedValue);
        }*/

        Decider.shutDownThreads();
        final long endTime = System.nanoTime();
        System.out.println("Computation time: " + (endTime - startTime)/1000000000.0 + "s");
    }
}
