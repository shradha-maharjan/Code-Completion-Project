## Summary of Datasets

All the datasets were prepared and preprocessed separately. 
These datasets are saved in the directory: 

- /home/user1-system11/Documents/research-shradha/CODE-SPT-Code/data_shradha
and backed up in:
- /home/user1-system11/Documents/deploy-spt-code/CODE-SPT-Code/data_shradha 
- /home/user1-system11/Documents/deploy-spt-code-01/CODE-SPT-Code/data_shradha 
- https://unomail-my.sharepoint.com/:f:/r/personal/myoungkyu_unomaha_edu/Documents/0Research/Research-Shradha/dataset-spt-code-new/spt-code-new-data?csf=1&web=1&e=YJe9NG

**Pre-Training:**
/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/pretrain_dataset_cleaned: 1st improved version for pretraining CAP task 
RAW dataset: /home/user1-system11/Documents/research-spt-code-data/workspace-mvn-vscode-eclipse/jdtparser-sptcode-data/input

Preprocessed and Validated to directly load all the datasets:

For Gen:
Source: pretrain_dataset_cleaned/pretrain-orgstr-singleline-combined.txt
codes: pretrain_dataset_cleaned/Tokenized_Code/pretrain_tokenized_gen.txt
AST: pretrain_dataset_cleaned/Asts/pretrain_asts_gen.txt
names: pretrain_dataset_cleaned/NL/NL_pretrain_source_wname_gen.txt
codes_wo_name: pretrain_dataset_cleaned/Tokenized_Code/pretrain_code_wo_name_gen.txt
names_wo_name: pretrain_dataset_cleaned/NL/NL_pretrain_source_wo_name_gen.txt
only_name: pretrain_dataset_cleaned/Only_Names/pretrain_onlynames.txt
docs: pretrain_dataset_cleaned/Docstrings/pretrain_docstrings.txt


For Non-Gen:
Source: pretrain_dataset_cleaned/pretrain-orgstr-singleline-combined.txt
codes: pretrain_dataset_cleaned/Tokenized_Code/pretrain_tokenized_nongen.txt
AST: pretrain_dataset_cleaned/Asts/pretrain_asts_nongen.txt
names: pretrain_dataset_cleaned/NL/NL_pretrain_source_wname_nongen.txt
codes_wo_name: pretrain_dataset_cleaned/Tokenized_Code/pretrain_code_wo_name_nongen.txt
names_wo_name: pretrain_dataset_cleaned/NL/NL_pretrain_source_wo_name_nongen.txt
only_name: pretrain_dataset_cleaned/Only_Names/pretrain_onlynames.txt
docs: pretrain_dataset_cleaned/Docstrings/pretrain_docstrings.txt


/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/pretrain_dataset_2ndVersion: 2nd improved version for pretraining CAP,MASS task 

RAW dataset: /home/user1-system11/Documents/research-spt-code-data/workspace-mvn-vscode-eclipse/jdtparser-sptcode-data/input (all json files)

Extracted and checked using: /home/user1-system11/Documents/research-spt-code-data/workspace-mvn-vscode-eclipse/jdtparser-sptcode-data/src/main/java/extract/pretrain/MainCheckPretrainDataParseStatus.java

TASK CAP:
/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/src/main/java/ParsingValidation.java

Preprocessed and Validated to directly load all the datasets:

For Gen:
Source: pretrain_dataset_2ndVersion/pretrain-fun.txt
codes: pretrain_dataset_2ndVersion/Tokenized_Code/pretrain_tokenized_gen.txt
AST: pretrain_dataset_2ndVersion/Asts/pretrain_asts_gen.txt
names: pretrain_dataset_2ndVersion/NL/NL_pretrain_source_wname_gen.txt
codes_wo_name: pretrain_dataset_2ndVersion/Tokenized_Code/pretrain_code_wo_name_gen.txt
names_wo_name: pretrain_dataset_2ndVersion/NL/NL_pretrain_source_wo_name_gen.txt
only_name: pretrain_dataset_2ndVersion/Only_Names/pretrain_onlynames.txt
docs: pretrain_dataset_2ndVersion/Docstrings/pretrain_docstrings.txt


