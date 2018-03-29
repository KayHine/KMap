public class Node {

    public long id;
    public double latitude;
    public double longitude;
    public String name;
    public boolean isHighway;
    private double gScore;         // distaance from the source (default value is INFINITY)
    private double fScore;    // f = gScore + euclidian
    private double INFINITY = Double.POSITIVE_INFINITY;

    public Node(long id, double lon, double lat) {
        this.id = id;
        this.latitude = lat;
        this.longitude = lon;
        this.isHighway = false;
        this.gScore = INFINITY;
        this.fScore = INFINITY;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getGScore() {
        return gScore;
    }

    public void setGScore(double gScore) {
        this.gScore = gScore;
    }

    public void setFScore(double fScore) {
        this.fScore = fScore;
    }

    public double getFScore() {
        return this.fScore;
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
