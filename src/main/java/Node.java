public class Node implements Comparable<Node> {

    public long id;
    public double latitude;
    public double longitude;
    public String name;
    public boolean isHighway;

    public Node(long id, double lat, double lon) {
        this.id = id;
        this.latitude = lat;
        this.longitude = lon;
        this.isHighway = false;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    @Override
    public int compareTo(Node o) {

        Double lon = new Double(this.longitude);
        Double oLon = new Double(o.longitude);
        int comp = lon.compareTo(oLon);

        if (comp != 0) {
            return comp;
        }
        else {
            Double lat = new Double(this.latitude);
            Double oLat = new Double(o.latitude);
            return lat.compareTo(oLat);
        }
    }
}
