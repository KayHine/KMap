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

    public HashMap<Node, HashSet<Node>> mapGraph;
    public HashMap<Long, Node> idMap;
    public HashMap<String, LinkedList<Node>> nodeNameMap;
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
        nodeNameMap = new HashMap<>();
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
        // clean the string by setting all letters to lower
        query = cleanString(query);
        LinkedList<String> cleanNames = autoComplete.getAutoSuggestions(query);

        if (cleanNames == null) return null;

        // retrieve the actual names with full capitalization from the nodeNameMap
        LinkedList<String> actualNames = new LinkedList<>();
        for (String name : cleanNames) {
            if (nodeNameMap.containsKey(name)) {
                Node matchingNode = nodeNameMap.get(name).getFirst();
                actualNames.add(matchingNode.name);
            }
        }

        return actualNames;
    }

    public LinkedList<Map<String, Object>> getLocationData(String query) {
        LinkedList<Map<String, Object>> locationData = new LinkedList<>();
        LinkedList<String> matchingLocations = new LinkedList<>();
        String cleanQuery = cleanString(query);
        matchingLocations = autoComplete.getAutoSuggestions(cleanQuery);
        for (String name : matchingLocations) {
            if (nodeNameMap.containsKey(name)) {
                // There could be multiple locations with the same name so
                // we build the location data for all of them, ie. multiple Chase Banks
                for (Node n : nodeNameMap.get(name)) {
                    HashMap<String, Object> location = new HashMap<>();
                    location.put("lat", n.latitude);
                    location.put("lon", n.longitude);
                    location.put("name", n.name);
                    location.put("id", n.id);
                    locationData.add(location);
                }
            }
        }

        return locationData;
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

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

}
