package visitor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import data.DefUseModel;

public class DefUseASTVisitor extends ASTVisitor {
    private Map<IVariableBinding, DefUseModel> defUseMap = new HashMap<>();
    private CompilationUnit compilationUnit;

    public DefUseASTVisitor(CompilationUnit compilationUnit) {
        this.compilationUnit = compilationUnit;
    }

    @Override
    public boolean visit(VariableDeclarationStatement varDecSta) {
        for (Iterator<?> iter = varDecSta.fragments().iterator(); iter.hasNext();) {
            VariableDeclarationFragment varDecFra = (VariableDeclarationFragment) iter.next();
            IVariableBinding varBin = varDecFra.resolveBinding();
            
            if (varBin == null || varBin.getType().isPrimitive()) {
                continue;
             }
        //   //Skip primitive type variables like int, float, etc.
	    //      if (varBin.getType().isPrimitive()) {
	    //          continue; 
	    //      }
            DefUseModel defUseModel = new DefUseModel(varDecSta, varDecFra, this.compilationUnit);
            defUseMap.put(varBin, defUseModel);
        }
        return super.visit(varDecSta);
    }
    
    @Override
    public boolean visit(SingleVariableDeclaration varDec) {
        IVariableBinding varBin = varDec.resolveBinding();

        // Skip primitive type variables like int, float, etc.
        if (varBin == null || varBin.getType().isPrimitive()) {
            return true; // Continue the visit
        }

        DefUseModel defUseModel = new DefUseModel(varDec, this.compilationUnit);
        defUseMap.put(varBin, defUseModel);
        return true;
    }

    @Override
    public boolean visit(SimpleName node) {
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

            if (binding != null && defUseMap.containsKey(binding)) {
                String qualifiedName = qualifier.getIdentifier() + "." + node.getName().getIdentifier();

                if (!node.getName().getIdentifier().equals(qualifier.getIdentifier())) {
                    //System.out.println("[DBG] Usage of '" + qualifiedName + "' at line " + cu.getLineNumber(node.getStartPosition()));
                    
                    defUseMap.get(binding).addQualifiedName(qualifiedName);  
                }
            }
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ArrayAccess node) {
        // Handle array access like 'dirs[index]'
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
        if (node.getExpression() != null && node.getExpression() instanceof SimpleName) {
            SimpleName expr = (SimpleName) node.getExpression();
            IBinding binding = expr.resolveBinding();
            if (binding instanceof IVariableBinding && defUseMap.containsKey(binding)) {
                defUseMap.get(binding).addMethodInvocation(node);  // Add method invocation to the group
            }
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(FieldAccess node) {
        IVariableBinding varBinding = node.resolveFieldBinding();
        if (varBinding != null && defUseMap.containsKey(varBinding)) {
            defUseMap.get(varBinding).addFieldAccess(node);  // Add field access to the group
        }
        return super.visit(node);
    }

    public Map<IVariableBinding, DefUseModel> getdefUseMap() {
        return this.defUseMap;
    }
}
