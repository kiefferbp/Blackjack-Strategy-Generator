/**
 * Created by Brian on 1/18/2017.
 */
public enum PlayResult {
    WIN(1.0),
    LOSE(-1.0),
    LOSE_DOUBLE(-2.0),
    PUSH(0.0);

    private double winAmount;

    PlayResult(double winAmount) {
        this.winAmount = winAmount;
    }

    public double getWinAmount() {
        return winAmount;
    }
}
