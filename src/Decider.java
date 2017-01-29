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
    private static final ExecutorService executor = Executors.newFixedThreadPool(2 * threadCount);

    private int deckCount = 4;
    private double penetrationValue = 1.0;

    public static void shutDownThreads() {
        executor.shutdownNow();
    }

    // returns a player with a random hand bounded by |scenario|
    private Player generatePlayer(Scenario scenario) throws Exception {
        final int targetValue = scenario.playerValue;
        final Card dealerCard = scenario.dealerCard;
        final boolean targetIsSoft = scenario.isPlayerSoft;

        final Callable<Player> task = () -> {
            final Shoe shoe = new Shoe(deckCount, penetrationValue);
            final Player player = new Player(shoe);

            final Callable<Optional<Void>> resetTask = () -> {
                player.resetHand();
                shoe.restoreShoe();

                // get the player's first card
                Card firstPlayerCard;
                if (targetIsSoft) {
                    firstPlayerCard = Card.ACE;
                    player.addCard(firstPlayerCard);
                    shoe.removeCard(firstPlayerCard);
                } else {
                    shoe.shuffle();
                    firstPlayerCard = player.hit();
                }

                // take out the dealer's card from the shoe
                shoe.removeCard(dealerCard);
                shoe.shuffle();

                // the first two cards cannot be a blackjack
                while (isBlackjackPair(firstPlayerCard, shoe.peekTopCard())) {
                    shoe.shuffle();
                }

                // give the player a second card
                player.hit();

                // to appease the Java overlords
                return Optional.empty();
            };

            resetTask.call();

            while (player.getHandValue() != targetValue || player.handIsSoft() != targetIsSoft) {
                player.hit();

                if (player.getHandValue() > 21) {
                    resetTask.call();
                }
            }

            return player;
        };

        final List<Callable<Player>> taskList = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            taskList.add(task);
        }
        
        return executor.invokeAny(taskList);
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

    // returns an Object array with two elements: the modified shoe, and the player
    //Map.Entry<Shoe, Player> = new Map.Entry<>(new Shoe(), new Player();
    private Pair<Shoe, Player> getShoePlayerPair(Scenario scenario) throws Exception {
        final Shoe shoe = new Shoe(deckCount, penetrationValue);
        final Player player = generatePlayer(scenario);
        final List<Card> playerCards = player.getCards();

        for (Card playerCard : playerCards) {
            shoe.removeCard(playerCard);
        }

        shoe.shuffle();

        return new Pair<>(shoe, player);
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
        final Decision bestDecision = computeBestScenarioResult(scenario, dealerHitsSoft17).get(Decision.class);

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

    private final Map<Scenario, Double> expectedHitMap = new HashMap<>(); // for memoization
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
            final Pair<Shoe, Player> shoePlayerPair = getShoePlayerPair(scenario);
            final Shoe shoe = shoePlayerPair.get(Shoe.class);
            final Player player = shoePlayerPair.get(Player.class);
            final Player dealer = new Player(shoe);
            dealer.addCard(scenario.dealerCard);

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

    private final Map<Scenario, Double> expectedStandMap = new HashMap<>(); // for memoization
    private double getExpectedStandValue(final Scenario scenario, final boolean dealerHitsSoft17) throws Exception {
        if (expectedStandMap.get(scenario) != null) {
            return expectedStandMap.get(scenario);
        }

        final Callable<Double> task = () -> {
            double unitsWon = 0;
            for (int i = 0; i < Math.ceil(SIMULATION_COUNT / threadCount); i++) {
                // generate a random shoe and player hand under this scenario
                final Pair<Shoe, Player> shoePlayerPair = getShoePlayerPair(scenario);
                final Shoe shoe = shoePlayerPair.get(Shoe.class);
                final Player player = shoePlayerPair.get(Player.class);
                final Player dealer = new Player(shoe);
                dealer.addCard(scenario.dealerCard);

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

    private double getExpectedSplitValue(Scenario scenario, boolean dealerHitsSoft17) throws Exception {
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
            shoe.shuffle();

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
                final double expectedBestHand1 =
                        computeBestScenarioResult(scenarioHand1, dealerHitsSoft17).get(Double.class);
                final double expectedBestHand2 =
                        computeBestScenarioResult(scenarioHand2, dealerHitsSoft17).get(Double.class);
                unitsWon += expectedBestHand1 + expectedBestHand2;
            }
        }

        return unitsWon / SIMULATION_COUNT;
    }

    private final Map<Scenario, Map<Decision, Double>> scenarioExpectedValues = new HashMap<>();
    public Map<Decision, Double> computeExpectedValues(Scenario scenario, boolean dealerHitsSoft17) throws Exception {
        if (scenarioExpectedValues.get(scenario) != null) {
            return scenarioExpectedValues.get(scenario);
        }

        final Map<Decision, Double> expectedValueMap = new HashMap<>();
        expectedValueMap.put(Decision.HIT, getExpectedHitValue(scenario, dealerHitsSoft17));
        expectedValueMap.put(Decision.STAND, getExpectedStandValue(scenario, dealerHitsSoft17));
        expectedValueMap.put(Decision.SPLIT, getExpectedSplitValue(scenario, dealerHitsSoft17));

        scenarioExpectedValues.put(scenario, expectedValueMap);
        return expectedValueMap;
    }

    public Pair<Decision, Double> computeBestScenarioResult(Scenario scenario, boolean dealerHitsSoft17) throws Exception {
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

        return new Pair<>(bestExpectedDecision, bestExpectedValue);
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
        final Map<Decision, Double> expectedValueMap = d.computeExpectedValues(scenario, false);
        final Pair<Decision, Double> p = d.computeBestScenarioResult(scenario, false);
        System.out.println(scenario + " best strategy: " + p.get(Decision.class) + " (" + p.get(Double.class) + ")");

        for (Map.Entry<Decision, Double> entry : expectedValueMap.entrySet()) {
            final Decision entryDecision = entry.getKey();
            final double entryExpectedValue = entry.getValue();

            System.out.println("Expected value of " + entryDecision + ": " + entryExpectedValue);
        }

        Decider.shutDownThreads();
        final long endTime = System.nanoTime();
        System.out.println("Computation time: " + (endTime - startTime)/1000000000.0 + "s");
    }
}
