import java.util.ArrayList;

public class AvlTree {
    class Node{
        Freelancer val;
        Node right;
        Node left;
        int height;
        Node (Freelancer val){
            this.val = val;
            this.height=0;
        }
        //gets the balance
        int getBalance(){
            if (right==null&&left==null) return 0;
            if (right == null) return left.height+1;
            if (left==null) return -right.height-1;
            return left.height-right.height;
        }


    }
    public Node root;
    AvlTree(Node root){
        this.root = root;
    }
    AvlTree(){}
    private void updateHeight(Node node){
        if (node.left==null&&node.right==null) {
            node.height = 0;
            return;
        }
        if (node.right==null){
            node.height=node.left.height+1;
            return;
        }
        if (node.left==null){
            node.height=node.right.height+1;
            return;
        }
        node.height = Math.max(node.left.height, node.right.height)+1;
    }
    /*
    compare method, first compares according to composite score, then ids
    returns 1 if f1 is greater
     */
    int compare(Freelancer f1, Freelancer f2){
        if (f1.compositeScore>f2.compositeScore)
            return 1;
        else if (f1.compositeScore<f2.compositeScore)
            return -1;
        if (f1.id.compareTo(f2.id)<0)
            return 1;
        else if (f1.id.compareTo(f2.id)>0) return -1;
        return 0;
    }

    // Right rotate subtree rooted with node
    Node rightRotate(Node node) {
        Node leftChild = node.left;
        Node temp = leftChild.right;

        // Perform rotation
        leftChild.right = node;
        node.left = temp;

        // Update heights
        updateHeight(node);
        updateHeight(leftChild);

        // Return new root
        return leftChild;
    }

    // Left rotate subtree rooted with node
    Node leftRotate(Node node) {
        Node rightChild = node.right;
        Node temp = rightChild.left;

        // Perform rotation
        rightChild.left = node;
        node.right = temp;

        // Update heights
        updateHeight(node);
        updateHeight(rightChild);

        // Return new root
        return rightChild;
    }
    Node balance(Node n) {
        int balance = n.getBalance();

        // Case 1: Left-heavy (balance > 1)
        if (balance > 1) {
            // Check LEFT child's balance
            // If left child is right-heavy (< 0), it's a Left-Right case
            if (n.left.getBalance() < 0) {
                n.left = leftRotate(n.left);
            }
            // Always finish with a right rotation (for both LL and LR)
            return rightRotate(n);
        }

        // Case 2: Right-heavy (balance < -1)
        if (balance < -1) {
            // Check RIGHT child's balance
            // If right child is left-heavy (> 0), it's a Right-Left case
            if (n.right.getBalance() > 0) {
                n.right = rightRotate(n.right);
            }
            // Always finish with a left rotation (for both RR and RL)
            return leftRotate(n);
        }

        return n; // No imbalance
    }
    Node insert(Node n, Freelancer f){
        if (n==null)
            return new Node(f);
        if (compare(n.val,f)>0)
            n.left=insert(n.left,f);
        if (compare(n.val,f)<0)
            n.right=insert(n.right,f);
        else return n;
        updateHeight(n);

        return balance(n);
    }
    Node delete(Node n, Freelancer f){
        if (n==null){
            return n;
        }
        else if (compare(n.val,f)<0){
            n.right=delete(n.right,f);
        }
        else if (compare(n.val,f)>0){
            n.left=delete(n.left,f);
        }
        else
        {
            if (n.right == null)
                n = n.left;

            else if (n.left == null)
                n = n.right;

            else
            {
                Node temp = successor(n.right);
                n.val = temp.val;
                n.right = delete(n.right, n.val);
            }
        }

        if (n == null)//returns null if the inserted place is empty
            return n;

        updateHeight(n);
        // Balances the tree after deletion
        return balance(n);
    }
    Node successor(Node root)
    {
        if (root.left != null)
            return successor(root.left);

        else
            return root;
    }
    public ArrayList<Freelancer> findBest(int n,HashTable<Freelancer> blacklisted){
        ArrayList<Freelancer> best = new ArrayList<>();
        findBestHelper(root,n,best,blacklisted);
        return best;
    }
    //doesnt add to best if blacklisted
    private void findBestHelper(Node node,int n, ArrayList<Freelancer> best,HashTable<Freelancer> blacklisted){
        if (node==null || best.size()>=n){
            return;
        }
        findBestHelper(node.right,n,best,blacklisted);
        if (best.size()<n && !blacklisted.containsKey(node.val.id)){
            best.add(node.val);
        }
        findBestHelper(node.left,n,best,blacklisted);
    }
}

