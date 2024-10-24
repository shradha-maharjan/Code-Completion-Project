from torch.utils.data.dataset import Dataset

import os
import logging

import difflib
import re

from .data_utils import load_lines

# Configure logging
logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)

def load_files_for_completion(source_target_file_path, asts_nl_file_path, split):
    source_path = os.path.join(source_target_file_path, f'source_tokenized_methods_{split}.txt')
    target_path = os.path.join(source_target_file_path, f'target_tokenized_methods_{split}.txt')
    ast_path = os.path.join(asts_nl_file_path, f'raw_methods_asts_{split}_PR.txt')
    nl_path = os.path.join(asts_nl_file_path, f'NL_methods_{split}.txt')

    if not asts_nl_file_path:
        raise ValueError("AST path and NL path must be provided when ast_type is 'jdt'.")
    source_lines = load_lines(source_path)
    target_lines = load_lines(target_path)
    ast_lines = load_lines(ast_path)
    nl_lines = load_lines(nl_path)
    source_lines, target_lines, ast_lines, nl_lines = source_lines[:1000], target_lines[:1000], ast_lines[:1000], nl_lines[:1000]
    #source_lines, target_lines, ast_lines, nl_lines = source_lines, target_lines, ast_lines, nl_lines
    assert len(source_lines) == len(target_lines) == len(ast_lines) == len(nl_lines)

    return source_lines, ast_lines, nl_lines, target_lines

def load_files_for_pretrain(pretrain_asts_nl_file_path):
    ast_path = os.path.join(pretrain_asts_nl_file_path, f'pretrain_methods_asts_PR.txt')
    nl_path = os.path.join(pretrain_asts_nl_file_path, f'NL_pretrain_source_wname.txt')
    nl_wo_name_path = os.path.join(pretrain_asts_nl_file_path, f'NL_pretrain_source_wo_name.txt')

    if not pretrain_asts_nl_file_path:
        raise ValueError("AST path and NL path must be provided when ast_type is 'jdt'.")
    ast_lines = load_lines(ast_path)
    nl_lines = load_lines(nl_path)
    nl_wo_name_lines = load_lines(nl_wo_name_path)
    ast_lines, nl_lines,nl_wo_name_lines = ast_lines[:1000], nl_lines[:1000], nl_wo_name_lines[:1000]
    #ast_lines, nl_lines,nl_wo_name_lines = ast_lines, nl_lines, nl_wo_name_lines
    assert len(ast_lines) == len(nl_lines) == len(nl_wo_name_lines)

    return ast_lines, nl_lines,nl_wo_name_lines

def load_lines_from_file(file_path):
    lines = []
    with open(file_path, 'r') as file:
        for line in file:
            lines.append(line.strip())
    return lines

def load_ast_from_file_jdt(jdt_file_path, context):
    sources = []
    asts = []
    valid_sources = []
    valid_asts = []
    if context == 'parse_for_completion':
        error_log_path = "error_parse_for_completion.txt"  # Specific file for parse_for_completion errors
    elif context == 'load_dataset':
        error_log_path = "error_sources.txt"

    # Load sources to skip based on content
    try:
        with open(error_log_path, 'r') as file:
            skipped_sources = {line.strip() for line in file}  # Assuming sources are stored directly
    except FileNotFoundError:
        skipped_sources = set()

    print("Skipped sources loaded:", skipped_sources)  # Diagnostic print

    with open(jdt_file_path, 'r') as file:
        for idx, line in enumerate(file):
            original_line = line.strip()

            try:
                if original_line.startswith('Source:') and 'Asts:' in original_line:
                    parts = original_line.split('", Asts: "')
                    source_part = parts[0][8:].strip('"')
                    ast_part = parts[1].rstrip('"')
                    if source_part in skipped_sources:
                        print(f"Skipping source due to previous errors. Source: {source_part}")  # Diagnostic print
                        continue  # Skip adding this source and AST to the lists
                    # Only add sources and ASTs that are not skipped
                    sources.append(source_part)
                    asts.append(ast_part)
            except Exception as e:
                #handle_error(str(e), source=source_part, file_path=jdt_file_path)
                continue
                
    valid_sources.extend(sources)
    valid_asts.extend(asts)

    print(f"Loaded {len(valid_sources)} valid sources and {len(valid_asts)} valid ASTs from {jdt_file_path}.")
    return valid_sources, valid_asts

