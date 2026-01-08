
/**
 * CMPE 250 Project 1 - Nightpass Survivor Card Game
 *
 * This skeleton provides file I/O infrastructure. Implement your game logic
 * as you wish. There are some import that is suggested to use written below.
 * You can use them freely and create as manys classes as you want. However,
 * you cannot import any other java.util packages with data structures, you
 * need to implement them yourself. For other imports, ask through Moodle before
 * using.
 *
 * TESTING YOUR SOLUTION:
 * ======================
 *
 * Use the Python test runner for automated testing:
 *
 * python test_runner.py              # Test all cases
 * python test_runner.py --type type1 # Test only type1
 * python test_runner.py --type type2 # Test only type2
 * python test_runner.py --verbose    # Show detailed diffs
 * python test_runner.py --benchmark  # Performance testing (no comparison)
 *
 * Flags can be combined, e.g.:
 * python test_runner.py -bv              # benchmark + verbose
 * python test_runner.py -bv --type type1 # benchmark + verbose + type1
 * python test_runner.py -b --type type2  # benchmark + type2
 *
 * MANUAL TESTING (For Individual Runs):
 * ======================================
 *
 * 1. Compile: cd src/ && javac *.java
 * 2. Run: java Main ../testcase_inputs/test.txt ../output/test.txt
 * 3. Compare output with expected results
 *
 * PROJECT STRUCTURE:
 * ==================
 *
 * project_root/
 * ├── src/                     # Your Java files (Main.java, etc.)
 * ├── testcase_inputs/         # Input test files
 * ├── testcase_outputs/        # Expected output files
 * ├── output/                  # Generated outputs (auto-created)
 * └── test_runner.py           # Automated test runner
 *
 * REQUIREMENTS:
 * =============
 * - Java SDK 8+ (javac, java commands)
 * - Python 3.6+ (for test runner)
 *
 * @author Ahmet Mete Atay
 */


import java.io.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.math.*;

public class Main {

    // --- Game State ---
    private static long entryTimeCounter = 0;
    private static int survivorScore = 0;
    private static int strangerScore = 0;

    // --- Core Data Structure ---
    private static CardDatabase cardDatabase;


    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Main <input_file> <output_file>");
            return;
        }
        String inputFile = args[0];
        String outputFile = args[1];

        cardDatabase = new CardDatabase();

        Scanner scanner = null;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            try {
                scanner = new Scanner(new File(inputFile));

                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.isEmpty()) continue; // Skip empty lines

                    String[] tokens = line.split(" ");
                    String command = tokens[0];

                    switch (command) {
                        case "draw_card":
                            handleDrawCard(tokens, writer);
                            break;
                        case "battle":
                            handleBattle(tokens, writer);
                            break;
                        case "steal_card":
                            handleStealCard(tokens, writer);
                            break;
                        case "deck_count":
                            writer.write("Number of cards in the deck: " + cardDatabase.getDeckCount() + "\n");
                            break;
                        case "discard_pile_count":
                            writer.write("Number of cards in the discard pile: " + cardDatabase.getDiscardCount() + "\n");
                            break;
                        case "find_winning":
                            handleFindWinning(writer);
                            break;
                    }
                }
            } catch (IOException e) {
                // Handle file reading errors
                e.printStackTrace();
            } finally {
                // Ensure scanner is closed
                if (scanner != null) {
                    scanner.close();
                }
            }

        } catch (IOException e) {
            // Handle file writing errors
            e.printStackTrace();
        }
    }

    // --- Command Handlers ---

    private static void handleDrawCard(String[] tokens, BufferedWriter writer) throws IOException {
        String name = tokens[1];
        int attack = Integer.parseInt(tokens[2]);
        int health = Integer.parseInt(tokens[3]);

        Card newCard = new Card(name, attack, health, entryTimeCounter++);
        cardDatabase.insert(newCard);

        writer.write("Added " + name + " to the deck\n");
    }


    private static void handleBattle(String[] tokens, BufferedWriter writer) throws IOException {
        int strangerAttack = Integer.parseInt(tokens[1]);
        int strangerHealth = Integer.parseInt(tokens[2]);
        int healPool = Integer.parseInt(tokens[3]);

        CardDatabase.BattleResult result = cardDatabase.findAndRemoveBestCard(strangerAttack, strangerHealth);
        Card bestCard = result.card;
        int priority = result.priority;
        int cardsRevived = 0;

        if (bestCard == null) {
            strangerScore += 2;
            cardsRevived = handleHealingPhase(healPool);
            writer.write("No card to play, " + cardsRevived + " cards revived\n");
        } else {
            int strangerCardBaseHealth = strangerHealth;
            boolean cardDied = bestCard.takeDamage(strangerAttack);
            strangerHealth -= bestCard.A_cur_before_damage;

            if (cardDied) strangerScore += 2;
            if (strangerHealth <= 0) survivorScore += 2;
            if (!cardDied && bestCard.H_cur < bestCard.H_base) strangerScore += 1;
            if (strangerHealth > 0 && strangerHealth < strangerCardBaseHealth) survivorScore += 1;

            if (cardDied) {
                cardDatabase.addToDiscard(bestCard);
            }

            cardsRevived = handleHealingPhase(healPool);

            if (cardDied) {
                writer.write("Found with priority " + priority + ", Survivor plays " + bestCard.name +
                        ", the played card is discarded, " + cardsRevived + " cards revived\n");
            } else {
                bestCard.entryTime = entryTimeCounter++;
                cardDatabase.insert(bestCard);
                writer.write("Found with priority " + priority + ", Survivor plays " + bestCard.name +
                        ", the played card returned to deck, " + cardsRevived + " cards revived\n");
            }
        }
    }

    private static int handleHealingPhase(int healPool) {
        if (healPool == 0) return 0;
        int revivedCount = cardDatabase.healCards(healPool, entryTimeCounter);
        entryTimeCounter += revivedCount;
        return revivedCount;
    }

    private static void handleStealCard(String[] tokens, BufferedWriter writer) throws IOException {
        int attackLimit = Integer.parseInt(tokens[1]);
        int healthLimit = Integer.parseInt(tokens[2]);
        Card stolenCard = cardDatabase.findAndRemoveStealCard(attackLimit, healthLimit);
        if (stolenCard == null) {
            writer.write("No card to steal\n");
        } else {
            writer.write("The Stranger stole the card: " + stolenCard.name + "\n");
        }
    }

    private static void handleFindWinning(BufferedWriter writer) throws IOException {
        if (survivorScore >= strangerScore) { // Survivor wins ties
            writer.write("The Survivor, Score: " + survivorScore + "\n");
        } else {
            writer.write("The Stranger, Score: " + strangerScore + "\n");
        }
    }
}









