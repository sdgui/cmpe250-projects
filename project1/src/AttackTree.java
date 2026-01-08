/*
 * ======================================================================
 * --- COMPONENT: AttackTree.java ---
 * (This is a fully augmented AVL Tree)
 * (Each node stores MAX A, MAX H, AND MIN H of its entire subtree)
 * ======================================================================
 */
public class AttackTree {

    // --- Helper class for passing search results by reference ---
    private static class SearchResult {
        Card bestCard = null;
    }

    // --- Reusable search object ---
    private final SearchResult reusableSearchResult = new SearchResult();

    // ---
    // --- Inner "HealthTree" class (Also Fully Augmented) ---
    // ---
    private class HealthTree {
        private class Node {
            Card data;
            Node left, right;
            int height, size;
            int H_max_subtree; //Max H_cur in this subtree
            int H_min_subtree; // Min H_cur in this subtree

            Node(Card data) {
                this.data = data;
                this.height = 1;
                this.size = 1;
                this.H_max_subtree = data.H_cur; // Base case
                this.H_min_subtree = data.H_cur; // Base case
            }
        }
        private Node root;

        public HealthTree() {}
        public boolean isEmpty() { return root == null; }
        public int size() { return (root == null) ? 0 : root.size; }
        private int getHeight(Node n) { return (n == null) ? 0 : n.height; }
        private int getSize(Node n) { return (n == null) ? 0 : n.size; }

        // --- Helpers for H_max and H_min ---
        private int getMaxH(Node n) {
            return (n == null) ? 0 : n.H_max_subtree;
        }
        private int getMinH(Node n) {
            // Use Integer.MAX_VALUE for null so min() functions work correctly
            return (n == null) ? Integer.MAX_VALUE : n.H_min_subtree;
        }

        // --- update() now maintains H_max and H_min ---
        private void update(Node n) {
            if (n == null) return;
            n.height = 1 + Math.max(getHeight(n.left), getHeight(n.right));
            n.size = 1 + getSize(n.left) + getSize(n.right);

            n.H_max_subtree = Math.max(n.data.H_cur,
                    Math.max(getMaxH(n.left), getMaxH(n.right)));

            n.H_min_subtree = Math.min(n.data.H_cur,
                    Math.min(getMinH(n.left), getMinH(n.right)));
        }

        // --- Public getters for augmented data ---
        public int getSubtreeMaxHealth() {
            return (root == null) ? 0 : root.H_max_subtree;
        }
        public int getSubtreeMinHealth() {
            return (root == null) ? Integer.MAX_VALUE : root.H_min_subtree;
        }

        public Card findMin() {
            if (root == null) return null;
            Node n = root;
            while (n.left != null) n = n.left;
            return n.data;
        }

        public void insert(Card card) { root = insert(root, card); }
        private Node insert(Node n, Card card) {
            if (n == null) return new Node(card);
            int cmp;
            if (card.H_cur < n.data.H_cur) cmp = -1;
            else if (card.H_cur > n.data.H_cur) cmp = 1;
            else cmp = Long.compare(card.entryTime, n.data.entryTime);

            if (cmp < 0) n.left = insert(n.left, card);
            else if (cmp > 0) n.right = insert(n.right, card);
            else return n; // Duplicate
            update(n); // This now updates height, size, H_max, AND H_min
            return rebalance(n);
        }

        public void delete(Card card) {
            root = delete(root, card);
        }

        private Node delete(Node n, Card card) {
            if (n == null) return null;

            int cmp;
            if (card.H_cur < n.data.H_cur) cmp = -1;
            else if (card.H_cur > n.data.H_cur) cmp = 1;
            else cmp = Long.compare(card.entryTime, n.data.entryTime);

            if (cmp < 0) n.left = delete(n.left, card);
            else if (cmp > 0) n.right = delete(n.right, card);
            else {
                if (n.left == null) return n.right;
                else if (n.right == null) return n.left;
                else {
                    Node successor = findMinNode(n.right);
                    n.data = successor.data;
                    n.right = delete(n.right, successor.data);
                }
            }
            if (n == null) return null;
            update(n); // This now updates height, size, H_max, AND H_min
            return rebalance(n);
        }
        private Node findMinNode(Node n) {
            while (n.left != null) n = n.left;
            return n;
        }

