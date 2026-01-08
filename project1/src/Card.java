public class Card {
    String name;
    int A_init, H_init;
    int A_base, H_base;
    int A_cur, H_cur;
    long entryTime; // Used for tie-breaking

    int A_cur_before_damage; // Used for simultaneous damage

    // --- Type-2 Discard Pile Fields ---
    int revival_progress = 0;
    long discardTime = -1;

    public Card(String name, int attack, int health, long entryTime) {
        this.name = name;
        this.A_init = attack;
        this.H_init = health;
        this.A_base = attack;
        this.H_base = health;
        this.A_cur = attack;
        this.H_cur = health;
        this.entryTime = entryTime;
    }

    public boolean takeDamage(int damage) {
        this.A_cur_before_damage = this.A_cur;
        this.H_cur -= damage;

        if (this.H_cur <= 0) {
            this.H_cur = 0;
            return true; // Card is discarded
        }

        long product = (long) this.A_base * this.H_cur;

        if (this.H_base == 0) {
            this.A_cur = 1;
        } else {
            int new_A_cur = (int) (product / this.H_base); // Integer division floors
            this.A_cur = (int) Math.max(1, new_A_cur); // Clamp at 1
        }
        return false; // Card returned to deck
    }

    public int getHMissing() {
        return this.H_base - this.revival_progress;
    }

    public void applyFullRevive() {
        this.A_base = (int) Math.floor(this.A_base * 0.90);
        this.H_cur = this.H_base;
        this.A_cur = (int) Math.max(1, this.A_base);
        this.revival_progress = 0;
        this.discardTime = -1;
    }

    public void applyPartialRevive(int healAmount) {
        this.A_base = (int) Math.floor(this.A_base * 0.95);
        int healthNeeded = this.getHMissing();
        int healToApply = Math.min(healAmount, healthNeeded);
        this.revival_progress += healToApply;
    }
}


