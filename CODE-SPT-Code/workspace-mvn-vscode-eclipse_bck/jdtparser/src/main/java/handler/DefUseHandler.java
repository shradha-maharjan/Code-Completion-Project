/**
 * @file DefUseHandler.java
 */
package handler;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import analysis.ProjectAnalyzerDefUse;
import data.DefUseModel;
import view.SimpleViewer;

/**
 * @since JavaSE-1.8
 */
public class DefUseHandler {
   final String viewId = "simplebindingproject.partdescriptor.simplebindingview";

   @Execute
   public void execute(EPartService service) {
      MPart part = service.findPart(viewId);

      if (part != null && part.getObject() instanceof SimpleViewer) {
         ProjectAnalyzerDefUse analyzer = new ProjectAnalyzerDefUse();
         try {
            analyzer.analyze();
         } catch (CoreException e) {
            e.printStackTrace();
         }
         List<Map<IVariableBinding, DefUseModel>> analysisDataList = analyzer.getAnalysisDataList();

         SimpleViewer viewer = (SimpleViewer) part.getObject();
         displayDefUsedView(viewer, analysisDataList);
      }
   }

   private void displayDefUsedView(SimpleViewer viewer, List<Map<IVariableBinding, DefUseModel>> analysisDataList) {
      viewer.reset();
      int counter = 1;
      for (Map<IVariableBinding, DefUseModel> iMap : analysisDataList) {
         for (Entry<IVariableBinding, DefUseModel> entry : iMap.entrySet()) {
            IVariableBinding iBinding = entry.getKey();
            DefUseModel iVariableAnal = entry.getValue();
            CompilationUnit cUnit = iVariableAnal.getCompilationUnit();
            VariableDeclarationStatement varDeclStmt = iVariableAnal.getVarDeclStmt();
            VariableDeclarationFragment varDecl = iVariableAnal.getVarDeclFrgt();

            viewer.appendText("[" + (counter++) + "] ABOUT VARIABLES '" + varDecl.getName() + "'\n");
            String method = "[METHOD] " + iBinding.getDeclaringMethod() + "\n";
            viewer.appendText(method);
            String stmt = "\t[DECLARE STMT] " + strTrim(varDeclStmt) + "\t [" + getLineNum(cUnit, varDeclStmt) + "]\n";
            viewer.appendText(stmt);
            String var = "\t[DECLARE VAR] " + varDecl.getName() + "\t [" + getLineNum(cUnit, varDecl) + "]\n";
            viewer.appendText(var);

            List<SimpleName> usedVars = iVariableAnal.getUsedVars();
            for (SimpleName iSimpleName : usedVars) {

               ASTNode parentNode = iSimpleName.getParent();
               if (parentNode != null && parentNode instanceof Assignment) {
                  String assign = "\t\t[ASSIGN VAR] " + strTrim(parentNode) + "\t [" + getLineNum(cUnit, iSimpleName) + "]\n";
                  viewer.appendText(assign);
               } else {
                  String use = "\t\t[USE VAR] " + strTrim(parentNode) + "\t [" + getLineNum(cUnit, iSimpleName) + "]\n";
                  viewer.appendText(use);
               }
            }
         }
      }
   }

   String strTrim(Object o) {
      return o.toString().trim();
   }

   int getLineNum(CompilationUnit compilationUnit, ASTNode node) {
      return compilationUnit.getLineNumber(node.getStartPosition());
   }
}