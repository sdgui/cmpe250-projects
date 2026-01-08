import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;
public class Graph{
    private ArrayList<Node> nodes;
    private HashTable<Node> nodeTable;
    public int indexnumber=0;
    public Graph(){
        this.nodes=new ArrayList<>();
        this.nodeTable=new HashTable<>();
    }

    public boolean addNode(String id,int clearanceLevel){
        if (nodeTable.get(id)!=null){
            return false;
        }
        Node node=new Node(id,clearanceLevel,indexnumber++);
        nodes.add(node);
        nodeTable.put(id,node);
        return true;
    }
    public boolean addLink(String fromId, String toId,int latency,int bandwidth, int firewall){
        Node fromNode=nodeTable.get(fromId);
        Node toNode=nodeTable.get(toId);
        if (fromNode==null || toNode==null||fromNode.hasLinkTo(toNode)||fromNode.hasSealTo(toNode)){
            return false;
        }
            fromNode.links.add(new Link(fromNode,toNode,latency,bandwidth,firewall));
            toNode.links.add(new Link(toNode,fromNode,latency,bandwidth,firewall));
        return true;
    }

    /**
     * seals or unseals a link between two nodes
     * @param id1
     * @param id2
     * @return 0 if error, 1 if sealed, 2 if unsealed
     */
    public int sealNode(String id1, String id2){
        Node n1 =nodeTable.get(id1);
        Node n2 =nodeTable.get(id2);
        if (n1 ==null || n2 ==null|| id1.equals(id2)||(!n1.hasLinkTo(n2)&&!n1.hasSealTo(n2))){
            //return 0 for error
            return 0;
        }
        if (n1.hasLinkTo(n2)){
            Link link = n1.getLinkTo(n2);
            n1.removeLink(n2);
            n1.addSeal(link);
            link=n2.getLinkTo(n1);
            n2.removeLink(n1);
            n2.addSeal(link);
            //return 1 for sealed
            return 1;
        }
        else{
            Link link= n1.getSealTo(n2);
            n1.removeSeal(n2);
            n1.addLink(link);
            link=n2.getSealTo(n1);
            n2.removeSeal(n1);
            n2.addLink(link);
            //return 2 for unsealed
            return 2;
        }
    }

    /**
     * traces the optimal route between two nodes given min bandwidth and lambda
     * uses a classical Dijkstra algorithm when lambda=0
     * uses a modified Dijkstra algorithm when lambda>0
     * @param fromId
     * @param toId
     * @param minBandwidth
     * @param lambda
     * @return string indicating optimal route or no route found
     */
    public String traceRoute(String fromId, String toId,int minBandwidth, int lambda) {
            Node n1=nodeTable.get(fromId);
            Node n2=nodeTable.get(toId);
            for (int i = 0; i < indexnumber; i++) {
                nodes.get(i).distance = Integer.MAX_VALUE;
            }
            if (n1==null || n2==null){
                return "Some error occurred in trace_route.";
            }
            if (lambda==0){
                MinHeap<TraceNode> pq=new MinHeap<>();
                boolean[] visited= new boolean[indexnumber];
                n1.distance=0;
                pq.offer(new TraceNode(n1,0,0,n1.id));
                TraceNode current;
                while(!pq.isEmpty()){
                    current=pq.poll();
                    if (visited[current.host.index]) continue;
                    visited[current.host.index]=true;
                    if (current.host.id.equals(toId)) {
                        return "Optimal route "+fromId+ " -> "+toId+": "+current.pathString+" (Latency = "+current.totalLatency+"ms)";
                    }
                    for (Link l:current.host.links){
                        if (l.bandwidth>=minBandwidth && l.firewall<=l.from.clearanceLevel
                            &&l.latency+l.from.distance<=l.to.distance){
                            l.to.distance=l.latency+l.from.distance;
                            pq.offer(new TraceNode(l.to,current.totalLatency+l.latency, current.hops+1,current.pathString+" -> "+l.to.id));
                        }
                    }
                }
                return "No route found from "+fromId+" to "+toId;
            }
            /*algorithm with lambda
            adds every path regardless of latency if it has lower hops than previously found path to that node
            this way, paths are evaluated even if they have higher latency but lower hops which could lead to a better overall path due to lambda penalty
             */
            else{
                int minHops[]=new int[indexnumber];
                for (int i=0;i<indexnumber;i++){
                    minHops[i]=Integer.MAX_VALUE;
                }
                MinHeap<TraceNode>pq=new MinHeap<>();
                pq.offer(new TraceNode(n1,0,0,n1.id));
                while(!pq.isEmpty()){
                    TraceNode current=pq.poll();
                    //skips if there is a path with lower hops than this since this path will also have a worse latency due to priority queue ordering
                    if (current.hops>=minHops[current.host.index]){
                        continue;
                    }
                    minHops[current.host.index]=current.hops;
                    if (current.host.id.equals(toId)){
                        return "Optimal route "+fromId+ " -> "+toId+": "+current.pathString+" (Latency = "+current.totalLatency+"ms)";
                    }
                    for (Link l:current.host.links){

                        if (l.bandwidth>=minBandwidth && l.firewall<=l.from.clearanceLevel ){
                            //skips if there exists a path with lower hops to that node
                            if (current.hops+1>=minHops[l.to.index]){
                                continue;
                            }
                            int newLatency=current.totalLatency+l.latency+lambda* current.hops;
                            pq.offer(new TraceNode(l.to,newLatency,current.hops+1,current.pathString+" -> "+l.to.id));

                        }
                    }
                }
                return "No route found from "+fromId+" to "+toId;
            }
    }

