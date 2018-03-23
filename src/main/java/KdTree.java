import java.util.*;

public class KdTree {

    private KdTreeNode root;

    private class KdTreeNode {
        private KdTreeNode left;
        private KdTreeNode right;
        private Node data;
        private boolean isLonAxis;

        public KdTreeNode(KdTreeNode left, KdTreeNode right, Node data, boolean isLonAxis) {
            this.left = left;
            this.right = right;
            this.data = data;
            this.isLonAxis = isLonAxis;
        }
    }

    private class LongitudeComparator implements Comparator<Node> {
        @Override
        public int compare(Node node, Node t1) {
            if (node.longitude < t1.longitude)
                return -1;
            else if (node.longitude > t1.longitude)
                return 1;
            else
                return 0;
        }
    }

    private class LatitudeComparator implements Comparator<Node> {
        @Override
        public int compare(Node node, Node t1) {
            if (node.latitude < t1.latitude)
                return -1;
            if (node.latitude > t1.latitude)
                return 1;
            else
                return 0;
        }
    }

    public KdTree(Set<Node> setNodes) {
        ArrayList<Node> nodeList = new ArrayList<>();
        nodeList.addAll(setNodes);
        root = buildTree(nodeList, true);
    }

    public KdTreeNode buildTree(List<Node> nodeList, boolean isLonAxis) {
        if (nodeList.size() == 1) {
            Node lastNode = nodeList.get(0);
            return new KdTreeNode(null, null, lastNode, isLonAxis);
        }

        if (nodeList.isEmpty()) return null;

        if (isLonAxis) {
            Collections.sort(nodeList, new LongitudeComparator());
        }
        else {
            Collections.sort(nodeList, new LatitudeComparator());
        }

        int median = nodeList.size() / 2;
        Node current = nodeList.get(median);
        List<Node> leftList = nodeList.subList(0, median);
        List<Node> rightList = nodeList.subList(median + 1, nodeList.size());

        return new KdTreeNode(buildTree(leftList, !isLonAxis),
                                buildTree(rightList, !isLonAxis),
                                    current, isLonAxis);
    }

    public Node nearest(Node target, double[] box) {
        return nearest(root, box, target, null);
    }

    /**
     * Inspired by: https://gist.github.com/beginor/32dce6904a556474e7ad
     *
     * @param kdNode
     * @param box
     * @param target
     * @param candidate
     * @return
     */
    public Node nearest(KdTreeNode kdNode, double[] box, Node target, Node candidate) {

        if (kdNode == null) return candidate;

        double distanceBetweenTargetandNearest = 0.0;
        double distanceBetweenTargetandBox = 0.0;
        Node nearest = candidate;

        if (nearest != null) {
            distanceBetweenTargetandNearest = target.distanceSquared(nearest);
            distanceBetweenTargetandBox = distanceSquaredPointToBox(box, target);
        }

        if (nearest == null ||
                distanceBetweenTargetandNearest > distanceBetweenTargetandBox) {
            Node point = kdNode.data;
            if (nearest == null ||
                    distanceBetweenTargetandNearest > target.distanceSquared(point)) {
                nearest = point;
            }

            if (kdNode.isLonAxis) {
                double[] leftBox = {box[0], box[1], point.longitude, box[3]};
                double[] rightBox = {point.longitude, box[1], box[2], box[3]};

                if (target.longitude < point.longitude) {
                    nearest = nearest(kdNode.left, leftBox, target, nearest);
                    nearest = nearest(kdNode.right, rightBox, target, nearest);
                }
                else {
                    nearest = nearest(kdNode.right, rightBox, target, nearest);
                    nearest = nearest(kdNode.left, leftBox, target, nearest);
                }
            }
            else {
                double[] leftBox = {box[0], box[1], box[2], point.latitude};
                double[] rightBox = {box[0], point.latitude, box[2], box[3]};

                if (target.latitude < point.latitude) {
                    nearest = nearest(kdNode.left, leftBox, target, nearest);
                    nearest = nearest(kdNode.right, rightBox, target, nearest);
                }
                else {
                    nearest = nearest(kdNode.right, rightBox, target, nearest);
                    nearest = nearest(kdNode.left, leftBox, target, nearest);
                }
            }
        }

        return nearest;
    }

    public double distanceSquaredPointToBox(double[] box, Node point) {
        double dx = 0.0;
        double dy = 0.0;

        if (point.longitude < box[0]) {
            dx = point.longitude - box[0];
        }
        else if (point.longitude > box[2]) {
            dx = point.longitude - box[2];
        }

        if (point.latitude < box[1]) {
            dy = point.latitude - box[1];
        }
        else if (point.latitude > box[3]) {
            dy = point.latitude - box[3];
        }

        return dx * dx + dy * dy;
    }

}
