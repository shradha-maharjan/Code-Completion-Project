import org.eclipse.jdt.core.dom.*;

public class SPTCODEASTVisitor extends ASTVisitor {

    // Helper method to print messages
    private void print(String message) {
        System.out.println(message);
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        print("method_declaration");
        return true; // Continue to visit children
    }

    @Override
    public boolean visit(IfStatement node) {
        print("if_statement__");
        return true; // Continue to visit children
    }

    @Override
    public void endVisit(IfStatement node) {
        print("__if_statement");
    }

    @Override
    public boolean visit(ReturnStatement node) {
        print("return_statement");
        return true; // Continue to visit children
    }

    @Override
    public boolean visit(CastExpression node) {
        print("cast_expression");
        return true; // Continue to visit children
    }

    @Override
    public boolean visit(ParenthesizedExpression node) {
        print("parenthesized_expression__");
        return true; // Continue to visit children
    }

    @Override
    public void endVisit(ParenthesizedExpression node) {
        print("__parenthesized_expression");
    }

    @Override
    public boolean visit(MethodInvocation node) {
        print("method_invocation");
        return true; // Continue to visit children
    }

    @Override
    public boolean visit(InstanceofExpression node) {
        print("instanceof_expression");
        return true; // Continue to visit children
    }

    public static void main(String[] args) {
        String source = "public class Test {\n"
            + "    private boolean isApplicable(RepositoryResource resource) {\n"
            + "        if (resource instanceof ApplicableToProduct) {\n"
            + "            if (((ApplicableToProduct) resource).getAppliesTo() == null) {\n"
            + "                return true; // No appliesTo -> applicable\n"
            + "            }\n"
            + "        }\n"
            + "        return ((RepositoryResourceImpl) resource).doesResourceMatch(productDefinitions, null);\n"
            + "    }\n"
            + "}\n";

        ASTParser parser = ASTParser.newParser(AST.JLS_Latest);
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        cu.accept(new SPTCODEASTVisitor());
    }
}

