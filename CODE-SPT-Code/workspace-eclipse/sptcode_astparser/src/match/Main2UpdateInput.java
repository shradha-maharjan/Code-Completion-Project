package match;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Main2UpdateInput implements InfoFileNames {

    public static void main(String[] args) {
        try {
            // Step 1: Read the intermediate results from matched methods file
            Path matchedMethodsPath = Paths.get(FILE_MATCHED_METHODS);
            if (!Files.exists(matchedMethodsPath)) {
                throw new IOException("Matched methods file not found: " + FILE_MATCHED_METHODS);
            }
            List<String> matchedMethods = Files.readAllLines(matchedMethodsPath);

            // Step 2: Remove the intermediate results from the raw methods file
            Path rawMethodsPath = Paths.get(FILE_PRE_METHODS);
            if (!Files.exists(rawMethodsPath)) {
                throw new IOException("Raw methods file not found: " + FILE_PRE_METHODS);
            }
            List<String> rawMethods = Files.readAllLines(rawMethodsPath);
            Set<String> matchedSet = new HashSet<>(matchedMethods);
            List<String> filteredRawMethods = rawMethods.stream()
                .filter(rawMethod -> !matchedSet.contains(rawMethod))
                .collect(Collectors.toList());

            // Step 3: Write the remaining raw methods to a new file
            Path outputPath = Paths.get("output/step1-raw-methods.txt");
            Files.write(outputPath, filteredRawMethods);
            System.out.println("Processed files successfully.");
        } catch (Exception e) {
            System.err.println("Error processing files: " + e.getMessage());
            e.printStackTrace();  // Print stack trace for deeper debugging
        }
    }
}


//package match;
//
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//public class Main2UpdateInput implements InfoFileNames {
//
//   public static void main(String[] args) {
//	   
//	   try {
//           // Step 1: Read the intermediate results from matched methods file
//           List<String> matchedMethods = Files.readAllLines(Paths.get(FILE_MATCHED_METHODS));
//
//           // Step 2: Remove the intermediate results from the raw methods file
//           List<String> rawMethods = Files.readAllLines(Paths.get(FILE_RAW_METHODS));
//           Set<String> matchedSet = new HashSet<>(matchedMethods);
//           List<String> filteredRawMethods = rawMethods.stream()
//               .filter(rawMethod -> !matchedSet.contains(rawMethod))
//               .collect(Collectors.toList());
//
//           // Step 3: Write the remaining raw methods to a new file
//           Files.write(Paths.get("output/step1-raw-methods.txt"), filteredRawMethods);
//
//           System.out.println("Processed files successfully.");
//       } catch (Exception e) {
//           System.err.println("Error processing files: " + e.getMessage());
//       }
//   }
//      /*
//       * Step 1.
//       * Read the intermediate results: FILE_MATCHED_METHODS
//       * 
//       * Step 2.
//       * Remove the intermediate results from FILE_RAW_METHODS
//       * 
//       * Step 3.
//       * Create another input: step1-raw-methods.txt
//       */
//   }
//
//   /*
//    * method for step 1, step 2, and step 3
//    */
//
