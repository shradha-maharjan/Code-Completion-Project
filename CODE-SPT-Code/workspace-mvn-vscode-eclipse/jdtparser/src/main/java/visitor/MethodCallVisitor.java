package visitor;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.util.ArrayList;
import java.util.List;

public class MethodCallVisitor extends ASTVisitor {
    private final List<MethodInvocation> methodCalls = new ArrayList<>();

    @Override
    public boolean visit(MethodInvocation node) {
        methodCalls.add(node);  // Collect each method invocation
        return super.visit(node);
    }

    public List<MethodInvocation> getMethodCalls() {
        return methodCalls;
    }
}
