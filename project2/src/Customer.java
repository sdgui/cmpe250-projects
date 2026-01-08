import java.util.ArrayList;
//customer class
public class Customer {
    public String id;
    public int totalSpent;
    public String loyaltyTier;
    public HashTable<Freelancer> blacklisted;
    public int employCount;
    public int loyaltyPoints;
    Customer(String id) {
        this.id = id;
        totalSpent = 0;
        loyaltyTier = "BRONZE";
        blacklisted = new HashTable<>(11);
        employCount = 0;
        loyaltyPoints = 0;
    }
}
