import java.awt.image.BufferedImage;

public class QTreeNode {
    // children[0] - upper left child
    // children[1] - upper right child
    // children[2] - lower left child
    // children[3] - lower right child
    public QTreeNode topLeft;
    public QTreeNode topRight;
    public QTreeNode bottomLeft;
    public QTreeNode bottomRight;
    public BufferedImage img;
    public String imageName;
    public double ullat, ullon, lrlat, lrlon;

    // constructor for QTreeNode
    // children will get populated by put method in QuadTree
    public QTreeNode(
            double ullon,
            double ullat,
            double lrlon,
            double lrlat,
            String imageName,
            BufferedImage img) {
        this.ullat = ullat;
        this.ullon = ullon;
        this.lrlat = lrlat;
        this.lrlon = lrlon;
        this.img = img;
        this.imageName = imageName;
    }
}