package dataflow;

// import org.eclipse.jdt.core.dom.*;
// import util.UtilAST;

// import java.io.BufferedReader;
// import java.io.FileReader;
// import java.io.FileWriter;
// import java.io.IOException;
// import java.util.ArrayList;
// import java.util.List;

// import visitor.MethodCallVisitor;

// public class SimpleExampleDefUse2 {

//     private static final String UNIT_NAME = "DummyClass";

//     private static final String INPUT_FILE_1 = "/home/user1-selab3/Documents/research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/input/input1.txt"; // I1
//     private static final String INPUT_FILE_2 = "/home/user1-selab3/Documents/research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/input/input2.txt";     // I2
//     private static final String OUTPUT_MASKED_FILE_PATH = "/home/user1-selab3/Documents/research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/output/output_1.txt";

//     public static void main(String[] args) {
//         try (BufferedReader reader1 = new BufferedReader(new FileReader(INPUT_FILE_1));
//              BufferedReader reader2 = new BufferedReader(new FileReader(INPUT_FILE_2));
//              FileWriter writer = new FileWriter(OUTPUT_MASKED_FILE_PATH)) {

//             StringBuilder codeSnippet = new StringBuilder();
//             String line;
//             while ((line = reader1.readLine()) != null) {
//                 codeSnippet.append(line).append("\n");
//             }
//             System.out.println("Source code from Input1:\n" + codeSnippet);

//             List<String> methodSequences = new ArrayList<>();
//             while ((line = reader2.readLine()) != null) {
//                 String[] methodCallsInSequence = line.split("\\s+");
//                 for (String methodCall : methodCallsInSequence) {
//                     methodSequences.add(normalizeMethodCall(methodCall.trim())); 
//                 }
//             }
//             System.out.println("Individual Method Sequences from Input2: " + methodSequences);

//             String formattedCode = formatCode(codeSnippet.toString());
//             ASTParser parser = UtilAST.parseSrcCode(formattedCode, UNIT_NAME + ".java");
//             CompilationUnit cu = (CompilationUnit) parser.createAST(null);

//             MethodCallVisitor visitor = new MethodCallVisitor();
//             cu.accept(visitor);
//             List<MethodInvocation> methodCalls = visitor.getMethodCalls();
//             System.out.println("Method calls found in Input1:");

//             for (MethodInvocation methodCall : methodCalls) {
//                 String expression = methodCall.getExpression() != null ? methodCall.getExpression().toString() : "";
//                 String fullMethodCall = expression + "." + methodCall.getName() + "()";
//                 fullMethodCall = normalizeMethodCall(fullMethodCall);

//                 System.out.println("Checking method call: '" + fullMethodCall + "'");
//                 System.out.println("Method Sequences: " + methodSequences);

//                 if (methodSequences.contains(fullMethodCall.trim())) {
//                     int startOffset = methodCall.getStartPosition();
//                     int endOffset = startOffset + methodCall.getLength();
//                     writer.write("Method: " + fullMethodCall + " | Start offset: " + startOffset + " | End offset: " + endOffset + "\n");
//                     System.out.println("Match found! Writing to output: " + fullMethodCall);
//                 } else {
//                     System.out.println("No match found for: " + fullMethodCall);
//                 }
//             }
//             System.out.println("Source...");
//             System.out.println(cu.toString());
//             System.out.println("--");
//             String str = cu.toString().replaceAll("\n", "");
//             System.out.println(str.substring(48,54));
//             System.out.println("--");
//             System.out.println(str.substring(48));
//             System.out.println("--");
//             for (int i = 0; i < str.length(); i++) {
//                 System.out.println(i + ": \"" + str.charAt(i) + "\"");
//             }
//             System.out.println("--");
//             for (int i = 0; i < formattedCode.length(); i++) {
//                 System.out.println(i + ": \"" + formattedCode.charAt(i) + "\"");
//             }
//         } catch (IOException e) {
//             e.printStackTrace();
//         }
        
//     }

//     private static String normalizeMethodCall(String methodCall) {
//         return methodCall.replaceAll("\\(.*?\\)", "()").replace(";", "").trim();
//     }

//     private static String formatCode(String codeSnippet) {
//         return "public class " + UNIT_NAME + " {\n" + codeSnippet + "\n}";
//     }
// }

// import org.eclipse.jdt.core.dom.ASTParser;
// import org.eclipse.jdt.core.dom.ASTVisitor;
// import org.eclipse.jdt.core.dom.CompilationUnit;
// import org.eclipse.jdt.core.dom.MethodInvocation;
// import java.util.ArrayList;
// import java.util.List;

// import util.UtilAST;

// public class SimpleExampleDefUse2 {

//     public static void main(String[] args) {
//         // Source code to parse (Input 1)
//         String sourceCode = "public class DummyClass {"
//             + "@CanIgnoreReturnValue "
//             + "public long copyTo(CharSink sink) throws IOException {"
//             + " checkNotNull(sink);"
//             + " Closer closer = Closer.create();"
//             + " try {"
//             + " Reader reader = closer.register(openStream());"
//             + " Writer writer = closer.register(sink.openStream());"
//             + " return CharStreams.copy(reader, writer);"
//             + " } catch (Throwable e) {"
//             + " throw closer.rethrow(e);"
//             + " } finally {"
//             + " closer.close();"
//             + " }"
//             + " };"
//             + "}";

