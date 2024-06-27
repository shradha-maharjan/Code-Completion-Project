package match;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.UtilFile;

public class Main1MatchRawPreMethods implements InfoFileNames {
   // Updated pattern to match specified combinations
   // private static final Pattern MODIFIERS_PATTERN = Pattern.compile(
   // "\\boverride\\s+(public|private|protected)\\b|\\b(public|private|protected)\\s+static\\b|\\b(public|private|protected)\\b");

   // private static final String FILE_PATH1 = "input/sorted_finetune_methods_valid_final.txt";
   // private static final String FILE_PATH2 = "input/sorted_data.TargetType.seq.valid.source.txt";
   // private static final String OUTPUT_FILE = "output/finetune_valid_binary_search_output.txt";

   // private static final Pattern STRING_PATTERN = Pattern.compile("\"[^\"]*\"|'[^']*'");
   private static final Pattern MODIFIERS_PATTERN = Pattern.compile("\\b(override\\s+)?(public|private|protected)(\\s+static)?\\b");
   private static final String SEARCH = "pred"; // Adjust SEARCH to match your actual search keyword
   // private static final String SEARCH1 = "pred";

   static List<String> listPreMethods = null, listRawMethods = null;
   static List<String> listPreMethodsClean = null, listRawMethodsClean = null;

   public static void main(String[] args) {
      long startTime = System.currentTimeMillis();

      listPreMethods = UtilFile.readFile(FILE_PRE_METHODS);
      listRawMethods = UtilFile.readFile(FILE_RAW_METHODS);
      System.out.println("Loaded " + listPreMethods.size() + " pre-methods and " + listRawMethods.size() + " raw methods.");
      // Step 1. Remove special characters.
      removeSpecialChars();

      // Step 2. Find matched raw methods.
      List<String> outputMatched = findMatchedRawMethods();

      // Step 3. Find unmatched raw methods.
      findUnmatchedRawMethods(listPreMethods, outputMatched);// (listRawMethods, outputMatched);

      long endTime = System.currentTimeMillis();
      System.out.println("Start Time: " + startTime);
      System.out.println("End Time: " + endTime);
      System.out.println("Duration: " + (endTime - startTime) + " ms");

      if (outputMatched.isEmpty()) {
         System.out.println("No matches found.");
      }
      else {
         System.out.println("Matches found and written to file.");
      }
   }

   static Map<String, String> preMethodMap = new HashMap<>();
   static Map<String, String> rawMethodMap = new HashMap<>();

   // Step 1. Remove special characters and build maps.
   static void removeSpecialChars() {
      List<String> cleanedListPreMethods = new ArrayList<>();
      for (String iPreMethod : listPreMethods) {
         String cleanedMethod = removeJavaModifiers(iPreMethod);
         cleanedMethod = cleanedMethod.replaceAll("[^A-Za-z0-9]+", "").toLowerCase();
         cleanedMethod = cleanedMethod.replaceAll("\\s+", "");
         cleanedListPreMethods.add(cleanedMethod);
         preMethodMap.put(cleanedMethod, iPreMethod); // Map cleaned to original
      }
      listPreMethodsClean = cleanedListPreMethods;
      System.out.println("[DBG] listPreMethodsClean: " + listPreMethodsClean);

      List<String> cleanedListRawMethods = new ArrayList<>();
      for (String rawMethod : listRawMethods) {
         String cleanedMethod = removeJavaModifiers(rawMethod);
         cleanedMethod = cleanedMethod.replaceAll("[^A-Za-z0-9]+", "").toLowerCase();
         cleanedMethod = cleanedMethod.replaceAll("\\s+", "");
         cleanedListRawMethods.add(cleanedMethod);
         rawMethodMap.put(cleanedMethod, rawMethod); // Map cleaned to original
      }
      listRawMethodsClean = cleanedListRawMethods;
      System.out.println("[DBG] listRawMethodsClean: " + listRawMethodsClean);
   }

   // Step 2. Find matched raw methods and ensure unique matching.
   static List<String> findMatchedRawMethods() {
      List<String> outputMatched = new ArrayList<>();
      Set<Integer> matchedIndices = new HashSet<>(); // To keep track of matched raw method indices
      String[] rawMethods = listRawMethodsClean.toArray(new String[0]);

      Arrays.sort(rawMethods);

      for (String iPreMethod : listPreMethodsClean) {
         Comparator<String> customComparator = createComparator();
         int foundIndex = Arrays.binarySearch(rawMethods, iPreMethod, customComparator);

         if (foundIndex >= 0 ) { // Check if not already matched
            matchedIndices.add(foundIndex); // Mark this index as matched
            outputMatched.add("Match Found:");
            outputMatched.add(preMethodMap.get(iPreMethod));
            outputMatched.add(rawMethodMap.get(rawMethods[foundIndex]));
            outputMatched.add(""); // Adding a blank line for better readability between entries
         }
         else {// For debugging: show the last comparison attempt if not matched
            int lastComparedIndex = (foundIndex >= 0) ? foundIndex : -(foundIndex + 1);
            if (lastComparedIndex >= 0 && lastComparedIndex < rawMethods.length) {
               outputMatched.add("[DBG] Method \"" + iPreMethod + "\" compared with \"" + rawMethods[lastComparedIndex] + "\" not found.");
            }
            else {
               outputMatched.add("[DBG] Method \"" + iPreMethod + "\" has no comparable raw method.");
            }
         }
      }
      UtilFile.writeFile(outputMatched, FILE_MATCHED_METHODS);
      return outputMatched;
   }

