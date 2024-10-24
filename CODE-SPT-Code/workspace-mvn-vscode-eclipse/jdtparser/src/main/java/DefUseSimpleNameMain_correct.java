
// import java.io.BufferedReader;
// import java.io.FileReader;
// import java.io.FileWriter;
// import java.io.IOException;
// import org.eclipse.jdt.core.dom.*;

// import util.UtilAST;

// public class DefUseSimpleNameMain_correct {

//    static CompilationUnit cuInput1, cuInput2;

//    // Input 1 and Input 2 file paths
//    static final String DIR = "/home/user1-selab3/Documents/research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/";
//    static final String INPUT1_FILE_PATH = DIR + "input/input_raw.txt";//"/home/user1-selab3/Documents/research-shradha/data_shradha/fine-tune/raw_methods_train.txt";  // Raw input (without PRED)
//    static final String INPUT2_FILE_PATH = DIR + "input/input_pred.txt";//"/home/user1-selab3/Documents/research-shradha/data_shradha/fine-tune/source_methods_train.txt";  // Input with PRED token
//    static final String OUTPUT_FILE_PATH = DIR + "input_test_targettype_output.txt";//"output/train_targettype_output.txt";  // Output file to write the results

//    public static void main(String args[]) throws IOException {
//       System.out.println("Hello World!!");
//    }

//    public static void main() throws IOException {

//        // Step 1: Read both Input 1 and Input 2 line by line
//        try (BufferedReader reader1 = new BufferedReader(new FileReader(INPUT1_FILE_PATH));
//             BufferedReader reader2 = new BufferedReader(new FileReader(INPUT2_FILE_PATH));
//             FileWriter writer = new FileWriter(OUTPUT_FILE_PATH)) {

//            String lineInput1;
//            String lineInput2;

//            int lineNum = 1;
//            while ((lineInput1 = reader1.readLine()) != null && (lineInput2 = reader2.readLine()) != null) {
//                // Step 2: Find the offset of '[PRED]' in the current line of Input 2
//                int predOffset = lineInput2.indexOf("PRED");

//                if (predOffset != -1) {
//                    String predOffsetMessage = "[DBG] PRED Offset Found in line " + lineNum + ": " + predOffset;
//                    System.out.println(predOffsetMessage);
//                    writer.write(predOffsetMessage + "\n");
//                } else {
//                    String predNotFoundMessage = "[DBG] [PRED] not found in line " + lineNum;
//                    System.out.println(predNotFoundMessage);
//                    writer.write(predNotFoundMessage + "\n");
//                    lineNum++;
//                    continue;  // Skip this line if no [PRED] is found
//                }

//                // Step 3: Parse the corresponding line from Input 1 (Raw input)
//                // Wrap the line of code from Input 1 in a class to allow AST parsing
//                String wrappedInput1Code = formatCode(lineInput1);
//                ASTParser parser1 = UtilAST.parseSrcCode(wrappedInput1Code, UNIT_NAME + ".java");
//                cuInput1 = (CompilationUnit) parser1.createAST(null);

//                // Step 4: Parse Input 2 (Source input with PRED)
//                String wrappedInput2Code = formatCode(lineInput2);
//                ASTParser parser2 = UtilAST.parseSrcCode(wrappedInput2Code, UNIT_NAME + ".java");
//                cuInput2 = (CompilationUnit) parser2.createAST(null);

//                // Step 5: Use PredOffsetFinder to check for 'PRED' in Input 2
//                PredOffsetFinder predFinder = new PredOffsetFinder(predOffset, writer);
//                cuInput2.accept(predFinder);

//                // Step 6: Use TargetTypeChecker to check for 'X' in Input 1
//                TargetTypeChecker checker = new TargetTypeChecker(writer, predFinder.getPredOffset());  // Pass the writer to checker
//                cuInput1.accept(checker);

//                String separator = "[DBG] ------------------------------------------------------";
//                System.out.println(separator);
//                writer.write(separator + "\n");

//                // Increment the line number
//                lineNum++;
//            }
//        }
//    }

//    // Helper function to wrap a single line of code into a minimal class structure for parsing
//    private static final String UNIT_NAME = "DummyClass";

//    private static String formatCode(String codeLine) {
//       return "public class " + UNIT_NAME + " {\n" + codeLine + "\n}";
//    }
// }

// class PredOffsetFinder extends ASTVisitor {
//    private int targetOffset;
//    private int predOffset;
//    private FileWriter writer;

