import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;

public class SimpleExampleDefUse2 {

    public static void main(String[] args) {
        String source = 
            "package pkg1;\n" +
            "public class ClassA {\n" +
            "   public void foo(int position, String buffer) {\n" +
            "      String data = buffer;\n" +
            "      int index = position / 100;\n" +
            "\n" +
            "      if (index < 0) {\n" +
            "         index = -1 * index;\n" +
            "         System.out.println(data.charAt(index));\n" +
            "      } else {\n" +
            "         index = index + 1;\n" +
            "         System.out.println(data.charAt(index));\n" +
            "      }\n" +
            "   }\n" +
            "}\n";

        ASTParser parser = ASTParser.newParser(AST.JLS_Latest);  // use the latest Java Language Specification
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);  // enable bindings to resolve names

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);  // parse the code

        cu.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                System.out.println("Method Name: " + node.getName());
                return true;  // to explore child nodes
            }

            public boolean visit(Statement node) {
                System.out.println("Statement: " + node);
                return super.visit(node);
            }
        });
    }
}