   // Step 3. Find unmatched raw methods
   static void findUnmatchedRawMethods(List<String> orgRawMethods, List<String> matchedMethods) {
      List<String> outputUnmatched = new ArrayList<String>();

      Set<String> matchedMethod = new HashSet<>(matchedMethods);
      for (String iOrgMethod : orgRawMethods) {
         if (!matchedMethod.contains(iOrgMethod)) {
            outputUnmatched.add(iOrgMethod);
         }
      }

      // Print unmatched methods
      if (outputUnmatched.isEmpty()) {
         System.out.println("No unmatched methods found.");
      }
      else {
         System.out.println("Unmatched Methods:");
         outputUnmatched.forEach(System.out::println);
      }

      // write unmatched methods
      try {
         UtilFile.writeFile(outputUnmatched, "output/unmatched_debug_pre_methods.txt");
         System.out.println("Unmatched methods written to file: output/unmatched_debug_pre_methods.txt");
      } catch (Exception e) {
         System.err.println("Error writing unmatched methods to file: " + e.getMessage());
         e.printStackTrace();
      }
   }

   private static Comparator<String> createComparator() {
      return new Comparator<String>() {
         @Override
         public int compare(String method, String target) {
            int predIndex = target.indexOf(SEARCH);

            if (predIndex == -1) {
               System.err.println("[ERR] SEARCH term '" + SEARCH + "' not found in target: " + target);
               return -1; // or some other handling strategy
            }

            String beforePred = target.substring(0, target.indexOf(SEARCH)).trim();
            String afterPred = target.substring(target.lastIndexOf(SEARCH) + SEARCH.length()).trim();

            System.out.println("[DBG] BeforePred: " + beforePred);
            System.out.println("[DBG] AfterPred: " + afterPred);

            String methodBeforePred = method.substring(0, Math.min(beforePred.length(), method.length())).trim();
            String methodAfterPred = method.substring(Math.max(method.length() - afterPred.length(), 0)).trim();

            System.out.println("[DBG] methodBeforePred: " + methodBeforePred);
            System.out.println("[DBG] methodAfterPred: " + methodAfterPred);

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

/*
// Step 1. Remove special characters.
static void removeSpecialChars() {
 List<String> cleanedListPreMethods = new ArrayList<>();
 for (String iPreMethod : listPreMethods) {
//    int predIndex = iPreMethod.indexOf(SEARCH);
//
//    if (predIndex == -1) {
//       System.out.println("[ERR] " + SEARCH + " term not found in target: " + iPreMethod);
//       throw new RuntimeException("[ERR] " + SEARCH + " term not found in target: " + iPreMethod);
//       // do not continue the iteration but need to exam the dataset first.
//    }

    String cleanedMethod = removeJavaModifiers(iPreMethod);
    cleanedMethod = cleanedMethod.replaceAll("[^A-Za-z0-9]+", "").toLowerCase();
    cleanedMethod = cleanedMethod.replaceAll("\\s+", "");
    cleanedListPreMethods.add(cleanedMethod);
 }
 listPreMethodsClean = cleanedListPreMethods;
 System.out.println("[DBG] listPreMethodsClean: " + listPreMethodsClean);

 List<String> cleanedListRawMethods = new ArrayList<>();
 for (String rawMethod : listRawMethods) {
    String cleanedMethod = removeJavaModifiers(rawMethod);
    cleanedMethod = cleanedMethod.replaceAll("[^A-Za-z0-9]+", "").toLowerCase();
    cleanedMethod = cleanedMethod.replaceAll("\\s+", "");
    cleanedListRawMethods.add(cleanedMethod);
 }
 listRawMethodsClean = cleanedListRawMethods;
 System.out.println("[DBG] listRawMethodsClean: " + listRawMethodsClean);
}

// Step 2. Find matched raw methods.
static List<String> findMatchedRawMethods() {
 List<String> outputMatched = new ArrayList<String>();

 String[] rawMethods = listRawMethodsClean.toArray(new String[0]);
 
 Arrays.sort(rawMethods);

 for (String iPreMethod : listPreMethodsClean) {
    Comparator<String> customComparator = createComparator();
    int foundIndex = Arrays.binarySearch(rawMethods, iPreMethod, customComparator);

     
//     Output format to be saved.
//     foundIndx
//     iPreMethod
//     rawMethods[foundIndex]
//    
//     1
//     m1() {.. pred ..}
//     m1() {...}
//     2
//     m2() {.. pred ..}
//     m2 {...}
    
    if (foundIndex >= 0) {
     outputMatched.add("Match Found");
       outputMatched.add("" + foundIndex);
       outputMatched.add(iPreMethod);
       outputMatched.add(rawMethods[foundIndex]);
    }
 }
 UtilFile.writeFile(outputMatched, FILE_MATCHED_METHODS);
 return outputMatched;
}
*/
