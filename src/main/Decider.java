package main;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 * Created by Brian on 1/17/2017.
 */
public class Decider {
    private static final int STATUS_UPDATE_INTERVAL = 500;

    private static final int threadCount = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private LongAdder currentSimulationNum = new LongAdder();

    private List<Runnable> statusListeners;
    private Map<Scenario, Semaphore> hitSemaphoreMap = new HashMap<>();
    private Map<Scenario, Semaphore> standSemaphoreMap = new HashMap<>();
    private Map<Scenario, Semaphore> splitSemaphoreMap = new HashMap<>();
    private Map<Scenario, Semaphore> doubleSemaphoreMap = new HashMap<>();

    // rules
    private int deckCount;
    private double penetrationValue;
    private boolean dealerHitsSoft17;
    private boolean canSurrender;
    private int maxSplitHands;

    // other params
    private int simulationCount;

    /**
     * Constructor.
     *
     * @param deckCount the number of decks in the shoe
     * @param penetrationValue a number from 0 to 1 that represents the proportion of the shoe
     *                         that play is made through. For example, if the penetration is 80%, 20% of the cards
     *                         in the shoe are removed.
     * @throws IllegalStateException if the ExecutorService used to generate hands has been shut down
     */
    public Decider(Rule rule, int simulationCount) {
        if (executor.isShutdown()) {
            throw new IllegalStateException("ExecutorService has been shut down already");
        }

        this.deckCount = rule.getDeckCount();
        this.penetrationValue = rule.getPenetrationValue();
        this.maxSplitHands = rule.getMaxSplitHands();
        this.simulationCount = simulationCount;
        this.statusListeners = new ArrayList<>();

        // init the semaphore maps
        // some don't make sense and are never used, but let's go with it
        for (int playerValue = 4; playerValue <= 21; playerValue++) {
            for (Card dealerCard : Card.values()) {
                for (boolean isPlayerSoft : new boolean[]{true, false}) {
                    for (boolean isPair : new boolean[]{true, false}) {
                        final Scenario scenario = new Scenario();
                        scenario.playerValue = playerValue;
                        scenario.dealerCard = dealerCard;
                        scenario.isPlayerSoft = isPlayerSoft;
                        scenario.isPair = isPair;

                        hitSemaphoreMap.put(scenario, new Semaphore(1));
                        standSemaphoreMap.put(scenario, new Semaphore(1));
                        splitSemaphoreMap.put(scenario, new Semaphore(1));
                        doubleSemaphoreMap.put(scenario, new Semaphore(1));
                    }
                }
            }
        }
    }

    /**
     * Shuts down the threads used to generate a player hand. This should be called only after you are through with
     * using deciders.
     */
    public static void shutDownThreads() {
        executor.shutdownNow();
    }

