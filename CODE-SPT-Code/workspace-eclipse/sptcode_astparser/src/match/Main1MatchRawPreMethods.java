package match;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import util.UtilFile;

public class Main1MatchRawPreMethods implements InfoFileNames {

   // private static final String FILE_PATH1 = "input/sorted_finetune_methods_valid_final.txt";
   // private static final String FILE_PATH2 = "input/sorted_data.TargetType.seq.valid.source.txt";
   // private static final String OUTPUT_FILE = "output/finetune_valid_binary_search_output.txt";
   
   private static final Pattern STRING_PATTERN = Pattern.compile("(\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*')");//("\"[^\"]*\"|'[^']*'");
   private static final Pattern MODIFIERS_PATTERN = Pattern.compile("\\b(@override\\s+)?(public|private|protected)(\\s+static)?\\b");
   private static final String SEARCH = "pred"; // Adjust SEARCH to match your actual search keyword
   
   private static final Set<String> JAVA_KEYWORDS = new HashSet<>(Arrays.asList(
           "private", "protected", "public", "static","@Overrideprotected","override","@Overridepublic",
           "@Overrideprivate", "Protected", "Public", "Private"
   ));
   
   static List<String> listPreMethods = null, listRawMethods = null;
   static List<String> listPreMethodsClean = null, listRawMethodsClean = null;

   public static void main(String[] args) {
	   
	  Instant startTime = Instant.now();
	  String timeStart = timeNow(ZonedDateTime.now(), "Start Time: ");

      listPreMethods = UtilFile.readFile(FILE_PRE_METHODS);
      listRawMethods = UtilFile.readFile(FILE_RAW_METHODS);
      System.out.println("Loaded " + listPreMethods.size() + " pre-methods and " + listRawMethods.size() + " raw methods.");
      // Step 1. Remove special characters.
      removeSpecialChars();

      // Step 2. Find matched raw methods.
      List<String> outputMatched = findMatchedRawMethods();

      // Step 3. Find unmatched preprocessed and raw methods.
      findUnmatchedRawMethods(listPreMethods, outputMatched, FILE_UNMATCHED_METHODS_PRE);
      findUnmatchedRawMethods(listRawMethods, outputMatched, FILE_UNMATCHED_METHODS_RAW);

      Instant endTime = Instant.now();
      Duration duration = Duration.between(startTime, endTime);
      long seconds = duration.getSeconds();
      String durStr = duration.toString().substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase();
      System.out.println("[DBG] Duration: " + seconds + ", " + durStr);
      System.out.println(timeStart);
      timeNow(ZonedDateTime.now(), "End Time: ");
      
      System.out.println("[DBG] # Matched Preproc Methods: " + countDistinctMatches(FILE_MATCHED_METHODS));
      System.out.println("[DBG] # Unmatched Preproc Methods: "+ countLines(FILE_UNMATCHED_METHODS_PRE));
      System.out.println("[DBG] # Unmatched Raw Methods: "+ countLines(FILE_UNMATCHED_METHODS_RAW));

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
         String cleanedMethod = replaceStrings(normalizeIdentifiers(removeJavaModifiers(formatCode(iPreMethod))));
         cleanedMethod = cleanedMethod.replaceAll("[^A-Za-z0-9{}()\\[\\]]+", "").toLowerCase(); // Include brackets in the regex
         cleanedMethod = cleanedMethod.replaceAll("\\s+", "");
         cleanedListPreMethods.add(cleanedMethod);
         preMethodMap.put(cleanedMethod, iPreMethod); // Map cleaned to original
      }
      listPreMethodsClean = cleanedListPreMethods;
      System.out.println("[DBG] listPreMethodsClean: " + listPreMethodsClean);

      List<String> cleanedListRawMethods = new ArrayList<>();
      for (String rawMethod : listRawMethods) {
         String cleanedMethod = removeJavaModifiers(formatCode(rawMethod));
         cleanedMethod = cleanedMethod.replaceAll("[^A-Za-z0-9{}()\\[\\]]+", "").toLowerCase(); // Include brackets in the regex
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
      String[] rawMethodsArray  = listRawMethodsClean.toArray(new String[0]);
      String[] preMethodsArray = listPreMethodsClean.toArray(new String[0]);

   // Sort both arrays using the same comparator
      Arrays.sort(rawMethodsArray);
      Arrays.sort(preMethodsArray);
      
   // Assign sorted arrays to new variables for further processing
      String[] sortedRawMethods = rawMethodsArray;
      String[] sortedPreMethods = preMethodsArray;

      for (String iPreMethod : sortedPreMethods) {
    	 Comparator<String> customComparator = createComparator();
         int foundIndex = Arrays.binarySearch(sortedRawMethods, iPreMethod, customComparator);

         if (foundIndex >= 0) { 
            outputMatched.add("" + foundIndex); // Mark this index as matched
            outputMatched.add("Match Found:");
            outputMatched.add(preMethodMap.get(iPreMethod));
            outputMatched.add(rawMethodMap.get(sortedRawMethods[foundIndex]));
            outputMatched.add(""); 
         }
         else {// For debugging: show the last comparison attempt if not matched
            int lastComparedIndex = (foundIndex >= 0) ? foundIndex : -(foundIndex + 1);
            if (lastComparedIndex >= 0 && lastComparedIndex < sortedRawMethods.length) {
               outputMatched.add("[DBG] Method \"" + iPreMethod + "\" compared with \"" + sortedRawMethods[lastComparedIndex] + "\" not found.");
            }
            else {
               outputMatched.add("[DBG] Method \"" + iPreMethod + "\" has no comparable raw method.");
            }
         }
      }
      UtilFile.writeFile(outputMatched, FILE_MATCHED_METHODS);
      return outputMatched;
   }
   
