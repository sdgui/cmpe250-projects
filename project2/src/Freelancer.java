//freelancer class
public class Freelancer {
    public String id;
    public int price;
    public double rating;
    public int ratingCount;
    public int completed;
    public int cancelled;
    public int compositeScore;
    public String service;
    public int[] skills;
    public boolean isWorking;
    public int completedLastMonth;
    public int cancelledLastMonth;
    public String currentCustomerID;
    public boolean isBurnout;
    Freelancer(String id,String service,int price,int T,int C,int R, int E, int A) {
        this.id = id;
        this.service = service;
        this.price = price;
        this.completed = 0;
        this.cancelled = 0;
        skills = new int[5];
        skills[0] = T;
        skills[1] = C;
        skills[2] = R;
        skills[3] = E;
        skills[4] = A;
        this.rating=5.0;
        this.ratingCount=1;
        completedLastMonth =0;
    }
    public void updateCompScore(){
        double reliabilityScore;
        if (completed+cancelled==0)
            reliabilityScore = 1.0;
        else
            reliabilityScore =  (double)completed/(completed+cancelled);
        double burnoutPenalty = isBurnout ? 0.45 : 0;
        double ratingScore = rating / 5.0;
        compositeScore=(int)Math.floor(10000*(0.55*skillScore()+0.25*ratingScore+0.20*reliabilityScore-burnoutPenalty));
    }

    //returns the skilscore based on the service
    private double skillScore() {


        switch (service) {
            case "paint":
                // (70, 60, 50, 85, 90)
                return  (skills[0]*70 + skills[1]*60 + skills[2]*50 + skills[3]*85 + skills[4]*90)/35500.0;

            case "web_dev":
                // (95, 75, 85, 80, 90)
                return  (skills[0]*95 + skills[1]*75 + skills[2]*85 + skills[3]*80 + skills[4]*90)/42500.0;

            case "graphic_design":
                // (75, 85, 95, 70, 85)
                return  (skills[0]*75 + skills[1]*85 + skills[2]*95 + skills[3]*70 + skills[4]*85)/41000.0;

            case "data_entry":
                // (50, 50, 30, 95, 95)
                return  (skills[0]*50 + skills[1]*50 + skills[2]*30 + skills[3]*95 + skills[4]*95)/32000.0 ;

            case "tutoring":
                // (80, 95, 70, 90, 75)
                return  (skills[0]*80 + skills[1]*95 + skills[2]*70 + skills[3]*90 + skills[4]*75)/41000.0;

            case "cleaning":
                // (40, 60, 40, 90, 85)
                return  (skills[0]*40 + skills[1]*60 + skills[2]*40 + skills[3]*90 + skills[4]*85)/31500.0;

            case "writing":
                // (70, 85, 90, 80, 95)
                return  (skills[0]*70 + skills[1]*85 + skills[2]*90 + skills[3]*80 + skills[4]*95)/42000.0;

            case "photography":
                // (85, 80, 90, 75, 90)
                return  (skills[0]*85 + skills[1]*80 + skills[2]*90 + skills[3]*75 + skills[4]*90)/42000.0;

            case "plumbing":
                // (85, 65, 60, 90, 85)
                return  (skills[0]*85 + skills[1]*65 + skills[2]*60 + skills[3]*90 + skills[4]*85)/38500.0;

            case "electrical":
                // (90, 65, 70, 95, 95)
                return  (skills[0]*90 + skills[1]*65 + skills[2]*70 + skills[3]*95 + skills[4]*95)/41500.0;

        }
        return 0;
    }
}
