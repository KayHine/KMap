import java.awt.image.BufferedImage;

public class QuadTree
{

    private QTreeNode root;

    public void addQTreeNode(double ullon, double ullat,
                      double lrlon, double lrlat, String imgName) {

        root = addQTreeNode(root, ullon, ullat, lrlon, lrlat, imgName);
    }

    public QTreeNode addQTreeNode(QTreeNode node, double ullon, double ullat,
                      double lrlon, double lrlat, String imgName) {

        if (node == null) {
            return new QTreeNode(ullon, ullat, lrlon, lrlat, imgName);
        }

        double latMid = (node.lrlat + node.ullat) / 2;

        // Indicates top half
        if (ullat > latMid) {
            if (ullon == node.ullon && ullat == node.ullat) {
                node.northWest = addQTreeNode(node.northWest, ullon, ullat, lrlon, lrlat, imgName);
            }
            else {
                node.northEast = addQTreeNode(node.northEast, ullon, ullat, lrlon, lrlat, imgName);
            }
        }
        // Indicates bottom half
        else {
            if (lrlon == node.lrlon && lrlat == node.lrlat) {
                node.southEast = addQTreeNode(node.southEast, ullon, ullat, lrlon, lrlat, imgName);
            }
            else {
                node.southWest = addQTreeNode(node.southWest, ullon, ullat, lrlon, lrlat, imgName);
            }
        }

        return node;
    }

}
