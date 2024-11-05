package dataflow;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SimpleExampleDefUseImp3 {
    private static final String INPUT_FILE_PATH = "output/no_usage_sequences.txt";
    private static final String OUTPUT_FILE_WITH_VAR_PATH = "output/with_artificial_variable.txt";
    private static final String OUTPUT_FILE_WITH_MASKING_PATH = "output/with_masking.txt";

    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new FileReader(INPUT_FILE_PATH));
             FileWriter writerWithVar = new FileWriter(OUTPUT_FILE_WITH_VAR_PATH);
             FileWriter writerWithMasking = new FileWriter(OUTPUT_FILE_WITH_MASKING_PATH)) {

            String line;
            int index = 1;

            while ((line = reader.readLine()) != null) {
                System.out.println("\nProcessing line " + index + ": " + line);
                
                // Step 1: Add artificial variable
                String modifiedCode = addArtificialVariable(line);
                if (modifiedCode == null) continue; // skip if modification couldn't be done
                String singleLineCodeWithVar = flattenToSingleLine(modifiedCode);
                writerWithVar.write(singleLineCodeWithVar + "\n");

                // Step 2: Mask the artificial variable
                String maskedCode = maskArtificialVariable(singleLineCodeWithVar);
                writerWithMasking.write(maskedCode + "\n");

                index++;
            }

            System.out.println("\nArtificial variable output saved to " + OUTPUT_FILE_WITH_VAR_PATH);
            System.out.println("Masked output saved to " + OUTPUT_FILE_WITH_MASKING_PATH);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds an artificial variable declaration and usage.
     */
    private static String addArtificialVariable(String code) {
        String singleLineCode = flattenToSingleLine(code);
        
        int startBodyIndex = singleLineCode.indexOf("{");
        int endBodyIndex = singleLineCode.lastIndexOf("}");
    
        if (startBodyIndex == -1 || endBodyIndex == -1) {
            return null;
        }

        return singleLineCode.substring(0, startBodyIndex + 1) 
             + " int __x__ = 1; " 
             + singleLineCode.substring(startBodyIndex + 1, endBodyIndex)
             + " __call__(__x__); "
             + singleLineCode.substring(endBodyIndex);
    }    

    /**
     * Masks the artificial variable "__x__".
     */
    private static String maskArtificialVariable(String code) {
        return code.replace("__x__", "[MASK]");
    }

    /**
     * Flattens multi-line code into a single line.
     */
    private static String flattenToSingleLine(String code) {
        return code.replaceAll("\\s+", " ").replace(" { ", "{").replace(" }", "}");
    }
}


// package dataflow;

// import java.io.BufferedReader;
// import java.io.FileReader;
// import java.io.FileWriter;
// import java.io.IOException;
// import java.util.*;

// import org.eclipse.jdt.core.dom.*;

// import util.UtilAST;

// public class SimpleExampleDefUseImp3 {
//     private static final String UNIT_NAME = "DummyClass";
//     private static final String INPUT_FILE_PATH = "output/no_usage_sequences.txt";
//     private static final String OUTPUT_FILE_PATH = "output/no_usage_sequences_output_2.txt";
//     private static final String NO_USAGE_FILE_PATH = "output/no_usage_sequences_2.txt";

//     public static void main(String[] args) {
//         int noUsageCount = 0;

//         try (BufferedReader reader = new BufferedReader(new FileReader(INPUT_FILE_PATH));
//              FileWriter writer = new FileWriter(OUTPUT_FILE_PATH);
//              FileWriter noUsageWriter = new FileWriter(NO_USAGE_FILE_PATH)) {

//             String line;
//             int index = 1;

//             while ((line = reader.readLine()) != null) {
//                 System.out.println("\nProcessing line " + index + ": " + line);
//                 String modifiedCode = addArtificialVariableIfNeeded(line);

//                 if (modifiedCode != null) {
//                     writer.write(modifiedCode + "\n");
//                 } else {
//                     noUsageWriter.write(line + "\n");
//                     noUsageCount++;
//                 }

//                 index++;
//             }

//             System.out.println("\nTotal lines without variable usage: " + noUsageCount);
//             System.out.println("Lines without variable usage saved to " + NO_USAGE_FILE_PATH);

//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }

//     private static String formatCode(String codeSnippet) {
//         return "public class " + UNIT_NAME + " {\n" + codeSnippet + "\n}";
//     }

//     /**
//      * Uses the AST to add an artificial variable declaration to methods that lack variable declarations.
//      */
//     private static String addArtificialVariableIfNeeded(String code) {
//         String formattedCode = formatCode(code);
        
