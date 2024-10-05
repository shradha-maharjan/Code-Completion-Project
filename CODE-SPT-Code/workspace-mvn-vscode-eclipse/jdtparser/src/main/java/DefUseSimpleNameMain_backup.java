//
///**
// * @(#) DefUseSimpleNameMain.java 
// */
//
//import org.eclipse.jdt.core.dom.ASTParser;
//import org.eclipse.jdt.core.dom.ASTVisitor;
//import org.eclipse.jdt.core.dom.CompilationUnit;
//import org.eclipse.jdt.core.dom.IBinding;
//import org.eclipse.jdt.core.dom.IMethodBinding;
//import org.eclipse.jdt.core.dom.ITypeBinding;
//import org.eclipse.jdt.core.dom.IVariableBinding;
//import org.eclipse.jdt.core.dom.SimpleName;
//
//
//import util.UtilAST;
//
///**
// * @since J2SE-1.17
// */
//public class DefUseSimpleNameMain {
//   static CompilationUnit cu;
//   // Sample target: path.hashCode() at Line 7
//   static final String _TARGET_HASHCODE_CALL_SAMPLE = "path.hashCode()";
//   static final int _OFFSET_HASHCODE_CALL_SAMPLE = 190;
//   static final int _LEN_HASHCODE_CALL_SAMPLE = 8;
//
//   // Sample target: File file at Line 10
//   static final String _TARGET_FILE_SAMPLE = "File file";
//   static final int _OFFSET_FILE_CALL_SAMPLE = 338;
//   static final int _LEN_FILE_CALL_SAMPLE = 8;
//
//   public static void main(String args[]) {
//      String javaFilePath = "input/ClassB.java";
//      ASTParser parser = UtilAST.parse(javaFilePath);
//      cu = (CompilationUnit) parser.createAST(null);
//
//      // Case 1
//      TargetTypeChecker checker = new TargetTypeChecker();
//      System.out.println("[DBG] Find Type: " + _TARGET_HASHCODE_CALL_SAMPLE);
//      checker.setTargetOffset(_OFFSET_HASHCODE_CALL_SAMPLE);
//      checker.setTargetLength(_LEN_HASHCODE_CALL_SAMPLE);
//      cu.accept(checker);
//      System.out.println("[DBG] ------------------------------------------------------");
//
//      // Case 2
//      checker = new TargetTypeChecker();
//      System.out.println("[DBG] Find Type: " + _TARGET_FILE_SAMPLE);
//      checker.setTargetOffset(_OFFSET_FILE_CALL_SAMPLE);
//      checker.setTargetLength(_LEN_FILE_CALL_SAMPLE);
//      cu.accept(checker);
//      System.out.println("[DBG] ------------------------------------------------------");
//
//      // Case 3: Unknown
//      checker = new TargetTypeChecker();
//      System.out.println("[DBG] Find Type: " + "UNKNOW");
//      checker.setTargetOffset(110);
//      checker.setTargetLength(5);
//      cu.accept(checker);
//
//      // SimpleNameChecker c = new SimpleNameChecker();
//      // cu.accept(c);
//   }
//}
//
//class TargetTypeChecker extends ASTVisitor {
//   String target;
//   int targetOffset, targetLength;
//
//   public boolean visit(SimpleName node) {
//      if (targetOffset <= node.getStartPosition() && //
//            node.getStartPosition() <= targetOffset + targetLength) {
//
//         IBinding binding = node.resolveBinding();
//
//         if (binding != null) {
//            checkTargetType(binding);
//         }
//         else {
//            System.out.println("Binding could not be resolved.");
//         }
//         node.getParent().accept(new AllASTVisitor());
//      }
//      return true;
//   }
//
//   private void checkTargetType(IBinding binding) {
//      if (binding instanceof IVariableBinding) {
//         IVariableBinding variableBinding = (IVariableBinding) binding;
//         System.out.println("[DBG] Type: Variable, Name: " + variableBinding.getName());
//      }
//      else if (binding instanceof ITypeBinding) {
//         ITypeBinding typeBinding = (ITypeBinding) binding;
//         System.out.println("[DBG] Type: Type, Name: " + typeBinding.getName());
//      }
//      else if (binding instanceof IMethodBinding) {
//         IMethodBinding methodBinding = (IMethodBinding) binding;
//         System.out.println("[DBG] Type: Method Call, Name: " + methodBinding.getName());
//      }
//   }
//
//   public void setTarget(String target) {
//      this.target = target;
//   }
//
//   public void setTargetOffset(int targetOffset) {
//      this.targetOffset = targetOffset;
//   }
//
//   public void setTargetLength(int targetLength) {
//      this.targetLength = targetLength;
//   }
//}
//
//class SimpleNameChecker extends ASTVisitor {
//
//   public boolean visit(SimpleName node) {
//      if (node.toString().equals("File"))
//         System.out.println(node + ", " + node.getStartPosition());
//      return true;
//   }
//
//}
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.io.IOException;
//import org.eclipse.jdt.core.dom.*;
//
//import util.UtilAST;
//
//public class DefUseSimpleNameMain_backup {
//
//    static CompilationUnit cuInput1;
//
//    // Input 1 file path
//    static final String INPUT1_FILE_PATH = "input/ClassB.java";
//
//    // Input 2 file path
//    static final String INPUT2_FILE_PATH = "input/ClassD.java";
//
//    public static void main(String args[]) throws IOException {
//        // Step 1: Manually find the offset of '[PRED]' in Input 2
//        String input2Code = new String(Files.readAllBytes(Paths.get(INPUT2_FILE_PATH)));
//        int predOffset = input2Code.indexOf("[PRED]");
//
//        if (predOffset != -1) {
//            System.out.println("[DBG] PRED Offset Found: " + predOffset);
//        } else {
//            System.out.println("[DBG] [PRED] not found in Input 2");
//            return; // Exit if [PRED] not found
//        }
//
//        // Step 2: Parse Input 1 and use the offset to determine the type at that offset
//        ASTParser parser1 = UtilAST.parse(INPUT1_FILE_PATH);
//        cuInput1 = (CompilationUnit) parser1.createAST(null);
//
//        // Find the type of the node in Input 1 based on the offset
//        TargetTypeChecker checker = new TargetTypeChecker();
//        checker.setTargetOffset(predOffset);  // Use the found offset from Input 2
//        checker.setTargetLength("[PRED]".length()); // Length of the placeholder
//        cuInput1.accept(checker);
//    }
//}
//
//class TargetTypeChecker extends ASTVisitor {
//    int targetOffset, targetLength;
//
//    @Override
//    public boolean visit(SimpleName node) {
//        if (targetOffset <= node.getStartPosition() &&
//            node.getStartPosition() <= targetOffset + targetLength) {
//
//            IBinding binding = node.resolveBinding();
//            if (binding != null) {
//                checkTargetType(binding);
//            } else {
//                System.out.println("Binding could not be resolved.");
//            }
//            node.getParent().accept(new AllASTVisitor()); // Visit with AllASTVisitor
//        }
//        return true;
//    }
//
//    private void checkTargetType(IBinding binding) {
//        if (binding instanceof IVariableBinding) {
//            IVariableBinding variableBinding = (IVariableBinding) binding;
//            System.out.println("[DBG] Type: Variable, Name: " + variableBinding.getName());
//        } else if (binding instanceof ITypeBinding) {
//            ITypeBinding typeBinding = (ITypeBinding) binding;
//            System.out.println("[DBG] Type: Type, Name: " + typeBinding.getName());
//        } else if (binding instanceof IMethodBinding) {
//            IMethodBinding methodBinding = (IMethodBinding) binding;
//            System.out.println("[DBG] Type: Method Call, Name: " + methodBinding.getName());
//        }
//    }
//
//    public void setTargetOffset(int targetOffset) {
//        this.targetOffset = targetOffset;
//    }
//
//    public void setTargetLength(int targetLength) {
//        this.targetLength = targetLength;
//    }
//}