   static String timeNow(ZonedDateTime zonedDateTime, String msg) {
	      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
	      String formattedTime = formatter.format(zonedDateTime);
	      System.out.println(msg + formattedTime);
	      return msg + formattedTime;
	   }
   
// Step 3. Find unmatched raw methods
//   static void findUnmatchedRawMethods(List<String> orgRawMethods, List<String> matchedMethods) {
//      List<String> outputUnmatched = new ArrayList<String>();
//      
//      Set<String> matchedMethod = new HashSet<>(matchedMethods);
//      for (String iOrgMethod : orgRawMethods) {
//         if (!matchedMethod.contains(iOrgMethod)) {
//            outputUnmatched.add(iOrgMethod);
//         }
//      }
//      
//   // Print unmatched methods to the console
//      if (outputUnmatched.isEmpty()) {
//          System.out.println("No unmatched methods found.");
//      } else {
//          System.out.println("Unmatched Methods:");
//          outputUnmatched.forEach(System.out::println);
//      }
//
//      // Optionally write unmatched methods to a file
//      try {
//          UtilFile.writeFile(outputUnmatched, "output/unmatched_raw_methods.txt");
//          System.out.println("Unmatched methods written to file: output/unmatched_raw_methods.txt");
//      } catch (Exception e) {
//          System.err.println("Error writing unmatched methods to file: " + e.getMessage());
//          e.printStackTrace();
//      }
//  }

//   // Step 3. Find unmatched preprocessed and raw methods
   static void findUnmatchedRawMethods(List<String> orgMethods, List<String> matchedMethods, String fileName) {

      // Case 1: Pre
      // Case 2: Raw

      String[] matchedMethodsArray = matchedMethods.toArray(new String[0]);
      Arrays.sort(matchedMethodsArray);

      List<String> outputUnmatched = new ArrayList<String>();
      for (String iOrgMethod : orgMethods) {
         // Find iOrgMethod from `matchedMethods`
         // if false then record
         int indexFound = Arrays.binarySearch(matchedMethodsArray, iOrgMethod);
         if (indexFound < 0) {
            outputUnmatched.add(iOrgMethod);
         }

      }

      // Set<String> matchedMethod = new HashSet<>(matchedMethods);
      // for (String iOrgMethod : orgMethods) {
      // if (!matchedMethod.contains(iOrgMethod)) {
      // outputUnmatched.add(iOrgMethod);
      // }
      // }

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
         UtilFile.writeFile(outputUnmatched, fileName);
         System.out.println("Unmatched methods written to file");
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
               return -1; 
            }

            String beforePred = target.substring(0, target.indexOf(SEARCH)).trim();
            String afterPred = target.substring(target.lastIndexOf(SEARCH) + SEARCH.length()).trim();

            String methodBeforePred = method.substring(0, Math.min(beforePred.length(), method.length())).trim();
            String methodAfterPred = method.substring(Math.max(method.length() - afterPred.length(), 0)).trim();

            int result = (methodBeforePred + methodAfterPred).compareTo(beforePred + afterPred);
            System.out.println("[DBG] Comparing: " + (methodBeforePred + methodAfterPred) + " with " + (beforePred + afterPred) + " result: " + result);
            return result;
         }
      };
   }
   
   
   private static String removeJavaModifiers(String text) {
	  text = text.replaceAll("@Overrideprotected\\s*", "");
	  text = text.replaceAll("@Overridepublic\\s*", "");
	  text = text.replaceAll("@Overrideprivate\\s*", "");
	  text = text.replaceAll("protected", "");
	  text = text.replaceAll("Protected", "");
	  text = text.replaceAll("public", "");
	  text = text.replaceAll("Public", "");
	  text = text.replaceAll("private", "");
	  text = text.replaceAll("Private", "");
	   
      Matcher matcher = MODIFIERS_PATTERN.matcher(text);
      StringBuffer sb = new StringBuffer();

      while (matcher.find()) {
         matcher.appendReplacement(sb, "");
      }
      matcher.appendTail(sb);

      String modifiedText = sb.toString().trim();
      for (String keyword : JAVA_KEYWORDS) {
          modifiedText = modifiedText.replaceAll("\\b" + keyword + "\\b", "");
      }

      return modifiedText.trim();
   }
   
   public static int countLines(String filename) {
       int lines = 0;
       try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
           while (reader.readLine() != null) lines++;
       } catch (IOException e) {
           System.out.println("Error reading file: " + e.getMessage());
       }
       return lines;
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
   
   public static int countDistinctMatches(String filename) {
	    int distinctMatches = 0;
	    boolean foundMatch = false;

	    try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
	        String line;
	        while ((line = reader.readLine()) != null) {
	            if (line.startsWith("Match Found:") && !foundMatch) {
	                distinctMatches++;
	                foundMatch = true; 
	            } else if (!line.isEmpty() && line.startsWith("\"")) {
	                foundMatch = false; 
	            }
	        }
	    } catch (IOException e) {
	        System.out.println("Error reading file: " + e.getMessage());
	    }

	    return distinctMatches;
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
