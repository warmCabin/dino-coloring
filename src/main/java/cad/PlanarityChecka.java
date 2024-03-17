package cad;

import cad.Main.DinoGraph;
import cad.Main.Node;
import org.jgrapht.Graph;
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
    private final PlanarityTestingAlgorithm<Node, DefaultEdge> testa;

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

        this.testa = new BoyerMyrvoldPlanarityInspector<>(this.graph);
    }

    public boolean isPlanar() {
        return testa.isPlanar();
    }

}
