The dino coloring problem: like regular graph coloring but for [pictures of dinosaurs](https://www.youtube.com/watch?v=FSZJJxFP62A). The goal is to find not just any 4-coloring, but the fastest one that can be input through Color a Dinosaur's awkward controls and slow flood fills.

Basically, each color and each vertex has a weight. The value of each vertex is equal to the vertex weight times the color weight, and we want to minimize the sum over all vertices. For much, much more detail on the processes and motivations, see https://tasvideos.org/8990S.
For the full TAS history, see https://tasvideos.org/Games/PublicationHistory/1855.

## Build & Run

Just do whatever it is you do to run maven projects. I recommend IntelliJ, but you do you.  

You can paste your graphs through stdin or specify a file path. There are a few other options as well:

```
[graph path] [-i <iteration scheme>] [-t <threshold>]
```

**graph path:** A file path to a graph. If unspecified, will read from stdin.  

**iteration scheme:** One of: `simple`, `shuffle`, `degree`, `weight`  
  This mostly exists for me to experiment with different vertex orderings to potentially speed things up.
  The default value of `degree` is fastest; you generally don't want to change this.
  * `simple` - Iterate vertices in the order declared in the graph. Decently likely to be fast on the given dinosaurs
       because they already have their 2 or 3 highest degree vertices first. **You need to use this scheme if your graph
       has M-type restrictions.**
  * `shuffle` - Iterate vertices in a random order. Very likely to be slow.
  * `degree` - Iterate vertices from highest to lowest degree. Very likely to be fast, since lots of possibilities get
       pruned early.
  * `weight` - Iterate vertices from highest to lowest weight. This can't quite compete with `degree`.

**threshold:** A Positive integer indicating how many frames of leeway to give the pruning. That is, the algorithm will
  consider all solutions within this many frames of the optimum. TASing is a bit of a fuzzy magic sometimes, so it's a
  good idea to consider slightly slower solutions that might be conducive to faster pen movement.

## Input Format

Graphs are contained in `.in` files with simple, plaintext, competitive programming style inputs. They have 3 sections: colordefs, the graph itself, and restrictions.

### Colordefs

The first line will contain a positive integer **C,** the number of colors. The next C lines will contain a color definition, like so:  
`<name> <weight> <constant>`  
Where `name` is a string with no spaces, and `weight` and `constant` are integers. `constant` may be negative.

### The Graph Itself

The first line will contain a positive integer **N,** the number of nodes. The next N lines will contain a node definition, like so:  
`<n> <weight> [e e e...]`  
`n`: The current node index from 0 to N - 1, mostly there to make the graphs more human readable.  
`weight`: The weight of this node, a positive integer  
`e`: Defines a bidirectional edge between this node and e. By convention, I only define them forward (e > n). There will be 0 or more of these.

### Restrictions

The first line will contain a nonnegative integer **R,** the number of restrictions. The next R lines will contain restrictions of the following types:  

* `F n c` - Freeze node n on color c. c can be a number 1 - C, or the name of the color. The main purpose of this is to describe regions that the pen can't reach, but it can also be used to try out ideas.
* `M a b` - Force nodes a and b to have the same color. I realized it's simpler to contract the nodes into one, so this is mostly unused. But I feel like leaving it in for now. **Make sure to run with -i simple**
