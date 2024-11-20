package dataflow;

import org.eclipse.jdt.core.dom.*;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import util.UtilAST;
import visitor.DefUseASTVisitor;
import visitor.MethodNameVisitor;
import data.DefUseModel;

public class MainExtractMethodCall {
    private static final String INPUT_DIR = "/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/pretrain_dataset_cleaned/";
    private static final String OUTPUT_DIR = "/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/output2/";

    private static final String UNIT_NAME = "ParsedAndToString";
    private static final String INPUT_FILE_PATH = INPUT_DIR + "pretrain-orgstr-singleline-combined.txt";
    private static final String OUTPUT_METHODS_FILE_PATH = OUTPUT_DIR + "methods_with_max_calls.txt";
    private static final String OUTPUT_LONGEST_SEQUENCES_PATH = OUTPUT_DIR + "longest_sequences.txt";
    private static final String OUTPUT_SINGLE_ACCESS_FILE_PATH = OUTPUT_DIR + "single_access_sequences.txt";

    public static void main(String[] args) {
        int counterLine = 0, counterControlFlow = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(INPUT_FILE_PATH));
                FileWriter methodsWriter = new FileWriter(OUTPUT_METHODS_FILE_PATH);
                FileWriter sequenceWriter = new FileWriter(OUTPUT_LONGEST_SEQUENCES_PATH);
                FileWriter singleAccessWriter = new FileWriter(OUTPUT_SINGLE_ACCESS_FILE_PATH)) {

            String line;
            int index = 1;

            while ((line = reader.readLine()) != null) {
                counterLine++;
                // String formattedCode = formatCode(line);
                // ASTParser parser = UtilAST.parseSrcCode(formattedCode, UNIT_NAME + ".java");

                ASTParser parser = UtilAST.parseSrcCode(line, UNIT_NAME + ".java");
                CompilationUnit cu = (CompilationUnit) parser.createAST(null);

                MethodNameVisitor methodNameVisitor = new MethodNameVisitor();
                cu.accept(methodNameVisitor);
                String methodName = methodNameVisitor.getMethodName();

                DefUseASTVisitor defUseVisitor = new DefUseASTVisitor(cu);
                cu.accept(defUseVisitor);

                MethodData methodData = processLongestSequence(defUseVisitor.getdefUseMap(), methodName, index, line);

                if (methodData.maxTotalCallsAndAccesses >= 2) {
                    methodsWriter.write(methodData.inputLine + "\n");
                    sequenceWriter.write(methodData.longestSequence + "\n");
                    counterControlFlow++;
                } else {
                    singleAccessWriter.write(methodData.inputLine + "\n");
                }

                index++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("[DBG] # of input lines: " + counterLine);
        System.out.println("[DBG] # of control flow: " + counterControlFlow);
    }

    // private static String formatCode(String codeSnippet) {
    // return "public class " + UNIT_NAME + " {\n" + codeSnippet + "\n}";
    // }

    static MethodData processLongestSequence(Map<IVariableBinding, DefUseModel> analysisDataMap, String methodName,
            int index, String inputLine) {
        StringBuilder longestSequence = new StringBuilder();
        String longestSequenceVariable = "";

        int maxMethodCalls = 0;
        int maxFieldAccesses = 0;
        int maxTotalCallsAndAccesses = 0;

        for (Entry<IVariableBinding, DefUseModel> entry : analysisDataMap.entrySet()) {
            IVariableBinding iBinding = entry.getKey();
            DefUseModel iVariableAnal = entry.getValue();

            String variableName = "";
            List<String> callSequence = new ArrayList<>();
            Set<String> distinctMethods = new HashSet<>();
            Set<String> distinctFieldAccesses = new HashSet<>();

            if (iVariableAnal.getSingleVarDecl() != null) {
                SingleVariableDeclaration paramDecl = iVariableAnal.getSingleVarDecl();
                variableName = paramDecl.getName().getIdentifier();
            }

            if (iVariableAnal.getVarDeclFrgt() != null) {
                VariableDeclarationFragment varDecl = iVariableAnal.getVarDeclFrgt();
                variableName = varDecl.getName().getIdentifier();
            }

            for (MethodInvocation method : iVariableAnal.getMethodInvocations()) {
                if (method.getExpression() instanceof SimpleName) {
                    SimpleName variable = (SimpleName) method.getExpression();
                    if (variable.getIdentifier().equals(variableName)) {
                        String methodCall = variable.getIdentifier() + "." + method.getName() + "()";
                        distinctMethods.add(methodCall);
                        callSequence.add(methodCall);
                    }
                }
            }

            for (FieldAccess fieldAccess : iVariableAnal.getFieldAccesses()) {
                String fieldAccessCall = variableName + "." + fieldAccess.getName().getIdentifier();
                distinctFieldAccesses.add(fieldAccessCall);
                callSequence.add(fieldAccessCall);
            }

            if (callSequence.size() > maxTotalCallsAndAccesses) {
                maxMethodCalls = distinctMethods.size();
                maxFieldAccesses = distinctFieldAccesses.size();
                maxTotalCallsAndAccesses = maxMethodCalls + maxFieldAccesses;
                longestSequence.setLength(0);
                longestSequence.append(String.join("; ", callSequence));
                longestSequenceVariable = variableName;
            }
        }

        return new MethodData(index, methodName, inputLine, maxMethodCalls, maxFieldAccesses, maxTotalCallsAndAccesses,
                longestSequence.toString());
    }

    static class MethodData {
        int index;
        String methodName;
        String inputLine;
        int maxMethodCalls;
        int maxFieldAccesses;
        int maxTotalCallsAndAccesses;
        String longestSequence;

        MethodData(int index, String methodName, String inputLine, int maxMethodCalls, int maxFieldAccesses,
                int maxTotalCallsAndAccesses, String longestSequence) {
            this.index = index;
            this.methodName = methodName;
            this.inputLine = inputLine;
            this.maxMethodCalls = maxMethodCalls;
            this.maxFieldAccesses = maxFieldAccesses;
            this.maxTotalCallsAndAccesses = maxTotalCallsAndAccesses;
            this.longestSequence = longestSequence;
        }
    }
}
