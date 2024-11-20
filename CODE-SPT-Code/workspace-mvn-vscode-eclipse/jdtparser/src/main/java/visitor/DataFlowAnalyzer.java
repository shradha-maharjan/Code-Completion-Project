package visitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class DataFlowAnalyzer extends ASTVisitor {
   Map<IBinding, Integer> counterVarUsage = new LinkedHashMap<>();
   Map<IBinding, List<Integer>> offsetOfVarUsage = new LinkedHashMap<>();

   CompilationUnit cu;
   String theLine, modifiedLine;
   Set<IBinding> bindings = new HashSet<>();

   public DataFlowAnalyzer(CompilationUnit cu) {
      this.cu = cu;
   }

   public DataFlowAnalyzer(CompilationUnit cu, String theLine, String modifiedMethod) {
      this.cu = cu;
      this.theLine = theLine;
      this.modifiedLine = modifiedMethod;
   }

   public boolean visit(VariableDeclarationFragment node) {
      SimpleName name = node.getName();
      IBinding binding = name.resolveBinding();
      if (binding == null) {
         return true;
      }
      int offset = node.getStartPosition();
      bindings.add(binding);
      counterVarUsage.put(binding, 0);
      offsetOfVarUsage.put(binding, new ArrayList<>());
      offsetOfVarUsage.get(binding).add(offset);
      // System.out.println("[DBG] Declaration of '" + name + "' at line " + cu.getLineNumber(name.getStartPosition()));
      return true;
   }

   public boolean visit(SingleVariableDeclaration node) {
      SimpleName name = node.getName();
      IBinding binding = name.resolveBinding();
      int offset = node.getName().getStartPosition();
      bindings.add(binding);
      counterVarUsage.put(binding, 0);
      offsetOfVarUsage.put(binding, new ArrayList<>());
      offsetOfVarUsage.get(binding).add(offset);
      // System.out.println("[DBG] Declaration2 of '" + name + "' at line " + cu.getLineNumber(name.getStartPosition()));
      return true;
   }

   public boolean visit(SimpleName node) {
      if (node.getParent() instanceof VariableDeclarationFragment || //
            node.getParent() instanceof SingleVariableDeclaration) {
         return true;
      }

      IBinding binding = node.resolveBinding();
      if (binding != null && bindings.contains(binding)) {
         int currentCount = counterVarUsage.get(binding) + 1;
         counterVarUsage.put(binding, currentCount);
         offsetOfVarUsage.get(binding).add(node.getStartPosition());
      }
      return true;
   }

   public Map<IBinding, Integer> getCounterVarUsage() {
      return counterVarUsage;
   }

   public Map<IBinding, List<Integer>> getOffsetOfVarUsage() {
      return offsetOfVarUsage;
   }

   public boolean checkDefUseRelationship() {
      boolean hasMultipleValues = false;
      if (offsetOfVarUsage.size() == 0) {
         System.out.println("[DBG] NOT OKAY");
         return false;
      }

      for (Map.Entry<IBinding, List<Integer>> entry : offsetOfVarUsage.entrySet()) {
         IBinding varBinding = entry.getKey();
         List<Integer> varUses = entry.getValue();
         if (varBinding != null && varUses != null && varUses.size() > 1) {
            hasMultipleValues = true;
            break;
         }
      }

      if (!hasMultipleValues) {
         System.out.println("[DBG] NOT OKAY");
      }
      return hasMultipleValues;
   }
}
