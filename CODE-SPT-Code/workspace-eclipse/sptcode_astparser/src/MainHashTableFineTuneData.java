import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class MainHashTableFineTuneData {

	private static final Pattern STRING_MATCHING_PATTERN = Pattern
			.compile("([bruf]*)(\"\"\"|'''|\"|')(?:(?!\\2)(?:\\\\.|[^\\\\]))*\\2");
	
	private static final Pattern MODIFIERS_PATTERN = Pattern.compile("\\b(@override\\s+)?(public|private|protected)(\\s+static)?\\b");
	//private static final String SEARCH = "pred"; // Adjust SEARCH to match your actual search keyword

	private static final Set<String> JAVA_KEYWORDS = new HashSet<>(Arrays.asList("private", "protected", "public", "static", "@Overrideprotected", "override", "@Overridepublic", "@Overrideprivate", "Protected", "Public", "Private"));

//	static String WORK_SPT_CODE = "/home/user1-selab3/Documents/research-shradha/CODE-SPT-Code/";
	// Datasets of raw and pred
	static String DIR_INPUT_RAW = //WORK_SPT_CODE +
			"dataset/finetune_raw/java-small-json/";
	static String INPUT_JSON = "java-small.val.json";//"test.json";//
	static String OUTPUT_PRED_RAW = "output/finetune-val-pre-raw-new.txt";//"output/test-raw-pre.txt";//
	static String matchedoutputFile = "output/output1-pre-matched.txt";
	static String unmatchedOutputFile = "output/output1-pre-Unmatched.txt";

	// Preprocessed data of SPT-Code
	static String DIR__PREP_SPT_CODE = //WORK_SPT_CODE + 
			"dataset/fine_tune/completion/";
	static String INPUT_PREP_SPT_CODE = "data.TargetType.seq.valid.source.txt";//"test.txt";//

	public static void main(String[] args) {
		if (!new File(DIR_INPUT_RAW + INPUT_JSON).exists()) {
			System.out.println("[WRN] File Not Found: " + DIR_INPUT_RAW + INPUT_JSON);
			System.exit(-1);
		}
		if (!new File(DIR__PREP_SPT_CODE + INPUT_PREP_SPT_CODE).exists()) {
			System.out.println("[WRN] File Not Found: " + DIR__PREP_SPT_CODE + INPUT_PREP_SPT_CODE);
			System.exit(-1);
		}
		System.out.println("[DBG] Input of JSON: " + INPUT_JSON);
		System.out.println("[DBG] Output of PRED and RAW: " + OUTPUT_PRED_RAW);
		System.out.println("[DBG] Input of SPT-Code data: " + INPUT_PREP_SPT_CODE);

		Instant startTime = Instant.now();
		timeNow(ZonedDateTime.now(), "Start Time: ");

		MainHashTableFineTuneData main = new MainHashTableFineTuneData();
//		try {
//			HashMap<String, String> methodMap = main.createHashMap(DIR_INPUT_RAW, INPUT_JSON, OUTPUT_PRED_RAW);
//			match(methodMap, DIR__PREP_SPT_CODE + INPUT_PREP_SPT_CODE);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		try {
            String[][] methodPairs = createMethodPairs(DIR_INPUT_RAW, INPUT_JSON, OUTPUT_PRED_RAW);
            match(methodPairs, DIR__PREP_SPT_CODE + INPUT_PREP_SPT_CODE, matchedoutputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

		Instant endTime = Instant.now();
		Duration duration = Duration.between(startTime, endTime);
		long seconds = duration.getSeconds();
		String durStr = duration.toString().substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase();
		System.out.println("[DBG] Duration: " + seconds + ", " + durStr);
		timeNow(ZonedDateTime.now(), "End Time: ");
		
		System.out.println("[DBG] # Matched Preproc Methods: " + countDistinctMatches(matchedoutputFile));
		System.out.println("[DBG] # Unmatched Preproc Methods: " + countLines(unmatchedOutputFile));
	}

//	private static void match(HashMap<String, String> methodMap, String filePath) throws Exception {
////		BufferedReader reader = new BufferedReader(new FileReader(filePath));
////
////		String line;
////		while ((line = reader.readLine()) != null) {
////			methodMap.get(line);
////		}
////	}
//		
//		 // Use try-with-resources to ensure the BufferedReader is closed after use
//		    try (BufferedReader reader = new BufferedReader(new FileReader(filePath));
//		         BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
//		        String line;
//		        int lineCount = 0;  // To keep track of line numbers for detailed output
//		        while ((line = reader.readLine()) != null) {
//		            lineCount++;
//		            String cleanedMethod = removeSpecialChars(line);
//		            if (methodMap.containsKey(cleanedMethod)) {
//		                String matchedMethod = methodMap.get(cleanedMethod);
//		                // Write the successful match along with the line number to the output file
//		                writer.write("[MATCH] Line " + lineCount + ": Found match for '" + cleanedMethod + "' -> " + matchedMethod + "\n");
//		            } else {
//		                // Write the absence of a match along with the line number to the output file
//		                writer.write("[NO MATCH] Line " + lineCount + ": No match found for '" + cleanedMethod + "'\n");
//		            }
//		        }
//		    } catch (IOException e) {
//		        System.err.println("Error reading the file or writing to the output file: " + e.getMessage());
//		    }
//		}
//
//	HashMap<String, String> createHashMap(String dirInput, String inputFile, String outputPath) throws Exception {
//		HashMap<String, String> methodMap = new HashMap<>();
//		int lineCount = 0;
//		BufferedReader reader = new BufferedReader(new FileReader(dirInput + inputFile));
//		BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));
//
//		JSONParser parser = new JSONParser();
//		String line;
//		while ((line = reader.readLine()) != null) {
//			lineCount++;
//			try {
//				JSONObject jsonObject = (JSONObject) parser.parse(line);
//				String leftContext = processJavaCode((String) jsonObject.get("left_context"));
//				String rightContext = processJavaCode((String) jsonObject.get("right_context"));
//				String targetSeq = processJavaCode((String) jsonObject.get("target_seq"));
//
//				String predictedMethod = removeSpecialChars(leftContext + "PRED" + rightContext);
//				String completeMethod = removeSpecialChars(leftContext + " " + targetSeq + " " + rightContext);
//
//				String DUP = "";
//				if (methodMap.containsKey(predictedMethod)) {
//					DUP = "___DUP_K___";
//				}
//				methodMap.put(predictedMethod, completeMethod);
//
//				writer.write(DUP + completeMethod + "\n");
//				writer.write(predictedMethod + "\n");
//			} catch (ParseException e) {
//				System.out.println("Error decoding JSON on input line " + lineCount);
//			}
//		}
//		reader.close();
//		writer.close();
//
//		System.out.println("[DBG] Input lines read: " + lineCount);
//		System.out.println("[DBG] Hash map size: " + methodMap.size());
//
//		return methodMap;
//	}
	
	private static String[][] createMethodPairs(String dirInput, String inputFile, String outputPath) throws Exception {
	    List<String[]> methodPairs = new ArrayList<>();
	    try (BufferedReader reader = new BufferedReader(new FileReader(dirInput + inputFile));
	         BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
	        JSONParser parser = new JSONParser();
	        String line;
	        while ((line = reader.readLine()) != null) {
	            try {
	                JSONObject jsonObject = (JSONObject) parser.parse(line);
	                String leftContext = processJavaCode((String) jsonObject.get("left_context"));
					String rightContext = processJavaCode((String) jsonObject.get("right_context"));
					String targetSeq = processJavaCode((String) jsonObject.get("target_seq"));
					
					String originalPredictedMethod = leftContext + "PRED" + rightContext;
	                String originalCompleteMethod = leftContext + " " + targetSeq + " " + rightContext;

	                // Cleaned Methods
	                String cleanedPredictedMethod = removeSpecialChars_raw(originalPredictedMethod);
	                System.out.println(cleanedPredictedMethod);
	                String cleanedCompleteMethod = removeSpecialChars_raw(originalCompleteMethod);
	                System.out.println(cleanedCompleteMethod);
	                
//	                String predictedMethod = removeSpecialChars_raw(leftContext + "PRED" + rightContext);
//	                String completeMethod = removeSpecialChars_raw(leftContext + " " + targetSeq + " " + rightContext);

//	                methodPairs.add(new String[]{predictedMethod, completeMethod});
//	                writer.write(predictedMethod + "\n" + completeMethod + "\n");
//	            } catch (ParseException e) {
//	                System.out.println("Error decoding JSON on input line: " + e.getMessage());
//	            }
//	        }
//	    }
//	    // Sort the method pairs based on predicted methods (first element)
//	    methodPairs.sort(Comparator.comparing(o -> o[0]));
//	    return methodPairs.toArray(new String[0][0]);
//	}
	                methodPairs.add(new String[]{cleanedPredictedMethod, originalPredictedMethod, cleanedCompleteMethod, originalCompleteMethod});
	                writer.write(cleanedPredictedMethod + "\n" + cleanedCompleteMethod + "\n");
	            } catch (ParseException e) {
	                System.out.println("Error decoding JSON on input line: " + e.getMessage());
	            }
	        }
	    }
	    // Sort the method pairs based on cleaned predicted methods (first element)
	    methodPairs.sort(Comparator.comparing(o -> o[0]));
	    return methodPairs.toArray(new String[0][0]);
	}

	private static void match(String[][] methodPairs, String filePath, String outputFilePath) {
	    try (BufferedReader reader = new BufferedReader(new FileReader(filePath));
	         BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath)) ;
	    	BufferedWriter unmatchedWriter = new BufferedWriter(new FileWriter(unmatchedOutputFile))) {
	        String line;
	        while ((line = reader.readLine()) != null) {
	            String cleanedMethod = removeSpecialChars(processJavaCode(line));
	            System.out.println(cleanedMethod);
	            //int resultIndex = Arrays.binarySearch(methodPairs, new String[]{cleanedMethod, null}, Comparator.comparing(o -> o[0]));
	            int resultIndex = Arrays.binarySearch(methodPairs, new String[]{cleanedMethod, null, null, null}, Comparator.comparing(o -> o[0]));
	            
	            if (resultIndex >= 0) {
	            	String[] matchedPair = methodPairs[resultIndex];
	                writer.write("Match found for '" + line + "' (Cleaned: '" + cleanedMethod + "'):\n");
	                writer.write("Predicted Method (Original): " + matchedPair[1] + "\n");
	                writer.write("Complete Method (Original): " + matchedPair[3] + "\n\n");
	            } else {
	            	unmatchedWriter.write( line + "' (Cleaned: '" + cleanedMethod + "')\n");
	            }
	        }
	    } catch (IOException e) {
	        System.err.println("Error reading the file or writing to the output file: " + e.getMessage());
	    }
	}


	private static String processJavaCode(String code) {
		code = removeCommentsAndStrings(code);
		Matcher matcher = STRING_MATCHING_PATTERN.matcher(code);
		String result = matcher.replaceAll("___STR");
		result = result.replace("\n", "").replace("=", "").replace("\\", "").replace("\"", "").replace("\r", "")
				.replace("\t", "");
		return result;
	}
	
	private static String removeSpecialChars(String method) {
	    // Assuming that the same normalization and cleaning operations are to be performed
	    method = removeJavaModifiers(formatCode(method));
	    method = method.replaceAll("[^A-Za-z0-9{}()\\[\\]]+", "").toLowerCase();
	    method = method.replaceAll("\\s+", "");
	    return method;
	}
	
	private static String removeSpecialChars_raw(String method) {
	    // Assuming that the same normalization and cleaning operations are to be performed
		method = removeJavaModifiers(formatCode(method));
	    method = method.replaceAll("[^A-Za-z0-9{}()\\[\\]]+", "").toLowerCase();
	    method = method.replaceAll("\\s+", "");
	    return method;
	}
	
	private static String removeCommentsAndStrings(String code) {
	    // Regular expression to identify comments and string literals
	    String regex = "//.*?$|/\\*.*?\\*/|'(?:\\\\.|[^\\\\'])*'|\"(?:\\\\.|[^\\\\\"])*\"";
	    Pattern pattern = Pattern.compile(regex, Pattern.DOTALL | Pattern.MULTILINE);
	    Matcher matcher = pattern.matcher(code);

	    // Use a StringBuffer to hold the transformed code
	    StringBuffer sb = new StringBuffer();
	    while (matcher.find()) {
	        String match = matcher.group(0);
	        // Replace comments with a space and keep string literals intact
	        if (match.startsWith("//") || match.startsWith("/*")) {
	            matcher.appendReplacement(sb, " ");
	        } else {
	            // Escape the backslashes and dollar signs in the replacement string
	            matcher.appendReplacement(sb, Matcher.quoteReplacement(match));
	        }
	    }
	    matcher.appendTail(sb);

	    return sb.toString();
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
	      text = text.replaceAll("Static", "");
	      text = text.replaceAll("static", "");
	      text = text.replaceAll("STATIC", "");

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

	//   private static String replaceStrings(String text) {
//	      Matcher matcher = STRING_PATTERN.matcher(text);
//	      return matcher.replaceAll("___STR");
	//   }

	   private static String formatCode(String rawCode) {
	      return rawCode.replace(";", ";\n").replace("{", "{\n").replace("}", "\n}");
	   }
	   
	   public static int countDistinctMatches(String filename) {
		    int distinctMatches = 0;
		    boolean foundMatch = false;

		    try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
		        String line;
		        while ((line = reader.readLine()) != null) {
		            // Check if the line indicates the start of a match entry
		            if (line.startsWith("Match found for") && !foundMatch) {
		                distinctMatches++;
		                foundMatch = true; // Set flag to indicate we are within a match description
		            }
		            // Check for the end of a match description
		            else if (line.isEmpty()) {
		                foundMatch = false; // Reset flag when an empty line is encountered, assuming each match is followed by an empty line
		            }
		        }
		    } catch (IOException e) {
		        System.out.println("Error reading file: " + e.getMessage());
		    }

		    return distinctMatches;
		}


	static void timeNow(ZonedDateTime zonedDateTime, String msg) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
		String formattedTime = formatter.format(zonedDateTime);
		System.out.println(msg + formattedTime);
	}
}
