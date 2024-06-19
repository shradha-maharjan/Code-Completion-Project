import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.eclipse.jdt.core.dom.*;

public class pred {

    private static String formatCode(String codeSnippet) {
        codeSnippet = codeSnippet.replaceAll(" _ ", "")
                                 .replaceAll(" <", "<")
                                 .replaceAll(" >", ">")
                                 .replaceAll("< ", "<")
                                 .replaceAll("> ", ">")
                                 .replaceAll(" \\[ \\]", "[]");
        return "public class DummyClass {\n" + codeSnippet + "\n}";
    }

    public static void main(String[] args) {
        String javaFileName = "input/input2.txt";
        String outputFileName = "output/output2.csv";

        try (BufferedReader reader = new BufferedReader(new FileReader(javaFileName));
             FileWriter writer = new FileWriter(outputFileName)) {

            StringBuilder snippetBuilder = new StringBuilder();
            writer.write("Index,Method Name,Prediction\n");  // Updated CSV header to include index
            int index = 1; // Initialize index counter

            String line;
            while ((line = reader.readLine()) != null) {
                snippetBuilder.append(line).append("\n");
                if (line.contains(";")) {
                    String formattedCode = formatCode(snippetBuilder.toString());
                    ASTParser parser = ASTParser.newParser(AST.JLS_Latest);
                    parser.setSource(formattedCode.toCharArray());
                    parser.setKind(ASTParser.K_COMPILATION_UNIT);

                    CompilationUnit cu = (CompilationUnit) parser.createAST(null);
                    StringBuilder astOutput = new StringBuilder();
                    MethodNameVisitor visitor = new MethodNameVisitor(astOutput);
                    cu.accept(visitor);

                    writer.write(index + "," + visitor.getMethodName() + "," + astOutput.toString().trim() + "\n");
                    snippetBuilder.setLength(0); // Reset the builder after processing
                    index++; // Increment index for the next entry
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class MethodNameVisitor extends ASTVisitor {
        private StringBuilder astOutput;
        private String methodName;

        public MethodNameVisitor(StringBuilder astOutput) {
            this.astOutput = astOutput;
        }

        @Override
        public boolean visit(MethodDeclaration node) {
            methodName = node.getName().getIdentifier(); // Capture the method name
            return true; // continue to visit children
        }

        @Override
        public boolean visit(SimpleName node) {
            if ("m02".equals(node.getIdentifier())) {
                astOutput.append("m02 found as ").append(node.getParent().getClass().getSimpleName());
            }
            return true;
        }

        public String getMethodName() {
            return methodName == null ? "Anonymous" : methodName; // Handle cases with no methods
        }
    }
}


//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import org.eclipse.jdt.core.dom.*;
//public class pred {
//
//	private static String formatCode(String codeSnippet) {
//	    // Replace " _ " with nothing to correct method and variable names
//	    codeSnippet = codeSnippet.replaceAll(" _ ", "")
//	                             .replaceAll(" <", "<")
//	                             .replaceAll(" >", ">")
//	                             .replaceAll("< ", "<")
//	                             .replaceAll("> ", ">")
//	                             .replaceAll(" \\[ \\]", "[]");
//
//	    // Wrap the cleaned up snippet in a dummy class structure
//	    return "public class DummyClass {\n" + codeSnippet + "\n}";
//	}
//
//    public static void main(String[] args) {
//        String javaFileName = "input/data.TargetType.seq.train.source.txt";
//        String outputFileName = "input/data.TargetType.seq.train.source-ast.txt";
//
//        try (BufferedReader reader = new BufferedReader(new FileReader(javaFileName));
//             FileWriter writer = new FileWriter(outputFileName)) {
//
//            StringBuilder snippetBuilder = new StringBuilder();
//            String line;
//            while ((line = reader.readLine()) != null) {
//                snippetBuilder.append(line).append("\n");
//                // Assuming the end of a snippet or Java statement
//                if (line.contains(";")) {
//                    String formattedCode = formatCode(snippetBuilder.toString());
//                    ASTParser parser = ASTParser.newParser(AST.JLS_Latest);
//                    parser.setSource(formattedCode.toCharArray());
//                    parser.setKind(ASTParser.K_COMPILATION_UNIT);
//
//                    CompilationUnit cu = (CompilationUnit) parser.createAST(null);
//                    StringBuilder astOutput = new StringBuilder();
//                    cu.accept(new CustomASTVisitor(astOutput));
//                    writer.write(astOutput.toString().trim() + "\n");
//                    snippetBuilder.setLength(0); // Reset the builder after processing
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    static class CustomASTVisitor extends ASTVisitor {
//        private StringBuilder astOutput;
//
//        public CustomASTVisitor(StringBuilder astOutput) {
//            this.astOutput = astOutput;
//        }
//
//        @Override
//        public boolean visit(SimpleName node) {
//            if ("PRED".equals(node.getIdentifier())) {
//                astOutput.append("PRED found as").append(node.getParent().getClass().getSimpleName()).append("\n");
//            }
//            return true;
//        }
//}
//}