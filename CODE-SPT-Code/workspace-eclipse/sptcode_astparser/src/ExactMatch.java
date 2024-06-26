import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.similarity.LevenshteinDistance;

public class ExactMatch {

    private static final Pattern STRING_PATTERN = Pattern.compile("\"[^\"]*\"|'[^']*'");
    private static final Pattern MODIFIERS_PATTERN = Pattern.compile("\\b(public|private|protected|privatestatic)\\b");
    //private static final Pattern MODIFIERS_PATTERN = Pattern.compile("\\b(public|private(?!\\s+static)|protected)\\b");

    private static final String FILE_PATH1 = "input/finetune_methods_valid_final.txt";
    private static final String FILE_PATH2 = "input/data.TargetType.seq.valid.source.txt";//"input/input1.txt";
    private static final String OUTPUT_FILE = "input/finetune_valid_exact_match_output.txt";

    public static void main(String[] args) {
        try {
            PrintStream fileOut = new PrintStream(new FileOutputStream(OUTPUT_FILE));
            System.setOut(fileOut);
            long startTime = System.currentTimeMillis();
            compareFiles(FILE_PATH1, FILE_PATH2);
            long endTime = System.currentTimeMillis();
            System.out.println("Start Time: " + startTime);
            System.out.println("End Time: " + endTime);
            System.out.println("Duration: " + (endTime - startTime) + " ms");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	
	public static void compareFiles(String filePath1, String filePath2) throws IOException {
	    try (BufferedReader reader1 = new BufferedReader(new FileReader(filePath1));
	         BufferedReader reader2 = new BufferedReader(new FileReader(filePath2))) {
	
	        List<String> file1Lines = new ArrayList<>();
	        String line1;
	        while ((line1 = reader1.readLine()) != null) {
	            String processedLine1 = processContentForFile1(line1);
	            file1Lines.add(processedLine1);
	            System.out.println("Processed File1 Line: " + processedLine1);  // Debug output
	        }
	
	        String line2;
	        int lineNumber = 1;
	        LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
	
	        while ((line2 = reader2.readLine()) != null) {
	            String processedLine2 = processContentForFile2(line2);
	            int predStart = processedLine2.indexOf("pred");
	            if (predStart == -1) {
	                continue;  // Skip lines without 'pred'
	            }
	
	            String beforePred = processedLine2.substring(0, predStart).trim();
	            String afterPred = processedLine2.substring(predStart + "pred".length()).trim();
	
	            // Normalize before comparing
	            beforePred = beforePred.replaceAll("[^A-Za-z0-9]+", "");
	            afterPred = afterPred.replaceAll("[^A-Za-z0-9]+", "");
	
	            System.out.printf("Pred Start: %d, Pred End: %d\n", predStart, predStart + "pred".length());  // Debug pred positions
	            System.out.printf("Before Pred: [%s]\nAfter Pred: [%s]\n", beforePred, afterPred);  // Debug the exact substrings
	            
	            boolean matchFound = false;
	            for (String content1 : file1Lines) {
	                String normalizedContent1 = content1.replaceAll("[^A-Za-z0-9]+", "");
	                
	                if (normalizedContent1.contains(beforePred) && normalizedContent1.endsWith(afterPred)) {
	                	System.out.printf("Line %d from File2 matched with Line in File1 based on 'beforePred' and 'afterPred':\n", lineNumber);
                        System.out.println("Matched Line: " + content1);
	                    matchFound = true;
	                    break;
	                }
	                
	                if (!matchFound) {
                        int distance = levenshteinDistance.apply(content1, beforePred + afterPred);
                        double similarity = 1 - (double) distance / Math.max(content1.length(), (beforePred + afterPred).length());
                        if (similarity > 0.7) {
                        	System.out.printf("Line %d from File2 matched with Line in File1 based on similarity > 80%%: %s\n", lineNumber, content1);
                            System.out.printf("Similarity Score: %.2f%%\n", similarity);
                            matchFound = true;
                            break;
                        }
                    }
                }

//	
//	                // Compute similarity score
//	                int distance = levenshteinDistance.apply(normalizedContent1, beforePred + afterPred);
//	                double similarity = (1 - (double) distance / Math.max(normalizedContent1.length(), (beforePred + afterPred).length()));
//	
//	                // Check if similarity exceeds the threshold
//	                if (normalizedContent1.contains(beforePred) && normalizedContent1.endsWith(afterPred) || similarity > 0.8) {
//	                    System.out.printf("Line %d from File2 matched with Line in File1 based on 'beforePred' and 'afterPred' or similarity score > 80%%:\n", lineNumber);
//	                    System.out.println("Matched Line: " + content1);
//	                    System.out.printf("Similarity Score: %.2f%%\n", similarity * 100);
//	                    matchFound = true;
//	                    break;
//	                }
//	            }
//	
	            if (!matchFound) {
	            	 System.out.printf("No match found for Line %d from File2. Before: [%s], After: [%s]\n", lineNumber, beforePred, afterPred);
	            }

	            lineNumber++;
	        }
	    }
	}

    private static String processContentForFile1(String text) {
        text = removeJavaModifiers(text);
        text = normalizeIdentifiers(text);
        text = text.replaceAll("\\s+", "");
        return text.toLowerCase();
    }

    private static String processContentForFile2(String text) {
        text = replaceStrings(formatCode(text));
        text = normalizeIdentifiers(text);
        text = text.replaceAll("\\s+", "");
        return text.toLowerCase();
    }

    private static String removeJavaModifiers(String text) {
        return MODIFIERS_PATTERN.matcher(text).replaceAll("");
    }

    private static String normalizeIdentifiers(String text) {
        text = text.replace("__STR", "PLACEHOLDER_STR");
        text = text.replace("_", "");
        text = text.replace("PLACEHOLDER_STR", "__STR");
        return text;
    }

    private static String replaceStrings(String text) {
        Matcher matcher = STRING_PATTERN.matcher(text);
        return matcher.replaceAll("___STR");
    }

    private static String formatCode(String rawCode) {
        return rawCode.replace(";", ";\n").replace("{", "{\n").replace("}", "\n}");
    }
}

//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.PrintStream;
//import java.io.FileOutputStream;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public class exactmatch {
//
//    private static final Pattern STRING_PATTERN = Pattern.compile("\"[^\"]*\"|'[^']*'");
//    private static final Pattern MODIFIERS_PATTERN = Pattern.compile("\\b(public|private|protected|privatestatic)\\b");
//    //private static final Pattern MODIFIERS_PATTERN = Pattern.compile("\\b(public|private(?!\\s+static)|protected)\\b");
//
//    private static final String FILE_PATH1 = "input/finetune_methods_valid_final.txt";
//    private static final String FILE_PATH2 = "input/input1.txt";//"input/data.TargetType.seq.valid.source.txt";
//    private static final String OUTPUT_FILE = "input/finetune_valid_exact_match_output.txt";
//
//    public static void main(String[] args) {
//        try {
//            PrintStream fileOut = new PrintStream(new FileOutputStream(OUTPUT_FILE));
//            System.setOut(fileOut);
//            long startTime = System.currentTimeMillis();
//            compareFiles(FILE_PATH1, FILE_PATH2);
//            long endTime = System.currentTimeMillis();
//            System.out.println("Start Time: " + startTime);
//            System.out.println("End Time: " + endTime);
//            System.out.println("Duration: " + (endTime - startTime) + " ms");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//    public static void compareFiles(String filePath1, String filePath2) throws IOException {
//        try (BufferedReader reader1 = new BufferedReader(new FileReader(filePath1));
//             BufferedReader reader2 = new BufferedReader(new FileReader(filePath2))) {
//
//            List<String> file1Lines = new ArrayList<>();
//            String line1;
//            while ((line1 = reader1.readLine()) != null) {
//                String processedLine1 = processContentForFile1(line1);
//                file1Lines.add(processedLine1);
//                System.out.println("Processed File1 Line: " + processedLine1);  // Debug output
//            }
//
//            String line2;
//            int lineNumber = 1;
//
//            while ((line2 = reader2.readLine()) != null) {
//                String processedLine2 = processContentForFile2(line2);
//                int predStart = processedLine2.indexOf("pred");
//                if (predStart == -1) {
//                    continue;  // Skip lines without 'pred'
//                }
//
//                String beforePred = processedLine2.substring(0, predStart).trim();
//                String afterPred = processedLine2.substring(predStart + "pred".length()).trim();
//
//                // Normalize before comparing
//                beforePred = beforePred.replaceAll("[^A-Za-z0-9]+", "");
//                afterPred = afterPred.replaceAll("[^A-Za-z0-9]+", "");
//
//                System.out.printf("Pred Start: %d, Pred End: %d\n", predStart, predStart + "pred".length());  // Debug pred positions
//                System.out.printf("Before Pred: [%s]\nAfter Pred: [%s]\n", beforePred, afterPred);  // Debug the exact substrings
//                
//                boolean matchFound = false;
//                for (String content1 : file1Lines) {
//                    String normalizedContent1 = content1.replaceAll("[^A-Za-z0-9]+", "");
//                    System.out.println("Normalized Content1: " + normalizedContent1);  // Debug output for normalizedContent1
//
//                    if (normalizedContent1.contains(beforePred) && normalizedContent1.endsWith(afterPred)) {
//                        System.out.printf("Line %d from File2 matched with Line in File1 based on 'beforePred' and 'afterPred':\n", lineNumber);
//                        System.out.println("Matched Line: " + content1);
//                        matchFound = true;
//                        break;
//                    }
//                }
//
//                if (!matchFound) {
//                    System.out.printf("No match found for Line %d from File2. Before: [%s], After: [%s]\n", lineNumber, beforePred, afterPred);
//                }
//
//                lineNumber++;
//            }
//        }
//    }
//
//
//
//    private static String processContentForFile1(String text) {
//        text = removeJavaModifiers(text);
//        text = normalizeIdentifiers(text);
//        text = text.replaceAll("\\s+", "");
//        return text.toLowerCase();
//    }
//
//    private static String processContentForFile2(String text) {
//        text = replaceStrings(formatCode(text));
//        text = normalizeIdentifiers(text);
//        text = text.replaceAll("\\s+", "");
//        return text.toLowerCase();
//    }
//
//    private static String removeJavaModifiers(String text) {
//        return MODIFIERS_PATTERN.matcher(text).replaceAll("");
//    }
//
//    private static String normalizeIdentifiers(String text) {
//        text = text.replace("__STR", "PLACEHOLDER_STR");
//        text = text.replace("_", "");
//        text = text.replace("PLACEHOLDER_STR", "__STR");
//        return text;
//    }
//
//    private static String replaceStrings(String text) {
//        Matcher matcher = STRING_PATTERN.matcher(text);
//        return matcher.replaceAll("___STR");
//    }
//
//    private static String formatCode(String rawCode) {
//        return rawCode.replace(";", ";\n").replace("{", "{\n").replace("}", "\n}");
//    }
//}

//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.PrintStream;
//import java.io.FileOutputStream;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public class exactmatch {
//
//    private static final Pattern STRING_PATTERN = Pattern.compile("\"[^\"]*\"|'[^']*'");
//    private static final Pattern MODIFIERS_PATTERN = Pattern.compile("\\b(public|private|protected)\\b");
//
//    private static final String FILE_PATH1 = "input/finetune_methods_valid_final.txt";
//    private static final String FILE_PATH2 = "input/input1.txt";
//    private static final String OUTPUT_FILE = "input/finetune_valid_exact_match_output.txt";
//
//    public static void main(String[] args) {
//        try {
//            PrintStream fileOut = new PrintStream(new FileOutputStream(OUTPUT_FILE));
//            System.setOut(fileOut);
//            long startTime = System.currentTimeMillis();
//            compareFiles(FILE_PATH1, FILE_PATH2);
//            long endTime = System.currentTimeMillis();
//            System.out.println("Start Time: " + startTime);
//            System.out.println("End Time: " + endTime);
//            System.out.println("Duration: " + (endTime - startTime) + " ms");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void compareFiles(String filePath1, String filePath2) throws IOException {
//        try (BufferedReader reader1 = new BufferedReader(new FileReader(filePath1));
//             BufferedReader reader2 = new BufferedReader(new FileReader(filePath2))) {
//
//            List<String> file1Lines = new ArrayList<>();
//            String line1;
//            while ((line1 = reader1.readLine()) != null) {
//                String processedLine1 = processContentForFile1(line1);
//                System.out.println("Processed File1 Line: " + processedLine1);  // Debug output
//                file1Lines.add(processedLine1);
//            }
//
//            String line2;
//            int lineNumber = 1;
//
//            while ((line2 = reader2.readLine()) != null) {
//                String processedLine2 = processContentForFile2(line2);
//                System.out.println("Processed File2 Line: " + processedLine2);  // Debug output
//                int predStart = processedLine2.indexOf("pred");
//                if (predStart == -1) {
//                    continue;  // Skip lines without 'pred'
//                }
//
//                String beforePred = processedLine2.substring(0, predStart).trim();
//
//                boolean matchFound = false;
//                for (String content1 : file1Lines) {
//                    if (content1.contains(beforePred)) {
//                        System.out.printf("Line %d from File2 matched with Line in File1 based on 'beforePred':\n", lineNumber);
//                        System.out.println("Matched Line: " + content1);
//                        matchFound = true;
//                        break;
//                    }
//                }
//
//                if (!matchFound) {
//                    System.out.printf("No match found for Line %d from File2. Before: [%s]\n", lineNumber, beforePred);
//                }
//
//                lineNumber++;
//            }
//        }
//    }
//
//
//    private static String processContentForFile1(String text) {
//        text = removeJavaModifiers(text);
//        text = normalizeIdentifiers(text);
//        text = text.replaceAll("\\s+", "");
//        return text.toLowerCase();
//    }
//
//    private static String processContentForFile2(String text) {
//        text = replaceStrings(formatCode(text));
//        text = text.replaceAll("\\s+", "");
//        return text.toLowerCase();
//    }
//
////    private static String removeJavaModifiers(String text) {
////        return MODIFIERS_PATTERN.matcher(text).replaceAll("");
////    }
//    private static String removeJavaModifiers(String text) {
//        // This regex removes 'public', 'private', 'protected' even if they are part of a larger word.
//        // It's crucial to ensure this doesn't unintentionally alter other identifiers.
//        text = text.replaceAll("public\\b", "");
//        text = text.replaceAll("private\\b", "");
//        text = text.replaceAll("protected\\b", "");
//        return text;
//    }
//
//    private static String replaceStrings(String text) {
//        Matcher matcher = STRING_PATTERN.matcher(text);
//        return matcher.replaceAll("___STR");
//    }
//
//    private static String formatCode(String rawCode) {
//        return rawCode.replace(" _ ", "")
//                     .replace(";", ";\n")
//                     .replace("{", "{\n")
//                     .replace("}", "\n}");
//    }
//    
//    private static String normalizeIdentifiers(String text) {
//        // Temporarily replace "__STR" with a placeholder that doesn't contain underscores.
//        String placeholder = "PLACEHOLDER_STR";  // Ensure this string does not appear anywhere else in your code.
//        text = text.replace("__STR", placeholder);
//
//        // Remove all underscores that are not part of the placeholder.
//        text = text.replace("_", "");
//
//        // Revert the placeholder back to "__STR".
//        text = text.replace(placeholder, "__STR");
//        return text;
//    }
//
//}



//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.PrintStream;
//import java.io.FileOutputStream;
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.PrintStream;
//import java.io.FileOutputStream;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public class exactmatch {
//
//    private static final Pattern STRING_PATTERN = Pattern.compile("\"[^\"]*\"|'[^']*'");
//
//
//    private static final String FILE_PATH1 = "input/finetune_methods_valid_final.txt";
//    private static final String FILE_PATH2 = "input/input1.txt";
//    private static final String OUTPUT_FILE = "input/finetune_test_exact_match_output.txt";
//
//    public static void main(String[] args) {
//        try {
//            PrintStream fileOut = new PrintStream(new FileOutputStream(OUTPUT_FILE));
//            System.setOut(fileOut);  // Redirects standard output to "output.txt"
//            long startTime = System.currentTimeMillis();  // Start time
//            compareFiles(FILE_PATH1, FILE_PATH2);
//            long endTime = System.currentTimeMillis();  // End time
//            System.out.println("Start Time: " + startTime);
//            System.out.println("End Time: " + endTime);
//            System.out.println("Duration: " + (endTime - startTime) + " ms");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void compareFiles(String filePath1, String filePath2) throws IOException {
//        try (BufferedReader reader1 = new BufferedReader(new FileReader(filePath1));
//             BufferedReader reader2 = new BufferedReader(new FileReader(filePath2))) {
//
//            List<String> file1Lines = new ArrayList<>();
//            String line1;
//            while ((line1 = reader1.readLine()) != null) {
//                String processed = processContent(line1);
//                file1Lines.add(processed);
//                System.out.println("Processed File1: " + processed);  // Debugging output
//            }
//
//            String line2;
//            int lineNumber = 1;
//
//            while ((line2 = reader2.readLine()) != null) {
//                line2 = processContent(replaceStrings(formatCode(line2)));
//                System.out.println("Processed File2: " + line2);  // Debugging output
//                int predStart = line2.indexOf("pred");
//                int predEnd = line2.lastIndexOf("pred") + "pred".length();
//
//                if (predStart == -1 || predEnd == -1) {
//                    System.out.println("No 'pred' found in File2 line: " + line2);
//                    continue;  // Skip lines without 'pred'
//                }
//
//                String beforePred = line2.substring(0, predStart);
//                String afterPred = line2.substring(predEnd);
//
//                boolean matchFound = false;
//                for (String content1 : file1Lines) {
//                    if (content1.startsWith(beforePred) && content1.endsWith(afterPred)) {
//                        System.out.printf("Line %d from File2 matched with Line in File1:\n", lineNumber);
//                        System.out.println("Matched Line: " + content1);
//                        matchFound = true;
//                        break;
//                    }
//                }
//
//                if (!matchFound) {
//                    System.out.printf("No match found for Line %d from File2.\n", lineNumber);
//                }
//
//                lineNumber++;
//            }
//        }
//    }
//
//    private static String processContent(String text) {
//        text = text.replaceAll("\\s+", "");
//        return text.toLowerCase();
//    }
//
//    private static String replaceStrings(String text) {
//        Matcher matcher = STRING_PATTERN.matcher(text);
//        return matcher.replaceAll("___STR");
//    }
//
//    private static String formatCode(String rawCode) {
//        return rawCode.replace(" _ ", "")
//                     .replace(";", ";\n")
//                     .replace("{", "{\n")
//                     .replace("}", "\n}");
//    }
//}

//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public class exactmatch {
//
//    private static final Pattern STRING_PATTERN = Pattern.compile("\"[^\"]*\"|'[^']*'");
//
//    private static final String FILE_PATH1 = "input/finetune_methods_valid_final.txt";
//    private static final String FILE_PATH2 = "input/input1.txt";
//    private static final String OUTPUT_FILE = "input/finetune_test_exact_match_output.txt";
//
//    public static void main(String[] args) {
//        try {
//            PrintStream fileOut = new PrintStream(new FileOutputStream(OUTPUT_FILE));
//            System.setOut(fileOut);  // Redirects standard output to "output.txt"
//            long startTime = System.currentTimeMillis();  // Start time
//            compareFiles(FILE_PATH1, FILE_PATH2);
//            long endTime = System.currentTimeMillis();  // End time
//            System.out.println("Start Time: " + startTime);
//            System.out.println("End Time: " + endTime);
//            System.out.println("Duration: " + (endTime - startTime) + " ms");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void compareFiles(String filePath1, String filePath2) throws IOException {
//        try (BufferedReader reader1 = new BufferedReader(new FileReader(filePath1));
//             BufferedReader reader2 = new BufferedReader(new FileReader(filePath2))) {
//
//            List<String> file1Lines = new ArrayList<>();
//            String line1;
//            while ((line1 = reader1.readLine()) != null) {
//                file1Lines.add(processContent(replaceStrings(formatCode(line1))));
//            }
//
//            String line2;
//            int lineNumber = 1;
//
//            while ((line2 = reader2.readLine()) != null) {
//                line2 = processContent(replaceStrings(formatCode(line2)));
//                int predStart = line2.indexOf("pred");
//                int predEnd = line2.lastIndexOf("pred") + "pred".length();
//
//                String beforePred = line2.substring(0, predStart);
//                String afterPred = line2.substring(predEnd);
//
//                for (String content1 : file1Lines) {
//                    if (content1.startsWith(beforePred) && content1.endsWith(afterPred)) {
//                        System.out.printf("Line %d from File2 matched with Line in File1:\n", lineNumber);
//                        System.out.println("Matched Line: " + content1);
//                        break;
//                    }
//                }
//
//                lineNumber++;
//            }
//        }
//    }
//
//    private static String processContent(String text) {
//        text = text.replaceAll("\\s+", "");
//        return text.toLowerCase();
//    }
//
//    private static String replaceStrings(String text) {
//        Matcher matcher = STRING_PATTERN.matcher(text);
//        return matcher.replaceAll("___STR");
//    }
//
//    private static String formatCode(String rawCode) {
//        return rawCode.replace(" _ ", "")
//                     .replace(";", ";\n")
//                     .replace("{", "{\n")
//                     .replace("}", "\n}");
//    }
//}
//
