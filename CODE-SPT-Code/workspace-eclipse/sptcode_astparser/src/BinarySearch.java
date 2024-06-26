//import java.io.BufferedReader;
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
//
//public class binarysearch {
//
//    private static final Pattern STRING_PATTERN = Pattern.compile("\"[^\"]*\"|'[^']*'");
//    private static final Pattern MODIFIERS_PATTERN = Pattern.compile(
//            "\\boverride\\s+(public|private|protected)\\b|\\b(public|private|protected)\\s+static\\b|\\b(public|private|protected)\\b");
//
//    private static final String FILE_PATH1 = "input/sorted_finetune_methods_valid_final.txt";
//    private static final String FILE_PATH2 = "input/sorted_data.TargetType.seq.valid.source.txt";
//    private static final String OUTPUT_FILE = "input/finetune_valid_binary_search_output.txt";
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
//                file1Lines.add(processedLine1);
//                System.out.println("Processed File1 Line: " + processedLine1);
//            }
//
//            String line2;
//            int lineNumber = 1;
//            while ((line2 = reader2.readLine()) != null) {
//                String processedLine2 = processContentForFile2(line2);
//                int predStart = processedLine2.indexOf("pred");
//                if (predStart == -1) {
//                    continue;  // Skip lines without 'pred'
//                }
//
//                String beforePred = processedLine2.substring(0, predStart).trim().replaceAll("[^A-Za-z0-9]+", "").toLowerCase();
//                String afterPred = processedLine2.substring(predStart + "pred".length()).trim().replaceAll("[^A-Za-z0-9]+", "").toLowerCase();
//
//                if (!customBinarySearch(file1Lines, beforePred, afterPred)) {
//                    System.out.printf("No match found for Line %d from File2. Before: [%s], After: [%s]\n", lineNumber, beforePred, afterPred);
//                }
//                lineNumber++;
//            }
//        }
//    }
//
//
//    private static boolean customBinarySearch(List<String> sortedLines, String beforePred, String afterPred) {
//        for (String line : sortedLines) {
//            if (line.contains(beforePred) && line.endsWith(afterPred)) {
//                if (compareStrings(line, beforePred + afterPred)) {
//                    System.out.println("Exact match found: " + line);
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//    private static boolean compareStrings(String str1, String str2) {
//        if (str1.length() == str2.length()) {
//            return str1.equals(str2);
//        }
//        return false;
//    }
//
//    private static String processContentForFile1(String text) {
//        text = removeJavaModifiers(text);
//        text = text.replaceAll("\\s+", "").toLowerCase();
//        return text;
//    }
//
//    private static String processContentForFile2(String text) {
//        text = replaceStrings(text);
//        text = removeJavaModifiers(text);
//        text = text.replaceAll("\\s+", "").toLowerCase();
//        return text;
//    }
//
//    private static String removeJavaModifiers(String text) {
//        return MODIFIERS_PATTERN.matcher(text).replaceAll("");
//    }
//
//    private static String replaceStrings(String text) {
//        Matcher matcher = STRING_PATTERN.matcher(text);
//        return matcher.replaceAll("___STR");
//    }
//
//    // Additional methods unchanged
//}


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BinarySearch {

    private static final Pattern STRING_PATTERN = Pattern.compile("\"[^\"]*\"|'[^']*'");
    //private static final Pattern MODIFIERS_PATTERN = Pattern.compile("\\b(public|private|protected)(\\s+static|override)?\\b");
    
    private static final Pattern MODIFIERS_PATTERN = Pattern.compile(
    	    "\\boverride\\s+(public|private|protected)\\b|\\b(public|private|protected)\\s+static\\b|\\b(public|private|protected)\\b");
    //private static final Pattern ACCESS_MODIFIERS_PATTERN = Pattern.compile("\\b(public|private|protected|Overrideprotected)\\b");
    //private static final Pattern OVERRIDE_MODIFIERS_PATTERN = Pattern.compile("\\boverride\\s+(public|private|protected)\\b");

    private static final String FILE_PATH1 = "input/sorted_finetune_methods_valid_final.txt";
    private static final String FILE_PATH2 = "input/sorted_data.TargetType.seq.valid.source.txt";
    private static final String OUTPUT_FILE = "input/finetune_valid_binary_search_output.txt";

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
                String normalizedContent1 = processedLine1.replaceAll("[^A-Za-z0-9]+", "").toLowerCase();
                file1Lines.add(normalizedContent1);
                System.out.println("Processed and Normalized File1 Line: " + normalizedContent1);
            }

            Collections.sort(file1Lines);

            String line2;
            int lineNumber = 1;
            while ((line2 = reader2.readLine()) != null) {
                String processedLine2 = processContentForFile2(line2);
                int predStart = processedLine2.indexOf("pred");
                if (predStart == -1) {
                    continue;
                }

                String beforePred = processedLine2.substring(0, predStart).trim().replaceAll("[^A-Za-z0-9]+", "").toLowerCase();
                String afterPred = processedLine2.substring(predStart + "pred".length()).trim().replaceAll("[^A-Za-z0-9]+", "").toLowerCase();

                int low = 0;
                int high = file1Lines.size() - 1;
                boolean matchFound = false;

                while (low <= high) {
                    int mid = low + (high - low) / 2;
                    if (file1Lines.get(mid).contains(beforePred)) {
                        // Scan around the mid point for a match
                        int checkRange = mid;
                        while (checkRange >= low && file1Lines.get(checkRange).contains(beforePred)) {
                            if (file1Lines.get(checkRange).endsWith(afterPred)) {
                                System.out.printf("Line %d from File2 matched with Line in File1 based on 'beforePred' and 'afterPred': %s\n", lineNumber, file1Lines.get(checkRange));
                                matchFound = true;
                                break;
                            }
                            checkRange--;
                        }
                        checkRange = mid + 1;
                        while (!matchFound && checkRange <= high && file1Lines.get(checkRange).contains(beforePred)) {
                            if (file1Lines.get(checkRange).endsWith(afterPred)) {
                                System.out.printf("Line %d from File2 matched with Line in File1 based on 'beforePred' and 'afterPred': %s\n", lineNumber, file1Lines.get(checkRange));
                                matchFound = true;
                                break;
                            }
                            checkRange++;
                        }
                        break;
                    } else if (file1Lines.get(mid).compareTo(beforePred) < 0) {
                        low = mid + 1;
                    } else {
                        high = mid - 1;
                    }
                }

                if (!matchFound) {
                    System.out.printf("No match found for Line %d from File2. Before: [%s], After: [%s]\n", lineNumber, beforePred, afterPred);
                }

                lineNumber++;
            }
        }
    }

    private static String processContentForFile1(String text) {
        text = removeJavaModifiers(text);
        //text = removeModifiersAfterOverride(text);
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
        // Matcher with replacement logic
        Matcher matcher = MODIFIERS_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            if (matcher.group(1) != null) { // This group matches 'overridepublic', 'overrideprivate', or 'overrideprotected'
                matcher.appendReplacement(result, "override"); // Keep 'override', remove the modifier
            } else {
                matcher.appendReplacement(result, ""); // Remove the modifier
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }



//	private static String removeJavaModifiers(String text) {
//	    // Apply the pattern to remove all specified cases
//	    return MODIFIERS_PATTERN.matcher(text).replaceAll("");
//	}
//	
	
//	private static String removeModifiersAfterOverride(String text) {
//	    // Removes 'public', 'private', 'protected' only when they directly follow 'override'
//	    return OVERRIDE_MODIFIERS_PATTERN.matcher(text).replaceAll("override");
//	}

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
//import java.util.Collections;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import org.apache.commons.text.similarity.LevenshteinDistance;
//
//public class binarysearch {
//
//    private static final Pattern STRING_PATTERN = Pattern.compile("\"[^\"]*\"|'[^']*'");
//    private static final Pattern MODIFIERS_PATTERN = Pattern.compile("\\b(public|private|public+static|private+static|protected)\\b");
//    //private static final Pattern MODIFIERS_PATTERN = Pattern.compile("\\b(public|private(?!\\s+static)|protected)\\b");
//
//    private static final String FILE_PATH1 = "input/sorted_finetune_methods_valid_final.txt";
//    private static final String FILE_PATH2 = "input/sorted_data.TargetType.seq.valid.source.txt";//"input/input1.txt";
//    private static final String OUTPUT_FILE = "input/finetune_valid_binary_search_output.txt";
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
//                String normalizedContent1 = processedLine1.replaceAll("[^A-Za-z0-9]+", "").toLowerCase();
//                file1Lines.add(normalizedContent1);
//                System.out.println("Processed and Normalized File1 Line: " + normalizedContent1);  // Debug output
//            }
//
//            // Sort the list for binary search
//            Collections.sort(file1Lines);
//
//            String line2;
//            int lineNumber = 1;
//            while ((line2 = reader2.readLine()) != null) {
//                String processedLine2 = processContentForFile2(line2);
//                int predStart = processedLine2.indexOf("pred");
//                if (predStart == -1) {
//                    continue;  // Skip lines without 'pred'
//                }
//
//                String beforePred = processedLine2.substring(0, predStart).trim().replaceAll("[^A-Za-z0-9]+", "").toLowerCase();
//                String afterPred = processedLine2.substring(predStart + "pred".length()).trim().replaceAll("[^A-Za-z0-9]+", "").toLowerCase();
//                
//                int low = 0;
//                int high = file1Lines.size() - 1;
//                boolean matchFound = false;
//
//                while (low <= high) {
//                    int mid = low + (high - low) / 2;
//                    if (file1Lines.get(mid).contains(beforePred)) {
//                        // Check for afterPred condition in a range around mid
//                        int checkRange = mid;
//                        while (checkRange >= low && file1Lines.get(checkRange).contains(beforePred)) {
//                            if (file1Lines.get(checkRange).endsWith(afterPred)) {
//                                System.out.printf("Line %d from File2 matched with Line in File1 based on 'beforePred' and 'afterPred': %s\n", lineNumber, file1Lines.get(checkRange));
//                                matchFound = true;
//                                break;
//                            }
//                            checkRange--;
//                        }
//                        checkRange = mid + 1;
//                        while (!matchFound && checkRange <= high && file1Lines.get(checkRange).contains(beforePred)) {
//                            if (file1Lines.get(checkRange).endsWith(afterPred)) {
//                                System.out.printf("Line %d from File2 matched with Line in File1 based on 'beforePred' and 'afterPred': %s\n", lineNumber, file1Lines.get(checkRange));
//                                matchFound = true;
//                                break;
//                            }
//                            checkRange++;
//                        }
//                        break;
//                    } else if (file1Lines.get(mid).compareTo(beforePred) < 0) {
//                        low = mid + 1;
//                    } else {
//                        high = mid - 1;
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
//        text = MODIFIERS_PATTERN.matcher(text).replaceAll(match -> {
//            if (match.group().contains("publicstatic")) {
//                return "public";
//            } else if (match.group().contains("privatestatic")) {
//                return "private";
//            } else {
//                return ""; // For "protected", remove it or handle as needed
//            }
//        });
//        return text;
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