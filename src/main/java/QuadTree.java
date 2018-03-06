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

    // help function to add a node to the quad tree
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

        QTreeNode[] nodeChildren = node.getChildren();

        // calculate children nodes by comparing coordinates to current node
        // upper left child
        if (ullat == node.getUllat() && ullon == node.getUllon()
                && lrlat / 2 == node.getLrlat() && lrlon / 2 == node.getLrlon()) {
            nodeChildren[0] =
                    put(nodeChildren[0], ullat, ullon, lrlat, lrlon, img);
            // upper right child
        } else if (ullat == node.getUllat() && ullon == node.getLrlat() / 2
                && lrlon == node.getLrlon() && lrlat == node.getUllat() / 2) {
            nodeChildren[1] =
                    put(nodeChildren[1], ullat, ullon, lrlat, lrlon, img);
            // lower left child
        } else if (ullon == node.getUllon() && ullat == node.getUllat()/2
                && lrlon == node.getLrlon() / 2 && lrlat == node.getLrlat()) {
            nodeChildren[2] =
                    put(nodeChildren[2], ullat, ullon, lrlat, lrlon, img);
            // lower right child
        } else if (ullon == node.getUllon() / 2 && ullat == node.getUllat() / 2
                && lrlon == node.getLrlon() && lrlat == node.getLrlat()) {
            nodeChildren[3] =
                    put(nodeChildren[3], ullat, ullon, lrlat, lrlon, img);
        }

        return node;
    }

}
