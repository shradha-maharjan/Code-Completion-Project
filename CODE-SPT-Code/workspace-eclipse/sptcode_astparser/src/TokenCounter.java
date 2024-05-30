import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

public class TokenCounter {

    private static String formatCode(String codeLine) {
        if (codeLine.startsWith("\"") && codeLine.endsWith("\"")) {
            codeLine = codeLine.substring(1, codeLine.length() - 1);
        }
        codeLine = codeLine.replace("\\n", "\n").replace("\\\"", "\"");
        return "public class DummyClass {\n" + codeLine + "\n}";
    }

    public static void main(String[] args) {
        String javaFileName = "input/methods.txt";
        String outputFileName = "input/methods_outputs2.csv";

        Set<String> methodNames = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(javaFileName));
             FileWriter writer = new FileWriter(outputFileName)) {

            writer.write("Index,Method Name,#Tokens,#Method Calls\n");

            String line;
            int index = 1;

            while ((line = reader.readLine()) != null) {
                String formattedCode = formatCode(line);
                MethodData methodData = countTokens(formattedCode);

                String uniqueMethodName = methodData.methodName;
                if (methodNames.contains(uniqueMethodName)) {
                    uniqueMethodName += "_" + index; // Append index to make name unique
                }
                methodNames.add(uniqueMethodName);

                writer.write(index + "," + uniqueMethodName + "," + methodData.tokenCount + "," + methodData.methodCallCount + "\n");
                index++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static MethodData countTokens(String source) {
        ASTParser parser = ASTParser.newParser(AST.JLS14);
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
        parser.setCompilerOptions(options);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        TokenCounterVisitorForMethods visitor = new TokenCounterVisitorForMethods();
        cu.accept(visitor);

        return new MethodData(visitor.getMethodName(), visitor.getTokenCount(), visitor.getMethodCallCount());
    }
}

class TokenCounterVisitorForMethods extends ASTVisitor {
    private int tokenCount = 0;
    private int methodCallCount = 0;
    private String methodName = null;

    public boolean visit(MethodDeclaration node) {
        this.methodName = node.getName().getIdentifier();
        return true;
    }

    public boolean visit(MethodInvocation node) {
        methodCallCount++;
        return true;
    }

    public boolean visit(SimpleName node) {
        tokenCount++;
        return true;
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public int getMethodCallCount() {
        return methodCallCount;
    }

    public String getMethodName() {
        return methodName;
    }
}

class MethodData {
    String methodName;
    int tokenCount;
    int methodCallCount;

    MethodData(String methodName, int tokenCount, int methodCallCount) {
        this.methodName = methodName;
        this.tokenCount = tokenCount;
        this.methodCallCount = methodCallCount;
    }
}