//         // Parse the code into an AST
//         ASTParser parser = UtilAST.parseSrcCode(formattedCode, UNIT_NAME + ".java");
//         CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        
//         // Visitor to determine if there are any variable declarations
//         VariableDeclarationChecker checker = new VariableDeclarationChecker();
//         cu.accept(checker);

//         if (checker.hasVariableDeclarations) {
//             return null; // No modification needed if there are variable declarations
//         }

//         // If no variable declarations found, add an artificial variable declaration
//         AST ast = cu.getAST();
//         cu.accept(new ASTVisitor() {
//             public boolean visit(MethodDeclaration node) {
//                 Block body = node.getBody();
//                 if (body != null) {
//                     VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
//                     fragment.setName(ast.newSimpleName("__x__"));
//                     fragment.setInitializer(ast.newNumberLiteral("1"));
                    
//                     VariableDeclarationStatement varDecl = ast.newVariableDeclarationStatement(fragment);
//                     varDecl.setType(ast.newPrimitiveType(PrimitiveType.INT));

//                     // Adding the variable declaration at the beginning of the method
//                     body.statements().add(0, varDecl);

//                     // Adding a dummy usage at the end of the method
//                     MethodInvocation dummyUsage = ast.newMethodInvocation();
//                     dummyUsage.setName(ast.newSimpleName("__call__"));
//                     dummyUsage.arguments().add(ast.newSimpleName("__x__"));
//                     ExpressionStatement dummyStmt = ast.newExpressionStatement(dummyUsage);
//                     body.statements().add(dummyStmt);
//                 }
//                 return false; // No need to visit inner elements
//             }
//         });

//         // Convert the modified AST back to code
//         return cu.toString();
//     }

//     /**
//      * Visitor to check if a method contains variable declarations.
//      */
//     static class VariableDeclarationChecker extends ASTVisitor {
//         boolean hasVariableDeclarations = false;

//         @Override
//         public boolean visit(VariableDeclarationFragment node) {
//             hasVariableDeclarations = true;
//             return false; // No need to continue if we found a variable declaration
//         }
//     }
// }

// package dataflow;

// import java.io.BufferedReader;
// import java.io.FileReader;
// import java.io.FileWriter;
// import java.io.IOException;
// import java.util.*;

// import org.eclipse.jdt.core.dom.*;

// import util.UtilAST;

// public class SimpleExampleDefUseImp3 {
//     private static final String UNIT_NAME = "DummyClass";
//     private static final String INPUT_FILE_PATH = "output/no_usage_sequences.txt";
//     private static final String OUTPUT_FILE_PATH = "output/no_usage_sequences_output_2.txt";
//     private static final String NO_USAGE_FILE_PATH = "output/no_usage_sequences_2.txt";

//     public static void main(String[] args) {
//         int noUsageCount = 0; 

//         try (BufferedReader reader = new BufferedReader(new FileReader(INPUT_FILE_PATH));
//              FileWriter writer = new FileWriter(OUTPUT_FILE_PATH) ;
//              FileWriter noUsageWriter = new FileWriter(NO_USAGE_FILE_PATH)) {

//             String line;
//             int index = 1;

//             while ((line = reader.readLine()) != null) {
//                 System.out.println("\nProcessing line " + index + ": " + line);
//                 String formattedCode = formatCode(line);

//                 ASTParser parser = UtilAST.parseSrcCode(formattedCode, UNIT_NAME + ".java");
//                 CompilationUnit cu = (CompilationUnit) parser.createAST(null);

//                 SimpleExampleDefUseImp3 example = new SimpleExampleDefUseImp3();
                
//                 example.longestUsageCounts.clear();
//                 example.usageOffsets.clear();

//                 MyVisitor myVisitor = example.new MyVisitor(cu);
//                 cu.accept(myVisitor);

//                 if (example.longestUsageCounts.isEmpty()) {
//                     // Write the line without variable usage to no_usage_count.txt
//                     noUsageWriter.write(line + "\n");
//                     noUsageCount++;
//                 } else {
//                     int offsetAdjustment = formattedCode.indexOf(line);
//                     String maskedLine = example.maskVariableWithLongestUsageSequence(line, offsetAdjustment);

//                     // Only write to output if a successful masking occurred
//                     if (!maskedLine.equals(line)) {
//                         writer.write(maskedLine + "\n");
//                     }
//                 }
//                 index++;
//             }

//             System.out.println("\nTotal lines without variable usage: " + noUsageCount);
//             System.out.println("Lines without variable usage saved to " + NO_USAGE_FILE_PATH);

//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }

