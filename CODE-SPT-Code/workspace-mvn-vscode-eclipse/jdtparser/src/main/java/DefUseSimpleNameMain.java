// //
// ///**
// // * @(#) DefUseSimpleNameMain.java 
// // */
// //
// //import org.eclipse.jdt.core.dom.ASTParser;
// //import org.eclipse.jdt.core.dom.ASTVisitor;
// //import org.eclipse.jdt.core.dom.CompilationUnit;
// //import org.eclipse.jdt.core.dom.IBinding;
// //import org.eclipse.jdt.core.dom.IMethodBinding;
// //import org.eclipse.jdt.core.dom.ITypeBinding;
// //import org.eclipse.jdt.core.dom.IVariableBinding;
// //import org.eclipse.jdt.core.dom.SimpleName;
// //
// //
// //import util.UtilAST;
// //
// ///**
// // * @since J2SE-1.17
// // */
// //public class DefUseSimpleNameMain {
// //   static CompilationUnit cu;
// //   // Sample target: path.hashCode() at Line 7
// //   static final String _TARGET_HASHCODE_CALL_SAMPLE = "path.hashCode()";
// //   static final int _OFFSET_HASHCODE_CALL_SAMPLE = 190;
// //   static final int _LEN_HASHCODE_CALL_SAMPLE = 8;
// //
// //   // Sample target: File file at Line 10
// //   static final String _TARGET_FILE_SAMPLE = "File file";
// //   static final int _OFFSET_FILE_CALL_SAMPLE = 338;
// //   static final int _LEN_FILE_CALL_SAMPLE = 8;
// //
// //   public static void main(String args[]) {
// //      String javaFilePath = "input/ClassB.java";
// //      ASTParser parser = UtilAST.parse(javaFilePath);
// //      cu = (CompilationUnit) parser.createAST(null);
// //
// //      // Case 1
// //      TargetTypeChecker checker = new TargetTypeChecker();
// //      System.out.println("[DBG] Find Type: " + _TARGET_HASHCODE_CALL_SAMPLE);
// //      checker.setTargetOffset(_OFFSET_HASHCODE_CALL_SAMPLE);
// //      checker.setTargetLength(_LEN_HASHCODE_CALL_SAMPLE);
// //      cu.accept(checker);
// //      System.out.println("[DBG] ------------------------------------------------------");
// //
// //      // Case 2
// //      checker = new TargetTypeChecker();
// //      System.out.println("[DBG] Find Type: " + _TARGET_FILE_SAMPLE);
// //      checker.setTargetOffset(_OFFSET_FILE_CALL_SAMPLE);
// //      checker.setTargetLength(_LEN_FILE_CALL_SAMPLE);
// //      cu.accept(checker);
// //      System.out.println("[DBG] ------------------------------------------------------");
// //
// //      // Case 3: Unknown
// //      checker = new TargetTypeChecker();
// //      System.out.println("[DBG] Find Type: " + "UNKNOW");
// //      checker.setTargetOffset(110);
// //      checker.setTargetLength(5);
// //      cu.accept(checker);
// //
// //      // SimpleNameChecker c = new SimpleNameChecker();
// //      // cu.accept(c);
// //   }
// //}
// //
// //class TargetTypeChecker extends ASTVisitor {
// //   String target;
// //   int targetOffset, targetLength;
// //
// //   public boolean visit(SimpleName node) {
// //      if (targetOffset <= node.getStartPosition() && //
// //            node.getStartPosition() <= targetOffset + targetLength) {
// //
// //         IBinding binding = node.resolveBinding();
// //
// //         if (binding != null) {
// //            checkTargetType(binding);
// //         }
// //         else {
// //            System.out.println("Binding could not be resolved.");
// //         }
// //         node.getParent().accept(new AllASTVisitor());
// //      }
// //      return true;
// //   }
// //
// //   private void checkTargetType(IBinding binding) {
// //      if (binding instanceof IVariableBinding) {
// //         IVariableBinding variableBinding = (IVariableBinding) binding;
// //         System.out.println("[DBG] Type: Variable, Name: " + variableBinding.getName());
// //      }
// //      else if (binding instanceof ITypeBinding) {
// //         ITypeBinding typeBinding = (ITypeBinding) binding;
// //         System.out.println("[DBG] Type: Type, Name: " + typeBinding.getName());
// //      }
// //      else if (binding instanceof IMethodBinding) {
// //         IMethodBinding methodBinding = (IMethodBinding) binding;
// //         System.out.println("[DBG] Type: Method Call, Name: " + methodBinding.getName());
// //      }
// //   }
// //
// //   public void setTarget(String target) {
// //      this.target = target;
// //   }
// //
// //   public void setTargetOffset(int targetOffset) {
// //      this.targetOffset = targetOffset;
// //   }
// //
// //   public void setTargetLength(int targetLength) {
// //      this.targetLength = targetLength;
// //   }
// //}
// //
// //class SimpleNameChecker extends ASTVisitor {
// //
// //   public boolean visit(SimpleName node) {
// //      if (node.toString().equals("File"))
// //         System.out.println(node + ", " + node.getStartPosition());
// //      return true;
// //   }
// //
// //}
// import java.io.BufferedReader; 
// import java.io.FileReader;
// import java.io.FileWriter;
// import java.io.IOException;
// import org.eclipse.jdt.core.dom.*;

// import util.UtilAST;

// public class DefUseSimpleNameMain {

//     static CompilationUnit cuInput1, cuInput2;

