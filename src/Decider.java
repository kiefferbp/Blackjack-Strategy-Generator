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
    private static final ExecutorService executorHands = Executors.newFixedThreadPool(threadCount);
    private static final ExecutorService executorStand = Executors.newFixedThreadPool(threadCount);

    private int deckCount = 4;
    private double penetrationValue = 1.0;

    public static void shutDownThreads() {
        executorHands.shutdownNow();
        executorStand.shutdownNow();
    }

    // returns a random hand with the given value with the given softness
    // NOTE: the caller must verify that this hand is valid for a given shoe.
    // (e.g., generateHand(21, false) may give [3, 3, 3, 3, 3, 6] which has too many 3's for one deck)
    private List<Card> generateHandWithValue(final int value, final boolean isSoft) throws Exception {
        Callable<List<Card>> task = () -> {
            Player player = new Player();

            if (isSoft) {
                player.addCard(Card.ACE);
            }

            while (player.getHandValue() != value || player.handIsSoft() != isSoft) {
                final Card randomCard = Card.getRandom();
                player.addCard(randomCard);

                if (player.getHandValue() > 21) {
                    player = new Player(); // very, very dirty
                }
            }

            return player.getCards();
        };

        List<Callable<List<Card>>> taskList = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            taskList.add(task);
        }

        List<Card> result = executorHands.invokeAny(taskList);
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

        // if the dealer has an Ace or a 10-valued card, the card at the top of the shoe cannot give him a blackjack
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

    private PlayResult simulateBestPlay(Player player, Player dealer, boolean dealerHitsSoft17) throws Exception {
        final Scenario scenario = buildScenario(player, dealer);
        final Decision bestDecision = computeBestScenarioResult(scenario, dealerHitsSoft17).getDecision();

        switch (bestDecision) {
            case HIT:
                player.hit();

                if (player.getHandValue() > 21) {
                    return PlayResult.LOSE;
                } else {
                    return simulateBestPlay(player, dealer, dealerHitsSoft17);
                }
            case STAND:
                return simulateStanding(player, dealer, dealerHitsSoft17);
            default:
                return null; // other decisions will come later
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

            if (player.getHandValue() > 21) {
                unitsWon -= 1;
            } else {
                // inductive step: simulate best play on the new hand
                final PlayResult bestPlayResult = simulateBestPlay(player, dealer, dealerHitsSoft17);
                unitsWon += bestPlayResult.getWinAmount();
            }
        }

        final double expectedWinnings = unitsWon / SIMULATION_COUNT;
        expectedHitMap.put(scenario, expectedWinnings);
        return expectedWinnings;
    }

    Map<Scenario, Double> expectedStandMap = new HashMap<>(); // for memoization
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
                final PlayResult result = simulateStanding(player, dealer, dealerHitsSoft17);
                unitsWon += result.getWinAmount();
            }

            return unitsWon;
        };

        final List<Callable<Double>> taskList = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            taskList.add(task);
        }

        double totalUnitsWon = 0;
        for (Future<Double> future : executorStand.invokeAll(taskList)) {
            totalUnitsWon += future.get();
        }

        final double expectedWinnings = totalUnitsWon / (threadCount * Math.ceil(SIMULATION_COUNT / threadCount));
        System.out.println("standing result (" + scenario + "): " + expectedWinnings);
        expectedStandMap.put(scenario, expectedWinnings);
        return expectedWinnings;
    }

    double getExpectedSplitValue(Scenario scenario, boolean dealerHitsSoft17) throws Exception {
        if (!scenario.isPair) { // can't split
            return Long.MIN_VALUE;
        }

        double unitsWon = 0;
        for (int i = 0; i < SIMULATION_COUNT; i++) {
            // generate a random shoe and player hand under this scenario
            final Object[] shoePlayerHandPair = getShoePlayerHandPair(deckCount, penetrationValue, scenario);
            final Shoe shoe = (Shoe) shoePlayerHandPair[0];
            final Player playerHand1 = new Player(shoe);
            final Player playerHand2 = new Player(shoe);
            final Player dealer = new Player(shoe);
            final Card playerCard = Card.getCardWithValue(scenario.playerValue / 2);
            dealer.addCard(scenario.dealerCard);
            playerHand1.addCard(playerCard);
            playerHand2.addCard(playerCard);

            // each player hand is required to take a second card
            playerHand1.hit();
            playerHand2.hit();

            PlayResult resultHand1;
            PlayResult resultHand2;
            if (playerCard.equals(Card.ACE)) {
                // if we split aces, we cannot take more cards
                resultHand1 = simulateStanding(playerHand1, dealer, dealerHitsSoft17);
                shoe.reset(); // to simulate the other hand independently (this is basic strategy after all)
                resultHand2 = simulateStanding(playerHand2, dealer, dealerHitsSoft17);
            } else {
                resultHand1 = simulateBestPlay(playerHand1, dealer, dealerHitsSoft17);
                shoe.reset();
                resultHand2 = simulateBestPlay(playerHand2, dealer, dealerHitsSoft17);
            }

            unitsWon += resultHand1.getWinAmount() + resultHand2.getWinAmount();
        }

        return unitsWon;
    }

    public DecisionValuePair computeBestScenarioResult(Scenario scenario, boolean dealerHitsSoft17) throws Exception {
        final Map<Decision, Double> expectedValueMap = new HashMap<>();
        expectedValueMap.put(Decision.HIT, getExpectedHitValue(scenario, dealerHitsSoft17));
        expectedValueMap.put(Decision.STAND, getExpectedStandValue(scenario, dealerHitsSoft17));
        expectedValueMap.put(Decision.SPLIT, getExpectedSplitValue(scenario, dealerHitsSoft17));

        final double bestExpectedValue = Collections.max(expectedValueMap.values());
        Decision bestExpectedDecision = null;
        for (Map.Entry<Decision, Double> entry : expectedValueMap.entrySet()) {
            final Decision entryDecision = entry.getKey();
            final double entryExpectedValue = entry.getValue();

            if (entryExpectedValue == bestExpectedValue) {
                bestExpectedDecision = entryDecision;
                break;
            }
        }

        return new DecisionValuePair(bestExpectedDecision, bestExpectedValue);
    }

    public static void main(String[] args) throws Exception {
        Decider d = new Decider();
        Scenario scenario = new Scenario();
        scenario.playerValue = 18;
        scenario.isPlayerSoft = true;
        scenario.isPair = false;
        scenario.dealerCard = Card.ACE;

        System.out.println("solving " + scenario);
        final long startTime = System.nanoTime();
        DecisionValuePair p = d.computeBestScenarioResult(scenario, false);
        if (p.getDecision().equals(Decision.HIT)) {
            System.out.println(scenario + " best strategy: HIT (" + p.getValue() + ")");
        } else {
            System.out.println(scenario + " best strategy: STAND (" + p.getValue() + ")");
        }

        Decider.shutDownThreads();
        final long endTime = System.nanoTime();
        System.out.println("Computation time: " + (endTime - startTime)/1000000000.0 + "s");
    }
}
