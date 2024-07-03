## Input of preprocess preprocessed datasets:
Pretrain dataset:
research-shradha/CODE-SPT-Code/dataset/pre_train/java/train.jsonl, test.jsonl, valid.jsonl

Finetune datasets:
The preprocessed dataset can be found in research-shradha/CODE-SPT-Code/dataset/fine_tune/completion
/data.TargetType.seq.train.source.txt, data.TargetType.seq.test.source.txt, data.TargetType.seq.valid.source.txt

## Input of raw datasets: Describe where to find and download these datasets.

The raw dataset can be found in research-shradha/CODE-SPT-Code/dataset/finetune_raw/java-small-json/finetune_methods_train_final.txt, finetune_methods_test_final.txt, finetune_methods_valid_final.txt

## Implementations
AST Mapping:
To find the unique tree-sitter ASTs used in SPT-Code, we used pickle file found in /home/user1-selab3/Documents/research-shradha/CODE-SPT-Code/dataset_backup --pre_train.pk and  all other .pk files for different downstream tasks. Then, we listed unique asts, sorted them, and filtered them resulting unique asts. Then, we mapped the corresponding tree-sitter ASTs to JDT ASTs. Mappings can be found in https://unomail-my.sharepoint.com/:x:/r/personal/myoungkyu_unomaha_edu/Documents/0Research/Research-Shradha/AST-Mappings.xlsx

For Pretraining:
1. Using those AST Mappings, we implemented SPTCODEASTVisitor to read Java method code snippets from a file, format them, parse them into an abstract syntax tree (AST) using the Eclipse JDT Core library, and write specific AST node types to another file. 

2. Then, the output files resulted from this implementation (AST expressions for each pretraining methods) is loaded directly inplace of generating AST from tree-sitter. 
Modified py files: data_utils.py, dataset.py

For Finetuning:
1. Since we had only preprocessed dataset for finetuning and raw method(with proper java code structure) is required for AST visitor implementation, we first searched for its corresponding raw datasets and cleaned the raw methods to make it similar to raw methods in pre-train datasets (suitable for comparison). 

2. Unlike pretrain dataset, the methods in raw and preprocessed dataset's index didn't match so we implemented a java program to find the corresponding raw methods of raw dataset with preprocessed methods of preprocessed dataset.

The implementation or java code is found in research-shradha\CODE-SPT-Code\workspace-eclipse\sptcode_astparser\src\match and research-shradha\CODE-SPT-Code\workspace-eclipse\sptcode_astparser\src\util

It consists of two main components: an interface InfoFileNames and a class Main1MatchRawPreMethods that implements this interface.

Interface: InfoFileNames
This interface defines constants for file paths used throughout the program. These paths represent input files containing preprocessed ("pre") and raw Java method snippets, as well as output files where matched and unmatched methods will be recorded. 

Class: Main1MatchRawPreMethods
Step 1. Loads method snippets from files defined in InfoFileNames.

Step 2. Utility method: removeSpecialChars -> Cleans up the method snippets by removing non-alphanumeric characters (except specific symbols) and Java modifiers and builds internal mappings of cleaned-up versions to their original forms.

Step 3. findMatchedRawMethods -> Matches preprocessed methods against raw methods using sorted arrays and a custom comparator that integrates a search keyword (SEARCH = 'pred').

    3.1 Converts listRawMethodsClean and listPreMethodsClean, which are lists of strings representing cleaned method snippets, into arrays. 

    3.2 Sorts both arrays. 

    3.3 After sorting, the arrays are assigned to sortedRawMethods and sortedPreMethods. 

    3.4 The method iterates over each preprocessed method in sortedPreMethods.

    3.5 For each method, it creates a custom comparator using createComparator(). This comparator is likely tailored to specific attributes or patterns within the method snippets that define a match.

    3.6 Arrays.binarySearch is used to find a matching raw method in sortedRawMethods using the custom comparator. 

    3.7 If a match is found, the index of the match in the array is returned. If no match is found, the method returns a negative value that can be transformed into an insertion point (i.e., the point where the item would be inserted to maintain order).

    3.8 If a match is found (foundIndex >= 0), the method logs this match by adding entries to outputMatched. It records the index of the match, a "Match Found:" marker, and both the preprocessed and raw method snippets associated with the found index. If no match is found, the method uses lastComparedIndex to handle the negative return value from binarySearch and logs a debug message indicating that no comparable raw method was found or that a last comparison attempt failed.
    
    3.9 The list outputMatched, which now contains all found matches and relevant debug information, is written to a file specified by FILE_MATCHED_METHODS.

Step 4. findUnmatchedRawMethods -> Identifies methods that were not matched in the previous step from both categories and logs them.

    4.1 Converts the matchedMethods list into an array (matchedMethodsArray) to facilitate the use of binary search. It sorts matchedMethodsArray since binary search requires the data to be sorted for effective operation.

    4.2 An empty list outputUnmatched is initialized to store the unmatched methods.
    The function then iterates over each method in the orgMethods list. For each method (iOrgMethod), it uses Arrays.binarySearch on matchedMethodsArray to check if the method exists in the matched methods.
    If binarySearch returns a negative value, this indicates that iOrgMethod is not found in matchedMethodsArray, and therefore, it is added to outputUnmatched.
    
    4.3 After processing all the original methods, the function checks if any methods are in the outputUnmatched list. If there are no unmatched methods, it prints a message indicating this. If there are unmatched methods, it prints each method and writes them to a file specified by the fileName parameter.

Step 5. Calculates and logs the execution duration and other relevant statistics.

Step 6. Various methods (countLines, countDistinctMatches) are used to handle file operations, including counting lines or specific patterns in files and pattern matching like STRING_PATTERN: A regular expression used to match and manipulate string literals in Java code and MODIFIERS_PATTERN: Regular expression used to find and remove Java method modifiers like public, private, protected, and annotations like @Override are used.

3. Once the match for all raw datasets is found, the SPTCODEASTVisitor is implemented to generate JDT ASTs and the ASTs resulted from this implementation is loaded directly in SPT-Code's original code.



