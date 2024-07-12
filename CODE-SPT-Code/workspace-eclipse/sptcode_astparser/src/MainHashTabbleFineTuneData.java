import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class MainHashTabbleFineTuneData {

	private static final Pattern STRING_MATCHING_PATTERN = Pattern
			//.compile("([bruf]*)(\"\"\"|'''|\"|')(?:(?!\\2)(?:\\\\.|[^\\\\]))*\\2");
	.compile("'(?:\\\\.|[^\\\\'])*'|\"(?:\\\\.|[^\\\\\"])*\"");
    
	
	private static final Pattern MODIFIERS_PATTERN = Pattern.compile("\\b(@override\\\\s+)?(public|private|protected)(\\s+static)?\\b");//
	//private static final String SEARCH = "pred"; // Adjust SEARCH to match your actual search keyword

	private static final Set<String> JAVA_KEYWORDS = new HashSet<>(Arrays.asList("private", "protected", "public", "override","static", "@Overrideprotected", "@Overridepublic", "@Overrideprivate", "Protected", "Public", "Private"));//

	//static String WORK_SPT_CODE = "/home/user1-selab3/Documents/research-shradha/CODE-SPT-Code/";
	// Datasets of raw and pred
	

//	static String DIR_INPUT_RAW = //WORK_SPT_CODE +
//			"dataset/finetune_raw/java-small-json/";
//	static String INPUT_JSON = "java-small.val.json";//"test.json";//
//	static String OUTPUT_PRED_RAW = "output/finetune-val-pre-raw-new.txt";//"output/test-raw-pre.txt";//
//	static String matchedoutputFile = "output/output1-pre-matched.txt";
//	static String unmatchedOutputFile = "output/output1-pre-Unmatched.txt";
//
//	// Preprocessed data of SPT-Code
//	static String DIR__PREP_SPT_CODE = //WORK_SPT_CODE + 
//			"dataset/fine_tune/completion/";
//	static String INPUT_PREP_SPT_CODE = "data.TargetType.seq.valid.source.txt";//"test.txt";//
	
//	static String DIR_INPUT_RAW = //WORK_SPT_CODE + 
//			"dataset/finetune_raw/java-small-json/";
//	static String INPUT_JSON = "java-small.test.json";//"test.json";//
//	static String OUTPUT_PRED_RAW = "output/finetune-test-pre-raw.txt";//"output/test-raw-pre.txt";//
//	static String matchedoutputFile = "output/test-pre-matched.txt";
//	static String unmatchedOutputFile = "output/test-pre-Unmatched.txt";
	
	static String DIR_INPUT_RAW = //WORK_SPT_CODE + 
			"dataset/finetune_raw/java-small-json/";
	static String INPUT_JSON = "java-small.train.json";//"test.json";//
	static String OUTPUT_PRED_RAW = "output/finetune-train-pre-raw.txt";//"output/test-raw-pre.txt";//
	static String matchedoutputFile = "output/train-pre-matched.txt";
	static String unmatchedOutputFile = "output/train-pre-Unmatched.txt";

	// Preprocessed data of SPT-Code
	static String DIR__PREP_SPT_CODE = //WORK_SPT_CODE + 
			"dataset/fine_tune/completion/";
	static String INPUT_PREP_SPT_CODE = "data.TargetType.seq.train.source.txt";//"test.txt";//

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

		MainHashTabbleFineTuneData main = new MainHashTabbleFineTuneData();
		try {
			HashMap<String, String> methodMap = main.createHashMap(DIR_INPUT_RAW, INPUT_JSON, OUTPUT_PRED_RAW);
			match(methodMap, DIR__PREP_SPT_CODE + INPUT_PREP_SPT_CODE);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Instant endTime = Instant.now();
		Duration duration = Duration.between(startTime, endTime);
		long seconds = duration.getSeconds();
		String durStr = duration.toString().substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase();
		System.out.println("[DBG] Duration: " + seconds + ", " + durStr);
		timeNow(ZonedDateTime.now(), "End Time: ");
		
		System.out.println("[DBG] # Matched Preproc Methods: " + countLines(matchedoutputFile));
		System.out.println("[DBG] # Unmatched Preproc Methods: " + countLines(unmatchedOutputFile));
	}

	private static void match(HashMap<String, String> methodMap, String filePath) throws Exception {
//		BufferedReader reader = new BufferedReader(new FileReader(filePath));
//
//		String line;
//		while ((line = reader.readLine()) != null) {
//			methodMap.get(line);
//		}
//	}
	
	 // Use try-with-resources to ensure the BufferedReader is closed after use
    try (BufferedReader reader = new BufferedReader(new FileReader(filePath));
    	 BufferedWriter writer = new BufferedWriter(new FileWriter(matchedoutputFile)) ;
	     BufferedWriter unmatchedWriter = new BufferedWriter(new FileWriter(unmatchedOutputFile))) {
        String line;
        int lineCount = 0;  // To keep track of line numbers for detailed output
        while ((line = reader.readLine()) != null) {
            lineCount++;
            String cleanedMethod = removeSpecialChars(processJavaCode(line));
            //System.out.println("cleanedMethod: " + cleanedMethod);
            if (methodMap.containsKey(cleanedMethod)) {
                String matchedMethod = methodMap.get(cleanedMethod);
                // Write the successful match along with the line number to the output file
                writer.write("[MATCH] Line " + lineCount + ": Found match for '" + cleanedMethod + "' -> " + matchedMethod + "\n");
            } else {
                // Write the absence of a match along with the line number to the output file
            	//unmatchedWriter.write("[NO MATCH] Line " + lineCount + ": No match found for '" + line + "'\n");
            	unmatchedWriter.write(line + "'\n");
            }
        }
    } catch (IOException e) {
        System.err.println("Error reading the file or writing to the output file: " + e.getMessage());
    }
}
	HashMap<String, String> createHashMap(String dirInput, String inputFile, String outputPath) throws Exception {
		HashMap<String, String> methodMap = new HashMap<>();
		int lineCount = 0;
		BufferedReader reader = new BufferedReader(new FileReader(dirInput + inputFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

		JSONParser parser = new JSONParser();
		String line;
		while ((line = reader.readLine()) != null) {
			lineCount++;
			try {
				JSONObject jsonObject = (JSONObject) parser.parse(line);
				String leftContext = (String) jsonObject.get("left_context");
				String rightContext = (String) jsonObject.get("right_context");
				String targetSeq = (String) jsonObject.get("target_seq");

				String predictedMethod = leftContext + "PRED" + rightContext;
				String completeMethod = leftContext + " " + targetSeq + " " + rightContext;
				
                // Cleaned Methods
                String cleanedPredictedMethod = removeSpecialChars(processJavaCode_raw(predictedMethod));
                //System.out.println("cleanedPredictedMethod: " + cleanedPredictedMethod);
                String cleanedCompleteMethod = removeSpecialChars(processJavaCode_raw(completeMethod));
                //System.out.println("cleanedCompleteMethod: " + cleanedCompleteMethod);

				String DUP = "";
				if (methodMap.containsKey(predictedMethod)) {
					DUP = "___DUP_K___";
				}
				methodMap.put(cleanedPredictedMethod, cleanedCompleteMethod);

				writer.write(DUP + completeMethod + "\n");
				writer.write(predictedMethod + "\n");
			} catch (ParseException e) {
				System.out.println("Error decoding JSON on input line " + lineCount);
			}
		}
		reader.close();
		writer.close();

		System.out.println("[DBG] Input lines read: " + lineCount);
		System.out.println("[DBG] Hash map size: " + methodMap.size());

		return methodMap;
	}

	private static String processJavaCode_raw(String code) {
		code = code.replaceAll(" _ 's","s");
		code = code.replaceAll("job's","jobs");
		Matcher matcher = STRING_MATCHING_PATTERN.matcher(code);
		String result = matcher.replaceAll("___STR");
		//result = result.replaceAll("//","");
		result = removeCommentsAndStrings_raw(result);
		result = result.replace("\n", "").replace("=", "").replace("\\", "").replace("\"", "").replace("\r", "")
				.replace("\t", "");
		return result;
	}
	
	private static String processJavaCode(String code) {
		code = code.replaceAll(" _ 's","s");
		code = code.replaceAll("job's","jobs");
		Matcher matcher = STRING_MATCHING_PATTERN.matcher(code);
		String result = matcher.replaceAll("___STR");
		//result = result.replaceAll("//","");
		result = removeComments(result);
		result = result.replace("\n", "").replace("=", "").replace("\\", "").replace("\"", "").replace("\r", "")
				.replace("\t", "");
		return result;
	}
	
	private static String removeSpecialChars(String method) {
	    // Assuming that the same normalization and cleaning operations are to be performed
	    method = removeJavaModifiers(formatCode(method));
	    //System.out.println(method);
	    method = method.replaceAll("[^A-Za-z0-9{}()\\[\\]]+", "").toLowerCase();
	    //System.out.println(method);
	    method = method.replaceAll("\\s+", "");
	    return method;
	}
	
	private static String removeCommentsAndStrings_raw(String code) {
		//String text = code.replaceAll("//", "");
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
	    
	    String result = sb.toString();
//	    System.out.println("RESULT :"+result);
//	    result = result.replaceAll("//.*?$", " "); // Remove inline single-line comments
//        result = result.replaceAll("/\\*.*?\\*/", " "); // Remove inline multi-line comments
        
	    return result;
	}
	
	private static String removeComments(String code) {
		return code = code.replaceAll("//", " ");
	}

	private static String removeJavaModifiers(String text) {
	      text = text.replaceAll("@Overrideprotected\\s*", "");
	      text = text.replaceAll("@Overridepublic\\s*", "");
	      text = text.replaceAll("@Overrideprivate\\s*", "");
	      text = text.replaceAll("@Override", "");
	      text = text.replaceAll("Override", "");
	      text = text.replaceAll("override", "");
	      text = text.replaceAll("OVERRIDE", "");
	      text = text.replaceAll("protected", "");
	      text = text.replaceAll("Protected", "");
	      text = text.replaceAll("PROTECTED", "");
	      text = text.replaceAll("public", "");
	      text = text.replaceAll("Public", "");
	      text = text.replaceAll("PUBLIC", "");
	      text = text.replaceAll("private", "");
	      text = text.replaceAll("Private", "");
	      text = text.replaceAll("PRIVATE", "");
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

