import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class similaritythreshold {

    private static final String INPUT_FILE = "input/finetune_valid_compare_output.txt"; 
    private static final String OUTPUT_FILE = "output/high_similarity_entries_7.txt"; 
    private static final double THRESHOLD = 0.7;

    public static void main(String[] args) {
        try {
            List<String> highSimilarityEntries = filterHighSimilarityEntries(INPUT_FILE, THRESHOLD);
            writeResultsToFile(highSimilarityEntries, OUTPUT_FILE); 
            System.out.println("Total entries with similarity > " + THRESHOLD + ": " + highSimilarityEntries.size());
        } catch (IOException e) {
            System.err.println("Error processing files: " + e.getMessage());
        }
    }

    private static List<String> filterHighSimilarityEntries(String filePath, double threshold) throws IOException {
        List<String> results = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder currentEntry = new StringBuilder();
            String line;
            double currentRatio = 0.0;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Line")) {
                    if (currentEntry.length() > 0 && currentRatio > threshold) {
                        results.add(extractRelevantDetails(currentEntry.toString()));
                    }
                    currentEntry.setLength(0);  
                } else if (line.startsWith("Highest Similarity Ratio:")) {
                    String ratioString = line.split(": ")[1].trim();
                    currentRatio = Double.parseDouble(ratioString);
                }

                currentEntry.append(line).append("\n");

                // Reset the ratio after processing each entry
                if (line.startsWith("Highest Similarity Ratio:")) {
                    if (currentRatio > threshold) {
                        results.add(extractRelevantDetails(currentEntry.toString())); 
                    }
                    currentEntry.setLength(0);  // Reset the entry string for the next block
                    currentRatio = 0.0; // Reset ratio for new entry
                }
            }

            // added if it meets the threshold
            if (currentEntry.length() > 0 && currentRatio > threshold) {
                results.add(extractRelevantDetails(currentEntry.toString()));
            }
        }

        return results;
    }

    private static void writeResultsToFile(List<String> entries, String resultFilePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(resultFilePath))) {
            entries.forEach(writer::println);
        }
    }

    // Extracts and formats relevant details from a complete entry
    private static String extractRelevantDetails(String entry) {
        StringBuilder details = new StringBuilder();
        String[] lines = entry.split("\n");

        for (String line : lines) {
            if (line.startsWith("File1:") || line.startsWith("File2:") || line.startsWith("Highest Similarity Ratio:")) {
                details.append(line).append("\n");
            }
        }
        return details.toString();
    }
}


//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//public class similaritythreshold {
//
//    private static final String INPUT_FILE = "input/finetune_valid_compare_output.txt"; // Path to your input file
//    private static final double THRESHOLD = 0.9;
//
//    public static void main(String[] args) {
//        try {
//            List<String> highSimilarityEntries = filterHighSimilarityEntries(INPUT_FILE, THRESHOLD);
//            highSimilarityEntries.forEach(System.out::println); // Print each high similarity entry
//        } catch (IOException e) {
//            System.err.println("Error reading from file: " + e.getMessage());
//        }
//    }
//
//    private static List<String> filterHighSimilarityEntries(String filePath, double threshold) throws IOException {
//        List<String> results = new ArrayList<>();
//        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
//            StringBuilder currentEntry = new StringBuilder();
//            String line;
//            double currentRatio = 0.0;  // Default to 0 to handle entries below threshold initially
//
//            while ((line = reader.readLine()) != null) {
//                if (line.startsWith("Line")) {
//                    if (currentEntry.length() > 0 && currentRatio > threshold) {
//                        results.add(extractRelevantDetails(currentEntry.toString())); // Process and add the entry
//                    }
//                    currentEntry.setLength(0); // Reset for the next entry
//                } else if (line.startsWith("Highest Similarity Ratio:")) {
//                    String ratioString = line.split(": ")[1].trim(); // Extract the ratio value
//                    currentRatio = Double.parseDouble(ratioString);
//                }
//
//                currentEntry.append(line).append("\n"); // Append line to the current entry
//
//                // Reset the ratio after processing each entry
//                if (line.startsWith("Highest Similarity Ratio:")) {
//                    if (currentRatio > threshold) {
//                        results.add(extractRelevantDetails(currentEntry.toString())); // Add entry if above threshold
//                    }
//                    currentEntry.setLength(0);  // Reset the entry string for the next block
//                    currentRatio = 0.0; // Reset ratio for new entry
//                }
//            }
//
//            // Ensure the last entry is added if it meets the threshold
//            if (currentEntry.length() > 0 && currentRatio > threshold) {
//                results.add(extractRelevantDetails(currentEntry.toString()));
//            }
//        }
//
//        return results;
//    }
//
//    // Extracts and formats relevant details from a complete entry
//    private static String extractRelevantDetails(String entry) {
//        StringBuilder details = new StringBuilder();
//        String[] lines = entry.split("\n");
//
//        for (String line : lines) {
//            if (line.startsWith("File1:") || line.startsWith("File2:") || line.startsWith("Highest Similarity Ratio:")) {
//                details.append(line).append("\n");
//            }
//        }
//        return details.toString();
//    }
//}


//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//public class similaritythreshold {
//
//    private static final String OUTPUT_FILE = "input/finetune_valid_compare_output.txt";
//
//    public static void main(String[] args) {
//        try {
//            List<String> highScoreEntries = getHighSimilarityScores(OUTPUT_FILE, 0.9);
//            System.out.println("Count of entries with similarity > 0.9: " + highScoreEntries.size());
//            highScoreEntries.forEach(System.out::println);
//        } catch (IOException e) {
//            System.err.println("Error reading from file: " + e.getMessage());
//        }
//    }
//
//    private static List<String> getHighSimilarityScores(String filePath, double threshold) throws IOException {
//        List<String> results = new ArrayList<>();
//        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
//            String line;
//            StringBuilder currentEntry = new StringBuilder();
//            double currentRatio = 0.0;
//
//            while ((line = reader.readLine()) != null) {
//                if (line.startsWith("Line")) {
//                    if (currentEntry.length() > 0 && currentRatio > threshold) {
//                        results.add(currentEntry.toString());
//                        currentEntry.setLength(0);  // Reset for the next entry
//                    }
//                    currentRatio = 0.0;  // Reset ratio for new entry
//                }
//
//                if (line.startsWith("Highest Similarity Ratio:")) {
//                    String ratioString = line.substring(line.lastIndexOf(":") + 1).trim();
//                    currentRatio = Double.parseDouble(ratioString);
//                }
//
//                currentEntry.append(line).append("\n");  // Continuously collect lines
//
//                // Ensure the last entry is added if it meets the threshold
//                if (!reader.ready() && currentEntry.length() > 0 && currentRatio > threshold) {
//                    results.add(currentEntry.toString());
//                }
//            }
//        }
//
//        return results;
//    }
//}



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
