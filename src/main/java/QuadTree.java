import java.awt.image.BufferedImage;

public class QuadTree {

    private QTreeNode root;

    // main function to add a node to the quad tree
    public void put(double ullat,
                    double ullon,
                    double lrlat,
                    double lrlon,
                    BufferedImage img) {
        root = put(root, ullat, ullon, lrlat, lrlon, img);
    }

    // put function to insert more nodes
    public QTreeNode put(QTreeNode node,
                    double ullat,
                    double ullon,
                    double lrlat,
                    double lrlon,
                    BufferedImage img) {
        // if tree is empty, make a node
        if (node == null) {
            return new QTreeNode(ullat, ullon, lrlat, lrlon, img);
        }

        // Check bounds of incoming coordinates
        // Returning null may break things - if we add an out of bound element,
        // does that set root to null???
        // Maybe we can throw an error instead?
        if (!inBound(node, ullat, ullon, lrlat, lrlon)) return null;

        QTreeNode[] nodeChildren = node.getChildren();

        // calculate children nodes by comparing coordinates to current node
        // Indicates left half
        if (ullon <= (node.getLrlon() + node.getUllon()) / 2) {
            // Indicates top left tree
            if (lrlat >= (node.getLrlat() + node.getUllat()) / 2) {
                nodeChildren[0] =
                        put(nodeChildren[0], ullat, ullon, lrlat, lrlon, img);
            }
            else {
                nodeChildren[2] =
                        put(nodeChildren[2], ullat, ullon, lrlat, lrlon, img);
            }
            // Indicates right half
        } else {
            // Indicates top right tree
            if (lrlat >= (node.getLrlat() + node.getUllat()) / 2) {
                nodeChildren[1] =
                        put(nodeChildren[1], ullat, ullon, lrlat, lrlon, img);
            } else {
                nodeChildren[3] =
                        put(nodeChildren[3], ullat, ullon, lrlat, lrlon, img);
            }
        }

        return node;
    }

    private boolean inBound(QTreeNode node,
                            double ullat,
                            double ullon,
                            double lrlat,
                            double lrlon) {

        return (ullat <= node.getUllat() &&
                ullon >= node.getUllon() &&
                lrlat >= node.getLrlat() &&
                lrlon <= node.getLrlon());
    }
}
