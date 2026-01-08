/*
 * ======================================================================
 * --- COMPONENT: CardDatabase.java ---
 * (Uses augmented deckTree to skip impossible priority searches)
 * ======================================================================
 */
public class CardDatabase {

    // --- Helper class for passing search results by reference ---
    private static class SearchResult {
        Card bestCard = null;
    }

    // --- Inner Class: DiscardTree
    private class DiscardTree {
        private class Node {
            Card data;
            Node left, right;
            int height;
            Node(Card data) {
                this.data = data;
                this.height = 1;
            }
        }
        private Node root;
        private boolean sortAscending;
        private final SearchResult reusableSearchResult = new SearchResult();

        public DiscardTree(boolean sortAscending) {
            this.sortAscending = sortAscending;
        }
        private int getHeight(Node n) { return (n == null) ? 0 : n.height; }
        private void updateHeight(Node n) { if(n != null) n.height = 1 + Math.max(getHeight(n.left), getHeight(n.right)); }

        public int size() { return size(root); }
        private int size(Node n) { return (n == null) ? 0 : 1 + size(n.left) + size(n.right); }

        public void insert(Card card) { root = insert(root, card); }
        private Node insert(Node n, Card card) {
            if (n == null) return new Node(card);

            //  Inlined comparison logic based on sortAscending flag
            int h1 = card.getHMissing();
            int h2 = n.data.getHMissing();
            int cmp;
            if (sortAscending) { // HMissing ASC, discardTime ASC
                if (h1 < h2) cmp = -1;
                else if (h1 > h2) cmp = 1;
                else cmp = Long.compare(card.discardTime, n.data.discardTime);
            } else { // HMissing DESC, discardTime ASC
                if (h1 > h2) cmp = -1;
                else if (h1 < h2) cmp = 1;
                else cmp = Long.compare(card.discardTime, n.data.discardTime);
            }

            if (cmp < 0) n.left = insert(n.left, card);
            else if (cmp > 0) n.right = insert(n.right, card);
            else return n;
            updateHeight(n);
            return rebalance(n);
        }

        public void delete(Card card) {
            root = delete(root, card);
        }
        private Node delete(Node n, Card card) {
            if (n == null) return null;

            // Inlined comparison logic based on sortAscending flag
            int h1 = card.getHMissing();
            int h2 = n.data.getHMissing();
            int cmp;
            if (sortAscending) { // HMissing ASC, discardTime ASC
                if (h1 < h2) cmp = -1;
                else if (h1 > h2) cmp = 1;
                else cmp = Long.compare(card.discardTime, n.data.discardTime);
            } else { // HMissing DESC, discardTime ASC
                if (h1 > h2) cmp = -1;
                else if (h1 < h2) cmp = 1;
                else cmp = Long.compare(card.discardTime, n.data.discardTime);
            }

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
            updateHeight(n);
            return rebalance(n);
        }
        private Node findMinNode(Node n) { while (n.left != null) n = n.left; return n; }
        public Card findMin() { return (root == null) ? null : findMinNode(root).data; }

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
            updateHeight(y); updateHeight(x);
            return x;
        }
        private Node leftRotate(Node x) {
            Node y = x.right; Node T2 = y.left;
            y.left = x; x.right = T2;
            updateHeight(x); updateHeight(y);
            return y;
        }