    public Future<?> addStatusListener(Consumer<Long> listener) {
        return executor.submit(() -> {
            long lastSimulationNum = currentSimulationNum.longValue();
            while (true) {
                final long difference = (long) Math.floor((1000.0 / STATUS_UPDATE_INTERVAL) * (currentSimulationNum.longValue() - lastSimulationNum));
                listener.accept(difference);
                lastSimulationNum = currentSimulationNum.longValue();
                Thread.sleep(STATUS_UPDATE_INTERVAL);
            }
        });
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
    private Pair<Shoe, Player> getShoePlayerPair(Scenario scenario) throws InterruptedException, ExecutionException, Exception {
        final int targetValue = scenario.playerValue;
        final Card dealerCard = scenario.dealerCard;
        final boolean targetIsSoft = scenario.isPlayerSoft;

        final Shoe shoe = new Shoe(deckCount, penetrationValue);
        final Player player = new Player(shoe);

        final Callable<?> resetTask = () -> {
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

            if (player.getHandValue() > 21 ||
                    (player.getHandValue() > targetValue && !player.handIsSoft() && !targetIsSoft) ||
                    ((targetValue - player.getHandValue() + 10) < 1 && player.handIsSoft() && !targetIsSoft)) {
                resetTask.call();
            }
        }
        
        return new Pair<>(shoe, player);
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
     * @return the result of standing (win, lose, push) under this simulation
     */
    private PlayResult simulateStanding(Player player, Player dealer, Shoe shoe) {
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
     * @return the result of perfect play (win, lose, push) under this simulation
     */
    private PlayResult simulateBestPlay(Player player, Player dealer, Shoe shoe) throws Exception {
        final Scenario scenario = buildScenario(player, dealer);
        final Decision bestDecision = computeBestScenarioResult(scenario, false, false).get(Decision.class);

        switch (bestDecision) {
            case HIT:
                player.hit();

                if (player.getHandValue() > 21) {
                    return PlayResult.LOSE;
                } else {
                    return simulateBestPlay(player, dealer, shoe);
                }
            case STAND:
                return simulateStanding(player, dealer, shoe);
            default:
                return null; // other decisions will come later
        }

    }

    /**
     * Performs at least <tt>simulationCount</tt> simulations of a given <tt>scenario</tt> (an encapsulation of the
     * player's hand total and the dealer's up-card) given that the first decision made is <tt>decision</tt>. Once that
     * decision is made, further moves are made with perfect play (by induction). This method takes a <tt>Semaphore</tt>
     * that is acquired during the computation and released afterwards. It also takes a memoization <tt>Map</tt> that is
     * updated with the result.
     * @param decision the player's initial decision to make with his hand
     * @param scenario the initial scenario (an encapsulation of the player's hand total
     * and the dealer's up-card)
     * @param map the map to put the computed result in
     * @param semaphore the semaphore to acquire during the computation
     * @return the expected value of a given scenario given that the first decision made is <tt>decision</tt> and
     * perfect play follows.
     * @throws Exception
     */
    private Double getExpectedDecisionValue(Decision decision, Scenario scenario, Map<Scenario, Double> map, Semaphore semaphore) throws Exception {
        final Callable<Double> task = () -> {
            double unitsWon = 0;
            for (int i = 0; i < Math.ceil(simulationCount / threadCount); i++) {
                currentSimulationNum.increment();

                // generate a random shoe and player hand under this scenario
                final Pair<Shoe, Player> shoePlayerPair = getShoePlayerPair(scenario);
                final Shoe shoe = shoePlayerPair.get(Shoe.class);
                final Player player = shoePlayerPair.get(Player.class);
                final Player dealer = new Player(shoe);
                final Card dealerCard = scenario.dealerCard;
                dealer.addCard(dealerCard);

                switch (decision) {
                    case HIT:
                        // take a hit
                        player.hit();

                        if (player.getHandValue() > 21) {
                            unitsWon -= 1;
                        } else {
                            // inductive step: simulate best play on the new hand
                            final PlayResult bestPlayResult = simulateBestPlay(player, dealer, shoe);
                            unitsWon += bestPlayResult.getWinAmount();
                        }

                        break;
                    case STAND:
                        // play out the dealer
                        final PlayResult result = simulateStanding(player, dealer, shoe);
                        unitsWon += result.getWinAmount();
                        break;
                    case DOUBLE:
                        // have the player take one more card and then stand
                        player.hit();

                        if (player.getHandValue() > 21) {
                            unitsWon -= 2;
                        } else {
                            unitsWon += 2 * simulateStanding(player, dealer, shoe).getWinAmount();
                        }

                        break;
                    case SPLIT:
                        // we need to reset the shoe
                        shoe.rebuildShoe();

                        // get the correct player card
                        final Card playerCard = Card.getCardWithValue(scenario.playerValue / 2);
                        shoe.removeCard(playerCard);
                        shoe.removeCard(playerCard);
                        shoe.removeCard(dealerCard);

                        // set up the list of player hands
                        // note: splitting two X's results in two hands with one X in each of them
                        final List<Player> playerHands = new ArrayList<>();
                        final Player playerHand1 = new Player(shoe);
                        final Player playerHand2 = new Player(shoe);
                        playerHand1.addCard(playerCard);
                        playerHand2.addCard(playerCard);
                        playerHands.add(playerHand1);
                        playerHands.add(playerHand2);

                        // process the hands, splitting new pairs as needed
                        final Stack<Player> handStack = new Stack<>();
                        handStack.push(playerHand1);
                        handStack.push(playerHand2);

                        int handCount = 2; // since we already split once to two hands
                        while (!handStack.isEmpty()) {
                            final Player playerHand = handStack.pop();

                            // get a second card for this hand
                            final Card topCard = shoe.removeTopCard();

                            // note: check topCard.getValue() == playerCard.getValue() instead of
                            // topCard.equals(playerCard) for 10/J/Q/K in particular
                            if (topCard.getValue() == playerCard.getValue() && handCount < maxSplitHands) {
                                // split the hand
                                final Player newPlayerHand = new Player(shoe);
                                newPlayerHand.addCard(topCard);
                                playerHands.add(newPlayerHand);
                                handStack.push(newPlayerHand);
                                handStack.push(playerHand); // we are not done processing |playerHand|
                                handCount += 1;
                            } else {
                                playerHand.addCard(topCard);
                            }
                        }

                        // play out the hands
                        for (Player playerHand : playerHands) {
                            final Scenario handScenario = buildScenario(playerHand, dealer);
                            final double handUnitsWon = playerCard.equals(Card.ACE)
                                    ? getExpectedStandValue(handScenario) // if we split aces, we cannot take more cards
                                    : computeBestScenarioResult(handScenario, true, false).get(Double.class);
                            unitsWon += handUnitsWon;
                        }

                        break;
                    default:
                        throw new IllegalArgumentException("Unknown decision " + decision);
                }
            }

            return unitsWon;
        };

        final List<Callable<Double>> taskList = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            taskList.add(task);
        }

        double totalUnitsWon = 0.0;
        for (Future<Double> future : executor.invokeAll(taskList)) {
            totalUnitsWon += future.get();
        }

        final double expectedWinnings = totalUnitsWon / (threadCount * Math.ceil(simulationCount / threadCount));
        System.out.println(decision + " result (" + scenario + "): " + expectedWinnings);
        map.put(scenario, expectedWinnings);
        semaphore.release();
        return expectedWinnings;
    }

    private final Map<Scenario, Double> expectedHitMap = new HashMap<>(); // for memoization

    /**
     * Computes the expected value under a given scenario (an encapsulation of the player's hand total
     * and the dealer's up-card) given that the first is to hit. All further moves are made with perfect play.
     * @param scenario the scenario to compute the expected value for
     * @return a double from -1 to 1 representing the average win amount
     * @throws Exception
     */
    private double getExpectedHitValue(Scenario scenario) throws Exception {
        final Semaphore hitSemaphore = hitSemaphoreMap.get(scenario);
        hitSemaphore.acquire();

        if (expectedHitMap.get(scenario) != null) {
            hitSemaphore.release();
            return expectedHitMap.get(scenario);
        }

        return getExpectedDecisionValue(Decision.HIT, scenario, expectedHitMap, hitSemaphore);
    }


    private final Map<Scenario, Double> expectedStandMap = new HashMap<>(); // for memoization

    /**
     * Computes the expected value of standing under a given scenario (an encapsulation of the player's hand total
     * and the dealer's up-card).
     *
     * @param scenario the scenario to compute the expected value for
     * @return a double from -2 to 2 representing the average win amount
     * @throws Exception
     */
    private double getExpectedStandValue(final Scenario scenario) throws Exception {
        final Semaphore standSemaphore = standSemaphoreMap.get(scenario);
        standSemaphore.acquire();

        if (expectedStandMap.get(scenario) != null) {
            standSemaphore.release();
            return expectedStandMap.get(scenario);
        }

        return getExpectedDecisionValue(Decision.STAND, scenario, expectedStandMap, standSemaphore);
    }

    private final Map<Scenario, Double> expectedSplitMap = new HashMap<>(); // for memoization

    /**
     * Computes the expected value of splitting under a given scenario (an encapsulation of the player's hand total
     * and the dealer's up-card).
     *
     * @param scenario the scenario to compute the expected value for
     * @return a double from -2 to 2 representing the average win amount
     * @throws Exception
     */
    private double getExpectedSplitValue(Scenario scenario) throws Exception {
        final Semaphore splitSemaphore = splitSemaphoreMap.get(scenario);
        splitSemaphore.acquire();

        if (!scenario.isPair) { // can't split
            expectedSplitMap.put(scenario, (double) Integer.MIN_VALUE);
            splitSemaphore.release();
            return Integer.MIN_VALUE;
        }

        return getExpectedDecisionValue(Decision.SPLIT, scenario, expectedSplitMap, splitSemaphore);
    }

    private final Map<Scenario, Double> expectedDoubleMap = new HashMap<>(); // for memoization

    /**
     * Computes the expected value of doubling down under a given scenario (an encapsulation of the player's hand total
     * and the dealer's up-card).
     *
     * @param scenario the scenario to compute the expected value for
     * @return a double from -2 to 2 representing the average win amount
     * @throws Exception
     */
    private double getExpectedDoubleValue(Scenario scenario) throws Exception {
        final Semaphore doubleSemaphore = standSemaphoreMap.get(scenario);
        doubleSemaphore.acquire();

        if (expectedDoubleMap.get(scenario) != null) {
            doubleSemaphore.release();
            return expectedDoubleMap.get(scenario);
        }

        return getExpectedDecisionValue(Decision.DOUBLE, scenario, expectedDoubleMap, doubleSemaphore);
    }

    private final Map<Scenario, Map<Decision, Double>> scenarioExpectedValues = new HashMap<>();

    /**
     * Computes the expected value of hitting, standing, doubling down, and splitting under a given scenario (an
     * encapsulation of the player's hand total and the dealer's up-card).
     * @param scenario the scenario to compute the expected values for
     * @param canDoubleDown whether the player has the right to double down. A player loses this right after taking a
     *                      third card and, under some casino rules, after splitting.
     * @return a map containing the expected values under the given scenario
     * @throws Exception
     */
    public Map<Decision, Double> computeExpectedValues(Scenario scenario, boolean canDoubleDown, boolean firstMove) throws Exception {
        if (scenarioExpectedValues.get(scenario) != null) {
            return scenarioExpectedValues.get(scenario);
        }

        final Map<Decision, Double> expectedValueMap = new HashMap<>();
        expectedValueMap.put(Decision.HIT, getExpectedHitValue(scenario));
        expectedValueMap.put(Decision.STAND, getExpectedStandValue(scenario));
        expectedValueMap.put(Decision.SPLIT, getExpectedSplitValue(scenario));

        if (canDoubleDown) {
            expectedValueMap.put(Decision.DOUBLE, getExpectedDoubleValue(scenario));
        }

        if (firstMove && canSurrender) {
            expectedValueMap.put(Decision.SURRENDER, -0.5);
        }

        scenarioExpectedValues.put(scenario, expectedValueMap);

        return expectedValueMap;
    }

    /**
     * Computes the best decision to make under a given scenario (an encapsulation of the player's hand total and the
     * dealer's up-card). The result is returned as a decision-value pair.
     * @param scenario the scenario to compute the best play for
     * @param canDoubleDown whether the player has the right to double down. A player loses this right after taking a
     *                      third card and, under some casino rules, after splitting.
     * @return a decision-value pair
     * @throws Exception
     */
    public Pair<Decision, Double> computeBestScenarioResult(Scenario scenario, boolean canDoubleDown, boolean firstMove) throws Exception {
        final Map<Decision, Double> expectedValueMap = computeExpectedValues(scenario, canDoubleDown, firstMove);
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
}
