/**
 * Created by Brian on 1/17/2017.
 */
public class ShoeBuilder {
    private int playerValue;
    private int dealerValue;
    private int deckCount = 4;
    private double penetrationValue = 1.0;
    private boolean isSoft = false;
    private boolean isPair = false;

    public ShoeBuilder setPlayerValue(int playerValue) {
        this.playerValue = playerValue;
        return this;
    }

    public ShoeBuilder setDealerValue(int dealerValue) {
        this.dealerValue = dealerValue;
        return this;
    }

    public ShoeBuilder setDeckCount(int deckCount) {
        this.deckCount = deckCount;
        return this;
    }

    public ShoeBuilder setPenetrationValue(double penetrationValue) {
        this.penetrationValue = penetrationValue;
        return this;
    }

    public ShoeBuilder setSoftFlag(boolean isSoft) {
        this.isSoft = isSoft;
        return this;
    }

    public ShoeBuilder setPairFlag(boolean isPair) {
        this.isPair = isPair;
        return this;
    }

    public Shoe build() {
        return new Shoe(playerValue, dealerValue, deckCount, penetrationValue, isSoft, isPair);
    }
}