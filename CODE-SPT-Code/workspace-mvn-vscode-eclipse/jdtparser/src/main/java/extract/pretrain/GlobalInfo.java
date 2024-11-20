package extract.pretrain;

public interface GlobalInfo {

   // ### Input: pretrain dataset
   String INPUT_TRAIN_JSON = "train.jsonl";

   // ### Output status:
   String OUTPUT_PARSE_STATUS = "pretrain-org-str-parse-status.txt";
   String OUTPUT_PARSE_STATUS_PARSED = "pretrain-org-str-parse-status-parsed.txt";
   String OUTPUT_LOG = "log.txt";

   // ### Output:
   String OUTPUT_TRAIN_ORG_STR = "train-orgstr.txt";
   String OUTPUT_TRAIN_ORG_STR_SINGLELINE = "train-orgstr-singleline.txt";

}
