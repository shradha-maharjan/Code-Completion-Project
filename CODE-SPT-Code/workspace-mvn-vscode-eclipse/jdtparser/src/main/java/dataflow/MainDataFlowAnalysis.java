package dataflow;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import base.GlobalInfo;
import base.MainBaseClass;
import util.UtilAST;
import util.UtilFile;

/*
Modify this program to include a visit method for MethodInvocation.
Given the USE infomation, extract the realted method calls.
For example, given the object "oA" in "input/ClassA", the methods "oA.m1()" and "oA.m2()"; given the object "oB", the methods "oB.m3()" and "oB.m4()".

The expected outputs: 2 rows 
ObjectA oA = new ObjectA(); oA.m1(); oA.m2();
ObjectB oB = new ObjectB(); oB.m3(); oB.m4();
*/
public class MainDataFlowAnalysis {
    static CompilationUnit cu;

    public static void main(String args[]) {
       String javaFilePath = System.getProperty("user.dir") + "/jdtparser/input/ClassA.java";
       System.out.println("[DBG] INPUT PATH: " + javaFilePath);
       ASTParser parser = UtilAST.parse(javaFilePath);
       cu = (CompilationUnit) parser.createAST(null);
       MyVisitor myVisitor = new MyVisitor();
       cu.accept(myVisitor);
 
    }
 
    static class MyVisitor extends ASTVisitor {
       Set<IBinding> bindings = new HashSet<>();

    //    public boolean visit(VariableDeclarationStatement node) {
    //       System.out.println("[DBG] Var Decl: " + node);
    //       return true;
    //    } 
    
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
             System.out.println("[DBG] Usage of '" + node + "' at line " + cu.getLineNumber(node.getStartPosition()));
             ASTNode declaringNode = cu.findDeclaringNode(binding);
             System.out.println("[DBG] declaringNode: " + declaringNode);
          }
          return true;
       }
    }
     
}