# def load_ast_from_file_jdt(jdt_file_path):
#     sources = []
#     asts = []
#     valid_sources = []
#     valid_asts = []
#     error_log_path = "error_indices.txt"  # Path to the error index file

#     # Load error indices to skip
#     try:
#         with open(error_log_path, 'r') as file:
#             skipped_indices = {int(line.strip()) for line in file}
#     except FileNotFoundError:
#         skipped_indices = set()
    
#     print("Skipped indices:", skipped_indices)

#     actual_idx = 0
#     with open(jdt_file_path, 'r') as file:
#         for idx, line in enumerate(file):
#             if idx in skipped_indices:
#                 print(f"Skipping index {idx} due to previous errors.")
#                 continue  # Skip processing for lines with errors logged in previous runs

#             line = line.strip()
#             try:
#                 if line.startswith('Source:') and 'Asts:' in line:
#                     parts = line.split('", Asts: "')
#                     source_part = parts[0][8:].strip('"')
#                     ast_part = parts[1].rstrip('"')
#                     sources.append(source_part)
#                     asts.append(ast_part)
#                     actual_idx += 1
#             except Exception as e:
#                 handle_error(str(e), index=idx, file_path=jdt_file_path)
#                 continue

#     # Only add non-skipped items to the valid lists
#     valid_sources.extend(sources)
#     valid_asts.extend(asts)

#     logger.info(f"Loaded {len(valid_sources)} valid sources and {len(valid_asts)} valid ASTs from {jdt_file_path}.")
#     return valid_sources, valid_asts
    # """
    # Load sources and ASTs from a file, excluding lines with previously logged errors.
    # Args:
    #     jdt_file_path (str): Path to the file containing source and AST information.
    # Returns:
    #     tuple: Two lists containing the sources and ASTs.
    # """
    # sources = []
    # asts = []
    # error_log_path = "error_indices.txt"  # Path to the error index file
    # # Load error indices to skip
    # try:
    #     with open(error_log_path, 'r') as file:
    #         skipped_indices = {int(line.strip()) for line in file}
    # except FileNotFoundError:
    #     skipped_indices = set()
    
    # print("Skipped indices:", skipped_indices)
    
    # with open(jdt_file_path, 'r') as file:
    #     for idx, line in enumerate(file):
    #         if idx in skipped_indices:
    #             continue  # Skip processing for lines with errors logged in previous runs
    #         line = line.strip()
    #         try:
    #             if line.startswith('Source:') and 'Asts:' in line:
    #                 parts = line.split('", Asts: "')
    #                 source_part = parts[0][8:].strip('"')
    #                 ast_part = parts[1].rstrip('"')
    #                 sources.append(source_part)
    #                 asts.append(ast_part)
    #         except Exception as e:
    #             handle_error(str(e), index=idx, file_path=jdt_file_path)
    #             continue
    # logger.info(f"Loaded {len(sources)} sources and {len(asts)} ASTs from {jdt_file_path}. Skipped {len(skipped_indices)} lines.")
    # return sources, asts

    # sources = []
    # asts = []
    # with open(jdt_file_path, 'r') as file:
    #     for line in file:
    #         line = line.strip()
    #         if line.startswith('Source:') and 'Asts:' in line:
    #             # Split the line into source and AST parts
    #             parts = line.split('", Asts: "')
    #              # Extract the source part, remove 'Source: "' prefix and the trailing double quote
    #             source_part = parts[0][8:]
    #             # If the first character of source_part is a double quote, remove it
    #             if source_part.startswith('"'):
    #                 source_part = source_part[1:]
    #             # Extract the AST part, remove the final double quote
    #             ast_part = parts[1][:-1]
    #             sources.append(source_part)
    #             asts.append(ast_part)
    # print(f"Loaded {len(sources)} sources and {len(asts)} ASTs from {jdt_file_path}")
    # logger.info(f"Loaded {len(sources)} sources and {len(asts)} ASTs from {jdt_file_path}")
    # # if len(sources) <= 10:  # Print all if there are 10 or fewer 
    # #     for source, ast in zip(sources, asts):
    # #         print(f"Source: {source}\nAST: {ast}\n")
    # # else:  # Print only the first few to check some examples
    # #     print("First 5 sources and ASTs:")
    # #     for source, ast in zip(sources[:5], asts[:5]):
    # #         print(f"Source: {source}\nAST: {ast}\n")
    # return sources, asts

