
/**
 * @(#) SimpleExampleDefUse.java
 */
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import util.UtilAST;

/**
 * @since J2SE-1.8
 */
public class SimpleExampleDefUse {
   static CompilationUnit cu;

   Map<VariableDeclarationFragment, List<MethodInvocation>> mapDeclCalls = new HashMap<>();

   public static void main(String args[]) {
      String javaFilePath = "input/ClassB.java";
      ASTParser parser = UtilAST.parse(javaFilePath);
      cu = (CompilationUnit) parser.createAST(null);
      MyVisitor myVisitor = new MyVisitor();
      cu.accept(myVisitor);

   }

   static class MyVisitor extends ASTVisitor {
      Set<IBinding> bindings = new HashSet<>();

      public boolean visit(VariableDeclarationFragment node) {
         SimpleName name = node.getName();
         this.bindings.add(node.resolveBinding());
         System.out.println("[DBG] Declaration of '" + name + "' at line " + cu.getLineNumber(name.getStartPosition()));
         return true;
      }

      public boolean visit(SimpleName node) {
         if (node.getParent() instanceof VariableDeclarationFragment //
               || node.getParent() instanceof SingleVariableDeclaration) {
            return true;
         }

         IBinding binding = node.resolveBinding();
         if (binding != null && bindings.contains(binding)) {
            int lineNumber = cu.getLineNumber(node.getStartPosition());
            if (lineNumber == 8) {
               System.out.print("");
            }

            System.out.println("[DBG] Usage of '" + node + "' at line " + lineNumber);
            ASTNode declaringNode = cu.findDeclaringNode(binding);
            System.out.println("[DBG] declaringNode: " + declaringNode);
         }
         return true;
      }
   }
}