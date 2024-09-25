package visitor;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class MethodNameVisitor extends ASTVisitor {
    private String methodName = "unknown";

    @Override
    public boolean visit(MethodDeclaration node) {
        // Extract the method name
        this.methodName = node.getName().getIdentifier();
        return false; // We stop visiting once we have the method name
    }

    public String getMethodName() {
        return this.methodName;
    }
}