package dataflow;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.eclipse.jdt.core.dom.*;

import util.UtilAST;

public class SimpleExampleDefUseImp2 {
    private static final String UNIT_NAME = "DummyClass";
    private static final String INPUT_FILE_PATH = "output/single_access_sequences.txt";
    private static final String OUTPUT_FILE_PATH = "output/single_access_output.txt";

    public static void main(String[] args) {
        int noUsageCount = 0; 

        try (BufferedReader reader = new BufferedReader(new FileReader(INPUT_FILE_PATH));
             FileWriter writer = new FileWriter(OUTPUT_FILE_PATH)) {

            String line;
            int index = 1;

            while ((line = reader.readLine()) != null) {
                System.out.println("\nProcessing line " + index + ": " + line);
                String formattedCode = formatCode(line);

                ASTParser parser = UtilAST.parseSrcCode(formattedCode, UNIT_NAME + ".java");
                CompilationUnit cu = (CompilationUnit) parser.createAST(null);

                SimpleExampleDefUseImp2 example = new SimpleExampleDefUseImp2();
                
                example.longestUsageCounts.clear();
                example.usageOffsets.clear();

                MyVisitor myVisitor = example.new MyVisitor(cu);
                cu.accept(myVisitor);

                if (example.longestUsageCounts.isEmpty()) {
                    noUsageCount++;
                }

                int offsetAdjustment = formattedCode.indexOf(line);

                String maskedLine = example.maskVariableWithLongestUsageSequence(line, offsetAdjustment);

                writer.write(maskedLine + "\n");
                index++;
            }

            System.out.println("\nTotal lines without variable usage: " + noUsageCount);
            System.out.println("Masked code saved to " + OUTPUT_FILE_PATH);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String formatCode(String codeSnippet) {
        return "public class " + UNIT_NAME + " {\n" + codeSnippet + "\n}";
    }

    Map<IBinding, Integer> longestUsageCounts = new HashMap<>();
    Map<IBinding, List<Integer>> usageOffsets = new HashMap<>();

    class MyVisitor extends ASTVisitor {
        private final CompilationUnit cu;
        Set<IBinding> bindings = new HashSet<>();

        public MyVisitor(CompilationUnit cu) {
            this.cu = cu;
        }

        public boolean visit(VariableDeclarationFragment node) {
            SimpleName name = node.getName();
            IBinding binding = name.resolveBinding();
            bindings.add(binding);
            longestUsageCounts.put(binding, 0); // Initialize the longest sequence count for each variable
            usageOffsets.put(binding, new ArrayList<>()); // Initialize offsets list
            System.out.println("[DBG] Declaration of '" + name + "' at line " + cu.getLineNumber(name.getStartPosition()));
            return true;
        }

        public boolean visit(SimpleName node) {
            if (node.getParent() instanceof VariableDeclarationFragment ||
                node.getParent() instanceof SingleVariableDeclaration) {
                return true;
            }

            IBinding binding = node.resolveBinding();
            if (binding != null && bindings.contains(binding)) {
                int currentCount = longestUsageCounts.get(binding) + 1;
                longestUsageCounts.put(binding, currentCount);
                usageOffsets.get(binding).add(node.getStartPosition());
            }
            return true;
        }
    }

    public String maskVariableWithLongestUsageSequence(String originalLine, int offsetAdjustment) {
        IBinding maxBinding = null;
        int maxUsageCount = 0;

        for (Map.Entry<IBinding, Integer> entry : longestUsageCounts.entrySet()) {
            IBinding binding = entry.getKey();
            int usageCount = entry.getValue();
            if (usageCount > maxUsageCount) {
                maxUsageCount = usageCount;
                maxBinding = binding;
            }
        }

        if (maxBinding != null) {
            System.out.println("Variable with highest usage: '" + maxBinding.getName() + "'");
            System.out.println("Usage count: " + maxUsageCount);
            System.out.println("Offsets: " + usageOffsets.get(maxBinding));
        }

        if (maxBinding != null) {
            List<Integer> offsets = usageOffsets.getOrDefault(maxBinding, Collections.emptyList());
            String variableName = maxBinding.getName();

            offsets.replaceAll(offset -> offset - offsetAdjustment);
            offsets.sort(Collections.reverseOrder());

            StringBuilder maskedLine = new StringBuilder(originalLine);
            for (int offset : offsets) {
                int endOffset = offset + variableName.length();
                if (offset >= 0 && offset < originalLine.length()) {
                    maskedLine.replace(offset, Math.min(endOffset, originalLine.length()), "[MASK]");
                }
            }
            return maskedLine.toString();
        } else {
            return originalLine; 
        }
    }
}

    