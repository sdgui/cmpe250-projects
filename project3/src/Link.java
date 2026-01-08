public class Link{
    public Node from;
    public Node to;
    public int latency;
    public int bandwidth;
    public int firewall;
    Link(Node from, Node to, int latency, int bandwidth, int firewall){
        this.from=from;
        this.to=to;
        this.latency=latency;
        this.bandwidth=bandwidth;
        this.firewall=firewall;
    }
}

