package dataflow;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.eclipse.jdt.core.dom.*;

public class MainInsertExtraDefUse {

    private static final String DIR = "research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/output2/";
    private static final String INPUT_FILE_PATH = DIR + "no_usage_sequences.txt"; 
    private static final String OUTPUT_FILE_WITH_VAR_PATH = DIR + "with_artificial_variable.txt";

    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new FileReader(INPUT_FILE_PATH));
             FileWriter writerWithVar = new FileWriter(OUTPUT_FILE_WITH_VAR_PATH)) {

            String line;
            int index = 1;

            while ((line = reader.readLine()) != null) {
                System.out.println("\nProcessing line " + index + ": " + line);

                String formattedCode = unwrapFormattedCode(line);
                String modifiedCode = addArtificialVariable(formattedCode);
                if (modifiedCode == null) continue; 
                
                writerWithVar.write(modifiedCode + "\n");

                index++;
            }

            System.out.println("\nArtificial variable output saved to " + OUTPUT_FILE_WITH_VAR_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String unwrapFormattedCode(String formattedCode) {
        int start = formattedCode.indexOf("{") + 1;
        int end = formattedCode.lastIndexOf("}");
        if (start != -1 && end != -1 && start < end) {
            return formattedCode.substring(start, end).trim(); 
        }
        return formattedCode;  
    }

    private static String addArtificialVariable(String code) {
        String singleLineCode = flattenToSingleLine(code);
        
        int startBodyIndex = singleLineCode.indexOf("{");
        int endBodyIndex = singleLineCode.lastIndexOf("}");
    
        if (startBodyIndex == -1 || endBodyIndex == -1) {
            return null;
        }

        return singleLineCode.substring(0, startBodyIndex + 1) 
             + " int __x__ = 1; " 
             + singleLineCode.substring(startBodyIndex + 1, endBodyIndex)
             + " __call__(__x__); "
             + singleLineCode.substring(endBodyIndex);
    }    


    private static String flattenToSingleLine(String code) {
        return code.replaceAll("\\s+", " ").replace(" { ", "{").replace(" }", "}");
    }
}

