package dataflow;

import org.eclipse.jdt.core.dom.*;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import util.UtilAST;
import visitor.DefUseASTVisitor;
import visitor.MethodNameVisitor;
import data.DefUseModel;
import java.util.regex.Pattern;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class SimpleExampleDefUse2 {

    private static final String INPUT_FILE_1 = "input/pretrain_source_test.txt"; // I1
    private static final String INPUT_FILE_2 = "input/longest_sequence.txt";     // I2
    private static final String OUTPUT_MASKED_FILE_PATH = "output/masked_output_test.txt"; 

    public static void main(String[] args) {
        try (BufferedReader reader1 = new BufferedReader(new FileReader(INPUT_FILE_1));
             BufferedReader reader2 = new BufferedReader(new FileReader(INPUT_FILE_2));
             FileWriter writer = new FileWriter(OUTPUT_MASKED_FILE_PATH)) {

            StringBuilder codeSnippet = new StringBuilder();
            String line;
            while ((line = reader1.readLine()) != null) {
                codeSnippet.append(line).append("\n");
            }

            List<String> methodSequences = new ArrayList<>();
            while ((line = reader2.readLine()) != null) {
                methodSequences.add(line.trim()); 
            }

            String maskedOutput = maskMethodCallsByRegex(codeSnippet.toString(), methodSequences);

            writer.write(maskedOutput);
            System.out.println("Masked Output saved to: " + OUTPUT_MASKED_FILE_PATH);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String maskMethodCallsByRegex(String codeSnippet, List<String> methodSequences) {
        String maskedCode = codeSnippet;

        for (String methodCall : methodSequences) {
            System.out.println("Masking method: " + methodCall);

            String[] individualMethodCalls = methodCall.split(";"); 

            for (String individualCall : individualMethodCalls) {
                individualCall = individualCall.trim();  
                
                int dotIndex = individualCall.indexOf('.');
                if (dotIndex != -1) {
                    String objectName = individualCall.substring(0, dotIndex);   
                    String methodNameWithArgs = individualCall.substring(dotIndex + 1); 

                    String regex = objectName + "\\." + methodNameWithArgs.split("\\(")[0] + "\\s*\\([^)]*\\)";

                    System.out.println("Regex being applied: " + regex);

                    maskedCode = maskedCode.replaceAll(regex, objectName + ".[mask]()");
                }
            }
        }

        return maskedCode;
    }
}
