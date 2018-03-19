import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Wraps the parsing functionality of the MapDBHandler as an example.
 * You may choose to add to the functionality of this class if you wish.
 * @author Alan Yao
 */
public class GraphDB {

    private TreeMap<Node, SortedSet<Node>> mapGraph;

    /**
     * Example constructor shows how to create and start an XML parser.
     * @param db_path Path to the XML file to be parsed.
     */
    public GraphDB(String db_path) {
        mapGraph = new TreeMap<>(new Comparator<Node>() {
            @Override
            public int compare(Node node, Node t1) {
                if (node.longitude < t1.longitude) {
                    return -1;
                }
                else if (node.longitude > t1.longitude) {
                    return 1;
                }
                else {
                    return 0;
                }
            }
        });

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
    }

    public void putNode(Node node) {
        if (!mapGraph.containsKey(node)) {
            mapGraph.put(node, new TreeSet<>(new Comparator<Node>() {
                @Override
                public int compare(Node node, Node t1) {
                    if (node.latitude < t1.latitude) {
                        return -1;
                    }
                    else if (node.latitude > t1.latitude) {
                        return 1;
                    }
                    else {
                        return 0;
                    }
                }
            }));
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
    }
}
