import java.io.*;
import java.util.Locale;
import java.util.ArrayList;
/**
 * Main entry point for GigMatch Pro platform.
 */
public class Main {
    //creating all the hashtables and avltrees
    static HashTable<Customer> customerTable=new HashTable<Customer>();
    static HashTable<Freelancer> freeTable=new HashTable<Freelancer>();
    static ArrayList<Trio> serviceChanges=new ArrayList<>();
    static ArrayList<Freelancer> burnouts=new ArrayList<>();
    static AvlTree paint = new AvlTree();
    static AvlTree webdev = new AvlTree();
    static AvlTree graphic = new AvlTree();
    static AvlTree data = new AvlTree();
    static AvlTree tutor = new AvlTree();
    static AvlTree clean = new AvlTree();
    static AvlTree write = new AvlTree();
    static AvlTree photo = new AvlTree();
    static AvlTree plumb = new AvlTree();
    static AvlTree electrical = new AvlTree();

    public static void main(String[] args) {

        Locale.setDefault(Locale.US);
        if (args.length != 2) {
            System.err.println("Usage: java Main <input_file> <output_file>");
            System.exit(1);
        }

        String inputFile = args[0];
        String outputFile = args[1];

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                processCommand(line, writer);
            }

        } catch (IOException e) {
            System.err.println("Error reading/writing files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processCommand(String command, BufferedWriter writer)
            throws IOException {

        String[] parts = command.split("\\s+");
        String operation = parts[0];

        int T;
        int C;
        int R;
        int E;
        int A;
        try {
            String result = "";
            AvlTree tree;
            Customer customer;
            Freelancer freelancer;
            switch (operation) {
                case "register_customer":
                    // Format: register_customer customerID
                    customer = new Customer(parts[1]);
                    if (customerTable.containsKey(parts[1])||freeTable.containsKey(parts[1])) {
                        result="Some error occurred in register_customer.";
                    }
                    customerTable.put(parts[1],customer);
                    result="registered customer "+parts[1];
                    break;

                case "register_freelancer":
                    // Format: register_freelancer freelancerID serviceName basePrice T C R E A
                    T=Integer.parseInt(parts[4]);
                    C=Integer.parseInt(parts[5]);
                    R=Integer.parseInt(parts[6]);
                    E=Integer.parseInt(parts[7]);
                    A=Integer.parseInt(parts[8]);
                    if (T<0 || T>100 || C<0 || C>100 || R<0 || R>100 || E<0 || E>100 || A<0 || A>100) {
                        result = "Some error occurred in register_freelancer.";
                        break;
                    }
                    if (freeTable.containsKey(parts[1])||getServiceTree(parts[2])==null||customerTable.containsKey(parts[1])) {
                        result="Some error occurred in register_freelancer.";
                        break;
                    }
                    int price=Integer.parseInt(parts[3]);
                    if (price<=0) {
                        result="Some error occurred in register_freelancer.";
                        break;
                    }
                    freelancer=new Freelancer(parts[1],parts[2],price,T,C,R,E,A);
                    freelancer.updateCompScore();
                    freeTable.put(parts[1],freelancer);
                    //inserts freelancer into their respective tree
                    tree=getServiceTree(parts[2]);
                    tree.root=tree.insert(tree.root,freelancer);
                    result="registered freelancer "+parts[1];

                    break;

                case "request_job":
                    // Format: request_job customerID serviceName topK
                    if(!customerTable.containsKey(parts[1])||getServiceTree(parts[2])==null) {
                        result="Some error occurred in request job.";
                    }
                    customer=customerTable.get(parts[1]);
                    tree=getServiceTree(parts[2]);
                    if (tree.root==null) {
                        result="no freelancers available";
                        break;
                    }
                    //number of jobs to list
                    int n=Integer.parseInt(parts[3]);
                    //returns a list of size of n+blacklisted to have at least n non blacklisted elements everytime
                    ArrayList<Freelancer> lancers=tree.findBest(Integer.parseInt(parts[3])+customer.blacklisted.size,customer.blacklisted);

                    if (lancers.size()<n) {
                        if(lancers.isEmpty()){
                            result="no freelancers available";
                            break;
                        }
                        n= lancers.size();
                    }
                    result="available freelancers for " +parts[2]+" (top "+n+"):";
                    for (int i=0;i<n;i++) {
                        Freelancer lancer=lancers.get(i);
                        //formats the string and adds it to the output
                        String add="\n%s - composite: %d, price: %d, rating: %.1f";
                        add=String.format(add,lancer.id,lancer.compositeScore,lancer.price,lancer.rating);
                        result+=add;
                    }
                    Freelancer winner=lancers.get(0);
                    result+="\nauto-employed best freelancer: "+ winner.id+" for customer "+customer.id;
                    winner.isWorking=true;
                    winner.currentCustomerID=customer.id;
                    tree.root=tree.delete(tree.root,winner);
                    customer.employCount+=1;
                    break;

                case "employ_freelancer":
                    // Format: employ_freelancer customerID freelancerID
                    if (!customerTable.containsKey(parts[1])||!freeTable.containsKey(parts[2])) {
                        result="Some error occurred in employ.";
                        break;
                    }
                    freelancer=freeTable.get(parts[2]);
                    customer=customerTable.get(parts[1]);
                    if (customer.blacklisted.containsKey(freelancer.id)||freelancer.isWorking) {
                        result="Some error occurred in employ.";
                        break;
                    }
                    freelancer.isWorking=true;
                    freelancer.currentCustomerID=customer.id;
                    tree=getServiceTree(freelancer.service);
                    tree.root=tree.delete(tree.root,freelancer);
                    customer.employCount+=1;
                    result= customer.id+" employed "+ freelancer.id + " for "+ freelancer.service;
                    break;

                case "complete_and_rate":

                    // Format: complete_and_rate freelancerID rating
                    int rating=Integer.parseInt(parts[2]);
                    freelancer=freeTable.get(parts[1]);
                    if (rating<0 || rating>5 || !freelancer.isWorking) {
                        result="Some error occurred in complete and rate.";
                        break;
                    }
                    freelancer.isWorking=false;
                    freelancer.completedLastMonth +=1;
                    freelancer.completed+=1;
                    if (freelancer.completedLastMonth ==5 && !freelancer.isBurnout) {
                        burnouts.add(freelancer);
                    }
                    freelancer.rating=(freelancer.ratingCount* freelancer.rating+rating)/(freelancer.ratingCount+1);
                    freelancer.ratingCount+=1;

                    if (rating>=4){
                        updateFreelancerSkills(freelancer);
                    }
                    //reinsert freelancer back into their tree
                    freelancer.updateCompScore();
                    tree=getServiceTree(freelancer.service);
                    tree.root=tree.insert(tree.root,freelancer);
                    result= freelancer.id+" completed job for "+ freelancer.currentCustomerID +" with rating "+rating;
                    customer=customerTable.get(freelancer.currentCustomerID);
                    //adds to the total spent of the customer according to the loyalty tiers
                    switch (customer.loyaltyTier) {
                        case "BRONZE":
                            customer.totalSpent+=freelancer.price;
                            customer.loyaltyPoints+=freelancer.price;
                            break;
                        case "SILVER":
                            customer.totalSpent+=Math.floor(freelancer.price*0.95);
                            customer.loyaltyPoints+=Math.floor(freelancer.price*0.95);
                            break;
                        case "GOLD":
                            customer.totalSpent+=Math.floor(freelancer.price*0.90);
                            customer.loyaltyPoints+=Math.floor(freelancer.price*0.90);
                            break;
                        case "PLATINUM":
                            customer.totalSpent+=Math.floor(freelancer.price*0.85);
                            customer.loyaltyPoints+=Math.floor(freelancer.price*0.85);
                            break;
                    }


                    freelancer.currentCustomerID=null;

                    break;

                case "cancel_by_freelancer":
                    // Format: cancel_by_freelancer freelancerID
                    if (!freeTable.containsKey(parts[1])) {
                        result="Some error occurred in cancel_by_freelancer.";
                        break;
                    }
                    freelancer=freeTable.get(parts[1]);
                    if (!freelancer.isWorking) {
                        result="Some error occurred in cancel_by_freelancer.";
                        break;
                    }
                    freelancer.isWorking=false;
                    freelancer.cancelled+=1;
                    result="cancelled by freelancer: "+ freelancer.id+" cancelled "+freelancer.currentCustomerID;
                    freelancer.cancelledLastMonth+=1;
                    //blacklists the freelancer
                    if (freelancer.cancelledLastMonth>=5) {
                        result+="\nplatform banned freelancer: "+freelancer.id;
                        freeTable.remove(freelancer.id);
                        break;
                    }
                    //updates the rating of the freelancer
                    freelancer.rating=(freelancer.ratingCount* freelancer.rating)/(freelancer.ratingCount+1);
                    freelancer.ratingCount+=1;
                    skillDegradation(freelancer);
                    freelancer.updateCompScore();
                    //reinsert freelancer back into their tree
                    tree=getServiceTree(freelancer.service);
                    tree.root=tree.insert(tree.root,freelancer);
                    break;

                case "cancel_by_customer":
                    // Format: cancel_by_customer customerID freelancerID
                    if (!customerTable.containsKey(parts[1])||!freeTable.containsKey(parts[2])) {
                        result="Some error occurred in cancel_by_customer.";
                        break;
                    }
                    customer=customerTable.get(parts[1]);
                    freelancer=freeTable.get(parts[2]);
                    if (!freelancer.isWorking || !freelancer.currentCustomerID.equals(customer.id)) {
                        result="Some error occurred in cancel_by_customer.";
                        break;
                    }
                    freelancer.isWorking=false;
                    customer.loyaltyPoints-=250;
                    tree=getServiceTree(freelancer.service);
                    tree.root=tree.insert(tree.root,freelancer);
                    result="cancelled by customer: "+ customer.id+" cancelled "+freelancer.id;

                    break;

                case "blacklist":
                    // Format: blacklist customerID freelancerID
                    if (!customerTable.containsKey(parts[1])||!freeTable.containsKey(parts[2])) {
                        result="Some error occurred in blacklist.";
                        break;
                    }
                    customer=customerTable.get(parts[1]);
                    freelancer=freeTable.get(parts[2]);
                    if (customer.blacklisted.containsKey(freelancer.id)) {
                        result="Some error occurred in blacklist.";
                        break;
                    }
                    customer.blacklisted.put(freelancer.id,freelancer);
                    result=customer.id+" blacklisted "+freelancer.id;
                    break;

                case "unblacklist":
                    // Format: unblacklist customerID freelancerID
                    if (!customerTable.containsKey(parts[1])||!freeTable.containsKey(parts[2])) {
                        result="Some error occurred in unblacklist.";
                        break;
                    }
                    customer=customerTable.get(parts[1]);
                    freelancer=freeTable.get(parts[2]);
                    if (!customer.blacklisted.containsKey(freelancer.id)) {
                        result="Some error occurred in unblacklist.";
                        break;
                    }
                    customer.blacklisted.remove(freelancer.id);
                    result=customer.id+" unblacklisted "+freelancer.id;
                    break;

                case "change_service":
                    // Format: change_service freelancerID newService newPrice
                    if (!freeTable.containsKey(parts[1])||getServiceTree(parts[2])==null) {
                        result="Some error occurred in change_service.";
                        break;
                    }
                    freelancer=freeTable.get(parts[1]);
                    if (freelancer.service==parts[2]) {
                        result="Some error occurred in change_service.";
                        break;
                    }

                    Trio change=new Trio(freelancer,parts[2],Integer.parseInt(parts[3]));
                    serviceChanges.add(change);
                    result="service change for "+freelancer.id+" queued from "+freelancer.service+" to "+ parts[2];
                    break;

                case "simulate_month":
                    // Format: simulate_month
                    //resets freelancer works last month
                    for (Freelancer f: freeTable.values()) {
                        f.cancelledLastMonth=0;
                        //removes burnout if applicable
                        if (f.isBurnout && f.completedLastMonth <= 2) {
                            f.isBurnout = false;
                            f.updateCompScore();
                            tree=getServiceTree(f.service);
                            tree.root=tree.delete(tree.root,f);
                            tree.root=tree.insert(tree.root,f);
                        }
                        f.completedLastMonth =0;
                    }
                    //applies burnouts
                    for (Freelancer f: burnouts) {
                        f.isBurnout=true;
                        tree=getServiceTree(f.service);
                        tree.root=tree.delete(tree.root,f);
                        f.updateCompScore();
                        tree.root=tree.insert(tree.root,f);
                    }
                    //does the service changes at the end of the month
                    for (Trio trio: serviceChanges) {
                        Freelancer f=trio.freelancer;
                        //remove from old tree
                        tree=getServiceTree(f.service);
                        tree.root=tree.delete(tree.root,f);
                        //update service and price
                        f.service=trio.newService;
                        f.price=trio.price;
                        f.updateCompScore();
                        //insert into new tree
                        tree=getServiceTree(f.service);
                        tree.root=tree.insert(tree.root,f);
                    }
                    //updates loyalty tiers for all customers
                    for (Customer c: customerTable.values()) {
                        updateLoyaltyTier(c);
                    }
                    serviceChanges.clear();
                    burnouts.clear();
                    result="month complete";
                    break;

                case "query_freelancer":
                    // Format: query_freelancer freelancerID
                    if (!freeTable.containsKey(parts[1])) {
                        result="Some error occurred in query_freelancer.";
                        break;
                    }
                    freelancer=freeTable.get(parts[1]);
                    result="%s: %s, price: %d, rating: %.1f, completed: %d, cancelled: " +
                            "%d, skills: (%d,%d,%d,%d,%d), available: %s, burnout: %s";
                    String avail= (freelancer.isWorking)? "no":"yes";
                    String burnout= (freelancer.isBurnout)? "yes":"no";
                    result=String.format(result,freelancer.id,freelancer.service,freelancer.price,freelancer.rating,freelancer.completed,
                            freelancer.cancelled,freelancer.skills[0],freelancer.skills[1],freelancer.skills[2],
                            freelancer.skills[3],freelancer.skills[4],avail,burnout);
                    break;

                case "query_customer":
                    // Format: query_customer customerID
                    if(!customerTable.containsKey(parts[1])) {
                        result="Some error occurred in query_customer.";
                        break;
                    }
                    customer=customerTable.get(parts[1]);
                    result="%s: total spent: $%d, loyalty tier: %s, blacklisted freelancer count: %d, total employment count: %d";
                    result=String.format(result,customer.id,customer.totalSpent,customer.loyaltyTier,customer.blacklisted.size,customer.employCount);
                    break;

                case "update_skill":
                    // Format: update_skill freelancerID T C R E A
                    if (!freeTable.containsKey(parts[1])) {
                        result="Some error occurred in update_skill.";
                        break;
                    }
                    T=Integer.parseInt(parts[2]);
                    C=Integer.parseInt(parts[3]);
                    R=Integer.parseInt(parts[4]);
                    E=Integer.parseInt(parts[5]);
                    A=Integer.parseInt(parts[6]);
                    if (T<0 || T>100 || C<0 || C>100 || R<0 || R>100 || E<0 || E>100 || A<0 || A>100) {
                        result = "Some error occurred in update_skill.";
                        break;
                    }
                    freelancer=freeTable.get(parts[1]);
                    //remove from tree
                    tree=getServiceTree(freelancer.service);
                    tree.root=tree.delete(tree.root,freelancer);
                    //update skills
                    freelancer.skills[0]=T;
                    freelancer.skills[1]=C;
                    freelancer.skills[2]=R;
                    freelancer.skills[3]=E;
                    freelancer.skills[4]=A;
                    freelancer.updateCompScore();
                    //reinsert into tree
                    tree.root=tree.insert(tree.root,freelancer);
                    result="updated skills of "+freelancer.id+" for "+ freelancer.service;
                    break;

                default:
                    result = "Unknown command: " + operation;
            }

            writer.write(result);
            writer.newLine();

        } catch (Exception e) {
            System.out.println(e.getMessage());
            writer.write("Error processing command: " + command);
            writer.newLine();
        }
    }

    /**
     * Updates a freelancer's skills after a job is completed with a high rating.
     * Implements the "Skill Gains Following Job Completion" logic (PDF Section 4.1).
     *
     * @param freelancer The Freelancer object whose skills will be updated.
     */
    static void updateFreelancerSkills(Freelancer freelancer) {



        // Get the current skills
        // This assumes your Freelancer object stores skills as an array [T, C, R, E, A]
        // If you have fields like freelancer.skillT, adjust as needed.
        int t = freelancer.skills[0];
        int c = freelancer.skills[1];
        int r = freelancer.skills[2];
        int e = freelancer.skills[3];
        int a = freelancer.skills[4];

        // Use a switch on the freelancer's service to apply gains
        switch (freelancer.service) {
            case "paint":
                // Primary: A=90 (+2), Secondaries: E=85 (+1), T=70 (+1)
                t += 1;
                e += 1;
                a += 2;
                break;

            case "web_dev":
                // Primary: T=95 (+2), Secondaries: A=90 (+1), R=85 (+1)
                t += 2;
                r += 1;
                a += 1;
                break;

            case "graphic_design":
                // Primary: R=95 (+2), Secondaries: C=85 (+1), A=85 (+1)
                c += 1;
                r += 2;
                a += 1;
                break;

            case "data_entry":
                // Primary: E=95 (+2), Secondaries: A=95 (+1), T=50 (+1)
                // Tie-breaker: E, A > T
                t += 1;
                e += 2;
                a += 1;
                break;

            case "tutoring":
                // Primary: C=95 (+2), Secondaries: E=90 (+1), T=80 (+1)
                t += 1;
                c += 2;
                e += 1;
                break;

            case "cleaning":
                // Primary: E=90 (+2), Secondaries: A=85 (+1), C=60 (+1)
                c += 1;
                e += 2;
                a += 1;
                break;

            case "writing":
                // Primary: A=95 (+2), Secondaries: R=90 (+1), C=85 (+1)
                c += 1;
                r += 1;
                a += 2;
                break;

            case "photography":
                // Primary: R=90 (+2), Secondaries: A=90 (+1), T=85 (+1)
                // Tie-breaker: R, A > T
                t += 1;
                r += 2;
                a += 1;
                break;

            case "plumbing":
                // Primary: E=90 (+2), Secondaries: T=85 (+1), A=85 (+1)
                // Tie-breaker: T, A
                t += 1;
                e += 2;
                a += 1;
                break;

            case "electrical":
                // Primary: E=95 (+2), Secondaries: A=95 (+1), T=90 (+1)
                // Tie-breaker: E, A > T
                t += 1;
                e += 2;
                a += 1;
                break;
        }

        // Rule: Upper limit. No skill can exceed 100 points.
        // We apply this *after* the gains are added.
        freelancer.skills[0] = Math.min(100, t);
        freelancer.skills[1] = Math.min(100, c);
        freelancer.skills[2] = Math.min(100, r);
        freelancer.skills[3] = Math.min(100, e);
        freelancer.skills[4] = Math.min(100, a);
    }

    /**
     * Degrades a freelancer's skills after a job cancellation.
     * @param freelancer The Freelancer object whose skills will be degraded.
     */
    static void skillDegradation(Freelancer freelancer) {
        for (int i = 0; i < freelancer.skills.length; i++) {
            freelancer.skills[i] = Math.max(0, freelancer.skills[i] - 3);
        }
    }
    /*method for updating each customer's loyalty tier
    called at the end of each month
     */
    static void updateLoyaltyTier(Customer customer) {
        int points = customer.loyaltyPoints;

        if (points<500) {
            customer.loyaltyTier = "BRONZE";
        } else if (points < 2000) {
            customer.loyaltyTier = "SILVER";
        } else if (points < 5000) {
            customer.loyaltyTier = "GOLD";
        } else {
            customer.loyaltyTier = "PLATINUM";
        }
    }
    static AvlTree getServiceTree(String serviceName) {
        switch (serviceName) {
            case "paint":
                return paint;
            case "web_dev":
                return webdev;
            case "graphic_design":
                return graphic;
            case "data_entry":
                return data;
            case "tutoring":
                return tutor;
            case "cleaning":
                return clean;
            case "writing":
                return write;
            case "photography":
                return photo;
            case "plumbing":
                return plumb;
            case "electrical":
                return electrical;
            default:
                return null;

        }
    }
}