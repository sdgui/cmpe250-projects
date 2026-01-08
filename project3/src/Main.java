import java.io.*;
import java.util.Locale;


public class Main {
    static Graph graph= new Graph();
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


        try {
            String result = "";
            switch (operation) {
                case "spawn_host":
                    if (!isAllowed(parts[1])) {
                        result = "Some error occurred in spawn_host.";
                        break;
                    }
                    int num=Integer.parseInt(parts[2]);
                    if(!graph.addNode(parts[1],num)){
                        result="Some error occurred in spawn_host.";
                    }
                    else{
                        result="Spawned host "+parts[1]+" with clearance level "+parts[2]+".";
                    }

                    break;
                case "link_backdoor":

                    if(!graph.addLink(parts[1],parts[2],Integer.parseInt(parts[3]),Integer.parseInt(parts[4]),Integer.parseInt(parts[5]))){
                        result="Some error occurred in link_backdoor.";
                    }
                    else{
                        result="Linked " +parts[1]+" <-> "+parts[2]+" with latency "+parts[3]+"ms, bandwidth "+parts[4]+"Mbps, firewall "+parts[5]+".";
                    }
                    break;
                case "seal_backdoor":
                    int sealResult= graph.sealNode(parts[1],parts[2]);
                    if(sealResult==0){
                        result="Some error occurred in seal_backdoor.";
                    }
                    else if(sealResult==1){
                        result="Backdoor "+parts[1]+" <-> "+parts[2]+" sealed.";
                    }
                    else{
                        result="Backdoor "+parts[1]+" <-> "+parts[2]+" unsealed.";
                    }
                    break;
                case "trace_route":
                    result= graph.traceRoute(parts[1],parts[2],Integer.parseInt(parts[3]),Integer.parseInt(parts[4]));
                    break;
                case "scan_connectivity":
                    result= graph.scanConnectivity();
                    break;
                case "simulate_breach":
                    if (parts.length==2)
                        result= graph.simulateHostBreach(parts[1]);
                    else
                        result= graph.simulateBackDoorBreach(parts[1],parts[2]);
                    break;
                case "oracle_report":
                    result= graph.oracleReport();
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
    private static boolean isAllowed(String str){
        return str.matches("[A-Z0-9_]+");
    }
}



