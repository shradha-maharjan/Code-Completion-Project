import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class similaritythreshold {

    private static final String OUTPUT_FILE = "input/finetune_valid_compare_output.txt";

    public static void main(String[] args) {
        try {
            List<String> highScoreEntries = getHighSimilarityScores(OUTPUT_FILE, 0.7);
            System.out.println("Count of entries with similarity > 0.7: " + highScoreEntries.size());
            highScoreEntries.forEach(System.out::println);
        } catch (IOException e) {
            System.err.println("Error reading from file: " + e.getMessage());
        }
    }

    private static List<String> getHighSimilarityScores(String filePath, double threshold) throws IOException {
        List<String> results = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            StringBuilder currentEntry = new StringBuilder();
            double currentRatio = 0.0;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Line")) {
                    if (currentEntry.length() > 0 && currentRatio > threshold) {
                        results.add(currentEntry.toString());
                        currentEntry.setLength(0);  // Reset for the next entry
                    }
                    currentRatio = 0.0;  // Reset ratio for new entry
                }

                if (line.startsWith("Highest Similarity Ratio:")) {
                    String ratioString = line.substring(line.lastIndexOf(":") + 1).trim();
                    currentRatio = Double.parseDouble(ratioString);
                }

                currentEntry.append(line).append("\n");  // Continuously collect lines

                // Ensure the last entry is added if it meets the threshold
                if (!reader.ready() && currentEntry.length() > 0 && currentRatio > threshold) {
                    results.add(currentEntry.toString());
                }
            }
        }

        return results;
    }
}



//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public class similaritythreshold {
//    
//    private static final String OUTPUT_FILE = "input/finetune_valid_compare_output.txt";
//
//    public static void main(String[] args) {
//        try {
//            System.out.println("Count of lines with similarity > 0.7: " + countHighSimilarityScores(OUTPUT_FILE, 0.7));
//        } catch (IOException e) {
//            System.err.println("Error reading from file: " + e.getMessage());
//        }
//    }
//
//    private static int countHighSimilarityScores(String filePath, double threshold) throws IOException {
//        int count = 0;
//        Pattern pattern = Pattern.compile("Highest Similarity Ratio: (\\d\\.\\d{2})");
//        
//        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                Matcher matcher = pattern.matcher(line);
//                if (matcher.find()) {
//                    double ratio = Double.parseDouble(matcher.group(1));
//                    if (ratio > threshold) {
//                        count++;
//                    }
//                }
//            }
//        }
//        
//        return count;
//    }
//}
