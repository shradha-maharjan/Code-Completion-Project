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

public class Main2MatchRawPreMethods implements InfoFileNames {

   // private static final String FILE_PATH1 =
   // "input/sorted_finetune_methods_valid_final.txt";
   // private static final String FILE_PATH2 =
   // "input/sorted_data.TargetType.seq.valid.source.txt";
   // private static final String OUTPUT_FILE =
   // "output/finetune_valid_binary_search_output.txt";

   private static final Pattern STRING_PATTERN = Pattern.compile("(\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*')");// ("\"[^\"]*\"|'[^']*'");
   private static final Pattern MODIFIERS_PATTERN = Pattern.compile("\\b(@override\\s+)?(public|private|protected)(\\s+static)?\\b");
   private static final String SEARCH = "pred"; // Adjust SEARCH to match your actual search keyword

   private static final Set<String> JAVA_KEYWORDS = new HashSet<>(Arrays.asList("private", "protected", "public", "static", "@Overrideprotected", "override", "@Overridepublic", "@Overrideprivate", "Protected", "Public", "Private"));

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
      System.out.println("[DBG] # Unmatched Preproc Methods: " + countLines(FILE_UNMATCHED_METHODS_PRE));
      System.out.println("[DBG] # Unmatched Raw Methods: " + countLines(FILE_UNMATCHED_METHODS_RAW));

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
         cleanedMethod = cleanedMethod.replaceAll("[^A-Za-z0-9{}()\\[\\]]+", "").toLowerCase(); // Include brackets
                                                                                                // in the regex
         cleanedMethod = cleanedMethod.replaceAll("\\s+", "");
         cleanedListPreMethods.add(cleanedMethod);
         preMethodMap.put(cleanedMethod, iPreMethod); // Map cleaned to original
      }
      listPreMethodsClean = cleanedListPreMethods;
      System.out.println("[DBG] listPreMethodsClean: " + listPreMethodsClean);

      List<String> cleanedListRawMethods = new ArrayList<>();
      for (String rawMethod : listRawMethods) {
         String cleanedMethod = removeJavaModifiers(formatCode(rawMethod));
         cleanedMethod = cleanedMethod.replaceAll("[^A-Za-z0-9{}()\\[\\]]+", "").toLowerCase(); // Include brackets
                                                                                                // in the regex
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
	    String[] rawMethods = listRawMethodsClean.toArray(new String[0]);
	    String[] preMethods = listPreMethodsClean.toArray(new String[0]);


	    for (String preMethod : preMethods) {
	        System.out.println("Processing preMethod: " + preMethod);
	        boolean matchFound = false;

	        for (String rawMethod : rawMethods) {
	            System.out.println("Comparing preMethod: " + preMethod + " with rawMethod: " + rawMethod);
	            
	            int predIndex = preMethod.indexOf(SEARCH);
	            if (predIndex == -1) {
	                continue;  // Skip this rawMethod if it doesn't contain 'pred'
	            }

	            // Extract parts before and after 'pred' in the rawMethod
	            String beforePred = preMethod.substring(0, predIndex).trim();
	            String afterPred = preMethod.substring(predIndex + SEARCH.length()).trim();

	            // Check if the preMethod contains these parts
	            if (rawMethod.contains(beforePred) || rawMethod.contains(afterPred)) {
	                System.out.println("Match found: preMethod matches before and after segments of 'pred' in rawMethod");
	                outputMatched.add("Match Found:");
	                outputMatched.add(preMethodMap.get(preMethod));
	                outputMatched.add(rawMethodMap.get(rawMethod));
//	                outputMatched.add(preMethod);
//	                outputMatched.add(rawMethod);
	                outputMatched.add("");  // For readability
	                matchFound = true;
	                break;  // Assuming one-to-one matching
	            }
	        }

	        if (!matchFound) {
	            System.out.println("No match found for preMethod: " + preMethod);
	            outputMatched.add("[No match found for: " + preMethod + "]");
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

   // // Step 3. Find unmatched preprocessed and raw methods
   static void findUnmatchedRawMethods(List<String> orgMethods, List<String> matchedMethods, String fileName) {
	    // Convert matchedMethods list to a HashSet for quick lookup
	    Set<String> matchedMethodSet = new HashSet<>(matchedMethods);

	    List<String> outputUnmatched = new ArrayList<>();

	    // Iterate over original methods and check if they are in the matched methods set
	    for (String iOrgMethod : orgMethods) {
	        if (!matchedMethodSet.contains(iOrgMethod)) {
	            outputUnmatched.add(iOrgMethod);
	        }
	    }

	    // Print unmatched methods
	    if (outputUnmatched.isEmpty()) {
	        System.out.println("No unmatched methods found.");
	    } else {
	        System.out.println("Unmatched Methods:");
	        outputUnmatched.forEach(System.out::println);
	    }

	    // Write unmatched methods to file
	    try {
	        UtilFile.writeFile(outputUnmatched, fileName);
	        System.out.println("Unmatched methods written to file");
	    } catch (Exception e) {
	        System.err.println("Error writing unmatched methods to file: " + e.getMessage());
	        e.printStackTrace();
	    }
	}

//
//   private static Comparator<String> createComparator() {
//      return new Comparator<String>() {
//         @Override
//         public int compare(String method, String target) {
//            String debugPoint = "swappedcreatefunc";
////            if (!method.contains(debugPoint)) {
////               System.out.println("[DBG] method " + method);
////               System.out.println("[DBG] target " + target);
////            }
//
//            int predIndex = target.indexOf(SEARCH);
//
//            if (predIndex == -1) {
//               return -1;
//            }
//
//            String beforePred = target.substring(0, target.indexOf(SEARCH)).trim();
//            String afterPred = target.substring(target.indexOf(SEARCH) + SEARCH.length()).trim();
//
//            String methodBeforePred = method.substring(0, Math.min(beforePred.length(), method.length())).trim();
//            String methodAfterPred = method.substring(Math.max(method.length() - afterPred.length(), 0)).trim();
//
//            int result = (methodBeforePred + methodAfterPred).compareTo(beforePred + afterPred);
//            System.out.println("[DBG] Comparinggg: " + (methodBeforePred + methodAfterPred) + " with " + (beforePred + afterPred) + " result: " + result);
//            return result;
//         }
//      };
//   }

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
      text = text.replaceAll("Static", "");
      text = text.replaceAll("static", "");

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
         while (reader.readLine() != null)
            lines++;
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

   public static String replaceStrings(String text) {
	// Patterns for single and double quotes
	   Pattern singleQuotePattern = Pattern.compile("'(?:[^'\\\\]|\\\\.)*'");
	   Pattern doubleQuotePattern = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"");

	   // Replace single-quoted strings first
	   Matcher doubleMatcher = doubleQuotePattern.matcher(text);
	   
	   String doubleReplaced = doubleMatcher.replaceAll("__STR");

	   // Now replace double-quoted strings in the result from the first replacement
	   Matcher singleMatcher = singleQuotePattern.matcher(doubleReplaced);
	   String singleReplaced = singleMatcher.replaceAll("__STR");

	   return singleReplaced;
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
            }
            else if (!line.isEmpty() && line.startsWith("\"")) {
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
 * // Step 1. Remove special characters. static void removeSpecialChars() {
 * List<String> cleanedListPreMethods = new ArrayList<>(); for (String
 * iPreMethod : listPreMethods) { // int predIndex = iPreMethod.indexOf(SEARCH);
 * // // if (predIndex == -1) { // System.out.println("[ERR] " + SEARCH +
 * " term not found in target: " + iPreMethod); // throw new
 * RuntimeException("[ERR] " + SEARCH + " term not found in target: " +
 * iPreMethod); // // do not continue the iteration but need to exam the dataset
 * first. // }
 * 
 * String cleanedMethod = removeJavaModifiers(iPreMethod); cleanedMethod =
 * cleanedMethod.replaceAll("[^A-Za-z0-9]+", "").toLowerCase(); cleanedMethod =
 * cleanedMethod.replaceAll("\\s+", "");
 * cleanedListPreMethods.add(cleanedMethod); } listPreMethodsClean =
 * cleanedListPreMethods; System.out.println("[DBG] listPreMethodsClean: " +
 * listPreMethodsClean);
 * 
 * List<String> cleanedListRawMethods = new ArrayList<>(); for (String rawMethod
 * : listRawMethods) { String cleanedMethod = removeJavaModifiers(rawMethod);
 * cleanedMethod = cleanedMethod.replaceAll("[^A-Za-z0-9]+", "").toLowerCase();
 * cleanedMethod = cleanedMethod.replaceAll("\\s+", "");
 * cleanedListRawMethods.add(cleanedMethod); } listRawMethodsClean =
 * cleanedListRawMethods; System.out.println("[DBG] listRawMethodsClean: " +
 * listRawMethodsClean); }
 * 
 * // Step 2. Find matched raw methods. static List<String>
 * findMatchedRawMethods() { List<String> outputMatched = new
 * ArrayList<String>();
 * 
 * String[] rawMethods = listRawMethodsClean.toArray(new String[0]);
 * 
 * Arrays.sort(rawMethods);
 * 
 * for (String iPreMethod : listPreMethodsClean) { Comparator<String>
 * customComparator = createComparator(); int foundIndex =
 * Arrays.binarySearch(rawMethods, iPreMethod, customComparator);
 * 
 * 
 * // Output format to be saved. // foundIndx // iPreMethod //
 * rawMethods[foundIndex] // // 1 // m1() {.. pred ..} // m1() {...} // 2 //
 * m2() {.. pred ..} // m2 {...}
 * 
 * if (foundIndex >= 0) { outputMatched.add("Match Found"); outputMatched.add(""
 * + foundIndex); outputMatched.add(iPreMethod);
 * outputMatched.add(rawMethods[foundIndex]); } }
 * UtilFile.writeFile(outputMatched, FILE_MATCHED_METHODS); return
 * outputMatched; }
 */
