import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import org.eclipse.jdt.core.dom.*;
import util.UtilAST;

public class ParsingValidation {
    private static final String UNIT_NAME = "DummyClass";
    private static final String INPUT_FILE_PATH = "/home/user1-selab3/Documents/research-shradha/CODE-SPT-Code/preprocess/pretrain_code_tokens.txt";
    private static final String OUTPUT_FILE_PATH = "output1/pretraining_code_tokens_output_nongen.txt";

    int counterSimpleNameVisit = 0;
    boolean isChecking = true;

    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new FileReader(INPUT_FILE_PATH));
             FileWriter writer = new FileWriter(OUTPUT_FILE_PATH);) {

            String line;
            int index = 1;

            // Read each line from the input file and process it
            while ((line = reader.readLine()) != null) {
                writer.write("\nProcessing line " + index + ": " + line + "\n");
                String formattedCode = formatCode(line);

                ASTParser parser = UtilAST.parseSrcCode(formattedCode, UNIT_NAME + ".java");
                CompilationUnit cu = (CompilationUnit) parser.createAST(null);

                ParseResultChecker checker = new ParseResultChecker();
                cu.accept(checker);

                int counterToken = 0;
                String[] tokens = line.split("\\s+");
                //writer.write("[DBG] Tokens in line:\n");
                for (String token : tokens) {
                    token = token.trim();
                    //writer.write("  Token: " + token);
                    if (token.matches("\\d+")) {
                        writer.write(" - Not Counted (Number)\n");
                        continue; // Skip counting this token
                    }

                    if (token.matches("0x[0-9A-Fa-f]+")) {
                        writer.write(" - Not Counted (Hexadecimal)\n");
                        continue;
                    }

                    
                    if (!Set.of(
                            // Java keywords
                            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", 
                            "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", 
                            "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package", 
                            "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", 
                            "this", "throw", "throws", "transient", "try", "void", "volatile", "while", "sealed", "permits",
                            
                            // Literals
                            "true", "false",

                            // Operators and special characters
                            "+", "-", "*", "/", "%", "++", "--", "!", "~", "?", ":", 
                            "==", "!=", ">", "<", ">=", "<=", 
                            "&&", "||", "=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>=",
                            "&", "|", "^", "<<", ">>", ">>>",
                            ".", ";", ",", "(", ")", "{", "}", "[", "]"
                        ).contains(token)) {
                        counterToken++;
                        //writer.write(" - Counted\n");
                    } else {
                        //writer.write(" - Not Counted\n");
                    }
                }

                writer.write("[DBG] counterToken: " + counterToken + "\n");
                writer.write("[DBG] tokens.length: " + tokens.length + "\n");

                if (counterToken == checker.getCounterSimpleNameVisit()) {
                    writer.write("[DBG] Okay\n");
                } else {
                    int largerValue = Math.max(counterToken, checker.getCounterSimpleNameVisit());
                    int smallerValue = Math.min(counterToken, checker.getCounterSimpleNameVisit());
                    double ratio = (double) smallerValue / largerValue;

                    writer.write(String.format("[DBG] Ratio: %.2f\n", ratio));

                    if (ratio > 0.7) {
                        writer.write("[DBG] Okay\n");
                    } else {
                        writer.write("[DBG] Not Okay\n");
                    }
                }
                
                // Reset counter for next line
                checker.resetCounter();
                index++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String formatCode(String codeSnippet) {
        return "public class " + UNIT_NAME + " {\n" + codeSnippet + "\n}";
    }

    // Static Inner Class
    static class ParseResultChecker extends ASTVisitor {
        private int counterSimpleNameVisit = 0;

        @Override
        public boolean visit(SimpleName node) {
            counterSimpleNameVisit++;
            //System.out.println("[DBG] SimpleName node visited: " + node.getIdentifier()); // Log each SimpleName
            return super.visit(node);
        }

        public int getCounterSimpleNameVisit() {
            return counterSimpleNameVisit;
        }
        public void resetCounter() {
            counterSimpleNameVisit = 0;
        }
    }
}


//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.Set;
//
//import org.eclipse.jdt.core.dom.*;
//import util.UtilAST;
//
//public class ParsingValidation {
//    private static final String UNIT_NAME = "DummyClass";
//    private static final String INPUT_FILE_PATH = "input/input.txt";
//    private static final String OUTPUT_FILE_PATH = "output/output_masked.txt";
//    private static final String NO_USAGE_FILE_PATH = "output/others.txt";
//
//    int counterSimpleNameVisit = 0;
//    boolean isChecking = true;
//
//    public static void main(String[] args) {
//        try (BufferedReader reader = new BufferedReader(new FileReader(INPUT_FILE_PATH));
//             FileWriter writer = new FileWriter(OUTPUT_FILE_PATH);
//             FileWriter noUsageWriter = new FileWriter(NO_USAGE_FILE_PATH)) {
//
//            String line;
//            int index = 1;
//
//            while ((line = reader.readLine()) != null) {
//                System.out.println("\nProcessing line " + index + ": " + line);
//                String formattedCode = formatCode(line);
//
//                ASTParser parser = UtilAST.parseSrcCode(formattedCode, UNIT_NAME + ".java");
//                CompilationUnit cu = (CompilationUnit) parser.createAST(null);
//
//                ParsingValidation example = new ParsingValidation();
//
//                if (example.isChecking) {
//                    ParseResultChecker checker = new ParseResultChecker();
//                    cu.accept(checker);
//
//                    System.out.println("[DBG] # of SimpleName Visit: " + checker.getCounterSimpleNameVisit());
//                    int counterToken = 0;
//                    String[] inputArr = {"DummyClass", "public", "final", "Mono", "<", "T", ">", "onErrorResume",
//                                         "(", "Function", "<", "?", "super", "Throwable", ",", "?", "extends", "Mono",
//                                         "<", "?", "extends", "T", ">", ">", "fallback", ")", "{", "return", "onAssembly", 
//                                         "(", "new", "MonoOnErrorResume", "<", ">", "(", "this", ",", "fallback", ")", ")", ";", "}"};
//
//                    for (String token : inputArr) {
////                        if (!Set.of("<", ">", "(", ")", "{", "}", ";", ",", "?", "public", "final", "this", 
////                                    "super", "return", "new", "extends").contains(token)) {
//                    	if (!Set.of(
//                    	        // Java keywords
//                    	        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", 
//                    	        "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", 
//                    	        "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package", 
//                    	        "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", 
//                    	        "this", "throw", "throws", "transient", "try", "void", "volatile", "while", "sealed", "permits",
//
//                    	        // Literals
//                    	        "true", "false",
//
//                    	        // Operators and special characters
//                    	        "+", "-", "*", "/", "%", "++", "--", "!", "~", "?", ":", 
//                    	        "==", "!=", ">", "<", ">=", "<=", 
//                    	        "&&", "||", "=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>=",
//                    	        "&", "|", "^", "<<", ">>", ">>>",
//                    	        ".", ";", ",", "(", ")", "{", "}", "[", "]"
//                    	    ).contains(token)) {
//                            counterToken++;
//                        }
//                    }
//
//                    System.out.println("[DBG] counterToken: " + counterToken);
//                    System.out.println("[DBG] " + inputArr.length);
//
//                    if (Math.abs(counterToken - checker.getCounterSimpleNameVisit()) > 5) {
//                        System.out.println("[DBG] Not Okay");
//                    } else {
//                        System.out.println("[DBG] Okay");
//                    }
//
//                    return;
//                }
//
//                index++;
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static String formatCode(String codeSnippet) {
//        return "public class " + UNIT_NAME + " {\n" + codeSnippet + "\n}";
//    }
//
//    // Static Inner Class
//    static class ParseResultChecker extends ASTVisitor {
//        private int counterSimpleNameVisit = 0;
//
//        @Override
//        public boolean visit(SimpleName node) {
//            counterSimpleNameVisit++;
//            return super.visit(node);
//        }
//
//        public int getCounterSimpleNameVisit() {
//            return counterSimpleNameVisit;
//        }
//    }
//}
