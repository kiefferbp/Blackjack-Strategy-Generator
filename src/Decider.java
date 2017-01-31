import java.util.*;
import java.util.concurrent.*;

/**
 * Created by Brian on 1/17/2017.
 */
public class Decider {
    private static final int SIMULATION_COUNT = 1000000;

    private static final int threadCount = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

    private int deckCount;
    private double penetrationValue;

    /**
     * Constructor.
     *
     * @param deckCount the number of decks in the shoe
     * @param penetrationValue a number from 0 to 1 that represents the proportion of the shoe
     *                         that play is made through. For example, if the penetration is 80%, 20% of the cards
     *                         in the shoe are removed.
     * @throws IllegalStateException if the ExecutorService used to generate hands has been shut down
     */
    Decider(int deckCount, double penetrationValue) {
        if (executor.isShutdown()) {
            throw new IllegalStateException("ExecutorService has been shut down already");
        }
        this.deckCount = deckCount;
        this.penetrationValue = penetrationValue;
    }

    /**
     * Shuts down the threads used to generate a player hand. This should be called only after you are through with
     * using deciders.
     */
    public static void shutDownThreads() {
        executor.shutdownNow();
    }

    /**
     * Generates a random shoe-player pair that corresponds with the setup given by the provided scenario
     * (player hand value/softness, dealer card, and whether the hand is a pair). All pairs generated should appear as
     * likely as they would in a normal game of Blackjack. Like in actual play, the dealer's card affects the outcome
     * of all cards beyond the player's first.
     *
     * @param scenario the scenario to generate a random hand for
     * @return a shoe-player pair that corresponds with the scenario
     * @throws InterruptedException if a thread used to generate a hand is interrupted
     * @throws ExecutionException if a thread throws an exception for some reason
     */
    private Pair<Shoe, Player> getShoePlayerPair(Scenario scenario) throws InterruptedException, ExecutionException {
        final int targetValue = scenario.playerValue;
        final Card dealerCard = scenario.dealerCard;
        final boolean targetIsSoft = scenario.isPlayerSoft;

        final Callable<Pair<Shoe, Player>> task = () -> {
            final Shoe shoe = new Shoe(deckCount, penetrationValue);
            final Player player = new Player(shoe);

            final Callable<Optional<Void>> resetTask = () -> {
                player.resetHand();
                shoe.rebuildShoe();

                // get the player's first card
                final Card firstPlayerCard = player.hit();

                // take out the dealer's card from the shoe
                shoe.removeCard(dealerCard);

                // the first two cards cannot be a blackjack
                Card secondPlayerCard = shoe.removeTopCard();
                while (isBlackjackPair(firstPlayerCard, secondPlayerCard)) {
                    shoe.putCardBack(secondPlayerCard);
                    secondPlayerCard = shoe.removeTopCard();
                }

                // give the player this second card
                player.addCard(secondPlayerCard);

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

            return new Pair<>(shoe, player);
        };

        final List<Callable<Pair<Shoe, Player>>> taskList = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            taskList.add(task);
        }
        
        return executor.invokeAny(taskList);
    }

    /**
     * Builds a scenario (an encapsulation of the player's hand total and the dealer's up-card)
     * given a player and dealer.
     * @param player the player of the scenario
     * @param dealer the dealer of the scenario
     * @return a <tt>Scenario</tt> that encapsulates the state of the player and dealer
     */
    private Scenario buildScenario(Player player, Player dealer) {
        final Scenario scenario = new Scenario();
        scenario.playerValue = player.getHandValue();
        scenario.dealerCard = Card.getCardWithValue(dealer.getHandValue());
        scenario.isPlayerSoft = player.handIsSoft();
        scenario.isPair = false; // for now

        return scenario;
    }

    /**
     * A helper function that returns whether two cards represent a Blackjack
     * @param card1 the first card
     * @param card2 the second card
     * @return true if <tt>card1</tt> and <tt>card2</tt> make a Blackjack, and false otherwise.
     */
    private boolean isBlackjackPair(Card card1, Card card2) {
        return ((card1.equals(Card.ACE) && card2.getValue() == 10) ||
                (card1.getValue() == 10 && card2.equals(Card.ACE)));
    }

