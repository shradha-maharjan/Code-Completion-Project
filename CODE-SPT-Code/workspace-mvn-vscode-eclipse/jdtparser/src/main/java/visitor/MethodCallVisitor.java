package visitor;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.util.ArrayList;
import java.util.List;

public class MethodCallVisitor extends ASTVisitor {
    private final List<MethodInvocation> methodCalls = new ArrayList<>();

    @Override
    public boolean visit(MethodInvocation node) {
        System.out.println("offset:" + node.getStartPosition() + ", " + node.getName());

        String parent = "" + node.getParent().getParent().getParent().getParent();
        System.out.println(parent);
        System.out.println(parent.length());
        System.out.println("\"" + parent.substring(48, 52) + "\"");
        System.exit(0);

        methodCalls.add(node);  // Collect each method invocation
        return super.visit(node);
    }

    public List<MethodInvocation> getMethodCalls() {
        return methodCalls;
    }
}
