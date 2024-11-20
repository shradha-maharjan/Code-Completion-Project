package dataflow;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;

import base.GlobalInfo;
import base.MainBaseClass;
import util.UtilAST;
import util.UtilFile;

public class MainDataFlowAnalysis_test {
    static CompilationUnit cu;

    private static final String UNIT_NAME = "DummyClass";

    private static String formatCode(String codeSnippet) {
        return "public class " + UNIT_NAME + " {\n" + codeSnippet + "\n}";
    }

    public static void main(String args[]) {
        String javaFilePath = "input/test.txt";;
        String outputFilePath ="output/MethodCalls.txt";

        try (BufferedReader br = new BufferedReader(new FileReader(javaFilePath));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath))) {

            String line;
            while ((line = br.readLine()) != null) {

                String formattedCode = formatCode(line);

                ASTParser parser = UtilAST.parseSrcCode(formattedCode,UNIT_NAME + ".java");
                cu = (CompilationUnit) parser.createAST(null);
                MyVisitor myVisitor = new MyVisitor();
                cu.accept(myVisitor);

                // Write the method calls to the output file
                myVisitor.writeMethodCalls(bw);
            }

            System.out.println("[DBG] Output written to: " + outputFilePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class MyVisitor extends ASTVisitor {
        Set<IBinding> bindings = new HashSet<>();
        Map<String, String> objectDeclarations = new HashMap<>();
        Map<String, Set<String>> objectMethodCalls = new HashMap<>();

        @Override
        public boolean visit(VariableDeclarationFragment node) {
            SimpleName name = node.getName();
            String initializer = node.getInitializer() != null ? node.getInitializer().toString() : "null";
            String fragment = name + " = " + initializer;

            bindings.add(node.resolveBinding());
            objectDeclarations.put(name.getIdentifier(), fragment);

            System.out.println("[DBG] Variable declaration fragment: '" + fragment + "' at line " + cu.getLineNumber(node.getStartPosition()));
            return true;
        }

        @Override
        public boolean visit(VariableDeclarationStatement node) {
            System.out.println("[DBG] Variable declaration: '" + node + "' at line " + cu.getLineNumber(node.getStartPosition()));
            return true;
        }

        @Override
        public boolean visit(SimpleName node) {
            if (node.getParent() instanceof VariableDeclarationFragment) {
                return true;
            }

            IBinding binding = node.resolveBinding();
            if (binding != null && bindings.contains(binding)) {
                System.out.println("[DBG] Usage of '" + node + "' at line " + cu.getLineNumber(node.getStartPosition()));
                ASTNode declaringNode = cu.findDeclaringNode(binding);
                System.out.println("[DBG] declaringNode: " + declaringNode);
            }
            return true;
        }

        @Override
        public boolean visit(MethodInvocation node) {
            String objectName = node.getExpression() != null ? node.getExpression().toString() : "this";
            String methodName = node.getName().toString();

            objectMethodCalls.computeIfAbsent(objectName, k -> new HashSet<>()).add(methodName);

            System.out.println("[DBG] Method call '" + objectName + "." + methodName + "' at line " + cu.getLineNumber(node.getStartPosition()));
            return true;
        }

        public void writeMethodCalls(BufferedWriter bw) throws IOException {
            for (Map.Entry<String, Set<String>> entry : objectMethodCalls.entrySet()) {
                String objectName = entry.getKey();
                String declaration = objectDeclarations.getOrDefault(objectName, "null");
                Set<String> methods = entry.getValue();

                if ("null".equals(declaration) || "this".equals(objectName)) {
                    continue;
                }

                bw.write(declaration + "; ");

                for (String method : methods) {
                    bw.write(objectName + "." + method + "(); ");
                }
                bw.newLine();
            }
        }
    }
}

//     static class MyVisitor extends ASTVisitor {
//         Set<IBinding> bindings = new HashSet<>();
//         Map<String, String> objectDeclarations = new HashMap<>();
//         Map<String, Set<String>> objectMethodCalls = new HashMap<>();

//         public boolean visit(VariableDeclarationFragment node) {
//             SimpleName name = node.getName();
//             String initializer = node.getInitializer() != null ? node.getInitializer().toString() : "null";
//             String fragment = name + " = " + initializer;

//             bindings.add(node.resolveBinding());
//             objectDeclarations.put(name.getIdentifier(), fragment);

//             System.out.println("[DBG] Variable declaration fragment: '" + fragment + "' at line " + cu.getLineNumber(node.getStartPosition()));
//             return true;
//         }

//         @Override
//         public boolean visit(VariableDeclarationStatement node) {
//             System.out.println("[DBG] Variable declaration: '" + node + "' at line " + cu.getLineNumber(node.getStartPosition()));
//             return true;
//         }

//         @Override
//         public boolean visit(SimpleName node) {
//             if (node.getParent() instanceof VariableDeclarationFragment) {
//                 return true; // Skip the fragment declaration, already handled
//             }

//             IBinding binding = node.resolveBinding();
//             if (binding != null && bindings.contains(binding)) {
//                 System.out.println("[DBG] Usage of '" + node + "' at line " + cu.getLineNumber(node.getStartPosition()));
//                 ASTNode declaringNode = cu.findDeclaringNode(binding);
//                 System.out.println("[DBG] declaringNode: " + declaringNode);
//             }
//             return true;
//         }

//         @Override
//         public boolean visit(MethodInvocation node) {
//             String objectName = node.getExpression() != null ? node.getExpression().toString() : "this";
//             String methodName = node.getName().toString();

//             objectMethodCalls.computeIfAbsent(objectName, k -> new HashSet<>()).add(methodName);

//             System.out.println("[DBG] Method call '" + objectName + "." + methodName + "' at line " + cu.getLineNumber(node.getStartPosition()));
//             return true;
//         }

//         public void writeMethodCalls(FileWriter writer) throws IOException {
//             for (Map.Entry<String, Set<String>> entry : objectMethodCalls.entrySet()) {
//                 String objectName = entry.getKey();
//                 String declaration = objectDeclarations.getOrDefault(objectName, "null");
//                 Set<String> methods = entry.getValue();

//                 writer.write(declaration + "; ");
//                 for (String method : methods) {
//                     writer.write(objectName + "." + method + "(); ");
//                 }
//                 writer.write("\n");
//             }
//         }
//     }
// }
