import org.eclipse.jdt.core.dom.*;

public class SPTCODEASTVisitor extends ASTVisitor {

    private void print(String message) {
        System.out.println(message);
    }

    public boolean visit(MethodDeclaration node) {
        print("method_declaration");
        return true; 
    }

    public boolean visit(IfStatement node) {
        print("if_statement__");
        return true; 
    }

    public void endVisit(IfStatement node) {
        print("__if_statement");
    }

    public boolean visit(ReturnStatement node) {
        print("return_statement");
        return true; 
    }

    public boolean visit(CastExpression node) {
        print("cast_expression");
        return true; 
    }

    public boolean visit(ParenthesizedExpression node) {
        print("parenthesized_expression__");
        return true; 
    }

    public void endVisit(ParenthesizedExpression node) {
        print("__parenthesized_expression");
    }

    public boolean visit(MethodInvocation node) {
        print("method_invocation");
        return true; 
    }

    public boolean visit(InstanceofExpression node) {
        print("instanceof_expression");
        return true; 
    }

    public boolean visit(VariableDeclarationStatement node) {
        print("variable_declaration_statement");
        return true; 
    }

    public boolean visit(ForStatement node) {
        print("for_statement");
        return true; 
    }

    public boolean visit(EnhancedForStatement node) {
        print("enhanced_for_statement");
        return true; 
    }

    public boolean visit(SwitchStatement node) {
        print("switch_statement");
        return true; 
    }

    public boolean visit(TryStatement node) {
        print("try_statement");
        return true; 
    }

    public boolean visit(CatchClause node) {
        print("catch_clause");
        return true; 
    }
    
    public boolean visit(InfixExpression node) {
        print("binary_expression");
        return true; 
    }
    
    public boolean visit(ConditionalExpression node) {
        print("conditional_expression");
        return true; 
    }
    
    public boolean visit(DoStatement node) {
        print("do_statement");
        return true; 
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

