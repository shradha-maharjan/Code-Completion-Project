import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.eclipse.jdt.core.dom.*;

public class pred2 {

    private static String formatCode(String codeSnippet) {
        codeSnippet = codeSnippet.replaceAll(" _ ", "")
                                 .replaceAll(" <", "<")
                                 .replaceAll(" >", ">")
                                 .replaceAll("< ", "<")
                                 .replaceAll("> ", ">");
        return "public class DummyClass {\n" + codeSnippet + "\n}";
    }

    public static void main(String[] args) {
        String javaFileName = "input/data.TargetType.seq.train.source.txt";
        int maxRowsPerFile = 1048576;
        int fileCounter = 1;
        String outputFileNamePrefix = "output/data.TargetType.seq.train.source";

        try (BufferedReader reader = new BufferedReader(new FileReader(javaFileName))) {
            FileWriter writer = new FileWriter(outputFileNamePrefix + fileCounter + ".csv");
            writer.write("Index,Method Name,Prediction\n");
            StringBuilder snippetBuilder = new StringBuilder();
            int index = 1;

            String line;
            while ((line = reader.readLine()) != null) {
                snippetBuilder.append(line).append("\n");
                if (line.contains(";")) { // Consider adjusting this condition
                    String formattedCode = formatCode(snippetBuilder.toString());
                    ASTParser parser = ASTParser.newParser(AST.JLS_Latest);
                    parser.setSource(formattedCode.toCharArray());
                    parser.setKind(ASTParser.K_COMPILATION_UNIT);

                    CompilationUnit cu = (CompilationUnit) parser.createAST(null);
                    StringBuilder astOutput = new StringBuilder();
                    MethodNameVisitor visitor = new MethodNameVisitor(astOutput);
                    cu.accept(visitor);

                    if (index > maxRowsPerFile) {
                        writer.close();
                        fileCounter++;
                        writer = new FileWriter(outputFileNamePrefix + fileCounter + ".csv");
                        writer.write("Index,Method Name,Prediction\n");
                        index = 1;
                    }

                    writer.write(index + "," + visitor.getMethodName() + "," + astOutput.toString().trim() + "\n");
                    writer.flush(); // Ensure data is written to the file system
                    snippetBuilder.setLength(0);
                    index++;
                }
            }
            writer.close(); // Ensure the file writer is closed properly
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
            methodName = node.getName().getIdentifier();
            return true;
        }

        @Override
        public boolean visit(SimpleName node) {
            if ("PRED".equals(node.getIdentifier())) {
                astOutput.append(node.getParent().getClass().getSimpleName());
            }
            return true;
        }

        public String getMethodName() {
            return methodName == null ? "Anonymous" : methodName;
        }
    }
}

//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import org.eclipse.jdt.core.dom.*;
//
//public class pred2 {
//
//    private static String formatCode(String codeSnippet) {
//        codeSnippet = codeSnippet.replaceAll(" _ ", "")
//                                 .replaceAll(" <", "<")
//                                 .replaceAll(" >", ">")
//                                 .replaceAll("< ", "<")
//                                 .replaceAll("> ", ">");
//        return "public class DummyClass {\n" + codeSnippet + "\n}";
//    }
//
//    public static void main(String[] args) {
//        String javaFileName = "input/data.TargetType.seq.train.source.txt";
//        int maxRowsPerFile = 1000000;
//        int fileCounter = 1;
//        String outputFileNamePrefix = "output/data.TargetType.seq.train.source";
//
//        try (BufferedReader reader = new BufferedReader(new FileReader(javaFileName))) {
//
//            FileWriter writer = new FileWriter(outputFileNamePrefix + fileCounter + ".csv");
//            writer.write("Index,Method Name,Prediction\n");
//            StringBuilder snippetBuilder = new StringBuilder();
//            int index = 1;
//
//            String line;
//            while ((line = reader.readLine()) != null) {
//                snippetBuilder.append(line).append("\n");
//                if (line.contains(";")) {
//                    String formattedCode = formatCode(snippetBuilder.toString());
//                    ASTParser parser = ASTParser.newParser(AST.JLS_Latest);
//                    parser.setSource(formattedCode.toCharArray());
//                    parser.setKind(ASTParser.K_COMPILATION_UNIT);
//
//                    CompilationUnit cu = (CompilationUnit) parser.createAST(null);
//                    StringBuilder astOutput = new StringBuilder();
//                    MethodNameVisitor visitor = new MethodNameVisitor(astOutput);
//                    cu.accept(visitor);
//
//                    if (index > maxRowsPerFile) {
//                        writer.close();
//                        fileCounter++;
//                        writer = new FileWriter(outputFileNamePrefix + fileCounter + ".csv");
//                        writer.write("Index,Method Name,Prediction\n");
//                        index = 1;  // Reset index for new file
//                    }
//
//                    writer.write(index + "," + visitor.getMethodName() + "," + astOutput.toString().trim() + "\n");
//                    snippetBuilder.setLength(0); // Reset the builder after processing
//                    index++; // Increment index for the next entry
//                }
//            }
//            writer.close(); // Close the last writer
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    static class MethodNameVisitor extends ASTVisitor {
//        private StringBuilder astOutput;
//        private String methodName;
//
//        public MethodNameVisitor(StringBuilder astOutput) {
//            this.astOutput = astOutput;
//        }
//
//        @Override
//        public boolean visit(MethodDeclaration node) {
//            methodName = node.getName().getIdentifier();
//            return true;
//        }
//
//        @Override
//        public boolean visit(SimpleName node) {
//            if ("PRED".equals(node.getIdentifier())) {
//                astOutput.append(node.getParent().getClass().getSimpleName());
//            }
//            return true;
//        }
//
//        public String getMethodName() {
//            return methodName == null ? "Anonymous" : methodName;
//        }
//    }
//}
