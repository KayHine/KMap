public class QTreeNode implements Comparable<QTreeNode> {

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

    public double[] getCoords() {
        double[] coords = {ullon, ullat, lrlon, lrlat};
        return coords;
    }

    @Override
    public int compareTo(QTreeNode o) {
        if (this.ullat < o.ullat) {
            return -1;
        }
        else if (this.ullat > o.ullat) {
            return 1;
        }
        else {
            return 0;
        }
    }
}