        // Rebalance logic is unchanged, but it calls the new augmented update()
        private Node rebalance(Node n) {
            int balance = getBalance(n);
            if (balance > 1) {
                if (getBalance(n.left) < 0) n.left = leftRotate(n.left);
                return rightRotate(n);
            }
            if (balance < -1) {
                if (getBalance(n.right) > 0) n.right = rightRotate(n.right);
                return leftRotate(n);
            }
            return n;
        }
        private int getBalance(Node n) { return (n == null) ? 0 : getHeight(n.left) - getHeight(n.right); }

        private Node rightRotate(Node y) {
            Node x = y.left; Node T2 = x.right;
            x.right = y; y.left = T2;
            update(y); update(x); // These updates propagate H_max/H_min
            return x;
        }
        private Node leftRotate(Node x) {
            Node y = x.right; Node T2 = y.left;
            y.left = x; x.right = T2;
            update(x); update(y); // These updates propagate H_max/H_min
            return y;
        }



        public Card findMinHealth(int hpLimit) { // min H_cur > hpLimit
            return findMinHealth(root, hpLimit);
        }
        private Card findMinHealth(Node n, int hpLimit) {
            if (n == null) return null;
            if (n.H_max_subtree <= hpLimit) return null; // Pruning

            if (n.data.H_cur <= hpLimit) {
                return findMinHealth(n.right, hpLimit);
            } else {
                Card fromLeft = findMinHealth(n.left, hpLimit);
                return (fromLeft != null) ? fromLeft : n.data;
            }
        }

        public Card findMinHealthMax(int hpLimit) { // min H_cur <= hpLimit
            return findMinHealthMax(root, hpLimit);
        }
        private Card findMinHealthMax(Node n, int hpLimit) {
            if (n == null) return null;
            // If the *minimum* health in this subtree is already > hpLimit,
            // then no node can satisfy H_cur <= hpLimit. Prune this branch.
            if (n.H_min_subtree > hpLimit) return null;

            if (n.data.H_cur > hpLimit) {
                return findMinHealthMax(n.left, hpLimit);
            } else {
                Card fromLeft = findMinHealthMax(n.left, hpLimit);
                return (fromLeft != null) ? fromLeft : n.data;
            }
        }
    }
    // --- End of Inner HealthTree class ---


    // ---
    // --- Outer "AttackTree" Fields and Methods (Also Fully Augmented) ---
    // ---
    private class AttackNode {
        int attackKey; // A_cur
        HealthTree healthTree; // Inner tree
        AttackNode left, right;
        int height;
        int A_max_subtree; // Max A_cur in this subtree
        int H_max_subtree; //  Max H_cur in this subtree
        int H_min_subtree; // Min H_cur in this subtree

        AttackNode(Card card) {
            this.attackKey = card.A_cur;
            this.healthTree = new HealthTree();
            this.healthTree.insert(card);
            this.height = 1;
            this.A_max_subtree = card.A_cur;
            this.H_max_subtree = card.H_cur;
            this.H_min_subtree = card.H_cur;
        }
    }

    private AttackNode root;


    // --- Getters for max/min values (used by CardDatabase) ---
    public int getDeckMaxAttack() {
        return (root == null) ? 0 : root.A_max_subtree;
    }
    public int getDeckMaxHealth() {
        return (root == null) ? 0 : root.H_max_subtree;
    }

    // --- Helpers for augmented values ---
    private int getMaxA(AttackNode n) {
        return (n == null) ? 0 : n.A_max_subtree;
    }
    private int getMaxH(AttackNode n) {
        return (n == null) ? 0 : n.H_max_subtree;
    }
    private int getMinH(AttackNode n) {
        return (n == null) ? Integer.MAX_VALUE : n.H_min_subtree;
    }

    // --- update() now maintains all augmented data ---
    private void update(AttackNode n) {
        if (n == null) return;
        n.height = 1 + Math.max(getHeight(n.left), getHeight(n.right));

        n.A_max_subtree = Math.max(n.attackKey,
                Math.max(getMaxA(n.left), getMaxA(n.right)));

        n.H_max_subtree = Math.max(n.healthTree.getSubtreeMaxHealth(),
                Math.max(getMaxH(n.left), getMaxH(n.right)));

        n.H_min_subtree = Math.min(n.healthTree.getSubtreeMinHealth(),
                Math.min(getMinH(n.left), getMinH(n.right)));
    }


    // --- Public Insert/Delete (Now call augmented update) ---

