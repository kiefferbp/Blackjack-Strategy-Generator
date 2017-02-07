package main;

/**
 * Created by Brian on 2/5/2017.
 */
public class RuleBuilder {
    private int deckCount = 4;
    private double penetrationValue = 1.0;
    private boolean dealerHitsSoft17 = false;
    private boolean canSurrender = false;
    private int maxSplitHands = 4;

    public RuleBuilder setDeckCount(int deckCount) {
        this.deckCount = deckCount;
        return this;
    }

    public RuleBuilder setPenetrationValue(double penetrationValue) {
        this.penetrationValue = penetrationValue;
        return this;
    }

    public RuleBuilder setCanSurrender(boolean canSurrender) {
        this.canSurrender = canSurrender;
        return this;
    }

    public RuleBuilder setDealerHitsSoft17(boolean dealerHitsSoft17) {
        this.dealerHitsSoft17 = dealerHitsSoft17;
        return this;
    }

    public RuleBuilder setMaxSplitHands(int maxSplitHands) {
        this.maxSplitHands = maxSplitHands;
        return this;
    }

    public Rule build() {
        return new Rule(deckCount, penetrationValue, dealerHitsSoft17, canSurrender, maxSplitHands);
    }
}
