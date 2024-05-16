
/**
 * @(#) SimpleExampleDefUse.java
 */
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import util.UtilAST;

/**
 * @since J2SE-1.8
 */
public class SimpleExampleDefUse {
   static CompilationUnit cu;

   public static void main(String args[]) {
      String javaFilePath = "input\\ClassA.java";
      ASTParser parser = UtilAST.parse(javaFilePath);
      cu = (CompilationUnit) parser.createAST(null);
      MyVisitor myVisitor = new MyVisitor();
      cu.accept(myVisitor);

   }

   static class MyVisitor extends ASTVisitor {
      Set<IBinding> bindings = new HashSet<>();

      // public boolean visit(VariableDeclarationFragment node) {
      // SimpleName name = node.getName();
      // // this.bindings.add(node.resolveBinding());
      // // System.out.println("[DBG] Declaration of '" + name + "' at line " + cu.getLineNumber(name.getStartPosition()));
      // System.out.println("[DBG] var: " + name);
      // return true;
      // }

      public boolean visit(MethodDeclaration node) {
         String qualifiedName = node.resolveBinding().getDeclaringClass().getQualifiedName();
         System.out.println("[DBG] dec: " + qualifiedName + "." + node.getName().getFullyQualifiedName());
         return true;
      }

      public boolean visit(IfStatement node) {
         System.out.println("__IF " + node.getExpression());
         return true;
      }

      public void endVisit(IfStatement node) {
         System.out.println("IF__");
      }

      public boolean visit(MethodInvocation node) {
         Object qualifiedName = node.resolveMethodBinding().getDeclaringClass().getName();
         String name = node.getName().getFullyQualifiedName();
         System.out.println("[DBG] inv: " + qualifiedName + "." + name);
         return true;
      }

          public boolean visit(VariableDeclarationStatement node) {
              print("local_variable_declaration");
              return true;
          }

          public boolean visit(ArrayCreation node) {
              print("array_creation_expression");
              return true;
          }

          public boolean visit(ForStatement node) {
              print("for_statement");
              return true;
          }

          public boolean visit(ExpressionStatement node) {
              print("expression_statement");
              if (node.getExpression() instanceof Assignment) {
                  print("assignment_expression");
              }
              return true;
          }

          public boolean visit(CastExpression node) {
              print("cast_expression");
              return true;
          }

          public boolean visit(ParenthesizedExpression node) {
              print("parenthesized_expression");
              return true;
          }

          public boolean visit(ReturnStatement node) {
              print("return_statement");
              return true;
          }

          private void print(String message) {
              System.out.print(message + "__");
          }  

      /*public boolean visit(SimpleName node) {
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
      }*/
   }
}