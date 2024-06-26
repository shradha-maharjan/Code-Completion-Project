package backup;
import java.io.FileReader;
import java.io.IOException;
import org.eclipse.jdt.core.dom.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONException; // Import JSONException
import org.eclipse.jdt.core.dom.*;

public class MethodTokenCounter {

    public static void main(String[] args) {
        String jsonFileName = "input/methods.jsonl"; // Path to your JSON file
        try (FileReader fileReader = new FileReader(jsonFileName)) {
            JSONTokener tokener = new JSONTokener(fileReader);
            JSONArray methodsArray = new JSONArray(tokener);

            for (int i = 0; i < methodsArray.length(); i++) {
                String methodCode = methodsArray.getString(i);
                String wrappedMethodCode = "class ClassTemp {\n" + methodCode + "\n}";
                int tokenCount = countTokens(wrappedMethodCode);
                System.out.println("Method " + (i + 1) + " token count: " + tokenCount);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) { // Catch JSONException
            System.err.println("Failed to parse JSON: " + e.getMessage());
        }
    }

    private static int countTokens(String source) {
        ASTParser parser = ASTParser.newParser(AST.JLS14);
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        TokenCounterVisitor visitor = new TokenCounterVisitor();
        cu.accept(visitor);

        return visitor.getTokenCount();
    }
}

class TokenCounterVisitor extends ASTVisitor {
    private int tokenCount = 0;

    @Override
    public boolean visit(SimpleName node) {
        tokenCount++;
        return super.visit(node);
    }

    public int getTokenCount() {
        return tokenCount;
    }
}
