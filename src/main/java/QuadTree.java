import java.awt.image.BufferedImage;
import java.util.LinkedList;

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
        double lonMid = (node.lrlon + node.ullon) / 2;

        if (ullon >= node.ullon && ullat <= node.ullat &&
                lrlon <= lonMid && lrlat >= latMid) {
            node.northWest = addQTreeNode(node.northWest, ullon, ullat, lrlon, lrlat, imgName);
        }
        else if (ullon >= lonMid && ullat <= node.ullat &&
                lrlon <= node.lrlon && lrlat >= latMid) {
            node.northEast = addQTreeNode(node.northEast, ullon, ullat, lrlon, lrlat, imgName);
        }
        else if (ullon >= node.ullon && ullat <= latMid &&
                lrlon <= lonMid && lrlat >= node.lrlat) {
            node.southWest = addQTreeNode(node.southWest, ullon, ullat, lrlon, lrlat, imgName);
        }
        else if (ullon >= lonMid && ullat <= latMid &&
                lrlon <= node.lrlon && lrlat >= node.lrlat) {
            node.southEast = addQTreeNode(node.southEast, ullon, ullat, lrlon, lrlat, imgName);
        }

        return node;
    }

    public Iterable<QTreeNode> gatherNodesInRange(double dpp, double[] viewBox) {
        LinkedList<QTreeNode> rasters = new LinkedList<>();
        double root_dpp = (root.lrlon - root.ullon) / 256;
        gatherNodesInRangeHelper(root, dpp, root_dpp, viewBox, rasters);
        return rasters;
    }

    public void gatherNodesInRangeHelper(QTreeNode node, double dpp, double node_dpp,
                                         double[] viewBox, Iterable<QTreeNode> rasters) {

        if (node_dpp <= dpp || node.northWest == null)
            return;

        node_dpp = (node.northEast.lrlon - node.northEast.ullon) / 256;
        double[] northWest = node.northWest.getCoords();
        double[] northEast = node.northEast.getCoords();
        double[] southWest = node.southWest.getCoords();
        double[] southEast = node.southEast.getCoords();

        if (viewBoxInBound(viewBox, northWest)) {
            if (node_dpp <= dpp) {
                ((LinkedList<QTreeNode>) rasters).add(node.northWest);
            }
            gatherNodesInRangeHelper(node.northWest, dpp, node_dpp, viewBox, rasters);
        }
        if (viewBoxInBound(viewBox, northEast)) {
            if (node_dpp <= dpp) {
                ((LinkedList<QTreeNode>) rasters).add(node.northEast);
            }
            gatherNodesInRangeHelper(node.northEast, dpp, node_dpp, viewBox, rasters);
        }
        if (viewBoxInBound(viewBox, southWest)) {
            if (node_dpp <= dpp) {
                ((LinkedList<QTreeNode>) rasters).add(node.southWest);
            }
            gatherNodesInRangeHelper(node.southWest, dpp, node_dpp, viewBox, rasters);
        }
        if (viewBoxInBound(viewBox, southEast)) {
            if (node_dpp <= dpp) {
                ((LinkedList<QTreeNode>) rasters).add(node.southEast);
            }
            gatherNodesInRangeHelper(node.southEast, dpp, node_dpp, viewBox, rasters);
        }
    }

    public boolean viewBoxInBound(double[] viewBox, double[] node_coords) {
        // If one rectangle is on the left side of the other
        if (node_coords[0] > viewBox[2] || viewBox[0] > node_coords[2]) {
            return false;
        }

        // If one rectangle is above the other
        if (node_coords[1] < viewBox[3] || viewBox[1] < node_coords[3]) {
            return false;
        }

        return true;
    }

}
