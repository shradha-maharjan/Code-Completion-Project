import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import util.UtilAST;
import visitor.DefUseASTVisitor;
import data.DefUseModel;

public class SimpleExampleDefUse {

    private static final String UNIT_NAME = "DummyClass";
    private static final String INPUT_FILE_PATH = "input/Training.txt";
    private static final String OUTPUT_FILE_PATH = "output/AnalysisOutput.txt";

    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new FileReader(INPUT_FILE_PATH));
             FileWriter writer = new FileWriter(OUTPUT_FILE_PATH)) {

            String line;
            while ((line = reader.readLine()) != null) {
                String formattedCode = formatCode(line);
       
                writer.write("Input Line:\n");
                writer.write(line + "\n\n");
                
                // Parse the formatted code using UtilAST (or ASTParser directly)
                ASTParser parser = UtilAST.parseSrcCode(formattedCode, UNIT_NAME + ".java");
                CompilationUnit cu = (CompilationUnit) parser.createAST(null);

                // Analyze the parsed code
                DefUseASTVisitor defUseVisitor = new DefUseASTVisitor(cu);
                cu.accept(defUseVisitor);

                // Directly write the analysis result to the output file
                writer.write("AST Analysis Output:\n");
                displayDefUsedView(defUseVisitor.getdefUseMap(), writer);
                writer.write("\n---\n\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Wraps each line of code inside a dummy class.
     */
    private static String formatCode(String codeSnippet) {
        return "public class " + UNIT_NAME + " {\n" + codeSnippet + "\n}";
    }
    
    static void displayDefUsedView(Map<IVariableBinding, DefUseModel> analysisDataMap, FileWriter writer) throws IOException {
        for (Entry<IVariableBinding, DefUseModel> entry : analysisDataMap.entrySet()) {
            IVariableBinding iBinding = entry.getKey();
            DefUseModel iVariableAnal = entry.getValue();
            StringBuilder output = new StringBuilder();

            String variableName = "";
            Set<String> distinctMethods = new HashSet<>(); // Set to store distinct method names

            // Check if it's a method parameter
            if (iVariableAnal.getSingleVarDecl() != null) {
                SingleVariableDeclaration paramDecl = iVariableAnal.getSingleVarDecl();
                variableName = paramDecl.getName().getIdentifier();
                
                // Add distinct method invocations
                for (MethodInvocation method : iVariableAnal.getMethodInvocations()) {
                    if (method.getExpression() instanceof SimpleName) {
                        SimpleName variable = (SimpleName) method.getExpression();
                        if (variable.getIdentifier().equals(variableName)) {
                            distinctMethods.add(method.getName().toString());
                        }
                    }
                }

                output.append("# Parameter: ").append(variableName).append(" (Use ")
                      .append(distinctMethods.size()).append(")\n");
                output.append(paramDecl.toString().replaceAll("\\r|\\n", "").trim()).append(";\n");
            }

            // Check if it's a variable declaration fragment
            if (iVariableAnal.getVarDeclFrgt() != null) {
                VariableDeclarationFragment varDecl = iVariableAnal.getVarDeclFrgt();
                VariableDeclarationStatement varDeclStmt = iVariableAnal.getVarDeclStmt();
                variableName = varDecl.getName().getIdentifier();
                
                // Add distinct method invocations
                for (MethodInvocation method : iVariableAnal.getMethodInvocations()) {
                    if (method.getExpression() instanceof SimpleName) {
                        SimpleName variable = (SimpleName) method.getExpression();
                        if (variable.getIdentifier().equals(variableName)) {
                            distinctMethods.add(method.getName().toString());
                        }
                    }
                }

                output.append("# Variable: ").append(variableName).append(" (Use ")
                      .append(distinctMethods.size()).append(")\n");
                output.append(varDeclStmt.toString().replaceAll("\\r|\\n", "").trim()).append("\n");
            }

            // Adding method invocations to the output
            for (MethodInvocation method : iVariableAnal.getMethodInvocations()) {
                if (method.getExpression() instanceof SimpleName) {
                    SimpleName variable = (SimpleName) method.getExpression();
                    if (variable.getIdentifier().equals(variableName)) {
                        output.append(variable.getIdentifier())
                              .append(".")
                              .append(method.getName())
                              .append("();"); // Mark it as an API call
                    }
                }
            }

            // Adding field accesses to the output (if needed)
            for (FieldAccess fieldAccess : iVariableAnal.getFieldAccesses()) {
                output.append(variableName)
                      .append(".")
                      .append(fieldAccess.getName());
            }

            // Adding qualified names to the output
            for (String qualifiedName : iVariableAnal.getQualifiedNames()) {
                output.append(qualifiedName).append("; ");
            }

            // Write the result to the output file
            writer.write(output.toString().trim() + "\n");
        }
    }
}   
    
/*
//    static void displayDefUsedView(Map<IVariableBinding, DefUseModel> analysisDataMap, FileWriter writer) throws IOException {
//        for (Entry<IVariableBinding, DefUseModel> entry : analysisDataMap.entrySet()) {
//            IVariableBinding iBinding = entry.getKey();
//            DefUseModel iVariableAnal = entry.getValue();
//            StringBuilder output = new StringBuilder();
//
//            String variableName = "";
//
//            // Check if it's a method parameter
//            if (iVariableAnal.getSingleVarDecl() != null) {
//                SingleVariableDeclaration paramDecl = iVariableAnal.getSingleVarDecl();
//                variableName = paramDecl.getName().getIdentifier();
//                output.append("# Parameter: ").append(variableName).append("\n");
//                output.append(paramDecl.toString().replaceAll("\\r|\\n", "").trim()).append(";\n");
//            }
//
//            // Check if it's a variable declaration fragment
//            if (iVariableAnal.getVarDeclFrgt() != null) {
//                VariableDeclarationFragment varDecl = iVariableAnal.getVarDeclFrgt();
//                VariableDeclarationStatement varDeclStmt = iVariableAnal.getVarDeclStmt();
//                variableName = varDecl.getName().getIdentifier();
//                output.append("# Variable: ").append(variableName).append("\n");
//                output.append(varDeclStmt.toString().replaceAll("\\r|\\n", "").trim()).append("\n");
//            }
//
//            // Adding method invocations to the output
//            for (MethodInvocation method : iVariableAnal.getMethodInvocations()) {
//                if (method.getExpression() instanceof SimpleName) {
//                    SimpleName variable = (SimpleName) method.getExpression();
//                    if (variable.getIdentifier().equals(variableName)) {
//                        output.append(variable.getIdentifier())
//                              .append(".")
//                              .append(method.getName())
//                              .append("();"); // Mark it as an API call
//                    }
//                }
//            }
//
//            // Adding field accesses to the output
//            for (FieldAccess fieldAccess : iVariableAnal.getFieldAccesses()) {
//                output.append(variableName)
//                      .append(".")
//                      .append(fieldAccess.getName());
//            }
//
//            // Adding qualified names to the output
//            for (String qualifiedName : iVariableAnal.getQualifiedNames()) {
//                output.append(qualifiedName).append("; ");
//            }
//
//            // Write the result to the output file
//            writer.write(output.toString().trim() + "\n");
//        }
//    }
}



//import org.eclipse.jdt.core.dom.ASTParser;
//import org.eclipse.jdt.core.dom.CompilationUnit;
//import org.eclipse.jdt.core.dom.FieldAccess;
//import org.eclipse.jdt.core.dom.IVariableBinding;
//import org.eclipse.jdt.core.dom.MethodInvocation;
//import org.eclipse.jdt.core.dom.SimpleName;
//import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
//import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//
//import util.UtilAST;
//import view.SimpleViewer;
//import visitor.DefUseASTVisitor; 
//import data.DefUseModel; 
//public class SimpleExampleDefUse {
//
//    static CompilationUnit cu;
//
//    public static void main(String args[]) {
//        String javaFilePath = "input/ClassA.java";
//
//        ASTParser parser = UtilAST.parse(javaFilePath);
//        cu = (CompilationUnit) parser.createAST(null);
//
//        DefUseASTVisitor defUseVisitor = new DefUseASTVisitor(cu);
//        cu.accept(defUseVisitor);
//
//        SimpleViewer viewer = new SimpleViewer();
//        displayDefUsedView(viewer, defUseVisitor.getdefUseMap());
//    }
//
//    static void displayDefUsedView(SimpleViewer viewer, Map<IVariableBinding, DefUseModel> analysisDataMap) {
//        for (Entry<IVariableBinding, DefUseModel> entry : analysisDataMap.entrySet()) {
//            IVariableBinding iBinding = entry.getKey();
//            DefUseModel iVariableAnal = entry.getValue();
//            VariableDeclarationStatement varDeclStmt = iVariableAnal.getVarDeclStmt();
//            VariableDeclarationFragment varDecl = iVariableAnal.getVarDeclFrgt();
//
//            StringBuilder output = new StringBuilder();
//            String variableName = varDecl.getName().getIdentifier();
//
//            // Header with variable name
//            output.append("# Group of '").append(variableName).append("' tracking the variable usage of '").append(variableName).append("':\n");
//            output.append(varDeclStmt.toString().replaceAll("\\r|\\n", "").trim());
//
//            // Adding method invocations to the output
//            List<MethodInvocation> methodInvocations = iVariableAnal.getMethodInvocations();
//            if (!methodInvocations.isEmpty()) {
//                for (MethodInvocation method : methodInvocations) {
//                    if (method.getExpression() instanceof SimpleName) {
//                        SimpleName variable = (SimpleName) method.getExpression();
//                        if (variable.getIdentifier().equals(variableName)) {
//                            output.append(variable.getIdentifier()) 
//                                  .append(".")
//                                  .append(method.getName()) 
//                                  .append("();"); // Mark it as an API call
//                        }
//                    }
//                }
//            }
//
//            // Adding field accesses to the output
//            List<FieldAccess> fieldAccesses = iVariableAnal.getFieldAccesses();
//            if (!fieldAccesses.isEmpty()) {
//                output.append("---\n");
//                output.append("Field accesses:\n");
//                for (FieldAccess fieldAccess : fieldAccesses) {
//                    output.append(variableName)
//                          .append(".")
//                          .append(fieldAccess.getName());
//                }
//            }
//
//            System.out.println(output.toString().trim());
//        }
//    }
//
//}


//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.ArrayList;
//import java.util.HashSet;
//
//import org.eclipse.jdt.core.dom.ASTParser;
//import org.eclipse.jdt.core.dom.ASTVisitor;
//import org.eclipse.jdt.core.dom.CompilationUnit;
//import org.eclipse.jdt.core.dom.IBinding;
//import org.eclipse.jdt.core.dom.IVariableBinding;
//import org.eclipse.jdt.core.dom.MethodInvocation;
//import org.eclipse.jdt.core.dom.SimpleName;
//import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
//import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
//
//import util.UtilAST;
//import view.SimpleViewer;
//
//public class SimpleExampleDefUse {
//    static CompilationUnit cu;
//
//    public static void main(String args[]) {
//        String javaFilePath = "input/ClassA.java";
//        ASTParser parser = UtilAST.parse(javaFilePath);
//        cu = (CompilationUnit) parser.createAST(null);
//
//        // Instantiate and apply the DefUseASTVisitor
//        DefUseASTVisitor defUseVisitor = new DefUseASTVisitor();
//        cu.accept(defUseVisitor);
//
//        // Display the DefUseView with a SimpleViewer
//        SimpleViewer viewer = new SimpleViewer();
//        displayDefUseView(viewer, defUseVisitor.getDefUseMap());
//    }
//
//    public static void displayDefUseView(SimpleViewer viewer, Map<IVariableBinding, DefUseModel> defUseMap) {
//        for (Map.Entry<IVariableBinding, DefUseModel> entry : defUseMap.entrySet()) {
//            DefUseModel defUseModel = entry.getValue();
//            VariableDeclarationStatement varDeclStmt = defUseModel.getVarDeclStmt();
//            VariableDeclarationFragment varDeclFrag = defUseModel.getVarDeclFrgt();
//
//            // Initialize the output with the variable declaration, but strip out any newline characters
//            StringBuilder output = new StringBuilder();
//            output.append(varDeclStmt.toString().replaceAll("\\r|\\n", ""));  // Remove any newlines and semicolon from the declaration
//
//            // Append the method invocations (if any)
//            List<SimpleName> usedVars = defUseModel.getUsedVars();
//            if (!usedVars.isEmpty()) {
//                output.append(" ");  // Add a space between the declaration and method invocations
//                for (SimpleName usedVar : usedVars) {
//                    output.append(varDeclFrag.getName().getFullyQualifiedName() + "." + usedVar.getFullyQualifiedName() + "(); ");
//                }
//            }
//
//            // Print the final output in a single line
//            System.out.println(output.toString().trim());
//        }
//    }
//
//
//
//
//    // Define DefUseASTVisitor to track variable declarations and usages
//    static class DefUseASTVisitor extends ASTVisitor {
//        private Map<IVariableBinding, DefUseModel> defUseMap = new HashMap<>();
//
//        @Override
//        public boolean visit(VariableDeclarationStatement node) {
//            for (Object fragment : node.fragments()) {
//                VariableDeclarationFragment vdf = (VariableDeclarationFragment) fragment;
//                IVariableBinding binding = vdf.resolveBinding();
//                DefUseModel defUseModel = new DefUseModel(node, vdf);
//                defUseMap.put(binding, defUseModel);
//            }
//            return super.visit(node);
//        }
//
//        @Override
//        public boolean visit(MethodInvocation node) {
//            if (node.getExpression() != null && node.getExpression() instanceof SimpleName) {
//                SimpleName expr = (SimpleName) node.getExpression();
//                IBinding binding = expr.resolveBinding();
//                if (binding instanceof IVariableBinding) {
//                    IVariableBinding varBinding = (IVariableBinding) binding;
//                    if (defUseMap.containsKey(varBinding)) {
//                        DefUseModel defUseModel = defUseMap.get(varBinding);
//                        defUseModel.addUsedVars(node.getName());
//                    }
//                }
//            }
//            return super.visit(node);
//        }
//
//        public Map<IVariableBinding, DefUseModel> getDefUseMap() {
//            return this.defUseMap;
//        }
//    }
//}
//
//class DefUseModel {
//    private VariableDeclarationStatement vds;
//    private VariableDeclarationFragment vdf;
//    private List<SimpleName> usedVars = new ArrayList<>();
//
//    public DefUseModel(VariableDeclarationStatement vds, VariableDeclarationFragment vdf) {
//        this.vds = vds;
//        this.vdf = vdf;
//    }
//
//    public void addUsedVars(SimpleName v) {
//        usedVars.add(v);
//    }
//
//    public List<SimpleName> getUsedVars() {
//        return usedVars;
//    }
//
//    public VariableDeclarationStatement getVarDeclStmt() {
//        return vds;
//    }
//
//    public VariableDeclarationFragment getVarDeclFrgt() {
//        return vdf;
//    }
//}


//
///**
// * @(#) SimpleExampleDefUse.java
// */
//import java.util.HashSet;
//import java.util.Set;
//
//import org.eclipse.jdt.core.dom.ASTParser;
//import org.eclipse.jdt.core.dom.ASTVisitor;
//import org.eclipse.jdt.core.dom.CompilationUnit;
//import org.eclipse.jdt.core.dom.IBinding;
//import org.eclipse.jdt.core.dom.IfStatement;
//import org.eclipse.jdt.core.dom.MethodDeclaration;
//import org.eclipse.jdt.core.dom.MethodInvocation;
//
//import util.UtilAST;
//
///**
// * @since J2SE-1.8
// */
//public class SimpleExampleDefUse {
//   static CompilationUnit cu;
//
//   public static void main(String args[]) {
//      String javaFilePath = "input/ClassA.java";
//      ASTParser parser = UtilAST.parse(javaFilePath);
//      cu = (CompilationUnit) parser.createAST(null);
//      MyVisitor myVisitor = new MyVisitor();
//      cu.accept(myVisitor);
//
//   }
//
//   static class MyVisitor extends ASTVisitor {
//      Set<IBinding> bindings = new HashSet<>();
//
//      // public boolean visit(VariableDeclarationFragment node) {
//      // SimpleName name = node.getName();
//      // // this.bindings.add(node.resolveBinding());
//      // // System.out.println("[DBG] Declaration of '" + name + "' at line " + cu.getLineNumber(name.getStartPosition()));
//      // System.out.println("[DBG] var: " + name);
//      // return true;
//      // }
//
//      public boolean visit(MethodDeclaration node) {
//         String qualifiedName = node.resolveBinding().getDeclaringClass().getQualifiedName();
//         System.out.println("[DBG] dec: " + qualifiedName + "." + node.getName().getFullyQualifiedName());
//         return true;
//      }
//
//      public boolean visit(IfStatement node) {
//         System.out.println("__IF " + node.getExpression());
//         return true;
//      }
//
//      public void endVisit(IfStatement node) {
//         System.out.println("IF__");
//      }
//
//      public boolean visit(MethodInvocation node) {
//         Object qualifiedName = node.resolveMethodBinding().getDeclaringClass().getName();
//         String name = node.getName().getFullyQualifiedName();
//         System.out.println("[DBG] inv: " + qualifiedName + "." + name);
//         return true;
//      }
//
//      /*public boolean visit(SimpleName node) {
//         if (node.getParent() instanceof VariableDeclarationFragment //
//               || node.getParent() instanceof SingleVariableDeclaration) {
//            return true;
//         }
//      
//         IBinding binding = node.resolveBinding();
//         if (binding != null && bindings.contains(binding)) {
//            System.out.println("[DBG] Usage of '" + node + "' at line " + cu.getLineNumber(node.getStartPosition()));
//            ASTNode declaringNode = cu.findDeclaringNode(binding);
//            System.out.println("[DBG] declaringNode: " + declaringNode);
//         }
//         return true;
//      }*/
//   }
//} */