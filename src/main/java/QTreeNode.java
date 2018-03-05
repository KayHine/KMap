import java.awt.image.BufferedImage;

public class QTreeNode {
    private QTreeNode childTopLeft;
    private QTreeNode childTopRight;
    private QTreeNode childBottomLeft;
    private QTreeNode childBottomRight;
    private QTreeNode parent;
    private BufferedImage img;

    public QTreeNode(
            QTreeNode parent,
            QTreeNode childTopLeft,
            QTreeNode childTopRight,
            QTreeNode childBottomLeft,
            QTreeNode childBottomRight,
            BufferedImage img) {

        this.parent = parent;
        this.childTopLeft = childTopLeft;
        this.childTopRight = childTopRight;
        this.childBottomLeft = childBottomLeft;
        this.childBottomRight = childBottomRight;
        this.img = img;
    }
}
