import java.awt.image.BufferedImage;

public class QTreeNode {
    // children[0] - upper left child
    // children[1] - upper right child
    // children[2] - lower left child
    // children[3] - lower right child
    private QTreeNode[] children;
    private BufferedImage img;
    private double ullat, ullon, lrlat, lrlon;

    // constructor for QTreeNode
    // children will get populated by put method in QuadTree
    public QTreeNode(
            double ullat,
            double ullon,
            double lrlat,
            double lrlon,
            BufferedImage img) {
        this.ullat = ullat;
        this.ullon = ullon;
        this.lrlat = lrlat;
        this.lrlon = lrlon;
        this.img = img;
        this.children = new QTreeNode[4];
    }

    public double getUllat() {
        return ullat;
    }

    public double getUllon() {
        return ullon;
    }

    public double getLrlat() {
        return lrlat;
    }

    public double getLrlon() {
        return lrlon;
    }

    public QTreeNode[] getChildren() {
        return children;
    }
}
