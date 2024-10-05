package dataflow;

import org.eclipse.jdt.core.dom.*;
import util.UtilAST;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import visitor.MethodCallVisitor;

public class SimpleExampleDefUse2 {

    private static final String UNIT_NAME = "DummyClass";

    private static final String INPUT_FILE_1 = "input/pretrain_source_test.txt"; // I1
    private static final String INPUT_FILE_2 = "input/longest_sequence.txt";     // I2
    private static final String OUTPUT_MASKED_FILE_PATH = "output/masked_output_test.txt";

    public static void main(String[] args) {
        try (BufferedReader reader1 = new BufferedReader(new FileReader(INPUT_FILE_1));
             BufferedReader reader2 = new BufferedReader(new FileReader(INPUT_FILE_2));
             FileWriter writer = new FileWriter(OUTPUT_MASKED_FILE_PATH)) {

            // Read all of input1 (source code)
            StringBuilder codeSnippet = new StringBuilder();
            String line;
            while ((line = reader1.readLine()) != null) {
                codeSnippet.append(line).append("\n");
            }
            System.out.println("Source code from Input1:\n" + codeSnippet);

            // Read all method sequences from input2 and split them into individual method calls
            List<String> methodSequences = new ArrayList<>();
            while ((line = reader2.readLine()) != null) {
                String[] methodCallsInSequence = line.split("\\s+"); // Split by whitespace to get individual calls
                for (String methodCall : methodCallsInSequence) {
                    methodSequences.add(normalizeMethodCall(methodCall.trim())); // Add each call individually
                }
            }
            System.out.println("Individual Method Sequences from Input2: " + methodSequences);

            // Step 1: Parse input1 using UtilAST.parseSrcCode
            String formattedCode = formatCode(codeSnippet.toString());
            ASTParser parser = UtilAST.parseSrcCode(formattedCode, UNIT_NAME + ".java");
            CompilationUnit cu = (CompilationUnit) parser.createAST(null);

            // Step 2: Visit the method calls in input1
            MethodCallVisitor visitor = new MethodCallVisitor();
            cu.accept(visitor);
            List<MethodInvocation> methodCalls = visitor.getMethodCalls();
            System.out.println("Method calls found in Input1:");

            // Step 3: Compare method calls from input1 with input2 sequences and write offsets to output
            for (MethodInvocation methodCall : methodCalls) {
                String expression = methodCall.getExpression() != null ? methodCall.getExpression().toString() : "";
                String fullMethodCall = expression + "." + methodCall.getName() + "()";
                fullMethodCall = normalizeMethodCall(fullMethodCall);

                // Log both values for comparison
                System.out.println("Checking method call: '" + fullMethodCall + "'");
                System.out.println("Method Sequences: " + methodSequences);

                if (methodSequences.contains(fullMethodCall.trim())) {
                    int startOffset = methodCall.getStartPosition();
                    int endOffset = startOffset + methodCall.getLength();
                    writer.write("Method: " + fullMethodCall + " | Start offset: " + startOffset + " | End offset: " + endOffset + "\n");
                    System.out.println("Match found! Writing to output: " + fullMethodCall);
                } else {
                    System.out.println("No match found for: " + fullMethodCall);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper function to normalize a method call
    private static String normalizeMethodCall(String methodCall) {
        // Remove arguments and semicolons, and normalize spaces
        return methodCall.replaceAll("\\(.*?\\)", "()").replace(";", "").trim();
    }

    // Helper function to wrap a single line of code into a minimal class structure for parsing
    private static String formatCode(String codeSnippet) {
        return "public class " + UNIT_NAME + " {\n" + codeSnippet + "\n}";
    }
}
