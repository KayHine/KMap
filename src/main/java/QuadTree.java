import java.awt.image.BufferedImage;

public class QuadTree
{

    private QTreeNode root;

    public void addQTreeNode(double ullon, double ullat,
                      double lrlon, double lrlat, String imgName, BufferedImage img) {

        root = addQTreeNode(root, ullon, ullat, lrlon, lrlat, imgName, img);
    }

    public QTreeNode addQTreeNode(QTreeNode node, double ullon, double ullat,
                      double lrlon, double lrlat, String imgName, BufferedImage img) {

        if (node == null) {
            return new QTreeNode(ullon, ullat, lrlon, lrlat, imgName, img);
        }

        double latMid = (node.lrlat + node.ullat) / 2;

        // Indicates top half
        if (ullat > latMid) {
            if (ullon == node.ullon && ullat == node.ullat) {
                addQTreeNode(node.northWest, ullon, ullat, lrlon, lrlat, imgName, img);
            }
            else {
                addQTreeNode(node.northEast, ullon, ullat, lrlon, lrlat, imgName, img);
            }
        }
        // Indicates bottom half
        else {
            if (lrlon == node.lrlon && lrlat == node.lrlat) {
                addQTreeNode(node.southWest, ullon, ullat, lrlon, lrlat, imgName, img);
            }
            else {
                addQTreeNode(node.southEast, ullon, ullat, lrlon, lrlat, imgName, img);
            }
        }

        return node;
    }

}