# def save_source_ast_pairs(self):
#     # Save every 10th source-AST pair for verification
#     with open('check_source_ast.txt', 'w') as file:
#         for i in range(0, len(self.sources), 10):
#             source_info = f"Source: {self.sources[i]}"
#             ast_info = f"AST: {self.asts[i]}"
#             file.write(f"Source: {self.sources[i]}\nAST: {self.asts[i]}\nNL: {self.names[i]}\n\n")
 
def remove_whitespaces(text):
    """Remove all whitespace characters from the text."""
    return ''.join(text.split())
def lowercase(text):
    """Convert text to lowercase."""
    return text.lower()
def compare(text1, text2):
    """Check if two strings are equal."""
    return text1 == text2

def compare_and_save_sources(self, sources_from_file, asts_from_file, compare_attribute, output_filename):
    """
    Compare sources from file with either self.sources or self.codes based on the compare_attribute.
    Writes mismatches to specified output file.
    """
    # Select the attribute for comparison based on the provided parameter
    comparison_list = getattr(self, compare_attribute)
    all_matched = True
    with open(output_filename, 'w') as file:
        for i, (file_source, file_ast) in enumerate(zip(sources_from_file, asts_from_file)):
            if i < len(comparison_list):
                self_source = comparison_list[i]
                normalized_file_source = remove_whitespaces(lowercase(file_source))
                normalized_self_source = remove_whitespaces(lowercase(self_source))

                if not compare(normalized_file_source, normalized_self_source):
                    all_matched = False
                    file.write(f"Source from file: {normalized_file_source}\n"
                               f"Normalized Self Source: {normalized_self_source}\n"
                               f"Original Source: {self_source}\n"
                               f"AST from file: {file_ast}\n")
    if all_matched == True:
        print('[INFO] All indices are matched b/w JDT datasets and original datasets.')            
# def compare_and_save_sources(self, sources_from_file, asts_from_file):
#     # target_index = 46635  
#     with open('mismatched_sources.txt', 'w') as file:
#         for i, (file_source, file_ast) in enumerate(zip(sources_from_file, asts_from_file)):
#             if i < len(self.sources):  # Check if index is within  of self.sources
#                 self_source = self.sources[i]
                
#                 normalized_file_source = remove_whitespaces(lowercase(file_source))
#                 normalized_self_source = remove_whitespaces(lowercase(self_source))
#                 # # Check and print the specific index information
#                 # if i == target_index:
#                 #     print(f"Index {target_index} details:")
#                 #     print(f"Source from file: {file_source}")
#                 #     print(f"Source from self.sources: {self_source}")
#                 #     print(f"AST from file: {file_ast}")
#                 # Compare 
#                 if not compare(normalized_file_source, normalized_self_source):
#                     # Write details directly to the file if sources do not match
#                     file.write(f"Source from file: {normalized_file_source}\n"
#                             f"Normalized Self Source: {normalized_self_source}\n"
#                             f"Original Source: {self_source}\n"
#                             f"AST from file: {file_ast}\n")
#             else:
#                 "Match"
#                 # Handling cases where sources_from_file might be longer than self.sources
#                 normalized_file_source = remove_whitespaces(lowercase(file_source))
#                 # Write details directly to the file for unmatched sources due to length discrepancy
#                 file.write(f"Source from file: {normalized_file_source}\n"
#                         f"Normalized Self Source: No corresponding source\n"
#                         f"Original Source: No corresponding source\n"
#                         f"AST from file: {file_ast}\n")
                
