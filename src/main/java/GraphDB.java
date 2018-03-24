import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Wraps the parsing functionality of the MapDBHandler as an example.
 * You may choose to add to the functionality of this class if you wish.
 * @author Alan Yao
 */
public class GraphDB {

    private HashMap<Node, HashSet<Node>> mapGraph;
    public HashMap<Long, Node> idMap;
    private KdTree nearestKdTree;
    public Trie autoComplete;
    double minlon, minlat, maxlon, maxlat;

    /**
     * Example constructor shows how to create and start an XML parser.
     * @param db_path Path to the XML file to be parsed.
     */
    public GraphDB(String db_path) {
        mapGraph = new HashMap<>();
        idMap = new HashMap<>();
        autoComplete = new Trie();
        try {
            File inputFile = new File(db_path);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            MapDBHandler maphandler = new MapDBHandler(this);
            saxParser.parse(inputFile, maphandler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
        nearestKdTree = new KdTree(mapGraph.keySet());
    }

    public void putNode(Node node) {
        if (!mapGraph.containsKey(node)) {
            mapGraph.put(node, new HashSet<>());
        }
    }

    public void addEdge(Node source, Node dest) {
        if (!mapGraph.containsKey(source)) {
            putNode(source);
        }
        else if (!mapGraph.containsKey(dest)) {
            putNode(dest);
        }

        // Add adjacent nodes to each source and dest because this is undirected
        mapGraph.get(source).add(dest);
        mapGraph.get(dest).add(source);
    }

    public HashSet<Node> getNeighbors(Node key) {
        return mapGraph.get(key);
    }

    /**
     * idMap function to access the HashMap
     *
     * @param id
     * @return
     */
    public Node getNodeByID(long id) {
        return idMap.get(id);
    }

    /**
     * kdTree function to get the nearest neighboring Node
     *
     * @param lon
     * @param lat
     * @return
     */
    public Node getNearestNode(double lon, double lat) {
        Node target = new Node(0, lon, lat);
        double[] boundingBox = {minlon, minlat, maxlon, maxlat};
        return nearestKdTree.nearest(target, boundingBox);
    }

    /**
     * Trie function to get auto-complete words
     */
    public LinkedList<String> getAutoCompleteSuggestions(String query) {
        return autoComplete.getAutoSuggestions(query);
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        // LESSON LEARNED: Iterating over a list and trying to remove items will get you
        // java.util.ConcurrentModificationException error. Alternatively, Use the remove() method
        // on the iterator itself. Note that this means you can't use the enhanced for loop.
        Iterator it = mapGraph.keySet().iterator();
        while (it.hasNext()) {
            Node key = (Node) it.next();
            if (mapGraph.get(key).isEmpty()) {
                it.remove();
            }
        }
//        System.out.println("highway nodes " + count);
//        System.out.println("total nodes: " + mapGraph.keySet().size());
    }


}
