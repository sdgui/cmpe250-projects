// class for tracking the path, latency and hops during trace route
public class TraceNode implements Comparable<TraceNode> {
    Node host;
    int totalLatency;
    int hops;
    String pathString;

    public TraceNode(Node host, int totalLatency, int hops, String pathString) {
        this.host = host;
        this.totalLatency = totalLatency;
        this.hops = hops;
        this.pathString = pathString;
    }

    @Override
    public int compareTo(TraceNode other) {
        if (this.totalLatency != other.totalLatency) {
            return Integer.compare(this.totalLatency, other.totalLatency);
        }
        if (this.hops != other.hops) {
            return Integer.compare(this.hops, other.hops);
        }
        return this.pathString.compareTo(other.pathString);
    }
}