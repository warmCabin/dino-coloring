package cad.iter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ShuffledIterationOrder implements IterationOrder {

    private static List<Integer> generateRandomPermutation(int n) {
        List<Integer> list = IntStream.range(0, n).boxed()
            .collect(Collectors.toList());

        Collections.shuffle(list);
        return list;
    }

    private final List<Integer> permutation;

    public ShuffledIterationOrder(int n) {
        permutation = generateRandomPermutation(n);
    }

    @Override
    public int get(int i) {
        return permutation.get(i);
    }
}
