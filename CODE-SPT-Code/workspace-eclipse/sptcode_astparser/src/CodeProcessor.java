import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;

public class CodeProcessor {

    private static final Pattern STRING_MATCHING_PATTERN = Pattern.compile("([bruf]*)(\"\"\"|'''|\"|')(?:(?!\\2)(?:\\\\.|[^\\\\]))*\\2");

    public static void main(String[] args) {
        String filePath = "input/java-small.train.json";
        String outputPath = "output/finetune-train-pre-raw.txt";
        HashMap<String, String> methodMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            JSONParser parser = new JSONParser();
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                try {
                    JSONObject jsonObject = (JSONObject) parser.parse(line);
                    String leftContext = processJavaCode((String) jsonObject.get("left_context"));
                    String rightContext = processJavaCode((String) jsonObject.get("right_context"));
                    String targetSeq = processJavaCode((String) jsonObject.get("target_seq"));

                    String completeMethod = leftContext + " " + targetSeq + " " + rightContext;
                    String predictedMethod = leftContext + "[PRED]" + rightContext;

                    methodMap.put("PRED method " + lineCount, predictedMethod);
                    methodMap.put("RAW method " + lineCount, completeMethod);

                    writer.write(completeMethod + "\n");
                    writer.write(predictedMethod + "\n");
                } catch (ParseException e) {
                    System.out.println("Error decoding JSON on input line " + lineCount);
                }
            }
            System.out.println("Total input lines read: " + lineCount);
            System.out.println("Total output lines prepared: " + methodMap.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String processJavaCode(String code) {
        Matcher matcher = STRING_MATCHING_PATTERN.matcher(code);
        String result = matcher.replaceAll("___STR");
        result = result.replace("\n", "").replace("=", "").replace("\\", "").replace("\"", "").replace("\r", "").replace("\t", "");
        return result;
    }
}
