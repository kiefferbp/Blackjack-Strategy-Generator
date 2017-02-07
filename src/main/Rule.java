package main;

/**
 * Created by Brian on 2/5/2017.
 */
public class Rule {
    private int deckCount;
    private double penetrationValue;
    private boolean dealerHitsSoft17;
    private boolean canSurrender;
    private int maxSplitHands;

    public Rule(int deckCount, double penetrationValue, boolean dealerHitsSoft17, boolean canSurrender, int maxSplitHands) {
        this.deckCount = deckCount;
        this.penetrationValue = penetrationValue;
        this.dealerHitsSoft17 = dealerHitsSoft17;
        this.canSurrender = canSurrender;
        this.maxSplitHands = maxSplitHands;
    }

    public int getDeckCount() {
        return deckCount;
    }

    public double getPenetrationValue() {
        return penetrationValue;
    }

    public boolean dealerHitsSoft17() {
        return dealerHitsSoft17;
    }

    public boolean canSurrender() {
        return canSurrender;
    }

    public int getMaxSplitHands() {
        return maxSplitHands;
    }
}