//     // Input 1 and Input 2 file paths
//     static final String INPUT1_FILE_PATH = "/home/user1-selab3/Documents/research-shradha/data_shradha/fine-tune/raw_methods_test.txt";  // Raw input (without PRED)
//     static final String INPUT2_FILE_PATH = "/home/user1-selab3/Documents/research-shradha/data_shradha/fine-tune/source_methods_test.txt";  // Input with PRED token
//     static final String OUTPUT_FILE_PATH = "output/test_targettype_output.txt";  // Output file to write the results

//     public static void main(String args[]) throws IOException {
//         // Step 1: Read both Input 1 and Input 2 line by line
//         try (BufferedReader reader1 = new BufferedReader(new FileReader(INPUT1_FILE_PATH));
//              BufferedReader reader2 = new BufferedReader(new FileReader(INPUT2_FILE_PATH));
//              FileWriter writer = new FileWriter(OUTPUT_FILE_PATH)) {

//             String lineInput1;
//             String lineInput2;

//             int lineNum = 1;
//             while ((lineInput1 = reader1.readLine()) != null && (lineInput2 = reader2.readLine()) != null) {
//                 // Step 2: Find the offset of '[PRED]' in the current line of Input 2
//                 int predOffset = lineInput2.indexOf("PRED");

//                 if (predOffset != -1) {
//                     String predOffsetMessage = "[DBG] PRED Offset Found in line " + lineNum + ": " + predOffset;
// //                    writer.write(predOffsetMessage + "\n");
//                 } else {
//                     String predNotFoundMessage = "[DBG] [PRED] not found in line " + lineNum;
// //                    writer.write(predNotFoundMessage + "\n");
//                     lineNum++;
//                     continue;  // Skip this line if no [PRED] is found
//                 }

//                 // Step 3: Parse the corresponding line from Input 1 (Raw input)
//                 String wrappedInput1Code = formatCode(lineInput1);
//                 ASTParser parser1 = UtilAST.parseSrcCode(wrappedInput1Code, UNIT_NAME + ".java");
//                 cuInput1 = (CompilationUnit) parser1.createAST(null);

//                 // Step 4: Parse Input 2 (Source input with PRED)
//                 String wrappedInput2Code = formatCode(lineInput2);
//                 ASTParser parser2 = UtilAST.parseSrcCode(wrappedInput2Code, UNIT_NAME + ".java");
//                 cuInput2 = (CompilationUnit) parser2.createAST(null);

//                 // Step 5: Use PredOffsetFinder to check for 'PRED' in Input 2
//                 PredOffsetFinder predFinder = new PredOffsetFinder(predOffset, writer);
//                 cuInput2.accept(predFinder);

//                 // Step 6: Use TargetTypeChecker to check for matching node in Input 1
//                 TargetTypeChecker checker = new TargetTypeChecker(writer, predFinder.getPredOffset());
//                 cuInput1.accept(checker);

//                 String separator = "[DBG] ------------------------------------------------------";
// //                writer.write(separator + "\n");

//                 // Increment the line number
//                 lineNum++;
//             }
//         }
//     }

//     // Helper function to wrap a single line of code into a minimal class structure for parsing
//     private static final String UNIT_NAME = "DummyClass";

//     private static String formatCode(String codeLine) {
//         return "public class " + UNIT_NAME + " {\n" + codeLine + "\n}";
//     }
// }
// class PredOffsetFinder extends ASTVisitor {
//     private int targetOffset;
//     private int predOffset;
//     private FileWriter writer;

//     // Constructor to accept offset of PRED and a FileWriter for output
//     public PredOffsetFinder(int predOffset, FileWriter writer) {
//         this.predOffset = predOffset;
//         this.writer = writer;
//     }

//     @Override
//     public boolean visit(SimpleName node) {
//         // Find the node that corresponds to 'PRED' in the source input
//         if (node.toString().equals("PRED")) {
//             targetOffset = node.getStartPosition();
//             String foundMessage = "[DBG] Found PRED node at offset: " + targetOffset;
// //                writer.write(foundMessage + "\n");
//         }
//         return true;
//     }

//     public int getPredOffset() {
//         return targetOffset;
//     }
// }
// class TargetTypeChecker extends ASTVisitor {
//     private int predOffset;
//     private FileWriter writer;

//     // Constructor to accept the predOffset and FileWriter for output
//     public TargetTypeChecker(FileWriter writer, int predOffset) {
//         this.writer = writer;
//         this.predOffset = predOffset;
//     }

//     @Override
//     public boolean visit(SimpleName node) {
//         // Check if the offset matches the offset of 'PRED'
//         if (node.getStartPosition() == predOffset) {
//             IBinding binding = node.resolveBinding();
//             if (binding != null) {
//                 checkTargetType(binding);
//             } else {
//                 String unresolvedBindingMessage = "Binding could not be resolved for SimpleName: " + node.getIdentifier();
// //                    writer.write(unresolvedBindingMessage + "\n");
//             }
//             // Visit parent node using AllASTVisitor
//             node.getParent().accept(new AllASTVisitor(writer));
//         }
//         return true;
//     }

//     private void checkTargetType(IBinding binding) {
//         if (binding instanceof IVariableBinding) {
// 		    IVariableBinding variableBinding = (IVariableBinding) binding;
// 		    String output = "[DBG] Type: Variable, Name: " + variableBinding.getName();
// //                writer.write(output + "\n");
// 		} else if (binding instanceof ITypeBinding) {
// 		    ITypeBinding typeBinding = (ITypeBinding) binding;
// 		    String output = "[DBG] Type: Type, Name: " + typeBinding.getName();
// //                writer.write(output + "\n");
// 		} else if (binding instanceof IMethodBinding) {
// 		    IMethodBinding methodBinding = (IMethodBinding) binding;
// 		    String output = "[DBG] Type: Method Call, Name: " + methodBinding.getName();
// //                writer.write(output + "\n");
// 		}
//     }
// }
