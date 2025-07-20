package cad;

import cad.iter.DegreeBasedIterationOrder;
import cad.iter.IterationOrder;
import cad.iter.IterationOrder.IterationScheme;
import cad.iter.ShuffledIterationOrder;
import cad.iter.SimpleIterationOrder;
import cad.iter.WeightBasedIterationOrder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// Accepts graphs from stdin, or reads the file passed as a command line arg.
public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        Scanner in = null;
        IterationScheme iterationScheme = null;
        Integer threshold = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-i")) { // -v for vertex ordering?
                if (iterationScheme != null)
                    throw new IllegalArgumentException("Only one -i option allowed");
                if (i == args.length - 1)
                    throw new IllegalArgumentException("No value supplied to -i option");
                try {
                    iterationScheme = IterationScheme.valueOf(args[++i].toUpperCase());
                    System.out.println("Iteration scheme selected: " + iterationScheme);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Unrecognized iteration scheme \"" + args[i] + "\"");
                }
            } else if (args[i].equals("-t")) {
                // "Don't Repeat Yourself" proponents when I repeat myself >:0 (they are very irritated)
                if (threshold != null)
                    throw new IllegalArgumentException("Only one -t option allowed");
                if (i == args.length - 1)
                    throw new IllegalArgumentException("No value supplied to -t option");
                try {
                    threshold = Integer.parseInt(args[++i]);
                    System.out.println("Solution threshold: " + threshold);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid -t option: must be integer");
                }
            } else {
                if (in != null)
                    throw new IllegalArgumentException("Multiple filepath arguments supplied");
                System.out.printf("Opening %s...\n\n", args[i]);
                in = new Scanner(new File(args[i]));
            }
        }

        if (in == null) {
            System.out.println("Reading from stdin...\n");
            in = new Scanner(System.in);
        }
        if (iterationScheme == null) {
            iterationScheme = IterationScheme.DEGREE;
        }
        if (threshold == null) {
            threshold = 0;
        }

        new Main().start(in, iterationScheme, threshold);
    }

    int N, C, R;
    DinoGraph graph;
    static int colorWeights[], colorConstants[];
    static String colorNames[];

    // When pruning, allow solutions within this much of the current optimum.
    // TASing is a bit of a fuzzy magic sometimes.
    private int solutionThreshold;

    public void start(Scanner in, IterationScheme iterationScheme, int threshold) {

        C = Integer.parseInt(in.nextLine());
        colorWeights = new int[C + 1];
        colorConstants = new int[C + 1];
        colorNames = new String[C + 1];
        for (int i = 1; i <= C; i++) {
            String[] line = in.nextLine().split(" ");
            colorNames[i] = line[0];
            colorWeights[i] = Integer.parseInt(line[1]);
            colorConstants[i] = Integer.parseInt(line[2]);
        }

        N = Integer.parseInt(in.nextLine());
        graph = new DinoGraph(N);
        for (int i = 0; i < N; i++) {
            String[] nodeLine = in.nextLine().split(" ");
            int cur = Integer.parseInt(nodeLine[0]);
            int weight = Integer.parseInt(nodeLine[1]);
            graph.setWeight(cur, weight);

            Arrays.stream(nodeLine)
                .skip(2)
                .map(Integer::parseInt)
                .forEach(dest -> graph.addEdge(cur, dest));
        }

        R = Integer.parseInt(in.nextLine());
        for (int i = 0; i < R; i++) {
            String[] line = in.nextLine().split(" ");
            char type = line[0].charAt(0);
            int node = Integer.parseInt(line[1]);
            switch (type) {
                case 'F': // Freeze. Maybe do R for restrictions?
                    for (int f = 2; f < line.length; f++) {
                        int freezy = parseColor(line[f]);
                        graph.addFreeze(node, freezy);
                    }
                    break;
                case 'M': // Match
                    // Now that I think about this, this is the same thing as just contracting the vertices...
                    if (iterationScheme != IterationScheme.SIMPLE)
                        throw new UnsupportedOperationException("Can't have islands with weird iteration orders for now...");
                    int dest = Integer.parseInt(line[2]);
                    graph.addIsland(node, dest);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid restriction '" + type + "'");
            }
        }

        // Checking planarity just because it's interesting and fast.
        // I don't fully understand the big boy computer science going on inside this method.
        long time = -System.currentTimeMillis();
        boolean planar = new PlanarityChecka(graph).isPlanar();
        time += System.currentTimeMillis();
        System.out.println("Planar? " + planar);
        System.out.printf("(Planarity time: %d ms)\n", time);

        // The main recursion may churn for a long time before it realizes there are no valid colorings.
        // This checka can quickly tell us that from the get go.
        // I DO fully understand the big boy computer science going on inside THIS method!
        time = -System.currentTimeMillis();
        int chromaticNumber = new PlanarityChecka(graph).chromaticNumba();
        time += System.currentTimeMillis();
        System.out.println("chromaticity: " + chromaticNumber);
        System.out.printf("(chromaticity time: %d ms)\n", time);

        if (chromaticNumber > C) {
            throw new IllegalArgumentException(String.format("Invalid dinosaur; %d colors defined, %d required.",
                C, chromaticNumber));
        }

        switch (iterationScheme) {
            case SIMPLE:
                iterationOrder = new SimpleIterationOrder();
                break;
            case SHUFFLE:
                iterationOrder = new ShuffledIterationOrder(N);
                break;
            case DEGREE:
                iterationOrder = new DegreeBasedIterationOrder(graph);
                break;
            case WEIGHT:
                iterationOrder = new WeightBasedIterationOrder(graph);
        }

        solutionThreshold = threshold;
        minScore = Integer.MAX_VALUE - solutionThreshold - 1;

        // This is what it's all about, baby!
        time = -System.currentTimeMillis();
        generateAllColorings(0, 0);
        time += System.currentTimeMillis();
        System.out.printf("(Algo time: %d ms)\n", time);

        if (totalConsidered == 0) {
            System.out.println("\nNo valid colorings!!!");
        } else {
            System.out.printf("\nbest score = %d\n", minScore);
            System.out.println("Total considered: " + totalConsidered);
            time = -System.currentTimeMillis();
            colorings.removeIf(graph -> graph.getTotalWeight() > minScore + solutionThreshold);
            colorings.sort(Comparator.comparing(DinoGraph::getTotalWeight).reversed());
            time += System.currentTimeMillis();

            System.out.printf("(sort time: %d ms)\n", time);

            System.out.println("\n= Total in range: " + colorings.size() + " =\n");
            for (int i = 0; i < colorings.size(); i++) {
                int w = colorings.get(i).getTotalWeight();
                System.out.printf("= Solution #%d =\n%d frames", i + 1, w);
                if (w == minScore)
                    System.out.println(" (optimal)");
                else
                    System.out.printf(" (%d frames from optimum)\n", w - minScore);
                System.out.println(colorings.get(i) + "\n");
            }
        }
    }

    private int parseColor(String str) {
        int color;
        try {
            color = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            color = IntStream.rangeClosed(1, C)
                .filter(c -> colorNames[c].equalsIgnoreCase(str))
                .findFirst().orElse(-1);
        }
        if (color <= 0 || color > C)
            throw new IllegalArgumentException("Invalid color \"" + str + "\"");

        return color;
    }

    int minScore;
    int totalConsidered = 0;
    List<DinoGraph> colorings = new ArrayList<>();
    IterationOrder iterationOrder;

    /**
     * An approach borrowed from <a href="https://math.stackexchange.com/questions/120531/how-to-find-all-proper-colorings-four-coloring-of-a-graph-with-a-brute-force-a">StackExchange</a>.
     * I promise I'll give it back!
     * <p>
     * Iterates through all possible colorings, keeping track of the weight along the way. Prunes if the running total
     * exceeds the known minimum. Returns values in minScore and totalConsidered. Hello, 80's!
     * <p>
     * You generally want to call this like generateAllColorings(0, 0).
     *
     * @param index current index into the selected iteration order.
     * @param runningTotal running total of the coloring weight so far.
     */
    void generateAllColorings(int index, int runningTotal) {
        int cur = iterationOrder.get(index);
        for (int color = 1; color <= C; color++) {
            if (!graph.canColor(cur, color))
                continue;

            graph.setColor(cur, color);
            int newTotal = runningTotal + graph.getMultipliedWeight(cur);

            if (newTotal > minScore + solutionThreshold) {
                // It's annoying that I have to set and reset the color to get newTotal.
                // ...I say that as if it's not totally within my control to not do that.
                graph.resetColor(cur);
                continue;
            }

            if (index == N - 1) {
                // FOUND ONE! Save a copy of the current graph state to the list of colorings.
                colorings.add(new DinoGraph(graph));
                totalConsidered++;
                minScore = Math.min(minScore, newTotal);
            } else {
                generateAllColorings(index + 1, runningTotal + graph.getMultipliedWeight(cur));
            }
            graph.resetColor(cur);
        }
    }

    public static class DinoGraph {

        private final int N;
        private final Node[] nodes;

        public DinoGraph(int N) {
            this.N = N;
            nodes = new Node[N];
            for (int i = 0; i < N; i++) {
                nodes[i] = new Node();
                nodes[i].index = i;
            }
        }

        /**
         * Copy constructor
         */
        public DinoGraph(DinoGraph original) {
            this.N = original.N;
            this.nodes = new Node[N];

            for (int i = 0; i < N; i++) {
                this.nodes[i] = new Node(original.nodes[i]);
            }
        }

        public void setWeight(int i, int weight) {
            nodes[i].weight = weight;
        }

        public int getMultipliedWeight(int i) {
            return nodes[i].getMultipliedWeight();
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
                addFreeze(dest, color);
            }
        }

        public void resetColor(int i) {
            if (nodes[i].frozen)
                return;
            setColor(i, 0);
            for (int dest : nodes[i].islands) {
                nodes[dest].freezies.clear();
            }
        }

        public void addFreeze(int i, int color) {
            nodes[i].frozen = true;
            nodes[i].freezies.add(color);
        }

        public boolean isFrozen(int i) {
            return nodes[i].frozen;
        }

        public boolean canColor(int i, int color) {
            if (isFrozen(i) && !nodes[i].freezies.contains(color))
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
                .map(i -> String.format("%2d %s", i, nodes[i]))
                .collect(Collectors.joining(",\n  ", "nodes=[\n  ", "\n],\n"));

            ret += "weight=" + getTotalWeight() + ",\n";

            Map<Integer, List<Integer>> nodesByColor = IntStream.range(0, N).boxed()
                .collect(Collectors.groupingBy(this::getColor));

            ret += IntStream.range(1, colorNames.length)
                .mapToObj(color -> colorNames[color] + ": " + nodesByColor.get(color))
                .collect(Collectors.joining("\n  ", "bycolor:\n  ", ""));

            return ret + "\n)";
        }

        public int size() {
            return N;
        }

        public Node getNode(int i) {
            return nodes[i];
        }

    }

    public static class Node {
        public int weight;
        public int color;
        public int index;
        public boolean frozen;
        public List<Integer> edges = new ArrayList<>();
        public List<Integer> islands = new ArrayList<>();
        public Set<Integer> freezies = new HashSet<>();

        public Node() {
            this(-1);
        }

        public Node(int weight) {
            this.weight = weight;
        }

        // copy constructor
        public Node(Node original) {
            this.weight = original.weight;
            this.color = original.color;
            this.index = original.index;
            this.frozen = original.frozen;
            this.edges.addAll(original.edges);
            this.islands.addAll(original.islands);
            this.freezies.addAll(original.freezies);
        }

        public int getMultipliedWeight() {
            return weight * colorWeights[color] + colorConstants[color];
        }

        @Override
        public String toString() {
            return String.format("Node{i=%d, color=%d(%s), multipliedWeight=%d}", index, color, colorNames[color], getMultipliedWeight());
//            return colorNames[color];
        }
    }

}
