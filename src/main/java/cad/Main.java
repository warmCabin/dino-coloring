package cad;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// Accepts graphs from stdin, or reads the file passed as a command line arg.
public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        Scanner in;
        if (args.length > 0) {
            System.out.printf("Opening %s...\n\n", args[0]);
            in = new Scanner(new File(args[0]));
        } else {
            System.out.println("Reading from stdin...\n");
            in = new Scanner(System.in);
        }
        new Main().start(in);
    }

    int N, C, R;
    DinoGraph graph;
    // Seems like it's actually 2n - 1 when you use a gradient fill, so this approach isn't quite perfect. Hmm...
    static int colorWeights[];
    // Hardcoded to correspond to my CaD palette choice for now.
    // static final String colorNames[] = {"Empty", "Pink", "Yellow", "Purple", "Gray"}; // BG and purple count
    static final String colorNames[] = {"Empty", "Pink", "Yellow", "Gray", "Lavender"}; // BG and purple don't count

    // Jank from before I added "frozen" type restrictions.
    boolean forceBgBlank = false;
    // TODO: Flesh these out into a proper IterationOrder scheme thing.
    boolean doShuffle = false;
    List<Integer> shuffle;
    // When pruning, allow solutions within this much of the current optimum.
    // TASing is a bit of a fuzzy magic sometimes.
    static final int SOLUTION_THRESHOLD = 0;

    public void start(Scanner in) {

        String[] line = in.nextLine().split(" ");
        C = Integer.parseInt(line[0]);
        colorWeights = new int[C + 1];
        for (int i = 1; i <= C; i++) {
            colorWeights[i] = Integer.parseInt(line[i]);
        }

        N = Integer.parseInt(in.nextLine());
        graph = new DinoGraph(N);
        shuffle = generateRandomPermutation(N);
        for (int i = 0; i < N; i++) {
            String[] nodeLine = in.nextLine().split(" ");
            int cur = Integer.parseInt(nodeLine[0]);
            if (doShuffle)
                cur = shuffle.get(cur);
            int weight = Integer.parseInt(nodeLine[1]);
            graph.setWeight(cur, weight);

            int finalCur = cur; // fu java
            Arrays.stream(nodeLine)
                .skip(2)
                .map(Integer::parseInt)
                .forEach(dest -> graph.addEdge(finalCur, doShuffle ? shuffle.get(dest) : dest));
        }

        // TODO: Doesn't work with doShuffle.
        R = Integer.parseInt(in.nextLine());
        for (int i = 0; i < R; i++) {
            line = in.nextLine().split(" ");
            char type = line[0].charAt(0);
            int node = Integer.parseInt(line[1]);
            switch (type) {
                case 'F': // Freeze
                    int value = Integer.parseInt(line[2]);
                    graph.freezeColor(node, value);
                    break;
                case 'M': // Match
                    int dest = Integer.parseInt(line[2]);
                    graph.addIsland(node, dest);
            }


        }

        if (forceBgBlank) {
            // Set the color and start the recursion at node 1.
            // This can be used as an optimization for normal four coloring, but isn't applicable to dinosaur coloring.
            // TODO: Can just use an F 0 3 constraint in the data.
            graph.setColor(0, 3);
            generateAllColorings(1, 0);
        } else {
            // Normal recursion allowing for all possibilities in node 0.
            generateAllColorings(0, 0);
        }

        if (totalConsidered == 0) {
            System.out.println("\nNo valid colorings!!!");
        } else {
            System.out.printf("\nbest score = %d\n", minScore);
            System.out.println("Total considered: " + totalConsidered);
        }
    }

    int minScore = Integer.MAX_VALUE - SOLUTION_THRESHOLD - 1;
    int totalConsidered = 0;

    /**
     * An approach borrowed from https://math.stackexchange.com/questions/120531/how-to-find-all-proper-colorings-four-coloring-of-a-graph-with-a-brute-force-a.
     * I promise I'll give it back!
     * <p/>
     * Iterates through all possible colorings, keeping track of the weight along the way. Prunes if the running total
     * exceeds the known minimum. Returns values in minScore and totalConsidered. Hello, 80's!
     * <p/>
     * You generally want to call this like generateAllColorings(0, 0).
     *
     * @param cur the current node
     * @param runningTotal running total of the coloring weight so far.
     */
    void generateAllColorings(int cur, int runningTotal) {
        for (int color = 1; color <= C; color++) {
            if (!graph.canColor(cur, color))
                continue;

            graph.setColor(cur, color);
            int newTotal = runningTotal + graph.getMultipliedWeight(cur);

            // condense this all into one big elseif chain?
            if (newTotal > minScore + SOLUTION_THRESHOLD) {
                // It's annoying that I have to set and reset the color to get newTotal.
                // ...I say that as if it's not totally within my control to not do that.
                graph.resetColor(cur);
                continue;
            }

            if (cur == N - 1) {
                System.out.printf("FOUND ONE! score = %d\n", newTotal);
                System.out.println(graph);
                totalConsidered++;
                minScore = Math.min(minScore, newTotal);
            } else {
                generateAllColorings(cur + 1, runningTotal + graph.getMultipliedWeight(cur));
            }
            graph.resetColor(cur);
        }
    }

    List<Integer> generateRandomPermutation(int n) {
        List<Integer> list = IntStream.range(0, n).boxed()
            .collect(Collectors.toList());

        Collections.shuffle(list);
        return list;
    }

    static class DinoGraph {

        private final int N;
        private final Node[] nodes;

        public DinoGraph(int N) {
            this.N = N;
            nodes = new Node[N];
            for (int i = 0; i < N; i++) {
                nodes[i] = new Node();
            }
        }

        public void setWeight(int i, int weight) {
            nodes[i].weight = weight;
        }

        public int getMultipliedWeight(int i) {
            return nodes[i].weight * colorWeights[nodes[i].color];
            // TODO: weight * colorWeight - 1 ?
        }

        public void addEdge(int a, int b) {
            nodes[a].edges.add(b);
            nodes[b].edges.add(a);
        }

        public List<Integer> edges(int i) {
            return nodes[i].edges;
        }

        // This is dependent on the fact that we traverse the nodes in order.
        // Creating this behavior bidirectionally would be tough but doable.
        // I guess I could make it always add it from b to a if b is lesser.
        public void addIsland(int a, int b) {
            Node nodeA = nodes[a], nodeB = nodes[b];
            nodeA.islands.add(b);
            nodeB.frozen = true;
        }

        public int getColor(int i) {
            return nodes[i].color;
        }

        public void setColor(int i, int color) {
            nodes[i].color = color;
            for (int dest : nodes[i].islands) {
                freezeColor(dest, color);
            }
        }

        public void resetColor(int i) {
            if (nodes[i].frozen)
                return;
            setColor(i, 0);
            for (int dest : nodes[i].islands) {
                nodes[dest].color = 0;
            }
        }

        public void freezeColor(int i, int color) {
            setColor(i, color);
            nodes[i].frozen = true;
        }

        public boolean isFrozen(int i) {
            return nodes[i].frozen;
        }

        public boolean canColor(int i, int color) {
            if (isFrozen(i) && color != nodes[i].color)
                return false;

            return edges(i).stream().allMatch(dest -> getColor(dest) != color);
        }

        public int getTotalWeight() {
            if (Arrays.stream(nodes)
                .anyMatch(node -> node.color == 0)
            ) {
                throw new IllegalStateException("There are some uncolored nodes. You've probably made a big mistake.");
            }

            return IntStream.range(0, N)
                .map(this::getMultipliedWeight)
                .reduce(0, Integer::sum);
        }

        @Override
        public String toString() {
            String ret = "DinoGraph(";

            ret += IntStream.range(0, N).boxed()
                .map(i -> i + " " + nodes[i])
                .collect(Collectors.joining(",\n  ", "nodes=[\n  ", "\n],\n"));

            ret += "weight=" + Arrays.stream(nodes)
                .map(node -> node.weight * colorWeights[node.color])
                .reduce(0, Integer::sum);

            return ret + "\n)";
        }

    }

    static class Node {
        public int weight;
        public int color;
        public boolean frozen;
        public List<Integer> edges = new ArrayList<>();
        public List<Integer> islands = new ArrayList<>();

        public Node() {
            this(-1);
        }

        public Node(int weight) {
            this.weight = weight;
        }

        @Override
        public String toString() {
//            return String.format("Node(%d(%s), %d)", color, colorNames[color], colorWeights[color] * weight);
            return colorNames[color];
        }
    }

}
