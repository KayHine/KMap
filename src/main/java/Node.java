public class Node {

    public long id;
    public double latitude;
    public double longitude;
    public String name;
    public boolean isHighway;
    private double dist;         // distance from the source (default value is INFINITY)
    private double euclidian;    // euclidian distance to destination
    private double heuristic;    // f = dist + euclidian

    private double INFINITY = Double.POSITIVE_INFINITY;

    public Node(long id, double lat, double lon) {
        this.id = id;
        this.latitude = lat;
        this.longitude = lon;
        this.isHighway = false;
        this.dist = INFINITY;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getDist() {
        return dist;
    }

    public void setDist(double dist) {
        this.dist = dist;
    }

    public void setHeuristic(Node destination) {
        euclidian = Math.sqrt(Math.pow(destination.longitude - this.longitude, 2) +
                                Math.pow(destination.latitude - this.latitude, 2));

        heuristic = dist + euclidian;
    }

    public double getHeuristic() {
        return this.heuristic;
    }

    public double distanceBetweenNodes(Node destination) {
        return Math.sqrt(Math.pow(destination.longitude - this.longitude, 2) +
                Math.pow(destination.latitude - this.latitude, 2));
    }

    public double distanceSquared(Node destination) {
        return Math.pow(destination.longitude - this.longitude, 2) +
                Math.pow(destination.latitude - this.latitude, 2);
    }

    // Overriding equals() and hashCode in order to use Nodes as a
    // key in a TreeMap. I want to use the node's refID as the key but i also want
    // to store the entire Node with all its information as well
    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != this.getClass()) {
            return false;
        }
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        Node node = (Node) obj;
        if (node.id == this.id) {
            return true;
        }

        return false;
    }
}
