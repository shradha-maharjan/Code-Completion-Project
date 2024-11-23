package extract.pretrain;

import java.io.*;
import java.util.*;

public class SearchDocString {

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("Usage: java SearchDocString <input1_path> <input2_path> <output_path>");
            return;
        }

        String input1Path = args[0];
        String input2Path = args[1];
        String outputPath = args[2];

        List<String> input1Lines = readFileLines(input1Path);
        List<Entry> input2Entries = readInput2AsEntries(input2Path);

        // Perform search for each line in Input 1
        List<String> results = new ArrayList<>();
        for (String codeLine : input1Lines) {
            String docString = binarySearchDocString(codeLine, input2Entries);
            results.add(docString != null ? docString : "Not Found");
        }

        // Save results to output file
        saveResults(outputPath, results);
        System.out.println("Search complete. Results saved to: " + outputPath);
    }

    private static List<String> readFileLines(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line.trim());
            }
        }
        return lines;
    }

    private static List<Entry> readInput2AsEntries(String filePath) throws IOException {
        List<Entry> entries = new ArrayList<>();
    
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.contains("#START_CODE") && line.contains("#END_CODE") && line.contains("#START_FUNC") && line.contains("#END_FUNC")) {
                    String codeString = line.substring(line.indexOf("#START_CODE") + 11, line.indexOf("#END_CODE")).trim();
                    String funcName = line.substring(line.indexOf("#START_FUNC") + 11, line.indexOf("#END_FUNC")).trim();
                    entries.add(new Entry(codeString, funcName));
                } else {
                    System.err.println("Malformed line in Input 2: " + line);
                }
            }
        }
    
        // Sort entries based on codeString for binary search
        entries.sort(Comparator.comparing(Entry::getCodeString));
        return entries;
    }
    
    private static String binarySearchDocString(String codeLine, List<Entry> entries) {
        int left = 0, right = entries.size() - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            int comparison = codeLine.compareTo(entries.get(mid).getCodeString());

            if (comparison == 0) {
                return entries.get(mid).getDocString(); // Match found
            } else if (comparison < 0) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        return null; // Not found
    }

    private static void saveResults(String filePath, List<String> results) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String result : results) {
                writer.write(result);
                writer.newLine();
            }
        }
    }

    static class Entry {
        private final String codeString;
        private final String docString;

        public Entry(String codeString, String docString) {
            this.codeString = codeString;
            this.docString = docString;
        }

        public String getCodeString() {
            return codeString;
        }

        public String getDocString() {
            return docString;
        }
    }
}


// import java.io.*;
// import java.util.*;

// public class SearchCodeString {

//     public static void main(String[] args) throws IOException {
//         if (args.length != 3) {
//             System.out.println("Usage: java SearchCodeString <input1_path> <input2_path> <output_path>");
//             return;
//         }

//         String input1Path = args[0];
//         String input2Path = args[1];
//         String outputPath = args[2];

//         List<String> input1Lines = readFileLines(input1Path);
//         List<Entry> input2Entries = readInput2AsEntries(input2Path);

//         // Perform search for each line in Input 1
//         List<String> results = new ArrayList<>();
//         for (String codeLine : input1Lines) {
//             String docString = findDocString(codeLine, input2Entries);
//             results.add(docString != null ? docString : "Not Found");
//         }

//         // Save results to output file
//         saveResults(outputPath, results);
//         System.out.println("Search complete. Results saved to: " + outputPath);
//     }

//     private static List<String> readFileLines(String filePath) throws IOException {
//         List<String> lines = new ArrayList<>();
//         try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
//             String line;
//             while ((line = reader.readLine()) != null) {
//                 lines.add(line.trim());
//             }
//         }
//         return lines;
//     }

//     private static List<Entry> readInput2AsEntries(String filePath) throws IOException {
//         List<Entry> entries = new ArrayList<>();
    
//         try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
//             String line;
//             while ((line = reader.readLine()) != null) {
//                 line = line.trim();
//                 System.out.println("DEBUG: Line read from Input 2: " + line);
    
//                 // Split the line into parts using `|` as the delimiter and filter out empty strings
//                 String[] parts = Arrays.stream(line.split("\\|"))
//                                        .filter(part -> !part.trim().isEmpty())
//                                        .toArray(String[]::new);
//                 System.out.println("DEBUG: Parts after splitting: " + Arrays.toString(parts));
    
//                 // Validate parts length (must have exactly 2 parts: code_string and doc_string)
//                 if (parts.length == 2) {
//                     String codeString = parts[0].trim();
//                     String docString = parts[1].trim();
    
//                     System.out.println("DEBUG: Code String: " + codeString);
//                     System.out.println("DEBUG: Doc String: " + docString);
    
//                     // Validate that both parts are non-empty
//                     if (!codeString.isEmpty() && !docString.isEmpty()) {
//                         entries.add(new Entry(codeString, docString));
//                     } else {
//                         System.err.println("Malformed line after cleanup: " + line);
//                     }
//                 } else {
//                     System.err.println("Malformed line in Input 2 (incorrect parts length): " + line);
//                 }
//             }
//         }
    
//         return entries;
//     }
    
    

