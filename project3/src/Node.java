import java.util.ArrayList;
public class Node implements Comparable<Node>{

    public int distance;
    public String id;
    public ArrayList<Link> links;
    public ArrayList<Link> seals;
    public int clearanceLevel;
    public int index;
    Node(String id,int clearanceLevel,int index){
        this.id=id;
        this.links=new ArrayList<>();
        this.seals=new ArrayList<>();
        this.distance=Integer.MAX_VALUE;
        this.clearanceLevel=clearanceLevel;
        this.index=index;

    }
    public boolean hasLinkTo(Node other){
        for (Link link : links){
            if (link.to==other){
                return true;
            }
        }
        return false;
    }
    public boolean hasSealTo(Node other){
        for (Link seal : seals){
            if (seal.to==other){
                return true;
            }
        }
        return false;
    }
    public Link getLinkTo(Node other){
        for (Link link : links){
            if (link.to==other){
                return link;
            }
        }
        return null;
    }
    public Link getSealTo(Node other){
        for (Link seal : seals){
            if (seal.to==other){
                return seal;
            }
        }
        return null;
    }
    public void removeLink(Node other){
        links.removeIf(link -> link.to==other);
    }
    public void removeSeal(Node other){
        seals.removeIf(seal -> seal.to==other);
    }
    public void addLink(Link link){
        links.add(link);
    }
    public void addSeal(Link link){
        seals.add(link);
    }

    @Override
    public int compareTo(Node node){
        if (distance>node.distance) return 1;
        if (distance<node.distance) return -1;
        return 0;
    }
}