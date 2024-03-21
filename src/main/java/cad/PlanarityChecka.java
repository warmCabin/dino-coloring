package cad;

import cad.Main.DinoGraph;
import cad.Main.Node;
import org.jgrapht.Graph;
import org.jgrapht.alg.color.BrownBacktrackColoring;
import org.jgrapht.alg.drawing.FRLayoutAlgorithm2D;
import org.jgrapht.alg.drawing.model.Box2D;
import org.jgrapht.alg.drawing.model.MapLayoutModel2D;
import org.jgrapht.alg.interfaces.PlanarityTestingAlgorithm;
import org.jgrapht.alg.planar.BoyerMyrvoldPlanarityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

/**
 * <p>
 * Bridge between my silly DinoGraph and some real computer science.
 * <p>
 * Wait... I imported a whole ass graph theory library into this project only to not use it for the main algorithm!?
 * Yeah. Even though jgrapht has plenty of coloring algorithms, the dino coloring problem is much more specific.
 * Maybe you could come up with some unholy way to reduce it to some weird metametagraph that one of the coloring
 * algorithms could consume, but idk. I think dino coloring is just brute forcey.
 * <p>
 * Why go through all this trouble? I'm just curious to see if the weird exclaves or my interpretations of the awkward
 * pixel art borders result in any non-planar graphs. And planarity checking is <i>way</i> harder than I thought.
 */
public class PlanarityChecka {

    private final Graph<Node, DefaultEdge> graph;
    private final PlanarityTestingAlgorithm<Node, DefaultEdge> planarityTesta;
    private final BrownBacktrackColoring<Node, DefaultEdge> colorTesta;

    public PlanarityChecka(DinoGraph dinoGraph) {
        graph = new DefaultUndirectedGraph<>(DefaultEdge.class);

        for (int i = 0; i < dinoGraph.size(); i++) {
            Node src = dinoGraph.getNode(i);
            graph.addVertex(src);
            for (int j = 0; j < src.edges.size(); j++) {
                Node dest = dinoGraph.getNode(src.edges.get(j));
                graph.addVertex(dest);
                graph.addEdge(src, dest);
            }
        }

        planarityTesta = new BoyerMyrvoldPlanarityInspector<>(graph);
        colorTesta = new BrownBacktrackColoring<>(graph);
    }

    public boolean isPlanar() {
//        if (!planarityTesta.isPlanar()) {
            // The Kuratowski subdivision is the kernel of nonplanarity that the testa found;
            // described as a "certificate" in the Javadocs. But you already knew that because you've
            // cloned this onto your computer and imported it into an IDE so you can use keyboard shortcuts
            // to read all the docs... right?
//            displayGraph(planarityTesta.getKuratowskiSubdivision());
//        }
        return planarityTesta.isPlanar();
    }

    // Just playing around with the LayoutModel stuff. I'll need another library to actually display anything--
    // or just write it myself.
    private void displayGraph(Graph<Node, DefaultEdge> graph) {
        var layoutModel = new MapLayoutModel2D<Node>(new Box2D(600, 400));
        new FRLayoutAlgorithm2D<Node, DefaultEdge>().layout(graph, layoutModel);
        System.out.println("Node draw coords: ");
        for (var entry : layoutModel) {
            System.out.println("  " + entry);
        }
        System.out.println("Edges: ");
        for (var e : graph.edgeSet()) {
            System.out.println("  " + e);
        }
    }

    public int chromaticNumba() {
        return colorTesta.getChromaticNumber();
    }

}
