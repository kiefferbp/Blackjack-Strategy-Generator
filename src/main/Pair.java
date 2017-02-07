package main;

/**
 * Created by Brian on 1/28/2017.
 */
public class Pair<U, V> {
    private U first;
    private V second;

    public Pair(U first, V second) {
        if (first.getClass().equals(second.getClass())) {
            throw new IllegalArgumentException();
        }

        this.first = first;
        this.second = second;
    }

    public <T> T get(Class<T> targetClass) {
        if (targetClass.isInstance(first)) {
            return (T) first;
        } else if (targetClass.isInstance(second)) {
            return (T) second;
        } else {
            throw new IllegalArgumentException("targetClass is not in the pair");
        }
    }
}
