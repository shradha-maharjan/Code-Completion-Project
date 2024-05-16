/**
 */
package analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IVariableBinding;

import data.DefUseModel;
import util.UtilAST;
import visitor.DefUseASTVisitor;

public class ProjectAnalyzerDefUse {
   private static final String JAVANATURE = "org.eclipse.jdt.core.javanature";
   IProject[] projects;

   private List<Map<IVariableBinding, DefUseModel>> dataList = null;

   public ProjectAnalyzerDefUse() {
      dataList = new ArrayList<Map<IVariableBinding, DefUseModel>>();
   }

   public void analyze() throws CoreException {
      // =============================================================
      // 1st step: Project
      // =============================================================
      projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
      for (IProject project : projects) {
         if (!project.isOpen() || !project.isNatureEnabled(JAVANATURE)) { // Check if we have a Java project.
            continue;
         }
         analyzePackages(JavaCore.create(project).getPackageFragments());
      }
   }

   protected void analyzePackages(IPackageFragment[] packages) throws CoreException, JavaModelException {
      // =============================================================
      // 2nd step: Packages
      // =============================================================
      for (IPackageFragment iPackage : packages) {
         if (iPackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
            if (iPackage.getCompilationUnits().length < 1) {
               continue;
            }
            analyzeCompilationUnit(iPackage.getCompilationUnits());
         }
      }
   }

   private void analyzeCompilationUnit(ICompilationUnit[] iCompilationUnits) throws JavaModelException {
      // =============================================================
      // 3rd step: ICompilationUnits
      // =============================================================
      for (ICompilationUnit iUnit : iCompilationUnits) {
         CompilationUnit compilationUnit = UtilAST.parse(iUnit);
         DefUseASTVisitor javaASTvisitor = new DefUseASTVisitor(compilationUnit);
         compilationUnit.accept(javaASTvisitor);

         // Map<IVariableBinding, DefUseModel> getdefUseMap = javaASTvisitor.getdefUseMap();
         //
         // for (Entry<IVariableBinding, ?> entry : getdefUseMap.entrySet()) {
         // IVariableBinding binding = entry.getKey();
         // ASTNode declaringNode = compilationUnit.findDeclaringNode(binding);
         // System.out.println("DECLARING NODE: " + declaringNode.toString() + ", LOCATION: [" + declaringNode.getStartPosition() + "]");
         //
         // IJavaElement javaElement = binding.getJavaElement();
         // String elementName = javaElement.getElementName();
         // System.out.println("\t ELEMENT NAME: " + elementName);
         // }

         dataList.add(javaASTvisitor.getdefUseMap());
      }
   }

   public List<Map<IVariableBinding, DefUseModel>> getAnalysisDataList() {
      return dataList;
   }
}