/**
 * Created by Brian on 1/25/2017.
 */
public class DecisionValuePair {
    private Decision decision;
    private double value;

    DecisionValuePair(Decision decision, double value) {
        this.decision = decision;
        this.value = value;
    }

    public Decision getDecision() {
        return decision;
    }

    public double getValue() {
        return value;
    }
}