    /**
     * scans the network for connectivity using BFS
     * @return string indicating if network is fully connected or number of connected components
     */
    public String scanConnectivity(){
        if (nodes.size()<=1){
            return "Network is fully connected.";
        }
        boolean visited[]=new boolean[indexnumber];
        int componentCount =0;
        for (Node node:nodes){
            if (!visited[node.index]){
                componentCount++;
                bfs(node,visited);
            }
        }
        if (componentCount ==1){
            return "Network is fully connected.";
        }
        else{
            return "Network has " + componentCount + " disconnected components.";
        }

    }
    //bfs method used for scanConnectivity()
    private void bfs(Node start, boolean[] visited){
        LinkedList<Node> queue=new LinkedList<>();
        queue.offer(start);
        visited[start.index]=true;
        while(!queue.isEmpty()){
            Node current=queue.poll();
            for (Link link:current.links){
                if (!visited[link.to.index]){
                    visited[link.to.index]=true;
                    queue.add(link.to);
                }
            }
        }
    }
    //overloaded method that excludes a node from the scan for simulateHostBreach
    private void bfs(Node start, boolean[] visited,Node excludeNode){
        LinkedList<Node> queue=new LinkedList<>();
        queue.offer(start);
        visited[start.index]=true;
        while(!queue.isEmpty()){
            Node current=queue.poll();
            for (Link link:current.links){
                if (link.to==excludeNode){
                    continue;
                }
                if (!visited[link.to.index]){
                    visited[link.to.index]=true;
                    queue.add(link.to);
                }
            }
        }
    }