    /**
     * Performs a simulation of what occurs when a player stands and the dealer plays out his hand.
     * @param player the player who is standing on his hand
     * @param dealer the dealer
     * @param shoe the shoe that the player and dealer are using
     * @param dealerHitsSoft17 <tt>true</tt> if the dealer hits a soft 17, <tt>false</tt> if he stands on a soft 17
     * @return the result of standing (win, lose, push) under this simulation
     */
    private PlayResult simulateStanding(Player player,
                                        Player dealer,
                                        Shoe shoe,
                                        boolean dealerHitsSoft17) {
        // if the dealer has an Ace or a 10-valued card, the card at the top of the shoe cannot give him a blackjack
        // if the top card yields a BJ, shuffle the shoe until it no longer does
        final Card firstDealerCard = dealer.getCards().get(0); // the dealer only has one card right now
        Card secondDealerCard = shoe.removeTopCard();
        while (isBlackjackPair(firstDealerCard, secondDealerCard)) {
            shoe.putCardBack(secondDealerCard);
            secondDealerCard = shoe.removeTopCard();
        }

        // give the dealer his second card
        dealer.addCard(secondDealerCard);

        // play out the dealer's hand
        while (dealer.getHandValue() < 17 || (dealer.getHandValue() == 17 && dealer.handIsSoft() && dealerHitsSoft17)) {
            dealer.hit();
        }

        // determine the result
        final int finalDealerValue = dealer.getHandValue();
        final int playerValue = player.getHandValue();
        if (playerValue > finalDealerValue || finalDealerValue > 21) {
            return PlayResult.WIN;
        } else if (playerValue < finalDealerValue) {
            return PlayResult.LOSE;
        } else {
            return PlayResult.PUSH;
        }
    }

    /**
     * Performs a simulation of what occurs when a player makes the best decisions when playing his hand.
     * @param player the player
     * @param dealer the dealer
     * @param shoe the shoe that the player and dealer are using
     * @param dealerHitsSoft17 <tt>true</tt> if the dealer hits a soft 17, <tt>false</tt> if he stands on a soft 17
     * @return the result of perfect play (win, lose, push) under this simulation
     */
    private PlayResult simulateBestPlay(Player player,
                                        Player dealer,
                                        Shoe shoe,
                                        boolean dealerHitsSoft17) throws InterruptedException, ExecutionException {
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
    private double getExpectedHitValue(Scenario scenario, boolean dealerHitsSoft17)
            throws InterruptedException, ExecutionException {
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

    /**
     * Computes the expected value of standing under a given scenario (an encapsulation of the player's hand total
     * and the dealer's up-card).
     *
     * @param scenario the scenario to compute the expected value for
     * @param dealerHitsSoft17 <tt>true</tt> if the dealer hits a soft 17, <tt>false</tt> if he stands on a soft 17
     * @return a double from -2 to 2 representing the average win amount
     * @throws InterruptedException if a thread used to generate a hand is interrupted
     * @throws ExecutionException if a thread throws an exception for some reason
     */
    private double getExpectedStandValue(final Scenario scenario, final boolean dealerHitsSoft17)
            throws InterruptedException, ExecutionException {
        if (expectedStandMap.get(scenario) != null) {
            return expectedStandMap.get(scenario);
        }

        double unitsWon = 0;
        for (int i = 0; i < SIMULATION_COUNT; i++) {
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

        final double expectedWinnings = unitsWon / SIMULATION_COUNT;
        System.out.println("standing result (" + scenario + "): " + expectedWinnings);
        expectedStandMap.put(scenario, expectedWinnings);
        return expectedWinnings;
    }

    private double getExpectedSplitValue(Scenario scenario, boolean dealerHitsSoft17)
            throws InterruptedException, ExecutionException {
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
    public Map<Decision, Double> computeExpectedValues(Scenario scenario, boolean dealerHitsSoft17)
            throws InterruptedException, ExecutionException {
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

    public Pair<Decision, Double> computeBestScenarioResult(Scenario scenario, boolean dealerHitsSoft17)
            throws InterruptedException, ExecutionException {
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

    public static void main(String[] args) {
        final Decider d = new Decider(4, 1.0);
        final Scenario scenario = new Scenario();
        scenario.playerValue = 16;
        scenario.isPlayerSoft = false;
        scenario.isPair = true;
        scenario.dealerCard = Card.ACE;

        System.out.println("solving " + scenario);
        final long startTime = System.nanoTime();

        try {
            final Map<Decision, Double> expectedValueMap = d.computeExpectedValues(scenario, false);
            final Pair<Decision, Double> p = d.computeBestScenarioResult(scenario, false);
            System.out.println(scenario + " best strategy: " + p.get(Decision.class) + " (" + p.get(Double.class) + ")");

            for (Map.Entry<Decision, Double> entry : expectedValueMap.entrySet()) {
                final Decision entryDecision = entry.getKey();
                final double entryExpectedValue = entry.getValue();

                System.out.println("Expected value of " + entryDecision + ": " + entryExpectedValue);
            }

            final long endTime = System.nanoTime();
            System.out.println("Computation time: " + (endTime - startTime) / 1000000000.0 + "s");
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            Decider.shutDownThreads();
        }
    }
}
