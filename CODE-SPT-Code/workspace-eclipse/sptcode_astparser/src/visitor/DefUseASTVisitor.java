/**
 * @file DefUseASTVisitor.java
 */
package visitor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import data.DefUseModel;

/**
 * @since JavaSE-1.8
 */
public class DefUseASTVisitor extends ASTVisitor {
   private Map<IVariableBinding, DefUseModel> defUseMap = new HashMap<IVariableBinding, DefUseModel>();
   private int fAccessesToSystemFields;
   private CompilationUnit compilationUnit;

   public DefUseASTVisitor(CompilationUnit compilationUnit) {
      this.compilationUnit = compilationUnit;
   }

   @Override
   public boolean visit(VariableDeclarationStatement varDecSta) {
      for (Iterator<?> iter = varDecSta.fragments().iterator(); iter.hasNext();) {
         VariableDeclarationFragment varDecFra = (VariableDeclarationFragment) iter.next();

         IVariableBinding varBin = varDecFra.resolveBinding();
         DefUseModel defUseModel = new DefUseModel(varDecSta, varDecFra, this.compilationUnit);
         defUseMap.put(varBin, defUseModel);
      }
      return super.visit(varDecSta);
   }

   public boolean visit(SimpleName node) {
      if (node.getParent() instanceof VariableDeclarationFragment) {
         return true;
      } else if (node.getParent() instanceof SingleVariableDeclaration) {
         return true;
      }
      IBinding binding = node.resolveBinding();
      // Some SimpleName doesn't have binding information, returns null
      // But all SimpleName nodes will be binded
      if (binding == null) {
         return true;
      }
      if (defUseMap.containsKey(binding)) {
         defUseMap.get(binding).addUsedVars(node);
      }
      // countNumOfRefToFieldOfJavaLangSystem(node);
      return super.visit(node);
   }

   public Map<IVariableBinding, DefUseModel> getdefUseMap() {
      return this.defUseMap;
   }

   void countNumOfRefToFieldOfJavaLangSystem(SimpleName node) {
      IBinding binding = node.resolveBinding();
      if (binding instanceof IVariableBinding) {
         IVariableBinding varBinding = (IVariableBinding) binding;
         ITypeBinding declaringClass = varBinding.getDeclaringClass();
         if (varBinding.isField() && "java.lang.System".equals(declaringClass.getQualifiedName())) {
            fAccessesToSystemFields++;
            System.out.println(fAccessesToSystemFields);
         }
      }
   }

   /*@Override
   public boolean visit(ConditionalExpression node) {
      Expression expr1 = node.getExpression();
      Expression thenExpr1 = node.getThenExpression();
      Expression elseExpr1 = node.getElseExpression();
      System.out.println(expr1.toString() + ", LOCATION: [" + expr1.getStartPosition() + "]");
      System.out.println(thenExpr1.toString() + ", LOCATION: [" + thenExpr1.getStartPosition() + "]");
      System.out.println(elseExpr1.toString() + ", LOCATION: [" + elseExpr1.getStartPosition() + "]");
   
      Expression expr2 = (Expression) node.getStructuralProperty(ConditionalExpression.EXPRESSION_PROPERTY);
      Expression thenExpr2 = (Expression) node.getStructuralProperty(ConditionalExpression.THEN_EXPRESSION_PROPERTY);
      Expression elseExpr2 = (Expression) node.getStructuralProperty(ConditionalExpression.ELSE_EXPRESSION_PROPERTY);
   
      System.out.println(expr2.toString() + ", LOCATION: [" + expr1.getStartPosition() + "]");
      System.out.println(thenExpr2.toString() + ", LOCATION: [" + thenExpr1.getStartPosition() + "]");
      System.out.println(elseExpr2.toString() + ", LOCATION: [" + elseExpr1.getStartPosition() + "]");
      return super.visit(node);
   }*/
}
