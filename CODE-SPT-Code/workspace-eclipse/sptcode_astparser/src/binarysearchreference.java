import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class binarysearchreference {
    private static final Pattern STRING_PATTERN = Pattern.compile("\"[^\"]*\"|'[^']*'");
    // Updated pattern to match specified combinations
//    private static final Pattern MODIFIERS_PATTERN = Pattern.compile(
//    	    "\\boverride\\s+(public|private|protected)\\b|\\b(public|private|protected)\\s+static\\b|\\b(public|private|protected)\\b");
    
    private static final Pattern MODIFIERS_PATTERN = Pattern.compile(
    	    "\\b(override\\s+)?(public|private|protected)(\\s+static)?\\b");
    private static final String FILE_PATH1 = "input/sorted_finetune_methods_valid_final.txt";
    private static final String FILE_PATH2 = "input/input1.txt";
    private static final String OUTPUT_FILE = "input/finetune_valid_binary_search_output.txt";
    private static final String SEARCH = "pred";  // Adjust SEARCH to match your actual search keyword

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
                String processedLine1 = processContent(line1);
                file1Lines.add(processedLine1);
                System.out.println("processedLine1: " + processedLine1);
            }

            Collections.sort(file1Lines);

            String line2;
            while ((line2 = reader2.readLine()) != null) {
                String processedLine2 = processContent(line2);
                System.out.println("processedLine2: " + processedLine2);

                Comparator<String> customComparator = createComparator();
                int index = Arrays.binarySearch(file1Lines.toArray(new String[0]), processedLine2, customComparator);

                if (index >= 0) {
                    System.out.println("[DBG] ------------------------------------------------------");
                    System.out.println("[DBG] Method Found at: " + index);
                    System.out.println("[DBG] \t" + processedLine2);
                    System.out.println("[DBG] \t" + file1Lines.get(index));
                } else {
                    System.out.println("[DBG] Method \"" + processedLine2 + "\" not found. Insertion point: " + (-index - 1));
                }
            }
        }
    }

    private static Comparator<String> createComparator() {
        return new Comparator<String>() {
            @Override
            public int compare(String method, String target) {
                int predIndex = target.indexOf(SEARCH);
                if (predIndex == -1) {
                    System.out.println("[DBG] SEARCH term not found in target: " + target);
                    return 1; // Default non-zero return if SEARCH term isn't found
                }

                String beforePred = target.substring(0, predIndex).trim();
                String afterPred = target.substring(predIndex + SEARCH.length()).trim();

                String methodBeforePred = method.substring(0, Math.min(beforePred.length(), method.length())).trim();
                String methodAfterPred = method.substring(Math.max(method.length() - afterPred.length(), 0)).trim();

                int result = (methodBeforePred + methodAfterPred).compareTo(beforePred + afterPred);
                System.out.println("[DBG] Comparing: " + (methodBeforePred + methodAfterPred) + " with " + (beforePred + afterPred) + " result: " + result);
                return result;
            }
        };
    }

    private static String processContent(String text) {
        text = removeJavaModifiers(text);
        text = text.replaceAll("[^A-Za-z0-9]+", "").toLowerCase();
        text = text.replaceAll("\\s+", "");
        return text;
    }

    private static String removeJavaModifiers(String text) {
        System.out.println("Original text: " + text);  // Debug output of original text

        Matcher matcher = MODIFIERS_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        // Using appendReplacement and appendTail to build the new string
        while (matcher.find()) {
            System.out.println("Match found: " + matcher.group());  // Prints the matched group
            // Replace matched group with an empty string
            matcher.appendReplacement(sb, "");
        }
        matcher.appendTail(sb);

        String modifiedText = sb.toString();
        System.out.println("Modified text: " + modifiedText);  // Debug output after replacement

        return modifiedText;
    }





}
