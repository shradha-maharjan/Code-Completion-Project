package finetune;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.*;

import util.UtilAST;

public class MainFineTuneClassifier {

    static CompilationUnit cuInput1, cuInput2;

    static final String DIR = "/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/";
    static final String INPUT1_FILE_PATH = "/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/data_shradha/fine-tune/raw_methods_train.txt";
    static final String INPUT2_FILE_PATH = "/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/data_shradha/fine-tune/source_methods_train.txt";
    static final String OUTPUT_FILE_PATH = DIR + "output_finetune/Train1/Train_targettype_output_1.txt";
    static final String OUTPUT_FILE_PRED_NULL_PATH = DIR + "output_finetune/Train1/null_targettype_output_newTrain.txt";
    static final String OUTPUT_FOR_WITH_PRED = DIR + "output_finetune/Train1/Train_methods_with_ForLoop.txt";
    static final String OUTPUT_FOR_WITHOUT_PRED = DIR + "output_finetune/Train1/Train_methods_without_ForLoop.txt";
    static final String OUTPUT_IF_WITH_PRED = DIR + "output_finetune/Train1/Train_methods_with_IfElseIf.txt";
    static final String OUTPUT_IF_WITHOUT_PRED = DIR + "output_finetune/Train1/Train_methods_without_IfElseIf.txt";
    static final String OUTPUT_CLASSIFICATION = DIR + "output_finetune/Train1/Train_methods_classification.txt";

