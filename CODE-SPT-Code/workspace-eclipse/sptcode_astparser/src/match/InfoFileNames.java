package match;

public interface InfoFileNames {

   // Information:
   // Assume that the files below were sorted.
   // Small sample datasets
   // After removing the first matching.
   // static String FILE_PRE_METHODS = "input/step1-valid.source.txt";
   // static String FILE_RAW_METHODS = "input/step1-raw-methods.txt";
   // Large datasets
   // static String FILE_PRE_METHODS = "";
   // static String FILE_RAW_METHODS = "";
   // Output files

   static String FILE_PRE_METHODS = "input/input1-raw.txt";// "input/step0-valid.source.txt";//
   static String FILE_RAW_METHODS = "input/input1-pre.txt";// "input/step0-raw-methods.txt";
   // static String FILE_PRE_METHODS = "input/step0-valid.source.txt";//
   // static String FILE_RAW_METHODS = "input/step0-raw-methods.txt";

   // Testing
   // Include 10 methods in FILE_RAW_METHODS
   // Include 2 methods in FILE_PRE_METHODS

   static String FILE_MATCHED_METHODS = "output/output1-matched.txt";// "output/step1-raw-pre.txt";
   static String FILE_UNMATCHED_METHODS_PRE = "output/output1-unmatched_pre.txt";// "output/step1-raw-pre.txt";
   static String FILE_UNMATCHED_METHODS_RAW = "output/output1-unmatched_raw.txt";// "output/step1-raw-pre.txt";
   // static String FILE_MATCHED_METHODS = "output/step1-raw-pre.txt";

}