//     private static String findDocString(String codeLine, List<Entry> entries) {
//         // Normalize the codeLine for consistent comparison
//         String normalizedCodeLine = normalizeString(codeLine);
//         System.out.println("DEBUG: Normalized Input 1 Code String: " + normalizedCodeLine);
    
//         for (Entry entry : entries) {
//             // Normalize the codeString from Input 2 for consistent comparison
//             String normalizedEntryCodeString = normalizeString(entry.getCodeString());
//             System.out.println("DEBUG: Comparing with Input 2 Code String: " + normalizedEntryCodeString);
    
//             // Compare the normalized Input 1 line to the normalized Input 2 code_string
//             if (normalizedCodeLine.equals(normalizedEntryCodeString)) {
//                 System.out.println("DEBUG: Match found! Returning Doc String: " + entry.getDocString());
//                 return entry.getDocString();
//             }
//         }
    
//         // If no match is found, return null
//         System.out.println("DEBUG: No match found for Code String: " + normalizedCodeLine);
//         return null; // Not found
//     }
    
//     private static String normalizeString(String input) {
//         return input.trim().replaceAll("\\s+", " "); // Remove extra spaces and normalize to a single space
//     }
    

//     private static void saveResults(String filePath, List<String> results) throws IOException {
//         try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
//             for (String result : results) {
//                 writer.write(result);
//                 writer.newLine();
//             }
//         }
//     }

//     static class Entry {
//         private final String codeString;
//         private final String docString;

//         public Entry(String codeString, String docString) {
//             this.codeString = codeString;
//             this.docString = docString;
//         }

//         public String getCodeString() {
//             return codeString;
//         }

//         public String getDocString() {
//             return docString;
//         }
//     }
// }

// public class SearchDocString {

//     public static void main(String[] args) throws IOException {
//         if (args.length != 3) {
//             System.out.println("Usage: java SearchDocString <input1_path> <input2_path> <output_path>");
//             return;
//         }

//         String input1Path = args[0];
//         String input2Path = args[1];
//         String outputPath = args[2];

        
//         // String input1Path = "/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/output-classify/pretrain-fun.txt";
//         // String input2Path = "/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/output-pretrain/unixcoder/valid-orgstr-singleline.txt";
//         // String outputPath = "/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/output-pretrain/func_names.txt";

//         List<String> input1Lines = readFileLines(input1Path);
//         List<Entry> input2Entries = readInput2AsEntries(input2Path);

//         // Perform search for each line in Input 1
//         List<String> results = new ArrayList<>();
//         for (String codeLine : input1Lines) {
//             String docString = binarySearchDocString(codeLine, input2Entries);
//             results.add(docString != null ? docString : "Not Found");
//         }

//         // Save results to output file
//         saveResults(outputPath, results);
//         System.out.println("Search complete. Results saved to: " + outputPath);
//     }

//     private static List<String> readFileLines(String filePath) throws IOException {
//         List<String> lines = new ArrayList<>();
//         try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
//             String line;
//             while ((line = reader.readLine()) != null) {
//                 lines.add(line.trim());
//             }
//         }
//         return lines;
//     }

//     private static List<Entry> readInput2AsEntries(String filePath) throws IOException {
//         List<Entry> entries = new ArrayList<>();

//         try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
//             String line;
//             while ((line = reader.readLine()) != null) {
//                 line = line.trim();
//                 if (line.startsWith("{") && line.endsWith("}")) {
//                     // Remove curly braces
//                     line = line.substring(1, line.length() - 1).trim();
//                     // Split into code string and docstring
//                     String[] parts = line.split("\",\\s*\"");
//                     if (parts.length == 2) {
//                         String codeString = parts[0].replaceAll("^\"|\"$", ""); // Remove surrounding quotes
//                         String docString = parts[1].replaceAll("^\"|\"$", "");  // Remove surrounding quotes
//                         entries.add(new Entry(codeString, docString));
//                     } else {
//                         System.err.println("Malformed line in Input 2: " + line);
//                     }
//                 } else {
//                     System.err.println("Invalid format: " + line);
//                 }
//             }
//         }

//         // Sort entries based on codeString for binary search
//         entries.sort(Comparator.comparing(Entry::getCodeString));
//         return entries;
//     }

//     private static String binarySearchDocString(String codeLine, List<Entry> entries) {
//         int left = 0, right = entries.size() - 1;

//         while (left <= right) {
//             int mid = left + (right - left) / 2;
//             int comparison = codeLine.compareTo(entries.get(mid).getCodeString());

//             if (comparison == 0) {
//                 return entries.get(mid).getDocString(); // Match found
//             } else if (comparison < 0) {
//                 right = mid - 1;
//             } else {
//                 left = mid + 1;
//             }
//         }

//         return null; // Not found
//     }

//     private static void saveResults(String filePath, List<String> results) throws IOException {
//         try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
//             for (String result : results) {
//                 writer.write(result);
//                 writer.newLine();
//             }
//         }
//     }

//     static class Entry {
//         private final String codeString;
//         private final String docString;

//         public Entry(String codeString, String docString) {
//             this.codeString = codeString;
//             this.docString = docString;
//         }

//         public String getCodeString() {
//             return codeString;
//         }

//         public String getDocString() {
//             return docString;
//         }
//     }
// }

