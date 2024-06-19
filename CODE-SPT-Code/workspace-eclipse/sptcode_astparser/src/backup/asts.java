package backup;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class asts {

    public static void main(String[] args) {
        String pathToFile = "input/finetune_methods_train_asts.txt";  // Replace with your file path
        System.out.println("Number of lines: " + countLines(pathToFile));
    }

    public static int countLines(String filename) {
        int lines = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            while (reader.readLine() != null) lines++;
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
        return lines;
    }
}


//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import org.eclipse.jdt.core.dom.*;
//
//public class asts {
//
//    private static String escapeJsonString(String codeSnippet) {
//        // Escaping for JSON string literals
//        return codeSnippet.replace("\\", "\\\\")
//                          .replace("\"", "\\\"")
//                          .replace("\n", "\\n")
//                          .replace("\r", "\\r")
//                          .replace("\t", "\\t");
//    }
//
//    public static void main(String[] args) {
//        String javaFileName = "input/methods.txt";  // Ensure this path is correct
//        String outputFileName = "input/output_final.txt";  // Output file path, with .jsonl extension
//
//        try (BufferedReader reader = new BufferedReader(new FileReader(javaFileName));
//             FileWriter writer = new FileWriter(outputFileName)) {
//
//            StringBuilder snippetBuilder = new StringBuilder();
//            String line;
//            boolean isCodeSnippet = false;
//
//            while ((line = reader.readLine()) != null) {
//                if (line.trim().startsWith("@") || isCodeSnippet) {
//                    snippetBuilder.append(line).append("\n");
//                    isCodeSnippet = true;
//                    if (line.trim().endsWith("}")) {
//                        isCodeSnippet = false;
//                    }
//                }
//
//                // Once a full snippet is collected
//                if (!isCodeSnippet && snippetBuilder.length() > 0) {
//                    String formattedCode = escapeJsonString(snippetBuilder.toString());
//                    ASTParser parser = ASTParser.newParser(AST.JLS_Latest);
//                    parser.setSource(snippetBuilder.toString().toCharArray());
//                    parser.setKind(ASTParser.K_COMPILATION_UNIT);
//
//                    CompilationUnit cu = (CompilationUnit) parser.createAST(null);
//                    final StringBuilder astOutput = new StringBuilder();
//                    cu.accept(new ASTVisitor() {
//                        @Override
//                        public boolean visit(MethodDeclaration node) {
//                            appendAstNode(astOutput, "MethodDeclaration");
//                            return true;
//                        }
//
//                        @Override
//                        public boolean visit(SingleVariableDeclaration node) {
//                            appendAstNode(astOutput, "SingleVariableDeclaration");
//                            return true;
//                        }
//
//                        // Add other AST nodes as needed
//                    });
//                    
//                    System.out.println("Formatted Code: " + formattedCode);
//                    System.out.println("AST Output: " + astOutput.toString());
//
//
//                    // Construct JSON object for each line
//                    writer.write("{\"CodeSnippet\": \"" + formattedCode + "\", \"ASTs\": \"" + astOutput.toString().trim() + "\"}\n");
//                    snippetBuilder.setLength(0); // Clear the builder for the next code snippet
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static void appendAstNode(StringBuilder astOutput, String nodeName) {
//        if (astOutput.length() > 0) astOutput.append(" ");
//        astOutput.append(nodeName);
//        System.out.println("Current ASTs: " + astOutput.toString());
//    }
//}