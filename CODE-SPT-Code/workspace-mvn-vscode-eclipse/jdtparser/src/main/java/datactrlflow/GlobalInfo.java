package datactrlflow;

public interface GlobalInfo {

   // ### Input: parsed `pretrain` dataset combined.
   String INPUT_FILE_PATH = "pretrain-orgstr-singleline-combined.txt";
   // String INPUT_FILE_PATH = "sample1000.txt";

   // ### Output status:
   String OUTPUT_LOG = "log.txt";

   // ### Output:
   String OUTPUT_CTRL_FLOW_METHODS = "pretrain-fun-ctrl-flow.txt";
   String OUTPUT_DATA_FLOW_METHODS = "pretrain-fun-data-flow.txt";
   String OUTPUT_OTHER_METHODS = "pretrain-fun-others-arti.txt";

   String OUTPUT_CTRL_FLOW_METHODS_MASK = "pretrain-fun-ctrl-flow-mask.txt";
   String OUTPUT_DATA_FLOW_METHODS_MASK = "pretrain-fun-data-flow-mask.txt";
   String OUTPUT_OTHER_METHODS_MASK = "pretrain-fun-others-arti-mask.txt";

   // ### Validator the masked output
   // ### Input for diff:
   String INPUT_CTRL_FLOW_DIFF1 = OUTPUT_CTRL_FLOW_METHODS;
   String INPUT_CTRL_FLOW_DIFF2 = OUTPUT_CTRL_FLOW_METHODS_MASK;

   String INPUT_DATA_FLOW_DIFF1 = OUTPUT_DATA_FLOW_METHODS;
   String INPUT_DATA_FLOW_DIFF2 = OUTPUT_DATA_FLOW_METHODS_MASK;

   String INPUT_OTHER_DIFF1 = OUTPUT_OTHER_METHODS;
   String INPUT_OTHER_DIFF2 = OUTPUT_OTHER_METHODS_MASK;

   // ### Output:
   String OUTPUT_DIFF_CTRL_FLOW = "diff-mask-ctrl-flow.txt";
   String OUTPUT_DIFF_DATA_FLOW = "diff-mask-data-flow.txt";
   String OUTPUT_DIFF_OTHERS = "diff-mask-others.txt";
}
