package cad.iter;

import cad.Main;
import cad.Main.DinoGraph;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WeightBasedIterationOrder implements IterationOrder {

    private final List<Integer> canonicalOrder;

    public WeightBasedIterationOrder(DinoGraph graph) {
        canonicalOrder = IntStream.range(0, graph.size()).boxed()
            .sorted(Comparator.comparing((Integer i) -> graph.getNode(i).weight).reversed())
            .collect(Collectors.toList());
    }

    @Override
    public int get(int i) {
        return canonicalOrder.get(i);
    }
}