//    // Constructor to accept offset of PRED and a FileWriter for output
//    public PredOffsetFinder(int predOffset, FileWriter writer) {
//        this.predOffset = predOffset;
//        this.writer = writer;
//    }

//    @Override
//    public boolean visit(SimpleName node) {
//        // Find the node that corresponds to 'PRED' in the source input
//        if (node.toString().equals("PRED")) {
//            targetOffset = node.getStartPosition();
//            try {
//                String foundMessage = "[DBG] Found PRED node at offset: " + targetOffset;
//                System.out.println(foundMessage);
//                writer.write(foundMessage + "\n");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        return true;
//    }

//    public int getPredOffset() {
//        return targetOffset;
//    }
// }

// class TargetTypeChecker extends ASTVisitor {
//    private int predOffset;
//    private FileWriter writer;

//    // Constructor to accept the predOffset and FileWriter for output
//    public TargetTypeChecker(FileWriter writer, int predOffset) {
//        this.writer = writer;
//        this.predOffset = predOffset;
//    }

//    @Override
//    public boolean visit(SimpleName node) {
//        // Check if the offset of 'X' matches the offset of 'PRED'
//        if (node.getStartPosition() == predOffset) {
//            IBinding binding = node.resolveBinding();
//            if (binding != null) {
//                checkTargetType(binding);
//            } else {
//                try {
//                    String unresolvedBindingMessage = "Binding could not be resolved for SimpleName: " + node.getIdentifier();
//                    System.out.println(unresolvedBindingMessage);
//                    writer.write(unresolvedBindingMessage + "\n");
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            node.getParent().accept(new AllASTVisitor(writer));
//        }
//        return true;
//    }

//    private void checkTargetType(IBinding binding) {
//        try {
//            if (binding instanceof IVariableBinding) {
//                IVariableBinding variableBinding = (IVariableBinding) binding;
//                String output = "[DBG] Type: Variable, Name: " + variableBinding.getName();
//                System.out.println(output);
//                writer.write(output + "\n");
//            } else if (binding instanceof ITypeBinding) {
//                ITypeBinding typeBinding = (ITypeBinding) binding;
//                String output = "[DBG] Type: Type, Name: " + typeBinding.getName();
//                System.out.println(output);
//                writer.write(output + "\n");
//            } else if (binding instanceof IMethodBinding) {
//                IMethodBinding methodBinding = (IMethodBinding) binding;
//                String output = "[DBG] Type: Method Call, Name: " + methodBinding.getName();
//                System.out.println(output);
//                writer.write(output + "\n");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
// }

// import java.io.BufferedReader;
// import java.io.FileReader;
// import java.io.FileWriter;
// import java.io.IOException;
// import org.eclipse.jdt.core.dom.*;
// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.List;

// import util.UtilAST;

// public class DefUseSimpleNameMain_correct {

//     static CompilationUnit cuInput1, cuInput2;

// static final String DIR = "/home/user1-selab3/Documents/research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/";
//    static final String INPUT1_FILE_PATH = DIR + "input/input_raw.txt";//"/home/user1-selab3/Documents/research-shradha/data_shradha/fine-tune/raw_methods_train.txt";  // Raw input (without PRED)
//    static final String INPUT2_FILE_PATH = DIR + "input/input_pred.txt";//"/home/user1-selab3/Documents/research-shradha/data_shradha/fine-tune/source_methods_train.txt";  // Input with PRED token
//    static final String OUTPUT_FILE_PATH = DIR + "output/output1.txt"; 
//     public static void main(String args[]) throws IOException {
//         try (BufferedReader reader1 = new BufferedReader(new FileReader(INPUT1_FILE_PATH));
//              BufferedReader reader2 = new BufferedReader(new FileReader(INPUT2_FILE_PATH));
//              FileWriter writer = new FileWriter(OUTPUT_FILE_PATH)) {

//             String lineInput1;
//             String lineInput2;

//             int lineNum = 1;
//             while ((lineInput1 = reader1.readLine()) != null && (lineInput2 = reader2.readLine()) != null) {
//                 int predOffset = lineInput2.indexOf("PRED");

//                 if (predOffset != -1) {
//                     String predOffsetMessage = "[DBG] PRED Offset Found in line " + lineNum + ": " + predOffset;
//                     writeMessage(predOffsetMessage, writer);
//                 } else {
//                     String predNotFoundMessage = "[DBG] [PRED] not found in line " + lineNum;
//                     writeMessage(predNotFoundMessage, writer);
//                     lineNum++;
//                     continue;  
//                 }