        // --- Custom Search for Healing P1/P2 ---
        public Card findRevivable(int healPool) {
            reusableSearchResult.bestCard = null; // Reset
            findRevivableHelper(root, healPool, reusableSearchResult);
            return reusableSearchResult.bestCard;
        }
        private void findRevivableHelper(Node n, int healPool, SearchResult result) {
            if (n == null) return;
            // This search is only called on discardTreeDesc (HMissing DESC)
            // We want the largest HMissing <= healPool
            if (n.data.getHMissing() > healPool) {
                findRevivableHelper(n.right, healPool, result);
                return;
            }
            // n.data.getHMissing() <= healPool, so this is a candidate
            // Check left for a *better* candidate (larger HMissing)
            findRevivableHelper(n.left, healPool, result);
            if (result.bestCard != null) return; // Found a better one in left subtree
            result.bestCard = n.data; // This is the best one
        }
    }
    // --- End of DiscardTree Inner Class ---

    // --- The "Outer" Trees for the DECK ---
    private AttackTree deckTree; // Only one tree (A_cur ASC)

    // --- The Trees for the DISCARD PILE ---
    private DiscardTree discardTreeDesc; // P1/P2 Heal
    private DiscardTree discardTreeAsc;  // P3 Heal

    // --- Game State Counters ---
    private int deckCount = 0;
    private int discardCount = 0;
    private static long discardTimeCounter = 0;


    public CardDatabase() {
        // Only one deck tree
        this.deckTree = new AttackTree(); // MODIFIED

        // Discard trees
        this.discardTreeDesc = new DiscardTree(false);
        this.discardTreeAsc = new DiscardTree(true);
    }

    public int getDeckCount() { return this.deckCount; }
    public int getDiscardCount() { return this.discardCount; }

    /**
     * Inserts a card into the *deck*.
     */
    public void insert(Card card) {
        card.revival_progress = 0;
        card.discardTime = -1;
        deckTree.insert(card); // Only insert into one tree
        deckCount++;
    }

    /**
     * Removes a card from the *deck*.
     */
    public void remove(Card card) {
        deckTree.delete(card); // Only remove from one tree
        deckCount--;
    }

    /**
     * Adds a card to the *discard pile*.
     */
    public void addToDiscard(Card card) {
        card.H_cur = 0;
        card.revival_progress = 0;
        card.discardTime = discardTimeCounter++;
        discardTreeDesc.insert(card);
        discardTreeAsc.insert(card);
        discardCount++;
    }

    // --- Wrapper for BattleResult ---
    public static class BattleResult {
        Card card;
        int priority;
        public BattleResult(Card card, int priority) {
            this.card = card;
            this.priority = priority;
        }
    }

    /**
     * Finds and removes the best card from the *deck* for battle.
     */
    public BattleResult findAndRemoveBestCard(int strAtt, int strHp) {
        Card bestCard = null;
        int priority = 0;

        // --- Priority Skipping ---
        // Get the absolute best card in the deck in O(1) time
        int A_max_deck = deckTree.getDeckMaxAttack();
        int H_max_deck = deckTree.getDeckMaxHealth();

        if (A_max_deck < strHp && H_max_deck <= strAtt) {
            // BEST POSSIBLE card is P4. Skip P1, P2, P3.
            bestCard = deckTree.findBestP4_Reverse();
            if (bestCard != null) priority = 4;

        } else if (A_max_deck < strHp && H_max_deck > strAtt) {
            // BEST POSSIBLE card is P2. Skip P1.
            bestCard = deckTree.findBestP2_Reverse(strAtt, strHp);
            if (bestCard != null) {
                priority = 2;
            } else {
                // P2 failed, try P3/P4
                bestCard = deckTree.findBestP3(strAtt, strHp);
                if (bestCard != null) {
                    priority = 3;
                } else {
                    bestCard = deckTree.findBestP4_Reverse();
                    if (bestCard != null) priority = 4;
                }
            }

        } else if (A_max_deck >= strHp && H_max_deck <= strAtt) {
            // BEST POSSIBLE card is P3. Skip P1, P2.
            bestCard = deckTree.findBestP3(strAtt, strHp);
            if (bestCard != null) {
                priority = 3;
            } else {
                // P3 failed, try P4
                bestCard = deckTree.findBestP4_Reverse();
                if (bestCard != null) priority = 4;
            }

        } else {
            // A P1 card *might* exist (A_max >= strHp && H_max > strAtt).
            // We must run the full priority search.
            bestCard = deckTree.findBestP1(strAtt, strHp);
            if (bestCard != null) {
                priority = 1;
            } else {
                bestCard = deckTree.findBestP2_Reverse(strAtt, strHp);
                if (bestCard != null) {
                    priority = 2;
                } else {
                    bestCard = deckTree.findBestP3(strAtt, strHp);
                    if (bestCard != null) {
                        priority = 3;
                    } else {
                        bestCard = deckTree.findBestP4_Reverse();
                        if (bestCard != null) {
                            priority = 4;
                        }
                    }
                }
            }
        }
        // --- END Priority Skipping ---

        if (bestCard != null) {
            this.remove(bestCard);
        }
        return new BattleResult(bestCard, priority);
    }

    /**
     * Finds and removes a card from the *deck* to be stolen.
     */
    public Card findAndRemoveStealCard(int attLimit, int hpLimit) {
        // P1-style search (min A_cur): Use standard in-order traversal
        Card stolenCard = deckTree.findForSteal(attLimit, hpLimit);
        if (stolenCard != null) {
            this.remove(stolenCard);
        }
        return stolenCard;
    }

    /**
     * Implements the Type-2 Healing Phase.
     * @return The number of cards that were fully revived.
     */
    public int healCards(int healPool, long baseEntryTimeCounter) {
        if (healPool <= 0 || discardCount == 0) return 0;
        int cardsRevivedCount = 0;
        long currentEntryTime = baseEntryTimeCounter;

        while (healPool > 0 && discardCount > 0) {
            // Find largest HMissing <= healPool (from DESC tree)
            Card cardToRevive = discardTreeDesc.findRevivable(healPool);
            if (cardToRevive == null) break; // No card can be fully revived

            int cost = cardToRevive.getHMissing();
            // findRevivable already guarantees cost <= healPool, but we double check
            if(cost > healPool) break;

            healPool -= cost;
            cardsRevivedCount++;

            // Remove from *both* discard trees
            discardTreeDesc.delete(cardToRevive);
            discardTreeAsc.delete(cardToRevive);
            discardCount--;

            // Revive and add back to deck
            cardToRevive.applyFullRevive();
            cardToRevive.entryTime = currentEntryTime++;
            this.insert(cardToRevive);
        }

        // P3: Partial heal
        if (healPool > 0 && discardCount > 0) {
            // Find smallest HMissing (from ASC tree)
            Card cardToPartial = discardTreeAsc.findMin();
            if (cardToPartial != null) {
                // Remove from *both* discard trees
                discardTreeDesc.delete(cardToPartial);
                discardTreeAsc.delete(cardToPartial);

                // Apply partial heal
                cardToPartial.applyPartialRevive(healPool);
                cardToPartial.discardTime = discardTimeCounter++; // Update discard time

                // Re-insert into *both* discard trees with updated stats
                discardTreeDesc.insert(cardToPartial);
                discardTreeAsc.insert(cardToPartial);
            }
        }
        return cardsRevivedCount;
    }
}

