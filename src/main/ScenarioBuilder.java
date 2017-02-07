package main;

/**
 * Created by Brian on 2/4/2017.
 */
public class ScenarioBuilder {
    public int playerValue;
    public Card dealerCard;
    public boolean isPlayerSoft;
    public boolean isPair;

    public ScenarioBuilder setPlayerValue(int playerValue) {
        this.playerValue = playerValue;
        return this;
    }

    public ScenarioBuilder setDealerCard(Card dealerCard) {
        this.dealerCard = dealerCard;
        return this;
    }

    public ScenarioBuilder setSoftFlag(boolean isPlayerSoft) {
        this.isPlayerSoft = isPlayerSoft;
        return this;
    }

    public ScenarioBuilder setPairFlag(boolean isPair) {
        this.isPair = isPair;
        return this;
    }

    public Scenario build() {
        final Scenario scenario = new Scenario();
        scenario.playerValue = playerValue;
        scenario.dealerCard = dealerCard;
        scenario.isPlayerSoft = isPlayerSoft;
        scenario.isPair = isPair;

        return scenario;
    }
}
