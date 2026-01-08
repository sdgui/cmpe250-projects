/**
 * class for adding freelancers, changed services and price to an arraylist for updating
 * at the end of the month
 */
public class Trio {
    public Freelancer freelancer;
    public String newService;
    public int price;
    Trio(Freelancer freelancer, String newService,int price){
        this.freelancer=freelancer;
        this.newService=newService;
        this.price=price;
    }
}
