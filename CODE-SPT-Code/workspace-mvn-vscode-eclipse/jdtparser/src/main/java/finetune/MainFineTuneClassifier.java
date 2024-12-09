package finetune;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Stack;

import org.eclipse.jdt.core.dom.*;

import util.UtilAST;

public class MainFineTuneClassifier {

    static CompilationUnit cuInput1, cuInput2;

    static final String DIR = "/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/";
    static final String INPUT1_FILE_PATH = "/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/data_shradha/fine-tune/raw_methods_train.txt";
    static final String INPUT2_FILE_PATH = "/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/data_shradha/fine-tune/source_methods_train.txt";
    static final String OUTPUT_FILE_PATH = DIR + "output_finetune/Train/train_targettype_output_1.txt";
    static final String OUTPUT_FILE_PRED_NULL_PATH = DIR + "output_finetune/Train/null_targettype_output_newTrain.txt";
    static final String OUTPUT_FOR_WITH_PRED = DIR + "output_finetune/Train/Train_methods_with_ForLoop.txt";
    static final String OUTPUT_FOR_WITHOUT_PRED = DIR + "output_finetune/Train/Train_methods_without_ForLoop.txt";
    static final String OUTPUT_IF_WITH_PRED = DIR + "output_finetune/Train/Train_methods_with_IfElseIf.txt";
    static final String OUTPUT_IF_WITHOUT_PRED = DIR + "output_finetune/Train/Train_methods_without_IfElseIf.txt";

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

    private String formatCode(String codeLine) {
        return "public class DummyClass {\n" + codeLine + "\n}";
    }
}