    //same method as scanConnectivity, returns int instead of string
    private int scanConnectivityComponent(){
        if (nodes.size()<=1){
            return 1;
        }
        boolean visited[]=new boolean[indexnumber];
        int componentCount =0;
        for (Node node:nodes){
            if (!visited[node.index]){
                componentCount++;
                bfs(node,visited);
            }
        }
        return componentCount;

    }
    //overloaded method that excludes a node from the scan for simulateHostBreach
    private int scanConnectivityComponent(Node excludeNode){
        if (nodes.size()<=1){
            return 1;
        }
        boolean visited[]=new boolean[indexnumber];
        int componentCount =0;
        for (Node node:nodes){
            if (node==excludeNode){
                continue;
            }
            if (!visited[node.index]){
                componentCount++;
                bfs(node,visited,excludeNode);
            }
        }
        return componentCount;
    }
    public String simulateHostBreach(String id){
        Node node=nodeTable.get(id);
        if (node==null){
            return "Some error occurred in simulate_breach.";
        }
        int componentCountbefore=scanConnectivityComponent();
        int componentCountafter=scanConnectivityComponent(node);
        int diff=componentCountafter - componentCountbefore;
        if (diff<=0){
            return "Host "+id +" is NOT an articulation point. Network remains the same.";
        }
        else{

            return "Host "+id+" IS an articulation point.\nFailure results in "+componentCountafter+" disconnected components.";
        }
    }
    public String simulateBackDoorBreach(String id1, String id2) {
        Node n1 = nodeTable.get(id1);
        Node n2 = nodeTable.get(id2);
        if (n1 == null || n2 == null || !n1.hasLinkTo(n2)) {
            return "Some error occurred in simulate_breach.";
        }
        Link link1 = n1.getLinkTo(n2);
        Link link2 = n2.getLinkTo(n1);
        n1.removeLink(n2);
        n2.removeLink(n1);
        int componentCount = scanConnectivityComponent();
        //adds the links again
        n1.addLink(link1);
        n2.addLink(link2);
        if (componentCount == 1) {
            return "Backdoor " + id1 + " <-> " + id2 + " is NOT a bridge. Network remains the same.";
        } else {
            return "Backdoor " + id1 + " <-> " + id2 + " IS a bridge." +
                    "\nFailure results in " + componentCount + " disconnected components.";
        }
    }
    public String oracleReport(){
        int unsealedBackdoorCount=0;
        int totalBandwidth=0;
        int clearanceLevelSum=0;

        for (Node node:nodes){
            clearanceLevelSum+=node.clearanceLevel;
            unsealedBackdoorCount+=node.links.size();
            for (Link l:node.links){
                totalBandwidth+=l.bandwidth;
            }
        }
        totalBandwidth/=2;
        unsealedBackdoorCount/=2;
        BigDecimal totalBandw=new BigDecimal(totalBandwidth);
        BigDecimal unsealedBackdoor=new BigDecimal(unsealedBackdoorCount);
        BigDecimal totalClearance=new BigDecimal(clearanceLevelSum);
        BigDecimal averageBandwidth=totalBandw.divide(unsealedBackdoor,1,RoundingMode.HALF_UP);
        BigDecimal averageClearance=totalClearance.divide(new BigDecimal(indexnumber),1,RoundingMode.HALF_UP);

        int componentCount= scanConnectivityComponent();
        String connectivity=componentCount==1?"Connected":"Disconnected";

        String cycles=containsCycle()?"Yes":"No";
        return "--- Resistance Network Report ---\n" +
                "Total Hosts: "+indexnumber+"\n" +
                "Total Unsealed Backdoors: "+unsealedBackdoorCount + "\n" +
                "Network Connectivity: " +connectivity +"\n" +
                "Connected Components: " +componentCount+"\n" +
                "Contains Cycles: " +cycles+"\n" +
                "Average Bandwidth: " +averageBandwidth+"Mbps\n" +
                "Average Clearance Level: "+averageClearance;
    }


    /*
    helper method for oracleReport to check if the graph contains cycles
    uses dfs to detect cycles
     */
    private boolean containsCycle(){
        boolean[] visited=new boolean[indexnumber];
        for (Node node:nodes){
            if (!visited[node.index]){
                if (dfsCycle(node,null,visited)){
                    return true;
                }
            }
        }
        return false;
    }
    //helper dfs method for containsCycle()
    private boolean dfsCycle(Node current,Node parent, boolean[] visited){
        visited[current.index]=true;
        for (Link link:current.links){
            if (!visited[link.to.index]){
                if (dfsCycle(link.to,current,visited)){
                    return true;
                }
            }
            else if (link.to!=parent){
                return true;
            }
        }
        return false;
    }
}