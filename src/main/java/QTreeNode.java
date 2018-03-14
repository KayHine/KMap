import java.awt.image.BufferedImage;

public class QTreeNode {
    public QTreeNode northWest;
    public QTreeNode northEast;
    public QTreeNode southWest;
    public QTreeNode southEast;
    public double ullon, ullat, lrlon, lrlat;
    public String imageName;

    public QTreeNode(double ullon, double ullat, double lrlon, double lrlat, String imageName) {
        this.ullon = ullon;
        this.ullat = ullat;
        this.lrlon = lrlon;
        this.lrlat = lrlat;
        this.imageName = imageName;
    }
}
