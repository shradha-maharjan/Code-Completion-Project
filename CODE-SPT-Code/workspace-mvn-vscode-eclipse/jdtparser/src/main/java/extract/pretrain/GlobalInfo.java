package extract.pretrain;

public interface GlobalInfo {

   // ### Input: pretrain dataset
   String INPUT_TRAIN_JSON = "train.jsonl";

   // ### Output status:
   String OUTPUT_PARSE_STATUS = "train-org-str-parse-status.txt";
   String OUTPUT_PARSE_STATUS_PARSED = "train-org-str-parse-status-parsed.txt";
   String OUTPUT_LOG = "trainlog.txt";

   // ### Output:
   String OUTPUT_TRAIN_ORG_STR = "train-orgstr.txt";
   String OUTPUT_TRAIN_ORG_STR_SINGLELINE = "train-orgstr-singleline.txt";

}