//                 String wrappedInput1Code = formatCode(lineInput1);
//                 ASTParser parser1 = UtilAST.parseSrcCode(wrappedInput1Code, UNIT_NAME + ".java");
//                 cuInput1 = (CompilationUnit) parser1.createAST(null);

//                 String wrappedInput2Code = formatCode(lineInput2);
//                 ASTParser parser2 = UtilAST.parseSrcCode(wrappedInput2Code, UNIT_NAME + ".java");
//                 cuInput2 = (CompilationUnit) parser2.createAST(null);

//                 PredOffsetFinder predFinder = new PredOffsetFinder(predOffset, writer);
//                 cuInput2.accept(predFinder);

//                 TargetTypeChecker checker = new TargetTypeChecker(writer, predFinder.getPredOffset());
//                 cuInput1.accept(checker);

//                 checker.writeSortedNodeTypes();

//                 writeMessage("[DBG] ------------------------------------------------------", writer);

//                 lineNum++;
//             }
//         }
//     }

//     private static final String UNIT_NAME = "DummyClass";

//     private static String formatCode(String codeLine) {
//         return "public class " + UNIT_NAME + " {\n" + codeLine + "\n}";
//     }

//     private static void writeMessage(String message, FileWriter writer) throws IOException {
//         System.out.println(message);
//         writer.write(message + "\n");
//     }
// }

// class PredOffsetFinder extends ASTVisitor {
//     private int targetOffset;
//     private int predOffset;
//     private FileWriter writer;

//     public PredOffsetFinder(int predOffset, FileWriter writer) {
//         this.predOffset = predOffset;
//         this.writer = writer;
//     }

//     @Override
//     public boolean visit(SimpleName node) {
//         if (node.toString().equals("PRED")) {
//             targetOffset = node.getStartPosition();
//             writeMessage("[DBG] Found PRED node at offset: " + targetOffset, writer);
//         }
//         return true;  
//     }

//     public int getPredOffset() {
//         return targetOffset;
//     }

//     private void writeMessage(String message, FileWriter writer) {
//         try {
//             System.out.println(message);
//             writer.write(message + "\n");
//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }
// }

// class TargetTypeChecker extends ASTVisitor {
//     private int predOffset;
//     private FileWriter writer;
//     private List<String> nodeTypes;  

//     public TargetTypeChecker(FileWriter writer, int predOffset) {
//         this.writer = writer;
//         this.predOffset = predOffset;
//         this.nodeTypes = new ArrayList<>(); 
//     }

//     @Override
//     public boolean visit(SimpleName node) {
//         checkNodePosition(node);
//         return true;
//     }

//     // @Override
//     // public boolean visit(MethodInvocation node) {
//     //     checkNodePosition(node);
//     //     return true;
//     // }

//     @Override
//     public boolean visit(FieldAccess node) {
//         checkNodePosition(node);
//         return true;
//     }

//     @Override
//     public boolean visit(QualifiedName node) {
//         checkNodePosition(node);
//         return true;
//     }

//     // @Override
//     // public boolean visit(ThrowStatement node) {
//     //     checkNodePosition(node);  
//     //     return true;
//     // }

//     // public boolean visit(ReturnStatement node) {
//     //     checkNodePosition(node.getExpression());  
//     //     return true;
//     // }
    
//     private void checkNodePosition(ASTNode node) {
//         int nodeOffset = node.getStartPosition();
//         int nodeLength = node.getLength();

//        // System.out.println("[DBG] Visiting node of type: " + node.getClass().getSimpleName() + " at offset: " + nodeOffset);

//         if (predOffset >= nodeOffset && predOffset < nodeOffset + nodeLength) {
//             System.out.println("[DBG] Matching node found for PRED at offset: " + nodeOffset);
//             IBinding binding = resolveBindingForNode(node);
//             if (binding != null) {
//                 checkTargetType(binding);
//             } else {
//                 writeMessage("[DBG] Binding could not be resolved for node: " + node.toString(), writer);
//             }
//             if (node.getParent() != null) {
//                 node.getParent().accept(new AllASTVisitor(writer));
//             }
//         }
//     }

