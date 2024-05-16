/**
 * @file VariableDefUseAnalysis.java
 */
package data;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

/**
 * @since JavaSE-1.8
 */
public class DefUseModel {
   private VariableDeclarationStatement vds;
   private VariableDeclarationFragment vdf;
   private List<SimpleName> usedVars = new ArrayList<SimpleName>();
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

   public CompilationUnit getCompilationUnit() {
      return compilationUnit;
   }

   public void setCompilationUnit(CompilationUnit compilationUnit) {
      this.compilationUnit = compilationUnit;
   }

}
