import java.util.*;

public class KdTree {

    private KdTreeNode root;

    private class KdTreeNode {
        private KdTreeNode left;
        private KdTreeNode right;
        private Node data;

        public KdTreeNode(KdTreeNode left, KdTreeNode right, Node data) {
            this.left = left;
            this.right = right;
            this.data = data;
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

    public KdTree(Set<Node> setNodes, int depth) {
        ArrayList<Node> nodeList = new ArrayList<>();
        nodeList.addAll(setNodes);
        root = buildTree(nodeList, depth);
    }

    public KdTreeNode buildTree(List<Node> nodeList, int depth) {
        if (nodeList.size() == 1) {
            Node lastNode = nodeList.get(0);
            return new KdTreeNode(null, null, lastNode);
        }

        if (nodeList.isEmpty()) return null;

        int axis = depth % 2;

        if (axis == 0) {
            Collections.sort(nodeList, new LongitudeComparator());
        }
        else {
            Collections.sort(nodeList, new LatitudeComparator());
        }

        int median = nodeList.size() / 2;
        Node current = nodeList.get(median);
        List<Node> leftList = nodeList.subList(0, median);
        List<Node> rightList = nodeList.subList(median + 1, nodeList.size());

        return new KdTreeNode(buildTree(leftList, depth + 1),
                                buildTree(rightList, depth + 1),
                                    current);
    }

    public Node nearest(Node target, double[] box) {
        return nearest(root, box, target, null);
    }

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
            Node point = new Node(0, kdNode.data.longitude, kdNode.data.latitude);
            if (nearest == null ||
                    distanceBetweenTargetandNearest > target.distanceSquared(point)) {
                nearest = point;
            }


        }

        return nearest;
    }

    public double distanceSquaredPointToBox(double[] box, Node point) {
        double dx = 0.0;
        double dy = 0.0;

        if (point.longitude < box[0]) dx = point.longitude - box[0];
        else if (point.longitude > box[2]) dx = point.longitude - box[2];

        if (point.latitude < box[1]) dy = point.latitude - box[1];
        else if (point.latitude > box[3]) dy = point.latitude - box[3];

        return dx * dx + dy * dy;
    }

}
