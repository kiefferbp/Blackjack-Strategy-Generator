import java.util.Objects;

/**
 * Created by Brian on 1/17/2017.
 */

public class Scenario {
    public int playerValue;
    public Card dealerCard;
    public boolean isPlayerSoft;
    public boolean isPair;

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Scenario)) {
            return false;
        }

        Scenario other = (Scenario) o;
        return playerValue == other.playerValue &&
                Objects.equals(dealerCard, other.dealerCard) &&
                isPlayerSoft == other.isPlayerSoft &&
                isPair == other.isPair;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerValue, dealerCard, isPlayerSoft, isPair);
    }

    @Override
    public String toString() {
        final String playerHandType = (isPlayerSoft ? "Soft" : "Hard");
        return "Player " + playerHandType + " " + playerValue + " against a Dealer " + dealerCard;
    }
}
