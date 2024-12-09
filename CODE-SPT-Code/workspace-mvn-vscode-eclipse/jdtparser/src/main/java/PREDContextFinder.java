import java.io.*;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PREDContextFinder {
    public static void main(String[] args) {
        String DIR = "/home/user1-selab3/Documents/research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/";
        String inputFilePath = DIR + "output/Valid_methods_without_ForLoop.txt";
        String outputFilePath = DIR + "output/Valid_PRED_contexts.txt";

        findPREDContexts(inputFilePath, outputFilePath);
    }

    public static void findPREDContexts(String inputFilePath, String outputFilePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {

            String line;
            StringBuilder methodBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                methodBuilder.append(line).append(" ");
                if (line.contains("}")) { // End of a method detected
                    String method = methodBuilder.toString();
                    analyzePREDContexts(method, writer);
                    methodBuilder.setLength(0); // Reset for the next method
                }
            }

            System.out.println("PRED contexts saved to " + outputFilePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    public static void analyzePREDContexts(String method, BufferedWriter writer) throws IOException {
        Stack<String> contextStack = new Stack<>();
        int index = 0;
        boolean foundPRED = false;
    
        Pattern predPattern = Pattern.compile(";\\s*PRED\\s*;");
    
        while (index < method.length()) {
            if (method.startsWith("if (", index) || method.startsWith("else if (", index)) {
                int endIndex = findStatementEnd(method, index);
                contextStack.push("if/else block");
                index = endIndex;
            } else if (method.startsWith("else", index)) {
                int endIndex = findStatementEnd(method, index);
                contextStack.push("else block");
                index = endIndex;
            } else if (method.startsWith("try {", index)) {
                contextStack.push("try-catch block");
                index += 4;
            } else if (method.startsWith("while (", index)) {
                int endIndex = findStatementEnd(method, index);
                contextStack.push("while loop");
                index = endIndex;
            } else if (method.startsWith("}", index)) {
                if (!contextStack.isEmpty()) {
                    contextStack.pop();
                }
                index++;
            } else if (method.startsWith("PRED", index)) {
                // Identify context for PRED token
                String context;
                if (contextStack.isEmpty()) {
                    Matcher matcher = predPattern.matcher(method.substring(Math.max(index - 1, 0), Math.min(index + 5, method.length())));
                    if (matcher.find()) {
                        context = "standalone PRED statement between semicolons (with possible whitespace)";
                    } else {
                        int prevStatementEnd = findPreviousStatementStart(method, index);
                        if (prevStatementEnd >= 0 && method.substring(prevStatementEnd, index)
                                .matches(".*\\b(int|double|String|boolean|float|char)\\b.*=.*;.*")) {
                            context = "variable declaration or assignment";
                        } else {
                            context = "No enclosing context";
                        }
                    }
                } else {
                    context = contextStack.peek();
                }
                writer.write("PRED found in context: " + context + "\n");
                foundPRED = true;
                index += 4;
            } else {
                index++;
            }
        }
    
        if (!foundPRED) {
            writer.write("No PRED token found in this method.\n");
        }
    }
    
    public static int findStatementEnd(String method, int startIndex) {
        int openBraceIndex = method.indexOf("{", startIndex);
        int semicolonIndex = method.indexOf(";", startIndex);

        if (openBraceIndex != -1 && (semicolonIndex == -1 || openBraceIndex < semicolonIndex)) {
            return openBraceIndex + 1;
        }

        return semicolonIndex != -1 ? semicolonIndex + 1 : method.length();
    }

    public static int findPreviousStatementStart(String method, int index) {
        int semicolonIndex = method.lastIndexOf(";", index - 1);
        return (semicolonIndex != -1) ? semicolonIndex + 1 : 0;
    }
}