    public static void main(String[] args) {
        try {
            MainFineTuneClassifier classifier = new MainFineTuneClassifier();
            classifier.process();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void process() throws IOException {
        System.out.println("Starting MainFineTuneClassifier...");
        processFineTuneVisitor();
        processFineTuneForStmtVisitor();
        processFineTuneIfStmtVisitor();
        //processClassification();
        findPREDContexts();
        System.out.println("Processing completed.");
    }

    // Step 1: FineTuneVisitor
    private void processFineTuneVisitor() throws IOException {
        try (BufferedReader reader1 = new BufferedReader(new FileReader(INPUT1_FILE_PATH));
             BufferedReader reader2 = new BufferedReader(new FileReader(INPUT2_FILE_PATH));
             FileWriter writer = new FileWriter(OUTPUT_FILE_PATH);
             FileWriter writer4PredNull = new FileWriter(OUTPUT_FILE_PRED_NULL_PATH)) {

            String lineInput1, lineInput2;
            int lineNum = 1;
            int counterNullPred = 0;

            while ((lineInput1 = reader1.readLine()) != null && (lineInput2 = reader2.readLine()) != null) {
                int predOffset = lineInput2.indexOf("PRED");

                if (predOffset != -1) {
                    writer.write("[DBG] PRED Offset Found in line " + lineNum + ": " + predOffset + "\n");
                } else {
                    writer.write("[DBG] [PRED] not found in line " + lineNum + "\n");
                    lineNum++;
                    continue;
                }

                // Step 3: Parse the corresponding line from Input 1 (Raw input)
                String wrappedInput1Code = formatCode(lineInput1);
                ASTParser parser1 = UtilAST.parseSrcCode(wrappedInput1Code, UNIT_NAME + ".java");
                cuInput1 = (CompilationUnit) parser1.createAST(null);
               

                // Step 4: Parse Input 2 (Source input with PRED)
                String wrappedInput2Code = formatCode(lineInput2);
                ASTParser parser2 = UtilAST.parseSrcCode(wrappedInput2Code, UNIT_NAME + ".java");
                cuInput2 = (CompilationUnit) parser2.createAST(null);

                PredOffsetFinder predFinder = new PredOffsetFinder(predOffset, writer);
                cuInput2.accept(predFinder);

                if (predFinder.getPredOffset() == -1) {
                    writer4PredNull.write(lineInput2 + "\n");
                    counterNullPred++;
                }

                AllASTVisitor allASTVisitor = new AllASTVisitor(writer, predFinder.getPredOffset());
                cuInput1.accept(allASTVisitor);
                allASTVisitor.logHighestPriorityNode();

                writer.write("[DBG] ------------------------------------------------------\n");
                lineNum++;
            }
            writer.close();
            System.out.println("[DBG] Total Number of Null PRED: " + counterNullPred);
        }

        
    }

    private static final String UNIT_NAME = "DummyClass";

class PredOffsetFinder extends ASTVisitor {
    private int targetOffset = -1;
    private int predOffset;
    private FileWriter writer;

    public PredOffsetFinder(int predOffset, FileWriter writer) {
        this.predOffset = predOffset;
        this.writer = writer;
    }

    @Override
    public boolean visit(SimpleName node) {
        // Find the node that corresponds to 'PRED' in the source input
        if (node.toString().equals("PRED")) {
            targetOffset = node.getStartPosition();
            try {
                String foundMessage = "[DBG] Found PRED node at offset: " + targetOffset;
                System.out.println(foundMessage);
                writer.write(foundMessage + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public int getPredOffset() {
        return targetOffset;
    }

}

    // Step 2: FineTuneForStmtVisitor
    private void processFineTuneForStmtVisitor() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(OUTPUT_FILE_PRED_NULL_PATH));
             BufferedWriter writerWithPRED = new BufferedWriter(new FileWriter(OUTPUT_FOR_WITH_PRED));
             BufferedWriter writerWithoutPRED = new BufferedWriter(new FileWriter(OUTPUT_FOR_WITHOUT_PRED))) {

            String line;
            StringBuilder methodBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                methodBuilder.append(line).append(" ");
                if (line.contains("}")) {
                    String method = methodBuilder.toString();
                    if (containsForLoopWithPRED(method)) {
                        writerWithPRED.write(method.trim() + "\n");
                    } else {
                        writerWithoutPRED.write(method.trim() + "\n");
                    }
                    methodBuilder.setLength(0);
                }
            }
            System.out.println("Processed FineTuneForStmtVisitor.");
        }
    }

    private boolean containsForLoopWithPRED(String method) {
        int index = 0;
        while ((index = method.indexOf("for (", index)) != -1) {
            int openParenIndex = index + 4;
            int closeParenIndex = findClosingParenthesis(method, openParenIndex);
            if (closeParenIndex != -1) {
                String forLoopCondition = method.substring(openParenIndex + 1, closeParenIndex);
                if (forLoopCondition.contains("PRED")) {
                    return true;
                }
            }
            index = closeParenIndex + 1;
        }
        return false;
    }

    private int findClosingParenthesis(String input, int openIndex) {
        Stack<Integer> stack = new Stack<>();
        for (int i = openIndex; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch == '(') stack.push(i);
            else if (ch == ')') {
                stack.pop();
                if (stack.isEmpty()) return i;
            }
        }
        return -1;
    }

    // Step 3: FineTuneIfStmtVisitor
    private void processFineTuneIfStmtVisitor() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(OUTPUT_FOR_WITHOUT_PRED));
             BufferedWriter writerWithIfElseIf = new BufferedWriter(new FileWriter(OUTPUT_IF_WITH_PRED));
             BufferedWriter writerWithoutIfElseIf = new BufferedWriter(new FileWriter(OUTPUT_IF_WITHOUT_PRED))) {

            String line;
            StringBuilder methodBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                methodBuilder.append(line).append(" ");
                if (line.contains("}")) {
                    String method = methodBuilder.toString();
                    if (containsIfElseIfWithPRED(method)) {
                        writerWithIfElseIf.write(method.trim() + "\n");
                    } else {
                        writerWithoutIfElseIf.write(method.trim() + "\n");
                    }
                    methodBuilder.setLength(0);
                }
            }
            System.out.println("Processed FineTuneIfStmtVisitor.");
        }
    }

    private boolean containsIfElseIfWithPRED(String method) {
        int index = 0;
        while (index < method.length()) {
            if (method.startsWith("if (", index) || method.startsWith("else if (", index) || method.startsWith("else", index)) {
                int openBraceIndex = method.indexOf("{", index);
                if (openBraceIndex != -1) {
                    int closeBraceIndex = findMatchingBrace(method, openBraceIndex);
                    if (closeBraceIndex != -1) {
                        String blockContent = method.substring(openBraceIndex + 1, closeBraceIndex);
                        if (blockContent.contains("PRED")) {
                            return true;
                        }
                        index = closeBraceIndex + 1;
                    } else {
                        break;
                    }
                } else {
                    index += method.startsWith("else if (", index) ? 8 : method.startsWith("if (", index) ? 3 : 4;
                }
            } else {
                index++;
            }
        }
        return false;
    }

    private int findMatchingBrace(String input, int openBraceIndex) {
        Stack<Integer> stack = new Stack<>();
        for (int i = openBraceIndex; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch == '{') stack.push(i);
            else if (ch == '}') {
                stack.pop();
                if (stack.isEmpty()) return i;
            }
        }
        return -1;
    }

    // Step 4: Process Classification
    
    public static void findPREDContexts() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(OUTPUT_IF_WITHOUT_PRED));
            BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_CLASSIFICATION))) {

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

            System.out.println("PRED contexts saved to " + OUTPUT_CLASSIFICATION);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void analyzePREDContexts(String method, BufferedWriter writer) throws IOException {
        // Define regex patterns for different constructs
        Pattern ifPattern = Pattern.compile("if\\s*\\(.*?\\).*?PRED", Pattern.DOTALL);
        Pattern elsePattern = Pattern.compile("else\\s*(if\\s*\\(.*?\\))?.*?PRED", Pattern.DOTALL);
        Pattern whilePattern = Pattern.compile("while\\s*\\(.*?\\).*?PRED", Pattern.DOTALL);
        Pattern forPattern = Pattern.compile("for\\s*\\(.*?\\).*?PRED", Pattern.DOTALL);
        Pattern tryCatchPattern = Pattern.compile("try\\s*\\{.*?PRED.*?\\}|catch\\s*\\(.*?\\).*?PRED", Pattern.DOTALL);
        Pattern switchPattern = Pattern.compile("switch\\s*\\(.*?\\).*?PRED", Pattern.DOTALL);
        Pattern throwsPattern = Pattern.compile("throws\\s+.*?PRED", Pattern.DOTALL);
        Pattern returnPattern = Pattern.compile("return\\s+.*?PRED", Pattern.DOTALL);

        // Match the method text against patterns
        if (matchesPattern(method, ifPattern)) {
            writer.write("PRED found in context: if\n");
        } else if (matchesPattern(method, elsePattern)) {
            writer.write("PRED found in context: else\n");
        } else if (matchesPattern(method, whilePattern)) {
            writer.write("PRED found in context: while\n");
        } else if (matchesPattern(method, forPattern)) {
            writer.write("PRED found in context: for\n");
        } else if (matchesPattern(method, tryCatchPattern)) {
            writer.write("PRED found in context: try-catch\n");
        } else if (matchesPattern(method, switchPattern)) {
            writer.write("PRED found in context: switch\n");
        } else if (matchesPattern(method, throwsPattern)) {
            writer.write("PRED found in context: throws\n");
        } else if (matchesPattern(method, returnPattern)) {
            writer.write("PRED found in context: return\n");
        } else {
            writer.write("PRED found in context: unclassified\n");
        }
    }

    private static boolean matchesPattern(String method, Pattern pattern) {
        Matcher matcher = pattern.matcher(method);
        return matcher.find();
    }

    // public static void analyzePREDContexts(String method, BufferedWriter writer) throws IOException {
    //     Stack<String> contextStack = new Stack<>();
    //     int index = 0;
    //     boolean foundPRED = false;
    
    //     Pattern predPattern = Pattern.compile(";\\s*PRED\\s*;");
    
    //     while (index < method.length()) {
    //         if (method.startsWith("if (", index) || method.startsWith("else if (", index)) {
    //             int endIndex = findStatementEnd(method, index);
    //             contextStack.push("if/else block");
    //             index = endIndex;
    //         } else if (method.startsWith("else", index)) {
    //             int endIndex = findStatementEnd(method, index);
    //             contextStack.push("else block");
    //             index = endIndex;
    //         } else if (method.startsWith("try {", index)) {
    //             contextStack.push("try-catch block");
    //             index += 4;
    //         } else if (method.startsWith("while (", index)) {
    //             int endIndex = findStatementEnd(method, index);
    //             contextStack.push("while loop");
    //             index = endIndex;
    //         } else if (method.startsWith("}", index)) {
    //             if (!contextStack.isEmpty()) {
    //                 contextStack.pop();
    //             }
    //             index++;
    //         } else if (method.startsWith("PRED", index)) {
    //             // Identify context for PRED token
    //             String context;
    //             if (contextStack.isEmpty()) {
    //                 Matcher matcher = predPattern.matcher(method.substring(Math.max(index - 1, 0), Math.min(index + 5, method.length())));
    //                 if (matcher.find()) {
    //                     context = "standalone PRED statement between semicolons (with possible whitespace)";
    //                 } else {
    //                     int prevStatementEnd = findPreviousStatementStart(method, index);
    //                     if (prevStatementEnd >= 0 && method.substring(prevStatementEnd, index)
    //                             .matches(".*\\b(int|double|String|boolean|float|char)\\b.*=.*;.*")) {
    //                         context = "variable declaration or assignment";
    //                     } else {
    //                         context = "No enclosing context";
    //                     }
    //                 }
    //             } else {
    //                 context = contextStack.peek();
    //             }
    //             writer.write("PRED found in context: " + context + "\n");
    //             foundPRED = true;
    //             index += 4;
    //         } else {
    //             index++;
    //         }
    //     }
    
    //     if (!foundPRED) {
    //         writer.write("No PRED token found in this method.\n");
    //     }
    // }
    
    // public static int findStatementEnd(String method, int startIndex) {
    //     int openBraceIndex = method.indexOf("{", startIndex);
    //     int semicolonIndex = method.indexOf(";", startIndex);

    //     if (openBraceIndex != -1 && (semicolonIndex == -1 || openBraceIndex < semicolonIndex)) {
    //         return openBraceIndex + 1;
    //     }

    //     return semicolonIndex != -1 ? semicolonIndex + 1 : method.length();
    // }

    // public static int findPreviousStatementStart(String method, int index) {
    //     int semicolonIndex = method.lastIndexOf(";", index - 1);
    //     return (semicolonIndex != -1) ? semicolonIndex + 1 : 0;
    // }


    private String formatCode(String codeLine) {
        return "public class DummyClass {\n" + codeLine + "\n}";
    }
}
