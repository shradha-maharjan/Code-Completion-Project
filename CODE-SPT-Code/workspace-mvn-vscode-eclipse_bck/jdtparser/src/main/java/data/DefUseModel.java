/**
 * @file VariableDefUseAnalysis.java
 */
package data;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

/**
 * @since JavaSE-1.8
 */
public class DefUseModel {
   private VariableDeclarationStatement vds;
   private VariableDeclarationFragment vdf;
   private SingleVariableDeclaration svd;
   private List<SimpleName> usedVars = new ArrayList<SimpleName>();
   private List<MethodInvocation> methodInvocations = new ArrayList<MethodInvocation>(); // Added for method invocations
   private List<FieldAccess> fieldAccesses = new ArrayList<FieldAccess>(); // Added for field accesses
   private List<String> qualifiedNames = new ArrayList<>();
   private CompilationUnit compilationUnit;

   public DefUseModel(VariableDeclarationStatement vds, VariableDeclarationFragment vdf) {
      this.vds = vds;
      this.vdf = vdf;
   }

   public DefUseModel(VariableDeclarationStatement vds, VariableDeclarationFragment vdf, CompilationUnit compilationUnit) {
      this.vds = vds;
      this.vdf = vdf;
      this.compilationUnit = compilationUnit;
   }
   
   public DefUseModel(SingleVariableDeclaration svd, CompilationUnit compilationUnit) {
       this.svd = svd;
       this.compilationUnit = compilationUnit;
   }

   // Getter for method parameters
   public SingleVariableDeclaration getSingleVarDecl() {
       return this.svd;
   }

   public VariableDeclarationStatement getVarDeclStmt() {
      return vds;
   }

   public VariableDeclarationFragment getVarDeclFrgt() {
      return this.vdf;
   }

   public List<SimpleName> getUsedVars() {
      return usedVars;
   }

   public void addUsedVars(SimpleName v) {
      usedVars.add(v);
   }
   
   public List<MethodInvocation> getMethodInvocations() { 
	      return methodInvocations;
	   }

	   public void addMethodInvocation(MethodInvocation method) { 
	      methodInvocations.add(method);
	   }

	   public List<FieldAccess> getFieldAccesses() { 
	      return fieldAccesses;
	   }

	   public void addFieldAccess(FieldAccess fieldAccess) {
	      fieldAccesses.add(fieldAccess);
	   }
	   
	// Method to add qualified names
	    public void addQualifiedName(String qualifiedName) {
	        qualifiedNames.add(qualifiedName);
	    }

	    public List<String> getQualifiedNames() {
	        return qualifiedNames;
	    }
	    
   public CompilationUnit getCompilationUnit() {
      return compilationUnit;
   }

   public void setCompilationUnit(CompilationUnit compilationUnit) {
      this.compilationUnit = compilationUnit;
   }

}
