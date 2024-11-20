package dataflow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;

import base.GlobalInfo;
import base.MainBaseClass;
import util.UtilAST;
import util.UtilFile;

public class MainDataFlowAnalysis extends MainBaseClass implements GlobalInfo {
    static CompilationUnit cu;

    private static final String UNIT_NAME = "DummyClass";

    private static String formatCode(String codeSnippet) {
        return "public class " + UNIT_NAME + " {\n" + codeSnippet + "\n}";
    }

    public static void main(String[] args) {
        // Use constants from GlobalInfo for file paths
        String javaFilePath = INPUT_DATA;
        String outputFilePath = DIR_OUTPUT + OUTPUT_DATA;

        try {
            // Use UtilFile to read the input file
            String[] lines = UtilFile.readFileArray(javaFilePath);
            List<String> outputLines = new ArrayList<>();

            for (String line : lines) {
                String formattedCode = formatCode(line);

                ASTParser parser = UtilAST.parseSrcCode(formattedCode, UNIT_NAME + ".java");
                cu = (CompilationUnit) parser.createAST(null);
                MyVisitor myVisitor = new MyVisitor();
                cu.accept(myVisitor);

                // Collect method calls to write to the output file
                myVisitor.collectMethodCalls(outputLines);
            }

            // Use UtilFile to save the output lines
            UtilFile.saveFile(outputFilePath, outputLines);

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

        public void collectMethodCalls(List<String> outputLines) {
            for (Map.Entry<String, Set<String>> entry : objectMethodCalls.entrySet()) {
                String objectName = entry.getKey();
                String declaration = objectDeclarations.getOrDefault(objectName, "null");
                Set<String> methods = entry.getValue();

                if ("null".equals(declaration) || "this".equals(objectName)) {
                    continue;
                }

                StringBuilder sb = new StringBuilder();
                sb.append(declaration).append("; ");

                for (String method : methods) {
                    sb.append(objectName).append(".").append(method).append("(); ");
                }
                outputLines.add(sb.toString());
            }
        }
    }
}


// package dataflow;

// import java.util.HashMap;
// import java.util.Map;
// import java.util.HashSet;
// import java.util.Set;

// import org.eclipse.jdt.core.dom.*;

// import base.GlobalInfo;
// import base.MainBaseClass;
// import util.UtilAST;
// import util.UtilFile;

// /*
// Modify this program to include a visit method for MethodInvocation.
// Given the USE infomation, extract the realted method calls.
// For example, given the object "oA" in "input/ClassA", the methods "oA.m1()" and "oA.m2()"; given the object "oB", the methods "oB.m3()" and "oB.m4()".

// The expected outputs: 2 rows 
// ObjectA oA = new ObjectA(); oA.m1(); oA.m2();
// ObjectB oB = new ObjectB(); oB.m3(); oB.m4();
// */
// public class MainDataFlowAnalysis {
//     static CompilationUnit cu;

//     public static void main(String args[]) {
//        String javaFilePath = System.getProperty("user.dir") + "/input/ClassA.java";
//        System.out.println("[DBG] INPUT PATH: " + javaFilePath);
//        ASTParser parser = UtilAST.parse(javaFilePath);
//        cu = (CompilationUnit) parser.createAST(null);
//        MyVisitor myVisitor = new MyVisitor();
//        cu.accept(myVisitor);

//        myVisitor.printMethodCalls();
 
//     }
 
//     static class MyVisitor extends ASTVisitor {
//        Set<IBinding> bindings = new HashSet<>();
//        Map<String, String> objectDeclarations = new HashMap<>();
//        Map<String, Set<String>> objectMethodCalls = new HashMap<>();

//     //    public boolean visit(VariableDeclarationStatement node) {
//     //       System.out.println("[DBG] Var Decl: " + node);
//     //       return true;
//     //    } 
//     public boolean visit(VariableDeclarationFragment node) {
//       SimpleName name = node.getName();
//       String initializer = node.getInitializer() != null ? node.getInitializer().toString() : "null";
//       String fragment = name + " = " + initializer;

//       bindings.add(node.resolveBinding());
//       objectDeclarations.put(name.getIdentifier(), fragment);

//       System.out.println("[DBG] Variable declaration fragment: '" + fragment + "' at line " + cu.getLineNumber(node.getStartPosition()));
//       return true;
//   }

//   @Override
//   public boolean visit(VariableDeclarationStatement node) {
//       // This method handles the full variable declaration
//       System.out.println("[DBG] Variable declaration: '" + node + "' at line " + cu.getLineNumber(node.getStartPosition()));
//       return true;
//   }

//   @Override
//   public boolean visit(SimpleName node) {
//       if (node.getParent() instanceof VariableDeclarationFragment) {
//           return true; // Skip the fragment declaration, already handled
//       }

//       IBinding binding = node.resolveBinding();
//       if (binding != null && bindings.contains(binding)) {
//           System.out.println("[DBG] Usage of '" + node + "' at line " + cu.getLineNumber(node.getStartPosition()));
//           ASTNode declaringNode = cu.findDeclaringNode(binding);
//           System.out.println("[DBG] declaringNode: " + declaringNode);
//       }
//       return true;
//   }

//   @Override
//   public boolean visit(MethodInvocation node) {
//       String objectName = node.getExpression() != null ? node.getExpression().toString() : "this";
//       String methodName = node.getName().toString();

//       // Collect method calls related to the object
//       objectMethodCalls.computeIfAbsent(objectName, k -> new HashSet<>()).add(methodName);

//       System.out.println("[DBG] Method call '" + objectName + "." + methodName + "' at line " + cu.getLineNumber(node.getStartPosition()));
//       return true;
//   }

//   public void printMethodCalls() {
//     for (Map.Entry<String, Set<String>> entry : objectMethodCalls.entrySet()) {
//         String objectName = entry.getKey();
//         String declaration = objectDeclarations.getOrDefault(objectName, "null");
//         Set<String> methods = entry.getValue();

//         // Skip entries where the declaration is null or not meaningful
//         if ("null".equals(declaration) || "this".equals(objectName)) {
//             continue;
//         }

//         // Print the declaration of the object/variable
//         System.out.print(declaration + "; ");

//         // Print method calls related to the object
//         for (String method : methods) {
//             System.out.print(objectName + "." + method + "(); ");
//         }
//         System.out.println();
//     }
// }
// }
// }
//          public boolean visit(VariableDeclarationFragment node) {
//          SimpleName name = node.getName();
//          String typeName = node.resolveBinding().getType().getName();
//          String objectDeclaration = typeName + " " + name.getIdentifier() + " = new " + typeName + "();";
//          this.bindings.add(node.resolveBinding());
//          objectDeclarations.put(name.getIdentifier(), objectDeclaration);

//          System.out.println("[DBG] Declaration of '" + name + "' at line " + cu.getLineNumber(name.getStartPosition()));
//          return true;
//   }
//       //  public boolean visit(VariableDeclarationFragment node) {
//       //     SimpleName name = node.getName();
//       //     this.bindings.add(node.resolveBinding());
//       //     System.out.println("[DBG] Declaration of '" + name + "' at line " + cu.getLineNumber(name.getStartPosition()));
//       //     return true;
//       //  }
 
//          public boolean visit(SimpleName node) {
//             if (node.getParent() instanceof VariableDeclarationFragment //
//                   || node.getParent() instanceof SingleVariableDeclaration) {
//                return true;
//             }
 
//           IBinding binding = node.resolveBinding();
//           if (binding != null && bindings.contains(binding)) {
//              System.out.println("[DBG] Usage of '" + node + "' at line " + cu.getLineNumber(node.getStartPosition()));
//              ASTNode declaringNode = cu.findDeclaringNode(binding);
//              System.out.println("[DBG] declaringNode: " + declaringNode);
//           }
//           return true;
//        }
//        @Override
//        public boolean visit(MethodInvocation node) {
//            String objectName = node.getExpression().toString();
//            String methodName = node.getName().toString();

//            // Collect method calls related to the object
//            objectMethodCalls.computeIfAbsent(objectName, k -> new HashSet<>()).add(methodName);

//            System.out.println("[DBG] Method call '" + objectName + "." + methodName + "' at line " + cu.getLineNumber(node.getStartPosition()));
//            return true;
//        }

//        public void printMethodCalls() {
//            for (Map.Entry<String, Set<String>> entry : objectMethodCalls.entrySet()) {
//                String objectName = entry.getKey();
//                String declaration = objectDeclarations.get(objectName);
//                Set<String> methods = entry.getValue();

//                System.out.print(declaration + " ");
//                for (String method : methods) {
//                    System.out.print(objectName + "." + method + "(); ");
//                }
//                System.out.println();
//            }
//        }
//    }
// }