    public void insert(Card card) {
        root = insert(root, card);
    }
    private AttackNode insert(AttackNode n, Card card) {
        if (n == null) return new AttackNode(card);

        int cmp = Integer.compare(card.A_cur, n.attackKey);

        if (cmp < 0) {
            n.left = insert(n.left, card);
        } else if (cmp > 0) {
            n.right = insert(n.right, card);
        } else {
            n.healthTree.insert(card);
            update(n); // Must update H_max/H_min even if no outer rebalance
            return n;
        }
        update(n); // Updates height, A_max, H_max, H_min
        return rebalance(n);
    }

    public void delete(Card card) {
        root = delete(root, card);
    }
    private AttackNode delete(AttackNode n, Card card) {
        if (n == null) return null;

        int cmp = Integer.compare(card.A_cur, n.attackKey);


        if (cmp < 0) {
            n.left = delete(n.left, card);
        } else if (cmp > 0) {
            n.right = delete(n.right, card);
        } else {
            n.healthTree.delete(card);
            if (n.healthTree.isEmpty()) {
                return deleteOuterNode(n, card.A_cur);
            }
        }
        if (n == null) return null;
        update(n); // Updates height, A_max, H_max, H_min
        return rebalance(n);
    }

    private AttackNode deleteOuterNode(AttackNode n, int attackKey) {
        if (n == null) return null;


        int cmp = Integer.compare(attackKey, n.attackKey);


        if (cmp < 0) n.left = deleteOuterNode(n.left, attackKey);
        else if (cmp > 0) n.right = deleteOuterNode(n.right, attackKey);
        else {
            if (n.left == null) return n.right;
            else if (n.right == null) return n.left;
            else {
                AttackNode successor = findMinNode(n.right);
                n.attackKey = successor.attackKey;
                n.healthTree = successor.healthTree;
                n.right = deleteOuterNode(n.right, successor.attackKey);
            }
        }
        if (n == null) return null;
        update(n); // Updates height, A_max, H_max, H_min
        return rebalance(n);
    }
    private AttackNode findMinNode(AttackNode n) {
        while (n.left != null) n = n.left;
        return n;
    }

    // --- Outer Tree Rebalancing (Calls augmented update) ---
    private int getHeight(AttackNode n) { return (n == null) ? 0 : n.height; }
    private int getBalance(AttackNode n) { return (n == null) ? 0 : getHeight(n.left) - getHeight(n.right); }

    private AttackNode rebalance(AttackNode n) {
        int balance = getBalance(n);
        if (balance > 1) {
            if (getBalance(n.left) < 0) n.left = leftRotate(n.left);
            return rightRotate(n);
        }
        if (balance < -1) {
            if (getBalance(n.right) > 0) n.right = rightRotate(n.right);
            return leftRotate(n);
        }
        return n;
    }
    private AttackNode rightRotate(AttackNode y) {
        AttackNode x = y.left; AttackNode T2 = x.right;
        x.right = y; y.left = T2;
        update(y); // Update y *first* (it's the child)
        update(x); // Update x *second* (it's the new root)
        return x;
    }
    private AttackNode leftRotate(AttackNode x) {
        AttackNode y = x.right; AttackNode T2 = y.left;
        y.left = x; x.right = T2;
        update(x); // Update x *first* (it's the child)
        update(y); // Update y *second* (it's the new root)
        return y;
    }


    // ---
    // --- Search Methods (with H_min pruning) ---
    // ---

    // P1: min A_cur >= strHp, min H_cur > strAtt
    public Card findBestP1(int strAtt, int strHp) {
        reusableSearchResult.bestCard = null; // Reset
        findBestP1Helper(root, strAtt, strHp, reusableSearchResult);
        return reusableSearchResult.bestCard;
    }
    private void findBestP1Helper(AttackNode n, int strAtt, int strHp, SearchResult result) {
        if (n == null) return;

        // --- PRUNING ---
        if (n.A_max_subtree < strHp) return;
        if (n.H_max_subtree <= strAtt) return;
        // (H_min pruning doesn't help P1)


        int cmp = Integer.compare(n.attackKey, strHp); // (A_cur vs strHp)

        if (cmp < 0) { // A_cur < strHp (invalid)
            findBestP1Helper(n.right, strAtt, strHp, result);
        } else { // A_cur >= strHp (valid)
            findBestP1Helper(n.left, strAtt, strHp, result); // Check left first (min A_cur)
            if (result.bestCard != null) return;

            if (n.healthTree.getSubtreeMaxHealth() > strAtt) { // Inner prune
                Card fromThis = n.healthTree.findMinHealth(strAtt);
                if (fromThis != null) {
                    result.bestCard = fromThis;
                    return;
                }
            }
            findBestP1Helper(n.right, strAtt, strHp, result);
        }
    }


