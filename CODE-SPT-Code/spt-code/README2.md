## Summary of Datasets

All the datasets were prepared and preprocessed separately. These datasets are saved in the directory: /home/user1-selab3/Documents/research-shradha/data_shradha
and backed up in /home/user1-selab3/Documents/research-shradha/deploy-spt-code/data_shradha and https://unomail-my.sharepoint.com/:f:/r/personal/myoungkyu_unomaha_edu/Documents/0Research/Research-Shradha/dataset-spt-code-new/spt-code-new-data?csf=1&web=1&e=YJe9NG

Pre-Training:
JDT AST: data_shradha/spt-code-new-data/asts/pretrain/ast_outputs_pretrain.txt
Extracted Source from Json file used to generate AST: data_shradha/spt-code-new-data/asts/pretrain/pretrain_source.txt

FineTuning Datasets(Source, Target, AST, NL):

Non-Generalized Dataset:

RAW Datasets compliled from java small dataset: 
Test: data_shradha/spt-code-new-data/raw_methods_test.txt
Train: data_shradha/spt-code-new-data/raw_methods_train.txt
Valid: data_shradha/spt-code-new-data/raw_methods_valid.txt

Source Datasets (Raw dataset with PRED token):
Test: data_shradha/spt-code-new-data/source_methods_test.txt
Train: data_shradha/spt-code-new-data/source_methods_train.txt
Valid: data_shradha/spt-code-new-data/source_methods_valid.txt

Target Datasets (Target of PRED token):
Test: data_shradha/spt-code-new-data/target_methods_test.txt
Train: data_shradha/spt-code-new-data/target_methods_train.txt
Valid: data_shradha/spt-code-new-data/target_methods_valid.txt

ASTs:
data_shradha/spt-code-new-data/raw_methods_asts_test.txt
data_shradha/spt-code-new-data/raw_methods_asts_train.txt
data_shradha/spt-code-new-data/raw_methods_asts_valid.txt

NL:
data_shradha/spt-code-new-data/NL_methods_test.txt
data_shradha/spt-code-new-data/NL_methods_train.txt
data_shradha/spt-code-new-data/NL_methods_valid.txt

Since we need tokenized datasets for finetuning, the tokenized datasets are saved in:

With Java Keywords: data_shradha/spt-code-new-data/tokenized_methods_with_javakw

Test: source_tokenized_methods_test.txt, target_tokenized_methods_test.txt
Train: source_tokenized_methods_train.txt, target_tokenized_methods_train.txt
Valid: source_tokenized_methods_valid.txt, target_tokenized_methods_valid.txt

Without Java Keywords: data_shradha/spt-code-new-data/tokenized_no_javakw

Test: source_tokenized_methods_test.txt, target_tokenized_methods_test.txt
Train: source_tokenized_methods_train.txt, target_tokenized_methods_train.txt
Valid: source_tokenized_methods_valid.txt, target_tokenized_methods_valid.txt

Generalized Dataset:

RAW Datasets compliled from java small dataset and generalized (replacing "<string>" with "__STR__CONST__"): 
Test: data_shradha/spt-code-new-data/gen/raw_methods_test_gen.txt
Train: data_shradha/spt-code-new-data/gen/raw_methods_train_gen.txt
Valid: data_shradha/spt-code-new-data/gen/raw_methods_valid_gen.txt

Source Datasets (Raw dataset with PRED token):
Test: data_shradha/spt-code-new-data/gen/source_methods_test_gen.txt
Train: data_shradha/spt-code-new-data/gen/source_methods_train_gen.txt
Valid: data_shradha/spt-code-new-data/gen/source_methods_valid_gen.txt

Target Datasets (Target of PRED token):
Test: data_shradha/spt-code-new-data/gen/target_methods_test_gen.txt
Train: data_shradha/spt-code-new-data/gen/target_methods_train_gen.txt
Valid: data_shradha/spt-code-new-data/gen/target_methods_valid_gen.txt

Tokenized datasets: data_shradha/spt-code-new-data/gen/

Test: source_tokenized_methods_test.txt, target_tokenized_methods_test.txt
Train: source_tokenized_methods_train.txt, target_tokenized_methods_train.txt
Valid: source_tokenized_methods_valid.txt, target_tokenized_methods_valid.txt