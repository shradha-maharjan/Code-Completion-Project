package match;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class MainHashTableFineTuneData {

	private static final Pattern STRING_MATCHING_PATTERN = Pattern
			.compile("([bruf]*)(\"\"\"|'''|\"|')(?:(?!\\2)(?:\\\\.|[^\\\\]))*\\2");

//	static String WORK_SPT_CODE = "/home/user1-selab3/Documents/research-shradha/CODE-SPT-Code/";
	// Datasets of raw and pred
	static String DIR_INPUT_RAW = //WORK_SPT_CODE +
			"dataset/finetune_raw/java-small-json/";
	static String INPUT_JSON = "java-small.val.json";
	static String OUTPUT_PRED_RAW = "output/finetune-val-pre-raw-new.txt";

	// Preprocessed data of SPT-Code
	static String DIR__PREP_SPT_CODE = //WORK_SPT_CODE + 
			"dataset/fine_tune/completion/";
	static String INPUT_PREP_SPT_CODE = "data.TargetType.seq.valid.source.txt";

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
	}

	private static void match(HashMap<String, String> methodMap, String filePath) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(filePath));

		String line;
		while ((line = reader.readLine()) != null) {
			methodMap.get(line);
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
				String leftContext = processJavaCode((String) jsonObject.get("left_context"));
				String rightContext = processJavaCode((String) jsonObject.get("right_context"));
				String targetSeq = processJavaCode((String) jsonObject.get("target_seq"));

				String predictedMethod = leftContext + "PRED" + rightContext;
				String completeMethod = leftContext + " " + targetSeq + " " + rightContext;

				String DUP = "";
				if (methodMap.containsKey(predictedMethod)) {
					DUP = "___DUP_K___";
				}
				methodMap.put(predictedMethod, completeMethod);

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

	private static String processJavaCode(String code) {
		Matcher matcher = STRING_MATCHING_PATTERN.matcher(code);
		String result = matcher.replaceAll("___STR");
		result = result.replace("\n", "").replace("=", "").replace("\\", "").replace("\"", "").replace("\r", "")
				.replace("\t", "");
		return result;
	}

	static void timeNow(ZonedDateTime zonedDateTime, String msg) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
		String formattedTime = formatter.format(zonedDateTime);
		System.out.println(msg + formattedTime);
	}
}
