package cad.iter;

public interface IterationOrder {

    int get(int i);

    enum IterationScheme {
        SIMPLE, SHUFFLE, DEGREE, WEIGHT
    }
}
