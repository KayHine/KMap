public class Node implements Comparable<Node> {

    public long id;
    public double latitude;
    public double longitude;
    public String name;

    public Node(long id, double lat, double lon) {
        this.id = id;
        this.latitude = lat;
        this.longitude = lon;
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
        if (this.longitude < o.longitude) {
            return -1;
        }
        else if (this.longitude > o.longitude) {
            return 1;
        }
        else {
            return 0;
        }
    }
}
