import java.awt.image.BufferedImage;
import java.util.LinkedList;

public class QuadTree {

    private QTreeNode root;

    // main function to add a node to the quad tree
    public void put(double ullon,
                    double ullat,
                    double lrlon,
                    double lrlat,
                    BufferedImage img) {
        root = put(root, ullon, ullat, lrlon, lrlat, img);
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
                    BufferedImage img) {
        // if tree is empty, make a node
        if (node == null) {
            return new QTreeNode(ullon, ullat, lrlon, lrlat, img);
        }

        // Check bounds of incoming coordinates
        // Returning null may break things - if we add an out of bound element,
        // does that set root to null???
        // Maybe we can throw an error instead?
        if (!inBound(node, ullon, ullat, lrlon, lrlat)) return null;

        QTreeNode[] nodeChildren = node.getChildren();

        // calculate children nodes by comparing coordinates to current node
        // Indicates left half
        if (ullon <= (node.getLrlon() + node.getUllon()) / 2) {
            // Indicates top left tree
            if (lrlat >= (node.getLrlat() + node.getUllat()) / 2) {
                nodeChildren[0] =
                        put(nodeChildren[0], ullon, ullat, lrlon, lrlat, img);
            }
            else {
                nodeChildren[2] =
                        put(nodeChildren[2], ullon, ullat, lrlon, lrlat, img);
            }
            // Indicates right half
        } else {
            // Indicates top right tree
            if (lrlat >= (node.getLrlat() + node.getUllat()) / 2) {
                nodeChildren[1] =
                        put(nodeChildren[1], ullon, ullat, lrlon, lrlat, img);
            } else {
                nodeChildren[3] =
                        put(nodeChildren[3], ullon, ullat, lrlon, lrlat, img);
            }
        }

        return node;
    }

    /**
     *
     * @param node: current node that you're trying to get the height of
     * @param level: the level that you want to return for level traversal
     * @param raster: an iterable Object that you're adding the QTreeNodes to
     */
    // Takes an iterable Object and adds the QTreeNodes on the bottom level
    public void getLowestLevel(QTreeNode node, int level, Iterable<QTreeNode> raster) {
        if (node == null) return;
        if (level == 1) {
            ((LinkedList<QTreeNode>) raster).add(node);
        } else if (level > 1) {
            QTreeNode[] children = node.getChildren();
            getLowestLevel(children[0], level - 1, raster);
            getLowestLevel(children[1], level - 1, raster);
            getLowestLevel(children[2], level - 1, raster);
            getLowestLevel(children[3], level - 1, raster);
        }
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
            QTreeNode[] children = node.getChildren();
            // compute the height of each substree
            int ULheight = getHeight(children[0]);
            int URheight = getHeight(children[1]);
            int LLheight = getHeight(children[2]);
            int LRheight = getHeight(children[3]);

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

        return (ullat <= node.getUllat() &&
                ullon >= node.getUllon() &&
                lrlat >= node.getLrlat() &&
                lrlon <= node.getLrlon());
    }
}
