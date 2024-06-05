package backup;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;

public class TokenCounter_org {

//	private static String formatCode(String codeLine) {
//	    System.out.println("Original: " + codeLine);
//	    if (codeLine.startsWith("\"") && codeLine.endsWith("\"")) {
//	        codeLine = codeLine.substring(1, codeLine.length() - 1);
//	        System.out.println("Trimmed Quotes: " + codeLine);
//	    }
//	    codeLine = codeLine.replace("\\n", "\n").replace("\\\"", "\"");
//	    System.out.println("Replaced Escapes: " + codeLine);
//
//	    return "public class DummyClass {\n" + codeLine + "\n}";
//	}
//	
	private static String formatCode(String codeLine) {
	    System.out.println("Original: " + codeLine);
	    if (codeLine.startsWith("\"") && codeLine.endsWith("\"")) {
	        codeLine = codeLine.substring(1, codeLine.length() - 1);
	        System.out.println("Trimmed Quotes: " + codeLine);
	    }
	    // Replace escaped characters with their actual character equivalents
	    codeLine = codeLine.replace("\\n", "\n")
	                       .replace("\\t", "\t")
	                       .replace("\\r", "\r")
	                       .replace("\\\"", "\"");
	    System.out.println("Replaced Escapes: " + codeLine);

	    return "public class DummyClass {\n" + codeLine + "\n}";
	}


	public static void main(String[] args) {
        String javaFileName = "input/longmethod.txt";
        String outputFileName = "input/methods_outputs3.csv"; // CSV output file path

        Set<String> methodNames = new HashSet<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(javaFileName));
             FileWriter writer = new FileWriter(outputFileName)) {
            
            // Write the CSV header
            writer.write("Index,Method Name,#Tokens\n");
            
            String line;
            int index = 1;

            while ((line = reader.readLine()) != null) {
            	String formattedCode = formatCode(line);
                MethodData methodData = countTokens(formattedCode);
                
                String uniqueMethodName = methodData.methodName;
                if (methodNames.contains(uniqueMethodName)) {
                    uniqueMethodName += "_" + index;  // Append index to make name unique
                }
                methodNames.add(uniqueMethodName);  // Add to set to track names

                writer.write(index + "," + uniqueMethodName + "," + methodData.tokenCount + "\n");
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

//        if (cu.getProblems().length > 0) {
//            System.out.println("Compilation problems found:");
//            for (IProblem problem : cu.getProblems()) {
//                System.out.printf("Error at line %d, column %d: %s\n", 
//                    problem.getSourceLineNumber(), 
//                    problem.getSourceStart(), 
//                    problem.getMessage());
//            }
//        }

        return new MethodData(visitor.getMethodName(), visitor.getTokenCount());
    }
}

class TokenCounterVisitorForMethods extends ASTVisitor {
    private int tokenCount = 0;
    private String methodName = null;

    @Override
    public boolean visit(MethodDeclaration node) {
        this.methodName = node.getName().getIdentifier();  // Capture the method name when visiting
      System.out.println("Visiting MethodDeclaration: " + node.getName());
        tokenCount++;
        return super.visit(node);  // Continue visiting children nodes
    }
    
    public boolean visit(SimpleName node) {
    	System.out.println("Token: " + node.getIdentifier());  // Print the token
        return true;  // Continue visiting other nodes
        
    }

    @Override
    public boolean visit(MethodInvocation node) {
    	System.out.println("Visiting MethodInvocation: " + node.getName());
        tokenCount++;
        return super.visit(node);
    }
    
    @Override
    public boolean visit(IfStatement node) {
    	System.out.println("Visiting IfStatement");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(ReturnStatement node) {
    	System.out.println("Visiting ReturnStatement");
    	tokenCount++;
        return super.visit(node);
    }
    
    @Override
    public boolean visit(CastExpression node) {
    	System.out.println("Visiting CastExpression");
    	tokenCount++;
        return super.visit(node);
    }
    
    @Override
    public boolean visit(ParenthesizedExpression node) {
    	System.out.println("Visiting ParenthesizedExpression");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(InstanceofExpression node) {
    	System.out.println("Visiting InstanceofExpression");
    	tokenCount++;
        return super.visit(node);
    }
    
    @Override
    public boolean visit(VariableDeclarationStatement node) {
    	System.out.println("Visiting VariableDeclarationStatement");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(ForStatement node) {
    	System.out.println("Visiting ForStatement");
    	tokenCount++;
        return super.visit(node);
    }
    
    @Override
    public boolean visit(EnhancedForStatement node) {
    	System.out.println("Visiting EnhancedForStatement");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(SwitchStatement node) {
    	System.out.println("Visiting SwitchStatement");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(TryStatement node) {
    	System.out.println("Visiting TryStatement");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(CatchClause node) {
    	System.out.println("Visiting CatchClause");
    	tokenCount++;
        return true; 
    }
    
    @Override
    public boolean visit(InfixExpression node) {
    	System.out.println("Visiting InfixExpression");
    	tokenCount++;
        return super.visit(node); 
    }
   
    @Override
    public boolean visit(ConditionalExpression node) {
    	System.out.println("Visiting ConditionalExpression");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(DoStatement node) {
    	System.out.println("Visiting DoStatement");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(LambdaExpression node) {
    	System.out.println("Visiting LambdaExpression");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(AnonymousClassDeclaration node) {
    	System.out.println("Visiting AnonymousClassDeclaration");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(ArrayCreation node) {
    	System.out.println("Visiting ArrayCreation");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(AssertStatement node) {
    	System.out.println("Visiting AssertStatement");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(Assignment node) {
    	System.out.println("Visiting Assignment");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(BreakStatement node) {
    	System.out.println("Visiting BreakStatement");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(ContinueStatement node) {
    	System.out.println("Visiting ContinueStatement");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(PostfixExpression node) {
    	System.out.println("Visiting PostfixExpression");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(PrefixExpression node) {
    	System.out.println("Visiting PrefixExpression");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(EmptyStatement node) {
    	System.out.println("Visiting EmptyStatement");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(FieldDeclaration node) {
    	System.out.println("Visiting FieldDeclaration");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(ExpressionStatement node) {
    	System.out.println("Visiting ExpressionStatement");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(LabeledStatement node) {
    	System.out.println("Visiting LabeledStatement");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(ClassInstanceCreation node) {
    	System.out.println("Visiting ClassInstanceCreation");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(SwitchExpression node) {
    	System.out.println("Visiting SwitchExpression");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(SynchronizedStatement node) {
    	System.out.println("Visiting SynchronizedStatement");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(ThrowStatement node) {
    	System.out.println("Visiting ThrowStatement");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(SingleVariableDeclaration node) {
    	System.out.println("Visiting SingleVariableDeclaration");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(WhileStatement node) {
    	System.out.println("Visiting WhileStatement");
    	tokenCount++;
        return super.visit(node); 
    }
    
    @Override
    public boolean visit(YieldStatement node) {
    	System.out.println("Visiting YieldStatement");
    	tokenCount++;
        return super.visit(node); 
    }
    
    public int getTokenCount() {
        return tokenCount;
    }
    
    public String getMethodName() {
        return methodName;
    }
}

class MethodData {
    String methodName;
    int tokenCount;

    MethodData(String methodName, int tokenCount) {
        this.methodName = methodName;
        this.tokenCount = tokenCount;
    }
}