    // P2: max A_cur < strHp, min H_cur > strAtt
    public Card findBestP2_Reverse(int strAtt, int strHp) {
        reusableSearchResult.bestCard = null; // Reset
        findBestP2Helper_Reverse(root, strAtt, strHp, reusableSearchResult);
        return reusableSearchResult.bestCard;
    }
    private void findBestP2Helper_Reverse(AttackNode n, int strAtt, int strHp, SearchResult result) {
        if (n == null) return;


        if (n.H_max_subtree <= strAtt) return;
        // (H_min pruning doesn't help P2)

        int cmp = Integer.compare(n.attackKey, strHp); // (A_cur vs strHp)


        if (cmp >= 0) { // A_cur >= strHp (invalid)
            findBestP2Helper_Reverse(n.left, strAtt, strHp, result); // Prune right
        } else { // A_cur < strHp (valid)
            findBestP2Helper_Reverse(n.right, strAtt, strHp, result); // Check right first (max A_cur)
            if (result.bestCard != null) return;

            if (n.healthTree.getSubtreeMaxHealth() > strAtt) { // Inner prune
                Card fromThis = n.healthTree.findMinHealth(strAtt);
                if (fromThis != null) {
                    result.bestCard = fromThis;
                    return;
                }
            }
            findBestP2Helper_Reverse(n.left, strAtt, strHp, result);
        }
    }

    // P3: min A_cur >= strHp, min H_cur <= strAtt
    public Card findBestP3(int strAtt, int strHp) {
        reusableSearchResult.bestCard = null; // Reset
        findBestP3Helper(root, strAtt, strHp, reusableSearchResult);
        return reusableSearchResult.bestCard;
    }
    private void findBestP3Helper(AttackNode n, int strAtt, int strHp, SearchResult result) {
        if (n == null) return;

        // --- H_MIN PRUNING ---
        if (n.A_max_subtree < strHp) return;
        // If the *minimum* health in this subtree is already > strAtt,

        if (n.H_min_subtree > strAtt) return;

        // MODIFIED: Inlined comparison
        int cmp = Integer.compare(n.attackKey, strHp); // (A_cur vs strHp)


        if (cmp < 0) { // A_cur < strHp (invalid)
            findBestP3Helper(n.right, strAtt, strHp, result);
        } else { // A_cur >= strHp (valid)
            findBestP3Helper(n.left, strAtt, strHp, result); // Check left first (min A_cur)
            if (result.bestCard != null) return;

            if (n.healthTree.getSubtreeMinHealth() <= strAtt) { // Inner prune
                Card fromThis = n.healthTree.findMinHealthMax(strAtt); // min H_cur <= strAtt
                if (fromThis != null) {
                    result.bestCard = fromThis;
                    return;
                }
            }
            findBestP3Helper(n.right, strAtt, strHp, result);
        }
    }


    // P4: max A_cur, min H_cur
    public Card findBestP4_Reverse() {
        if (root == null) return null;
        AttackNode n = root;
        while (n.right != null) n = n.right; // Go to max A_cur (O(log N_attack))
        return n.healthTree.findMin(); // Get min H_cur (O(log N_health))
    }

    // Steal: min A_cur > attLimit, min H_cur > hpLimit
    public Card findForSteal(int attLimit, int hpLimit) {
        reusableSearchResult.bestCard = null; // Reset
        findForStealHelper(root, attLimit, hpLimit, reusableSearchResult);
        return reusableSearchResult.bestCard;
    }
    private void findForStealHelper(AttackNode n, int attLimit, int hpLimit, SearchResult result) {
        if (n == null) return;

        if (n.A_max_subtree <= attLimit) return;
        if (n.H_max_subtree <= hpLimit) return;

        int cmp = Integer.compare(n.attackKey, attLimit); // (A_cur vs attLimit)

        if (cmp <= 0) { // A_cur <= attLimit (invalid)
            findForStealHelper(n.right, attLimit, hpLimit, result);
        } else { // A_cur > attLimit (valid)
            findForStealHelper(n.left, attLimit, hpLimit, result); // Check left first (min A_cur)
            if (result.bestCard != null) return;

            if (n.healthTree.getSubtreeMaxHealth() > hpLimit) { // Inner prune
                Card fromThis = n.healthTree.findMinHealth(hpLimit); // min H_cur > hpLimit
                if (fromThis != null) {
                    result.bestCard = fromThis;
                    return;
                }
            }
            findForStealHelper(n.right, attLimit, hpLimit, result);
        }
    }
}
