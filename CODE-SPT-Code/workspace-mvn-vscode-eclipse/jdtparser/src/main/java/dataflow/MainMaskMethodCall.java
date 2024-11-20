package dataflow;

import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

import util.UtilAST;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainMaskMethodCall {

    private static final String UNIT_NAME = "ParsedAndToString"; 

    private static final String DIR = "research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/output2/";
    private static final String INPUT_FILE_1 = DIR + "methods_with_max_calls.txt"; // Source code (Input 1) //"output/methods_with_max_calls.txt";//
    private static final String INPUT_FILE_2 = DIR + "longest_sequences.txt"; // Method calls to match (Input 2) "output/longest_sequences.txt";//
    private static final String OUTPUT_MASKED_FILE_PATH = DIR + "masked_method_calls.txt"; 

    public static void main(String[] args) {
        try (BufferedReader reader1 = new BufferedReader(new FileReader(INPUT_FILE_1));
             BufferedReader reader2 = new BufferedReader(new FileReader(INPUT_FILE_2));
             FileWriter writer = new FileWriter(OUTPUT_MASKED_FILE_PATH)) {

            List<String> methodCalls = new ArrayList<>();
            String line;
            while ((line = reader2.readLine()) != null) {
                String[] extractedMethods = extractMethodNames(line);
                for (String method : extractedMethods) {
                    if (!method.isEmpty()) {
                        methodCalls.add(method);
                    }
                }
            }
            System.out.println("Extracted Method Names from Input2: " + methodCalls);  
            if (methodCalls.isEmpty()) {
                System.err.println("Error reading method calls from Input2.");
                return;
            }

            StringBuilder maskedSourceBuilder = new StringBuilder(); 

            while ((line = reader1.readLine()) != null) {
                //String formattedSourceCode = formatCode(line); 
                //System.out.println("Formatted Source Code:\n" + formattedSourceCode);

                ASTParser parser = UtilAST.parseSrcCode(line, UNIT_NAME);

                CompilationUnit cu = (CompilationUnit) parser.createAST(null);

                StringBuilder sourceBuilder = new StringBuilder(line);

                List<int[]> maskPositions = new ArrayList<>();

                cu.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(MethodInvocation node) {
                        String methodName = node.getName().toString();
                        String objectName = "";

                        if (node.getExpression() instanceof SimpleName) {
                            objectName = ((SimpleName) node.getExpression()).getIdentifier();
                        }

                        String fullMethodCall = objectName + "." + methodName;
                        System.out.println("Found Method Invocation: " + fullMethodCall);  

                        if (methodCalls.contains(fullMethodCall)) {
                            System.out.println("Matched Method Call: " + fullMethodCall); 

                            int methodStart = node.getName().getStartPosition(); 
                            int methodEnd = methodStart + node.getName().getLength();  

                            System.out.println("Method Name Start: " + methodStart + ", Method Name End: " + methodEnd);

                            maskPositions.add(new int[]{methodStart, methodEnd});

                            System.out.println("Method Name to be Masked: " + sourceBuilder.substring(methodStart, methodEnd));
                        }

                        return super.visit(node);
                    }
                });

                maskPositions.sort((a, b) -> Integer.compare(b[0], a[0]));

                for (int[] pos : maskPositions) {
                    int start = pos[0];
                    int end = pos[1];

                    if (start < end && end <= sourceBuilder.length()) {
                        System.out.println("Applying mask from " + start + " to " + end);  
                        sourceBuilder.replace(start, end, "[MASK]");
                    }
                }

                String maskedCode = unwrapFormattedCode(sourceBuilder.toString());
                maskedSourceBuilder.append(maskedCode).append("\n");
            }

            writer.write(maskedSourceBuilder.toString());
            System.out.println("Masked source code successfully written to: " + OUTPUT_MASKED_FILE_PATH);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String[] extractMethodNames(String code) {
        String[] calls = code.split(";");

        for (int i = 0; i < calls.length; i++) {
            calls[i] = calls[i].replace("()", "").trim();  
        }

        return calls;
    }

    // private static String formatCode(String codeSnippet) {
    //     return "public class " + UNIT_NAME + " {\n" + codeSnippet + "\n}";
    // }

    private static String unwrapFormattedCode(String formattedCode) {
        int start = formattedCode.indexOf("{") + 1;
        int end = formattedCode.lastIndexOf("}");
        if (start != -1 && end != -1 && start < end) {
            return formattedCode.substring(start, end).trim(); 
        }
        return formattedCode;  
    }
}

