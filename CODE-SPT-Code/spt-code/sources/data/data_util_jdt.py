from torch.utils.data.dataset import Dataset

import os
import logging

from .data_utils import  handle_error

# Configure logging
logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)

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
                handle_error(str(e), source=source_part, file_path=jdt_file_path)
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
                
                