//     private static String formatCode(String codeSnippet) {
//         return "public class " + UNIT_NAME + " {\n" + codeSnippet + "\n}";
//     }

//     Map<IBinding, Integer> longestUsageCounts = new HashMap<>();
//     Map<IBinding, List<Integer>> usageOffsets = new HashMap<>();

//     class MyVisitor extends ASTVisitor {
//         private final CompilationUnit cu;
//         Set<IBinding> bindings = new HashSet<>();

//         public MyVisitor(CompilationUnit cu) {
//             this.cu = cu;
//         }

//         public boolean visit(VariableDeclarationFragment node) {
//             SimpleName name = node.getName();
//             IBinding binding = name.resolveBinding();
//             bindings.add(binding);
//             longestUsageCounts.put(binding, 0); // Initialize the longest sequence count for each variable
//             usageOffsets.put(binding, new ArrayList<>()); // Initialize offsets list
//             System.out.println("[DBG] Declaration of '" + name + "' at line " + cu.getLineNumber(name.getStartPosition()));
//             return true;
//         }

//         public boolean visit(SingleVariableDeclaration node) {
//             SimpleName name = node.getName();
//             IBinding binding = name.resolveBinding();
//             bindings.add(binding);
//             longestUsageCounts.put(binding, 0); // Initialize the longest sequence count for each variable
//             usageOffsets.put(binding, new ArrayList<>()); // Initialize offsets list
//             System.out.println("[DBG] Declaration2 of '" + name + "' at line " + cu.getLineNumber(name.getStartPosition()));
//             return true;
//         }

//         public boolean visit(SimpleName node) {
//             if (node.getParent() instanceof VariableDeclarationFragment )//||
//               //  node.getParent() instanceof SingleVariableDeclaration)
//             	{
//                 return true;
//             }

//             IBinding binding = node.resolveBinding();
//             if (binding != null && bindings.contains(binding)) {
//                 int currentCount = longestUsageCounts.get(binding) + 1;
//                 longestUsageCounts.put(binding, currentCount);
//                 usageOffsets.get(binding).add(node.getStartPosition());
//             }
//             return true;
//         }
//     }

//     public String maskVariableWithLongestUsageSequence(String originalLine, int offsetAdjustment) {
//         IBinding maxBinding = null;
//         int maxUsageCount = 0;

//         for (Map.Entry<IBinding, Integer> entry : longestUsageCounts.entrySet()) {
//             IBinding binding = entry.getKey();
//             int usageCount = entry.getValue();
//             if (usageCount > maxUsageCount) {
//                 maxUsageCount = usageCount;
//                 maxBinding = binding;
//             }
//         }

//         if (maxBinding != null) {
//             System.out.println("Variable with highest usage: '" + maxBinding.getName() + "'");
//             System.out.println("Usage count: " + maxUsageCount);
//             System.out.println("Offsets: " + usageOffsets.get(maxBinding));
//         }

//         if (maxBinding != null) {
//             List<Integer> offsets = usageOffsets.getOrDefault(maxBinding, Collections.emptyList());
//             String variableName = maxBinding.getName();

//             offsets.replaceAll(offset -> offset - offsetAdjustment);
//             offsets.sort(Collections.reverseOrder());

//             StringBuilder maskedLine = new StringBuilder(originalLine);
//             for (int offset : offsets) {
//                 int endOffset = offset + variableName.length();
//                 if (offset >= 0 && offset < originalLine.length()) {
//                     maskedLine.replace(offset, Math.min(endOffset, originalLine.length()), "[MASK]");
//                 }
//             }
//             return maskedLine.toString();
//         } else {
//             return originalLine; 
//         }
//     }
// }




