import java.io.*;
import java.util.*;

public class FineTuneIfStmtVisitor {
   public static void main(String[] args) {
      String DIR = "/home/user1-selab3/Documents/research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/";
      String inputFilePath = DIR + "output/Train_methods_without_ForLoop.txt";
      String outputWithIfElseIf = DIR + "output/Train_methods_with_IfElseIf.txt"; 
      String outputWithoutIfElseIf = DIR + "output/Train_methods_without_IfElseIf.txt";
      
      findIfElseIfWithMatchingBraces(inputFilePath, outputWithIfElseIf, outputWithoutIfElseIf);
   }

   public static void findIfElseIfWithMatchingBraces(String inputFilePath, String outputWithIfElseIf, String outputWithoutIfElseIf) {
      try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
           BufferedWriter writerWithIfElseIf = new BufferedWriter(new FileWriter(outputWithIfElseIf));
           BufferedWriter writerWithoutIfElseIf = new BufferedWriter(new FileWriter(outputWithoutIfElseIf))) {

         String line;
         StringBuilder methodBuilder = new StringBuilder();

         while ((line = reader.readLine()) != null) {
            methodBuilder.append(line).append(" ");
            if (line.contains("}")) { // End of method detected
               String method = methodBuilder.toString();
               if (containsIfElseIfWithPRED(method)) {
                  writerWithIfElseIf.write(method.trim());
                  writerWithIfElseIf.newLine();
               } else {
                  writerWithoutIfElseIf.write(method.trim());
                  writerWithoutIfElseIf.newLine();
               }
               methodBuilder.setLength(0); // Reset for next method
            }
         }
         
         System.out.println("Methods with 'if' or 'else if' containing 'PRED' saved to " + outputWithIfElseIf);
         System.out.println("Methods without 'if' or 'else if' containing 'PRED' saved to " + outputWithoutIfElseIf);
         
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   // Check for `if (` or `else if (` with matching braces, ensuring "PRED" is inside the block.
   public static boolean containsIfElseIfWithPRED(String method) {
      int index = 0;
      while (index < method.length()) {
          // Look for `if (` or `else if (` or `else` constructs specifically
          if (method.startsWith("if (", index) || method.startsWith("else if (", index) || method.startsWith("else", index)) {
              int openBraceIndex = method.indexOf("{", index); // Find `{` that starts the block
              
              if (openBraceIndex != -1) {
                  int closeBraceIndex = findMatchingBrace(method, openBraceIndex); // Match closing `}`
                  
                  if (closeBraceIndex != -1) {
                      // Extract the block content between braces and check for `PRED`
                      String ifElseBlock = method.substring(openBraceIndex + 1, closeBraceIndex);
                      if (ifElseBlock.contains("PRED")) {
                          return true; // Return true if "PRED" is found within the if/else block
                      }
                      // Move the index to continue after the closing brace of this block
                      index = closeBraceIndex + 1;
                  } else {
                      // If no matching closing brace is found, break the loop to avoid an infinite loop
                      break;
                  }
              } else {
                  // If no opening brace is found, move past the current `if` or `else` and continue
                  index += method.startsWith("else if (", index) ? 8 : method.startsWith("if (", index) ? 3 : 4;
              }
          } else {
              index++;
          }
      }
      return false; // `PRED` not found within if/else blocks
  }
  

   // Finds the index of the matching closing brace for a given open brace
   public static int findMatchingBrace(String input, int openBraceIndex) {
      Stack<Integer> stack = new Stack<>();
      for (int i = openBraceIndex; i < input.length(); i++) {
         char ch = input.charAt(i);
         if (ch == '{') {
            stack.push(i);
         } else if (ch == '}') {
            stack.pop();
            if (stack.isEmpty()) {
               return i; // Found the matching closing brace
            }
         }
      }
      return -1; // No matching closing brace found
   }
}