For Non-Gen:
Source: pretrain_dataset_2ndVersion/pretrain-fun.txt
codes: pretrain_dataset_2ndVersion/Tokenized_Code/pretrain_tokenized_nongen.txt
AST: pretrain_dataset_2ndVersion/Asts/pretrain_asts_nongen.txt
names: pretrain_dataset_2ndVersion/NL/NL_pretrain_source_wname_nongen.txt
codes_wo_name: pretrain_dataset_2ndVersion/Tokenized_Code/pretrain_code_wo_name_nongen.txt
names_wo_name: pretrain_dataset_2ndVersion/NL/NL_pretrain_source_wo_name_nongen.txt
only_name: pretrain_dataset_2ndVersion/Only_Names/pretrain_onlynames.txt
docs: pretrain_dataset_2ndVersion/Docstrings/pretrain_docstrings.txt

TASK MASS:
Code: pretrain_dataset_2ndVersion/pretrain-fun.txt
Masked:pretrain_dataset_2ndVersion/pretrain-fun-mask.txt

Data flow analysis and masking using:
/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/src/main/java/datactrlflow
1. MainClassifyMethodCalls.java
2. MainValidateMaskedMethods.java
3. OuterMostMethodTransformer.java

**FineTuning Datasets(Source, Target, AST, NL):**

Non-Generalized Dataset:

RAW Datasets compliled from java small dataset: 
- Test: data_shradha/spt-code-new-data/raw_methods_test.txt
- Train: data_shradha/spt-code-new-data/raw_methods_train.txt
- Valid: data_shradha/spt-code-new-data/raw_methods_valid.txt

Source Datasets (Raw dataset with PRED token):
- Test: data_shradha/spt-code-new-data/source_methods_test.txt
- Train: data_shradha/spt-code-new-data/source_methods_train.txt
- Valid: data_shradha/spt-code-new-data/source_methods_valid.txt

Target Datasets (Target of PRED token):
- Test: data_shradha/spt-code-new-data/target_methods_test.txt
- Train: data_shradha/spt-code-new-data/target_methods_train.txt
- Valid: data_shradha/spt-code-new-data/target_methods_valid.txt

ASTs:
- data_shradha/spt-code-new-data/raw_methods_asts_test.txt
- data_shradha/spt-code-new-data/raw_methods_asts_train.txt
- data_shradha/spt-code-new-data/raw_methods_asts_valid.txt

NL:
- data_shradha/spt-code-new-data/NL_methods_test.txt
- data_shradha/spt-code-new-data/NL_methods_train.txt
- data_shradha/spt-code-new-data/NL_methods_valid.txt

Since we need tokenized datasets for finetuning, the tokenized datasets are saved in:

With Java Keywords: data_shradha/spt-code-new-data/tokenized_methods_with_javakw

- Test: source_tokenized_methods_test.txt, target_tokenized_methods_test.txt
- Train: source_tokenized_methods_train.txt, target_tokenized_methods_train.txt
- Valid: source_tokenized_methods_valid.txt, target_tokenized_methods_valid.txt

Without Java Keywords: data_shradha/spt-code-new-data/tokenized_no_javakw

- Test: source_tokenized_methods_test.txt, target_tokenized_methods_test.txt
- Train: source_tokenized_methods_train.txt, target_tokenized_methods_train.txt
- Valid: source_tokenized_methods_valid.txt, target_tokenized_methods_valid.txt

Generalized Dataset:

RAW Datasets compliled from java small dataset and generalized (replacing "<string>" with "__STR__CONST__"): 
- Test: data_shradha/spt-code-new-data/gen/raw_methods_test_gen.txt
- Train: data_shradha/spt-code-new-data/gen/raw_methods_train_gen.txt
- Valid: data_shradha/spt-code-new-data/gen/raw_methods_valid_gen.txt

Source Datasets (Raw dataset with PRED token):
- Test: data_shradha/spt-code-new-data/gen/source_methods_test_gen.txt
- Train: data_shradha/spt-code-new-data/gen/source_methods_train_gen.txt
- Valid: data_shradha/spt-code-new-data/gen/source_methods_valid_gen.txt

Target Datasets (Target of PRED token):
- Test: data_shradha/spt-code-new-data/gen/target_methods_test_gen.txt
- Train: data_shradha/spt-code-new-data/gen/target_methods_train_gen.txt
- Valid: data_shradha/spt-code-new-data/gen/target_methods_valid_gen.txt

Tokenized datasets: data_shradha/spt-code-new-data/gen/

- Test: source_tokenized_methods_test.txt, target_tokenized_methods_test.txt
- Train: source_tokenized_methods_train.txt, target_tokenized_methods_train.txt
- Valid: source_tokenized_methods_valid.txt, target_tokenized_methods_valid.txt
