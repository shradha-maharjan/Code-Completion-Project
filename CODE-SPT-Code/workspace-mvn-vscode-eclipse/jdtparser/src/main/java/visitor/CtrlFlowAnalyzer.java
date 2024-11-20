package visitor;

import java.util.ArrayList;
//import java.util.HashMap; 
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.core.dom.*;
import data.DefUseModel;
import datactrlflow.EnclosingClassInfo;
import util.UtilAST;

public class CtrlFlowAnalyzer extends ASTVisitor {
   Map<IVariableBinding, DefUseModel> defUseMap = new LinkedHashMap<IVariableBinding, DefUseModel>(); // new HashMap<>();
   CompilationUnit compilationUnit = null;
   List<String> methodNameList = new ArrayList<String>();
   String methodName, unitName;
   int countMethodDeclVisit = 0;
   boolean isCurMethodInsideAnonymousClass = false;
   boolean log = false;

   public CtrlFlowAnalyzer(String unitName, boolean log) {
      this.unitName = unitName;
      this.log = log;
   }

   public CtrlFlowAnalyzer(CompilationUnit compilationUnit) {
      this.compilationUnit = compilationUnit;
   }

   public boolean visit(MethodDeclaration node) {
      countMethodDeclVisit++;
      this.methodNameList.add(node.getName().getIdentifier());
      EnclosingClassInfo findEnclosingClassInfo = UtilAST.findEnclosingClassInfo(node, this.unitName);

      if (log) {
         System.out.println("[DBG] Method (visited): " + node.getName());
         System.out.println("[DBG] \t " + String.format("%b, %s", findEnclosingClassInfo.isDirectClass(), findEnclosingClassInfo.getEnclosingClass().getName()));
      }
      return true;
   }

   public String getMethodName() {
      this.methodName = this.methodNameList.get(0);
      return methodName;
   }

   public int getCountMethodDeclVisit() {
      return countMethodDeclVisit;
   }

   public Map<IVariableBinding, DefUseModel> getdefUseMap() {
      return this.defUseMap;
   }

   @Override
   public boolean visit(VariableDeclarationStatement varDecSta) {
      if (log) {
         System.out.println("\t Visited VarDeclStm: " + varDecSta.toString().trim());
      }
      for (Iterator<?> iter = varDecSta.fragments().iterator(); iter.hasNext();) {
         VariableDeclarationFragment varDecFra = (VariableDeclarationFragment) iter.next();
         IVariableBinding varBin = varDecFra.resolveBinding();

         if (varBin == null || varBin.getType().isPrimitive()) {
            continue;
         }
         // //Skip primitive type variables like int, float, etc.
         // if (varBin.getType().isPrimitive()) {
         // continue;
         // }
         DefUseModel defUseModel = new DefUseModel(varDecSta, varDecFra, this.compilationUnit);
         defUseMap.put(varBin, defUseModel);
      }
      return super.visit(varDecSta);
   }

   @Override
   public boolean visit(SingleVariableDeclaration varDec) {
      if (log) {
         System.out.println("\t Visited SingleVarD: " + varDec.toString().trim());
      }
      IVariableBinding varBin = varDec.resolveBinding();

      // Skip primitive type variables like int, float, etc.
      // if (varBin == null || varBin.getType().isPrimitive()) {
      // return true; // Continue the visit
      // }

      DefUseModel defUseModel = new DefUseModel(varDec, this.compilationUnit);
      defUseMap.put(varBin, defUseModel);
      return true;
   }

   @Override
   public boolean visit(SimpleName node) {
      // System.out.println("Visited SimpleName: " + node.getIdentifier());
      if (node.getParent() instanceof VariableDeclarationFragment || node.getParent() instanceof SingleVariableDeclaration) {
         return true;
      }
      IBinding binding = node.resolveBinding();
      if (binding == null) {
         return true;
      }
      if (defUseMap.containsKey(binding)) {
         defUseMap.get(binding).addUsedVars(node);
      }
      return super.visit(node);
   }

   @Override
   public boolean visit(QualifiedName node) {
      if (node.getQualifier() instanceof SimpleName) {
         SimpleName qualifier = (SimpleName) node.getQualifier();
         IBinding binding = qualifier.resolveBinding();
         // System.out.println("Visited QualifiedName: " + node.getFullyQualifiedName());

         if (binding != null && defUseMap.containsKey(binding)) {
            String qualifiedName = qualifier.getIdentifier() + "." + node.getName().getIdentifier();

            if (!node.getName().getIdentifier().equals(qualifier.getIdentifier())) {
               // System.out.println("[DBG] Usage of '" + qualifiedName + "' at line " + cu.getLineNumber(node.getStartPosition()));

               defUseMap.get(binding).addQualifiedName(qualifiedName);
            }
         }
      }
      return super.visit(node);
   }

   @Override
   public boolean visit(ArrayAccess node) {
      // Handle array access like 'dirs[index]'
      // System.out.println("Visited ArrayAccess: " + node);
      if (node.getArray() instanceof SimpleName) {
         SimpleName arrayName = (SimpleName) node.getArray();
         IBinding binding = arrayName.resolveBinding();
         if (binding instanceof IVariableBinding && defUseMap.containsKey(binding)) {
            defUseMap.get(binding).addUsedVars(arrayName);
         }
      }
      return super.visit(node);
   }

   @Override
   public boolean visit(MethodInvocation node) {
      // System.out.println("Visited MethodInvocation: " + node);
      /*      String methodName = node.getName().getIdentifier();
      String qualifierString = node.getExpression() != null ? node.getExpression().toString() : ""; // Check if it's static or not
      
      if (!qualifierString.isEmpty()) {
         if (UtilAST.isClassReference(node.getExpression())) {
            System.out.println("Static method call: " + qualifierString + "." + methodName + "()");
        } else {
            System.out.println("Instance method call: " + qualifierString + "." + methodName + "()");
        }
      }
      else {
         System.out.println("Regular method call: " + methodName + "()");
      }*/

      if (node.getExpression() != null && node.getExpression() instanceof SimpleName) {
         SimpleName expr = (SimpleName) node.getExpression();
         IBinding binding = expr.resolveBinding();
         if (binding instanceof IVariableBinding && defUseMap.containsKey(binding)) {
            defUseMap.get(binding).addMethodInvocation(node); // Add method invocation to the group
         }
      }
      return super.visit(node);
   }

   @Override
   public boolean visit(FieldAccess node) {
      // System.out.println("Visited FieldAccess: " + node.getName().getIdentifier());
      IVariableBinding varBinding = node.resolveFieldBinding();
      if (varBinding != null && defUseMap.containsKey(varBinding)) {
         defUseMap.get(varBinding).addFieldAccess(node); // Add field access to the group
      }
      return super.visit(node);
   }

}