//     private IBinding resolveBindingForNode(ASTNode node) {
//         if (node instanceof SimpleName) {
//             return ((SimpleName) node).resolveBinding();
//         } 
//         // else if (node instanceof MethodInvocation) {
//         //     return ((MethodInvocation) node).resolveMethodBinding();
//        // } 
//         else if (node instanceof FieldAccess) {
//             return ((FieldAccess) node).resolveFieldBinding();
//         } else if (node instanceof QualifiedName) {
//             return ((QualifiedName) node).resolveBinding();
//         // // } else if (node instanceof ReturnStatement) {
//         // //     return ((ReturnStatement) node).getExpression().resolveTypeBinding();
//         // } else if (node instanceof ThrowStatement) {
//         //     return ((ThrowStatement) node).getExpression().resolveTypeBinding();  // Resolve the binding of the expression within the throw statement
//         }
//         return null;
//     }

    
//     private void checkTargetType(IBinding binding) {
//         if (binding instanceof IVariableBinding) {
//             IVariableBinding variableBinding = (IVariableBinding) binding;
//             writeMessage("[DBG] Type: Variable, Name: " + variableBinding.getName(), writer);
//         } else if (binding instanceof ITypeBinding) {
//             ITypeBinding typeBinding = (ITypeBinding) binding;
//             writeMessage("[DBG] Type: Type, Name: " + typeBinding.getName(), writer);
//         } else if (binding instanceof IMethodBinding) {
//             IMethodBinding methodBinding = (IMethodBinding) binding;
//             writeMessage("[DBG] Type: Method Call, Name: " + methodBinding.getName(), writer);
//         } else {
//             writeMessage("[DBG] Unhandled binding type for: " + binding.getName(), writer);
//         }
//     }

//     public void writeSortedNodeTypes() throws IOException {
//         Collections.sort(nodeTypes);

//         for (String nodeType : nodeTypes) {
//             writeMessage("\t=> " + nodeType, writer);
//         }

//         nodeTypes.clear();
//     }

//     private void writeMessage(String message, FileWriter writer) {
//         try {
//             System.out.println(message);
//             writer.write(message + "\n");
//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }
// }
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.eclipse.jdt.core.dom.*;

import util.UtilAST;

public class DefUseSimpleNameMain_correct {

    static CompilationUnit cuInput1, cuInput2;

    // Input 1 and Input 2 file paths
    static final String DIR = "/home/user1-selab3/Documents/research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/";
    static final String INPUT1_FILE_PATH = DIR + "input/input_raw.txt";
    static final String INPUT2_FILE_PATH = DIR + "input/input_pred.txt";
    static final String OUTPUT_FILE_PATH = DIR + "output/output1.txt";

    public static void main(String[] args) throws IOException {

        // Step 1: Read both Input 1 and Input 2 line by line
        try (BufferedReader reader1 = new BufferedReader(new FileReader(INPUT1_FILE_PATH));
             BufferedReader reader2 = new BufferedReader(new FileReader(INPUT2_FILE_PATH));
             FileWriter writer = new FileWriter(OUTPUT_FILE_PATH)) {

            String lineInput1;
            String lineInput2;

            int lineNum = 1;
            while ((lineInput1 = reader1.readLine()) != null && (lineInput2 = reader2.readLine()) != null) {
                // Step 2: Find the offset of '[PRED]' in the current line of Input 2
                int predOffset = lineInput2.indexOf("PRED");

                if (predOffset != -1) {
                    String predOffsetMessage = "[DBG] PRED Offset Found in line " + lineNum + ": " + predOffset;
                    System.out.println(predOffsetMessage);
                    writer.write(predOffsetMessage + "\n");
                } else {
                    String predNotFoundMessage = "[DBG] [PRED] not found in line " + lineNum;
                    System.out.println(predNotFoundMessage);
                    writer.write(predNotFoundMessage + "\n");
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

                // Step 5: Use PredOffsetFinder to check for 'PRED' in Input 2
                PredOffsetFinder predFinder = new PredOffsetFinder(predOffset, writer);
                cuInput2.accept(predFinder);

                // Step 6: Use AllASTVisitor to check all node types and match the PRED offset
                AllASTVisitor allASTVisitor = new AllASTVisitor(writer, predFinder.getPredOffset());
                cuInput1.accept(allASTVisitor);

                String separator = "[DBG] ------------------------------------------------------";
                System.out.println(separator);
                writer.write(separator + "\n");

                lineNum++;
            }
        }
    }

    private static final String UNIT_NAME = "DummyClass";

    private static String formatCode(String codeLine) {
        return "public class " + UNIT_NAME + " {\n" + codeLine + "\n}";
    }
}

class PredOffsetFinder extends ASTVisitor {
    private int targetOffset;
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