//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.Collections;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import org.eclipse.jdt.core.dom.ASTNode;
//import org.eclipse.jdt.core.dom.ASTParser;
//import org.eclipse.jdt.core.dom.ASTVisitor;
//import org.eclipse.jdt.core.dom.CompilationUnit;
//import org.eclipse.jdt.core.dom.IBinding;
//import org.eclipse.jdt.core.dom.SimpleName;
//import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
//import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
//
//import util.UtilAST;
//
//public class SimpleExampleDefUse {
//    static CompilationUnit cu;
//
//    // Track longest sequence count and offsets of each variable
//    Map<IBinding, Integer> longestUsageCounts = new HashMap<>();
//    Map<IBinding, List<Integer>> usageOffsets = new HashMap<>();
//
//    public static void main(String args[]) throws IOException {
//        String javaFilePath = "input/ClassA.java";
//        ASTParser parser = UtilAST.parse(javaFilePath);
//        cu = (CompilationUnit) parser.createAST(null);
//
//        SimpleExampleDefUse example = new SimpleExampleDefUse();
//        MyVisitor myVisitor = example.new MyVisitor();
//        cu.accept(myVisitor);
//        
//        example.printLongestUsageSequence();
//
//        // Mask and output the modified code
//        example.maskVariableWithLongestUsageSequence(javaFilePath);
//    }
//
//    class MyVisitor extends ASTVisitor {
//        Set<IBinding> bindings = new HashSet<>();
//
//        public boolean visit(VariableDeclarationFragment node) {
//            SimpleName name = node.getName();
//            IBinding binding = name.resolveBinding();
//            bindings.add(binding);
//            longestUsageCounts.put(binding, 0); // Initialize the longest sequence count for each variable
//            usageOffsets.put(binding, new ArrayList<>()); // Initialize offsets list
//            System.out.println("[DBG] Declaration of '" + name + "' at line " + cu.getLineNumber(name.getStartPosition()));
//            return true;
//        }
//
//        public boolean visit(SimpleName node) {
//            if (node.getParent() instanceof VariableDeclarationFragment ||
//                node.getParent() instanceof SingleVariableDeclaration) {
//                return true;
//            }
//
//            IBinding binding = node.resolveBinding();
//            if (binding != null && bindings.contains(binding)) {
//                int lineNumber = cu.getLineNumber(node.getStartPosition());
//                System.out.println("[DBG] Usage of '" + node + "' at line " + lineNumber);
//
//                // Update the longest usage count and store offset for each occurrence
//                int currentCount = longestUsageCounts.get(binding) + 1;
//                longestUsageCounts.put(binding, currentCount);
//                usageOffsets.get(binding).add(node.getStartPosition());
//
//                // Print the declaration node if it exists
//                ASTNode declaringNode = cu.findDeclaringNode(binding);
//                System.out.println("[DBG] declaringNode: " + declaringNode);
//            }
//            return true;
//        }
//    }
//
//    // Method to print only the variable with the highest longest usage sequence, along with all its offsets
//    public void printLongestUsageSequence() {
//        IBinding maxBinding = null;
//        int maxUsageCount = 0;
//
//        // Find the variable with the maximum usage count
//        for (Map.Entry<IBinding, Integer> entry : longestUsageCounts.entrySet()) {
//            IBinding binding = entry.getKey();
//            int usageCount = entry.getValue();
//            if (usageCount > maxUsageCount) {
//                maxUsageCount = usageCount;
//                maxBinding = binding;
//            }
//        }
//
//        // Print the highest longest usage sequence variable with all offsets
//        if (maxBinding != null) {
//            System.out.println("Variable '" + maxBinding.getName() + "' has longest usage sequence: " +
//                               maxUsageCount + " with offsets: " + usageOffsets.get(maxBinding));
//        }
//    }
//
//    // Method to mask the variable with the highest longest usage sequence
//    public void maskVariableWithLongestUsageSequence(String javaFilePath) throws IOException {
//        IBinding maxBinding = null;
//        int maxUsageCount = 0;
//
//        // Find the variable with the maximum usage count
//        for (Map.Entry<IBinding, Integer> entry : longestUsageCounts.entrySet()) {
//            IBinding binding = entry.getKey();
//            int usageCount = entry.getValue();
//            if (usageCount > maxUsageCount) {
//                maxUsageCount = usageCount;
//                maxBinding = binding;
//            }
//        }
//
//        // Read original code
//        String code = new String(Files.readAllBytes(Paths.get(javaFilePath)));
//
//        // Mask the variable with the highest usage sequence
//        if (maxBinding != null) {
//            List<Integer> offsets = usageOffsets.getOrDefault(maxBinding, Collections.emptyList());
//            String variableName = maxBinding.getName();
//
//            // Sort offsets in descending order to prevent re-indexing issues during replacement
//            offsets.sort(Collections.reverseOrder());
//
//            StringBuilder maskedCode = new StringBuilder(code);
//            for (int offset : offsets) {
//                int endOffset = offset + variableName.length();
//                maskedCode.replace(offset, endOffset, "[MASK]");
//            }
//
//            // Ensure the output directory exists
//            Path outputDir = Paths.get("output");
//            if (!Files.exists(outputDir)) {
//                Files.createDirectories(outputDir);
//            }
//
//            // Write the masked code to a .txt file
//            Files.write(Paths.get("output/MaskedClassA.txt"), maskedCode.toString().getBytes());
//            System.out.println("\nMasked code saved to output/MaskedClassA.txt");
//        } else {
//            System.out.println("No variable with maximum sequence found to mask.");
//        }
//    }
//}
