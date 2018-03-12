import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Map;

public class QuadTree {

    private QTreeNode root;

    // main function to add a node to the quad tree
    public void put(double ullon,
                    double ullat,
                    double lrlon,
                    double lrlat,
                    String imageName,
                    BufferedImage img) {
        root = put(root, ullon, ullat, lrlon, lrlat, imageName, img);
    }

    /**
     *
     * @param node: parent node you're trying to add to
     * @param ullat: upper left latitude of current child
     * @param ullon: upper left longitude of current child
     * @param lrlat: lower right latitude of current child
     * @param lrlon: lower right longtitude of current child
     * @param img: BufferedImage of the current node
     * @return: returns the root node
     */
    // put function to insert more nodes
    public QTreeNode put(QTreeNode node,
                    double ullon,
                    double ullat,
                    double lrlon,
                    double lrlat,
                    String imageName,
                    BufferedImage img) {
        // if tree is empty, make a node
        if (node == null) {
            return new QTreeNode(ullon, ullat, lrlon, lrlat, imageName, img);
        }

        // Check bounds of incoming coordinates
        // Returning null may break things - if we add an out of bound element,
        // does that set root to null???
        // Maybe we can throw an error instead?
        if (!inBound(node, ullon, ullat, lrlon, lrlat)) return null;

        // calculate children nodes by comparing coordinates to current node
        // Indicates left half
        if (ullon <= (node.lrlon + node.ullon) / 2) {
            // Indicates top left tree
            if (lrlat > (node.lrlat + node.ullat) / 2) {
                node.topLeft =
                        put(node.topLeft, ullon, ullat, lrlon, lrlat, imageName, img);
            }
            else {
                node.bottomLeft =
                        put(node.bottomLeft, ullon, ullat, lrlon, lrlat, imageName, img);
            }
            // Indicates right half
        } else {
            // Indicates top right tree
            if (lrlat > (node.lrlat + node.ullat) / 2) {
                node.topRight =
                        put(node.topRight, ullon, ullat, lrlon, lrlat, imageName, img);
            } else {
                node.bottomRight =
                        put(node.bottomRight, ullon, ullat, lrlon, lrlat, imageName, img);
            }
        }

        return node;
    }

    public void getLowestLevel(int level, Iterable<QTreeNode> raster,
                               Map<Double, Integer> latitudes) {
        getLowestLevel(root, level, raster, latitudes);
    }

    /**
     *
     * @param node: current node that you're trying to get the height of
     * @param level: the level that you want to return for level traversal
     * @param raster: an iterable Object that you're adding the QTreeNodes to
     */
    // Takes an iterable Object and adds the QTreeNodes on the bottom level
    public void getLowestLevel(QTreeNode node, int level, Iterable<QTreeNode> raster, Map<Double, Integer> latitudes) {
        if (node == null) return;
        if (level == 1) {
            // Build the list of QTnodes that we need to create the image
            ((LinkedList<QTreeNode>) raster).add(node);
            // Build a Map of the count of the latitudes <Latitude, Count of Latitude>
            if (!latitudes.containsKey(node.ullat)) {
                latitudes.put(node.ullat, 1);
            } else {
                latitudes.put(node.ullat, latitudes.get(node.ullat) + 1);
            }
        } else if (level > 1) {
            getLowestLevel(node.topLeft, level - 1, raster, latitudes);
            getLowestLevel(node.topRight, level - 1, raster, latitudes);
            getLowestLevel(node.bottomLeft, level - 1, raster, latitudes);
            getLowestLevel(node.bottomRight, level - 1, raster, latitudes);
        }
    }

    public int getHeight() {
        return getHeight(root);
    }

    /**
     *
     * @param node: current node you're trying to get the height of
     * @return: recursively return the height of the quadtree
     */
    public int getHeight(QTreeNode node) {
        if (node == null) {
            return 0;
        } else {
            // compute the height of each substree
            int ULheight = getHeight(node.topLeft);
            int URheight = getHeight(node.topRight);
            int LLheight = getHeight(node.bottomLeft);
            int LRheight = getHeight(node.bottomRight);

            // use the larger one
            int max = Math.max(Math.max(ULheight, URheight), Math.max(LLheight, LRheight));
            if (max == ULheight) {
                return (ULheight + 1);
            } else if (max == URheight) {
                return (URheight + 1);
            } else if (max == LLheight) {
                return (LLheight + 1);
            } else {
                return (LRheight +1);
            }
        }
    }

    /**
     *
     * @param node: parent node
     * @param ullat: upper left latitude of current child
     * @param ullon: upper left longitude of current child
     * @param lrlat: lower right latitude of current child
     * @param lrlon: lower right longtitude of current child
     * @return: true if current child you're trying to add is in bound
     */
    private boolean inBound(QTreeNode node,
                            double ullon,
                            double ullat,
                            double lrlon,
                            double lrlat) {

        return (ullat <= node.ullat &&
                ullon >= node.ullon &&
                lrlat >= node.lrlat &&
                lrlon <= node.lrlon);
    }
}
