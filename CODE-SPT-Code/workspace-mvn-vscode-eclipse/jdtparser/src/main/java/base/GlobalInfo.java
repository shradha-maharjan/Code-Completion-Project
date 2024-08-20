package base;

public interface GlobalInfo {

   //
   // ### Input: left-context, right-context, and target-seq of each method in java-small-json data
   //

   String DIR_INPUT = "input";
   String INPUT_JAVA_SMALL_JSON = DIR_INPUT + "/java-small.val.json";// "/valid-test.json";//
   // String DIR_TRAINING_DATA = DIR_INPUT + "training";
   String DIR_JAVA_SMALL = "java-small";

   //
   // ### Output : Raw methods corresponding to masked methods of input 1
   //
   String DIR_OUTPUT = "output/";
   String OUTPUT_JAVA_SMALL_JSON_SER = "java-small-train-json.ser";
   String OUTPUT_UNPARSED = DIR_OUTPUT + "nok-unparsed-train.txt";
   String OUTPUT_PARSE_FAILED = DIR_OUTPUT + "nok-parse-failed-train.txt";
   String FILE_PARSE_PASS = DIR_OUTPUT + "raw_methods_train_gen.txt";// "ok-parse-pass-val-debug.txt";
   String FILE_PARSE_PRED = DIR_OUTPUT + "source_methods_train_gen.txt";
   String FILE_PARSE_TARGET = DIR_OUTPUT + "target_methods_train_gen.txt";

   // String DIR_INPUT = "input";
   // String INPUT_JAVA_SMALL_JSON = DIR_INPUT + "/java-small.test.json";//"/valid-test.json";//
   // //String DIR_TRAINING_DATA = DIR_INPUT + "training";
   // String DIR_JAVA_SMALL = "java-small";
   //
   // //
   // // ### Output : Raw methods corresponding to masked methods of input 1
   // //
   // String DIR_OUTPUT = "output/";
   // String OUTPUT_JAVA_SMALL_JSON_SER = "java-small-test-json.ser";
   // String OUTPUT_UNPARSED = DIR_OUTPUT + "nok-unparsed-test-debug.txt";
   // String OUTPUT_PARSE_FAILED = DIR_OUTPUT + "nok-parse-failed-test-debug.txt";
   // String FILE_PARSE_PASS = DIR_OUTPUT + "raw_methods_test.txt";//"ok-parse-pass-val-debug.txt";
   // String FILE_PARSE_PRED = DIR_OUTPUT + "source_methods_test.txt";
   // String FILE_PARSE_TARGET = DIR_OUTPUT + "target_methods_test.txt";
}