//         // Input 2: Method calls to find in sourceCode
//         String code = "closer.register(); closer.register(); closer.rethrow(); closer.close()";

//         // Step 1: Extract method names from the `code` string (Input 2)
//         String[] targetMethodCalls = extractMethodNames(code);

//         // Create the ASTParser instance for sourceCode
//         ASTParser parser = UtilAST.parseSrcCode(sourceCode, "DummyClass");

//         // Parse the source and get the CompilationUnit (root of the AST)
//         CompilationUnit cu = (CompilationUnit) parser.createAST(null);

//         // Use StringBuilder to replace text directly in the loop
//         StringBuilder sourceBuilder = new StringBuilder(sourceCode);

//         // Step 2: Collect all positions for masking
//         List<int[]> maskPositions = new ArrayList<>();

//         // Visit the AST to gather method invocation positions
//         cu.accept(new ASTVisitor() {
//             @Override
//             public boolean visit(MethodInvocation node) {
//                 // Get the method name (without arguments)
//                 String methodName = node.getName().toString(); 
//                 System.out.println("Found Method Invocation: " + methodName); // Debugging method name

//                 // Check if this method name matches any in the target array
//                 for (String targetCall : targetMethodCalls) {
//                     if (methodName.equals(targetCall)) {
//                         // Get the start position of the entire invocation
//                         int startPosition = node.getStartPosition();
//                         int length = node.getLength();
//                         int endPosition = startPosition + length;

//                         // Get the start and end position of the method name itself
//                         int methodStart = node.getName().getStartPosition();
//                         int methodEnd = methodStart + node.getName().getLength();

//                         // Debugging the method name positions
//                         System.out.println("Method Name to Mask: " + methodName);
//                         System.out.println("Method Start Position: " + methodStart);
//                         System.out.println("Method End Position: " + methodEnd);

//                         // Add only the method name's start and end positions to the list for masking
//                         maskPositions.add(new int[] {methodStart, methodEnd});

//                         // Print the part of the source being masked (for debugging)
//                         System.out.println("Original Method Name to be Masked: " + sourceBuilder.substring(methodStart, methodEnd));

//                         // Break after finding the match to avoid duplicate masking
//                         break;
//                     }
//                 }
//                 return super.visit(node);
//             }
//         });

//         // Step 3: Sort positions in reverse order to avoid index shifting while replacing
//         maskPositions.sort((a, b) -> Integer.compare(b[0], a[0]));

//         // Step 4: Apply the masking from the last position to the first
//         for (int[] pos : maskPositions) {
//             int start = pos[0];
//             int end = pos[1];

//             // Debugging the actual replacement process
//             System.out.println("Applying mask from " + start + " to " + end);

//             if (start < end && end <= sourceBuilder.length()) {
//                 // Show the context before replacing
//                 System.out.println("Before Replacement: " + sourceBuilder.substring(Math.max(0, start - 10), Math.min(end + 10, sourceBuilder.length())));

//                 // Replace with [MASK]
//                 sourceBuilder.replace(start, end, "[MASK]");

//                 // Show the context after replacing
//                 System.out.println("After Replacement: " + sourceBuilder.substring(Math.max(0, start - 10), Math.min(start + 10, sourceBuilder.length())));
//             }
//         }

//         // Step 5: Output the modified source code with masked method invocations
//         System.out.println("\nMasked Source Code:\n" + sourceBuilder.toString());
//     }

//     // Helper function to extract method names from Input 2 (code)
//     public static String[] extractMethodNames(String code) {
//         // Example Input: "closer.register(); closer.register(); closer.rethrow(); closer.close()"
//         // Step 1: Split by ";"
//         String[] calls = code.split(";");

//         // Step 2: Extract just the method names (strip the object reference and parentheses)
//         for (int i = 0; i < calls.length; i++) {
//             // Remove any object references (like "closer.") and parentheses "()", then trim spaces
//             calls[i] = calls[i].replaceAll(".*\\.", "").replace("()", "").trim();
//         }

//         // Step 3: Return the cleaned method names
//         return calls;
//     }
// }
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

public class SimpleExampleDefUse2 {

    private static final String UNIT_NAME = "DummyClass"; 
    private static final String INPUT_FILE_1 = "input/input1.txt"; // Source code (Input 1) //"output/methods_with_max_calls.txt";//
    private static final String INPUT_FILE_2 = "input/input2.txt"; // Method calls to match (Input 2) "output/longest_sequences.txt";//
    private static final String OUTPUT_MASKED_FILE_PATH = "output/output_1.txt"; 

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
                String formattedSourceCode = formatCode(line); 
                System.out.println("Formatted Source Code:\n" + formattedSourceCode);

                ASTParser parser = UtilAST.parseSrcCode(formattedSourceCode, UNIT_NAME);

                CompilationUnit cu = (CompilationUnit) parser.createAST(null);

                StringBuilder sourceBuilder = new StringBuilder(formattedSourceCode);

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

    private static String formatCode(String codeSnippet) {
        return "public class " + UNIT_NAME + " {\n" + codeSnippet + "\n}";
    }

    private static String unwrapFormattedCode(String formattedCode) {
        int start = formattedCode.indexOf("{") + 1;
        int end = formattedCode.lastIndexOf("}");
        if (start != -1 && end != -1 && start < end) {
            return formattedCode.substring(start, end).trim(); 
        }
        return formattedCode;  
    }
}

