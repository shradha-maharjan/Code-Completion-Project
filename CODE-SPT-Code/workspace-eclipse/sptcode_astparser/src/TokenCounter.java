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

//import MethodData;
//import TokenCounterVisitorForMethods;

public class TokenCounter {

	private static String formatCode(String codeLine) {
//	    System.out.println("Original: " + codeLine);
		if (codeLine.startsWith("\"") && codeLine.endsWith("\"")) {
			codeLine = codeLine.substring(1, codeLine.length() - 1);
//	        System.out.println("Trimmed Quotes: " + codeLine);
		}
		codeLine = codeLine.replace("\\n", "\n").replace("\\\"", "\"");
//	    System.out.println("Replaced Escapes: " + codeLine);

		return "public class DummyClass {\n" + codeLine + "\n}";
	}

	public static void main(String[] args) {
		String javaFileName = "input/methods.txt";
		String outputFileName = "input/methods_outputs2.csv"; // CSV output file path

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
					uniqueMethodName += "_" + index; // Append index to make name unique
				}
				methodNames.add(uniqueMethodName); // Add to set to track names

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

	public boolean visit(MethodDeclaration node) {
		this.methodName = node.getName().getIdentifier(); // Capture the method name when visiting
//      System.out.println("Visiting MethodDeclaration: " + node.getName());
		return true;
	}

	public boolean visit(SimpleName node) {
		tokenCount++;
		return true;
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