# def compare_and_save_sources_completion(self, sources_from_file, asts_from_file):
#     # target_index = 46635  
#     with open('mismatched_sources_completion.txt', 'w') as file:
#         for i, (file_source, file_ast) in enumerate(zip(sources_from_file, asts_from_file)):
#             if i < len(self.codes):  # Check if index is within  of self.sources
#                 self_source = self.codes[i]
                
#                 normalized_file_source = remove_whitespaces(lowercase(file_source))
#                 normalized_self_source = remove_whitespaces(lowercase(self_source))
#                 # # Check and print the specific index information
#                 # if i == target_index:
#                 #     print(f"Index {target_index} details:")
#                 #     print(f"Source from file: {file_source}")
#                 #     print(f"Source from self.sources: {self_source}")
#                 #     print(f"AST from file: {file_ast}")
#                 # Compare 
#                 if not compare(normalized_file_source, normalized_self_source):
#                     # Write details directly to the file if sources do not match
#                     file.write(f"Source from file: {normalized_file_source}\n"
#                             f"Normalized Self Source: {normalized_self_source}\n"
#                             f"Original Source: {self_source}\n"
#                             f"AST from file: {file_ast}\n")
#             else:
#                 "Match"
#                 # Handling cases where sources_from_file might be longer than self.sources
#                 normalized_file_source = remove_whitespaces(lowercase(file_source))
#                 # Write details directly to the file for unmatched sources due to length discrepancy
#                 file.write(f"Source from file: {normalized_file_source}\n"
#                         f"Normalized Self Source: No corresponding source\n"
#                         f"Original Source: No corresponding source\n"
#                         f"AST from file: {file_ast}\n")
       
# Define string pattern to replace strings

STRING_MATCHING_PATTERN = re.compile(r'([bruf]*)(\"\"\"|\'\'\'|\"|\')(?:(?!\2)(?:\\.|[^\\]))*\2')

def levenshtein_ratio(s1, s2):
    """Calculate the Levenshtein ratio between two strings."""
    s = difflib.SequenceMatcher(None, s1, s2)
    return 1 - s.ratio()

def process_content(text, remove_first_word):
    """Process the text to prepare for comparison."""
    text = text.lower().replace(' ', '')  # convert to lower case and remove spaces
    if remove_first_word:
        text = remove_first_word_from_text(text)
    return text

def replace_strings(text):
    """Replace string literals in the text."""
    return STRING_MATCHING_PATTERN.sub('___STR', text)

def remove_first_word_from_text(text):
    """Remove the first word from the text."""
    return ' '.join(text.split()[1:])

def format_code(raw_code):
    """Format the raw code to standardize it for comparison."""
    formatted_code = raw_code.replace(' _ ', '').replace(';', ';\n').replace('{', '{\n').replace('}', '\n}')
    return formatted_code

def compare_similarity(source_file, code_file, threshold=0.7):
    """Compare lines from two sources for similarity and return indices of matched items."""
    matched_indices = []
    with open(source_file, 'r') as file1, open(code_file, 'r') as file2:
        for index, (line1, line2) in enumerate(zip(file1, file2)):
            content1 = process_content(line1, True)
            content2 = process_content(replace_strings(format_code(line2)), False)
            ratio = levenshtein_ratio(content1, content2)
            if ratio > threshold:
                matched_indices.append(index)  # Save index if similarity is above threshold
    return matched_indices

