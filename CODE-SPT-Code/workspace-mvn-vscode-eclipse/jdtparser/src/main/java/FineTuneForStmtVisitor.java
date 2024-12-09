import java.io.*;
import java.util.*;

public class FineTuneForStmtVisitor {
   public static void main(String[] args) {
      // Define the input and output file paths
      String DIR = "/home/user1-selab3/Documents/research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/";
      String inputFilePath = DIR + "output/null_targettype_output_newTrain.txt";           // Input file containing methods
      String outputWithPRED = DIR + "output/Train_methods_with_ForLoop.txt"; // Output file for methods with "PRED" in for loops
      String outputWithoutPRED = DIR + "output/Train_methods_without_ForLoop.txt"; // Output file for methods without "PRED" in for loops
      findForLoopsWithPRED(inputFilePath, outputWithPRED, outputWithoutPRED);
   }

   public static void findForLoopsWithPRED(String inputFilePath, String outputWithPRED, String outputWithoutPRED) {
      try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
           BufferedWriter writerWithPRED = new BufferedWriter(new FileWriter(outputWithPRED));
           BufferedWriter writerWithoutPRED = new BufferedWriter(new FileWriter(outputWithoutPRED))) {
         
         String line;
         StringBuilder methodBuilder = new StringBuilder();

         // Read each line from the file
         while ((line = reader.readLine()) != null) {
            methodBuilder.append(line).append(" ");
            if (line.contains("}")) { // Assuming end of a method on line with '}'
               String method = methodBuilder.toString();
               if (containsForLoopWithPRED(method)) {
                  writerWithPRED.write(method.trim());
                  writerWithPRED.newLine();
               } else {
                  writerWithoutPRED.write(method.trim());
                  writerWithoutPRED.newLine();
               }
               methodBuilder.setLength(0); // Clear for the next method
            }
         }
         
         System.out.println("Methods with 'for' loops containing 'PRED' saved to " + outputWithPRED);
         System.out.println("Methods without 'for' loops containing 'PRED' saved to " + outputWithoutPRED);
         
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   // Check if the method contains a "for (" loop with "PRED" in the condition
   public static boolean containsForLoopWithPRED(String method) {
      int index = 0;
      while ((index = method.indexOf("for (", index)) != -1) {
         int openParenIndex = index + 4;
         int closeParenIndex = findClosingParenthesis(method, openParenIndex);

         if (closeParenIndex != -1) {
            String forLoopCondition = method.substring(openParenIndex + 1, closeParenIndex);
            if (forLoopCondition.contains("PRED")) {
               return true; // Found a for loop with PRED
            }
         }
         index = closeParenIndex + 1; // Move to the next part of the string
      }
      return false; // No 'for' loop with 'PRED' found
   }

   // Helper method to find the index of the matching closing parenthesis
   public static int findClosingParenthesis(String input, int openIndex) {
      Stack<Integer> stack = new Stack<>();
      for (int i = openIndex; i < input.length(); i++) {
         char ch = input.charAt(i);
         if (ch == '(') {
            stack.push(i);
         } else if (ch == ')') {
            stack.pop();
            if (stack.isEmpty()) {
               return i;
            }
         }
      }
      return -1; // No matching closing parenthesis found
   }
}
