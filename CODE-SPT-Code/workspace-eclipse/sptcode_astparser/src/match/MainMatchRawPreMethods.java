package match;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.UtilFile;

public class MainMatchRawPreMethods {
   // Updated pattern to match specified combinations
   // private static final Pattern MODIFIERS_PATTERN = Pattern.compile(
   // "\\boverride\\s+(public|private|protected)\\b|\\b(public|private|protected)\\s+static\\b|\\b(public|private|protected)\\b");

   // private static final String FILE_PATH1 = "input/sorted_finetune_methods_valid_final.txt";
   // private static final String FILE_PATH2 = "input/sorted_data.TargetType.seq.valid.source.txt";
   // private static final String OUTPUT_FILE = "output/finetune_valid_binary_search_output.txt";

   // private static final Pattern STRING_PATTERN = Pattern.compile("\"[^\"]*\"|'[^']*'");
   private static final Pattern MODIFIERS_PATTERN = Pattern.compile("\\b(override\\s+)?(public|private|protected)(\\s+static)?\\b");
   private static final String SEARCH = "pred"; // Adjust SEARCH to match your actual search keyword

   // Information:
   // Assume that the files below were sorted.
   // Small sample datasets
   static String FILE_PRE_METHODS = "input/step0-valid.source.txt";
   static String FILE_RAW_METHODS = "input/step0-raw-methods.txt";
   // After removing the first matching.
   // static String FILE_PRE_METHODS = "input/step1-valid.source.txt";
   // static String FILE_RAW_METHODS = "input/step1-raw-methods.txt";
   // Large datasets
   // static String FILE_PRE_METHODS = "";
   // static String FILE_RAW_METHODS = "";
   // Output files
   static String FILE_MATCHED_METHODS = "output/step1-raw-pre.txt";

   static List<String> listPreMethods = null, listRawMethods = null;
   static List<String> listPreMethodsClean = null, listRawMethodsClean = null;

   public static void main(String[] args) {
      long startTime = System.currentTimeMillis();

      listPreMethods = UtilFile.readFile(FILE_PRE_METHODS);
      listRawMethods = UtilFile.readFile(FILE_RAW_METHODS);
      // Step 1. Remove special characters.
      removeSpecialChars();

      // Step 2. Find matched raw methods.
      findMatchedRawMethods();
      long endTime = System.currentTimeMillis();
      System.out.println("Start Time: " + startTime);
      System.out.println("End Time: " + endTime);
      System.out.println("Duration: " + (endTime - startTime) + " ms");
   }

   // Step 1. Remove special characters.
   static void removeSpecialChars() {
      List<String> cleanedListPreMethods = new ArrayList<>();
      for (String iPreMethod : listPreMethods) {
         int predIndex = iPreMethod.indexOf(SEARCH);

         if (predIndex == -1) {
            System.out.println("[ERR] " + SEARCH + " term not found in target: " + iPreMethod);
            throw new RuntimeException("[ERR] " + SEARCH + " term not found in target: " + iPreMethod);
            // do not continue the iteration but need to exam the dataset first.
         }

         String cleanedMethod = removeJavaModifiers(iPreMethod);
         cleanedMethod = cleanedMethod.replaceAll("[^A-Za-z0-9]+", "").toLowerCase();
         cleanedMethod = cleanedMethod.replaceAll("\\s+", "");
         cleanedListPreMethods.add(cleanedMethod);
      }
      listPreMethodsClean = cleanedListPreMethods;

      List<String> cleanedListRawMethods = new ArrayList<>();
      for (String rawMethod : listRawMethods) {
         String cleanedMethod = removeJavaModifiers(rawMethod);
         cleanedMethod = cleanedMethod.replaceAll("[^A-Za-z0-9]+", "").toLowerCase();
         cleanedMethod = cleanedMethod.replaceAll("\\s+", "");
         cleanedListRawMethods.add(cleanedMethod);
      }
      listRawMethodsClean = cleanedListRawMethods;
   }

   // Step 2. Find matched raw methods.
   static void findMatchedRawMethods() {
      List<String> outputMatched = new ArrayList<String>();

      String[] rawMethods = listRawMethodsClean.toArray(new String[0]);

      for (String iPreMethod : listPreMethodsClean) {
         Comparator<String> customComparator = createComparator();
         int foundIndex = Arrays.binarySearch(rawMethods, iPreMethod, customComparator);

         /* 
          Output format to be saved.
          foundIndx
          iPreMethod
          rawMethods[foundIndex]
         
          1
          m1() {.. pred ..}
          m1() {...}
          2
          m2() {.. pred ..}
          m2 {...}
         */
         if (foundIndex >= 0) {
            outputMatched.add("" + foundIndex);
            outputMatched.add(iPreMethod);
            outputMatched.add(rawMethods[foundIndex]);
         }
      }
      UtilFile.writeFile(outputMatched, FILE_MATCHED_METHODS);
   }

   private static Comparator<String> createComparator() {
      return new Comparator<String>() {
         @Override
         public int compare(String method, String target) {
            int predIndex = target.indexOf(SEARCH);

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

   private static String removeJavaModifiers(String text) {
      // System.out.println("Original text: " + text); // Debug output of original text

      Matcher matcher = MODIFIERS_PATTERN.matcher(text);
      StringBuffer sb = new StringBuffer();

      // Using appendReplacement and appendTail to build the new string
      while (matcher.find()) {
         // System.out.println("Match found: " + matcher.group()); // Prints the matched group
         // Replace matched group with an empty string
         matcher.appendReplacement(sb, "");
      }
      matcher.appendTail(sb);

      String modifiedText = sb.toString();
      // System.out.println("Modified text: " + modifiedText); // Debug output after replacement

      return modifiedText;
   }

}
