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
    source_lines, target_lines, ast_lines, nl_lines = source_lines[:100], target_lines[:100], ast_lines[:100], nl_lines[:100]
    #source_lines, target_lines, ast_lines, nl_lines = source_lines, target_lines, ast_lines, nl_lines
    print("Assertion success")
    assert len(source_lines) == len(target_lines) == len(ast_lines) == len(nl_lines)

    return source_lines, ast_lines, nl_lines, target_lines

def load_files_for_pretrain(pretrain_file_path):

    source_path = os.path.join(pretrain_file_path, f'pretrain-orgstr-singleline-combined.txt')
    code_path = os.path.join(pretrain_file_path, f'Tokenized_Code/pretrain_tokenized_gen.txt')
    ast_path = os.path.join(pretrain_file_path, f'Asts/pretrain_asts_gen.txt')
    nl_path = os.path.join(pretrain_file_path, f'NL/NL_pretrain_source_wname_gen.txt')
    codes_wo_name_path = os.path.join(pretrain_file_path, f'Tokenized_Code/pretrain_code_wo_name_gen.txt')
    nl_wo_name_path = os.path.join(pretrain_file_path, f'NL/NL_pretrain_source_wo_name_gen.txt')
    only_names_path = os.path.join(pretrain_file_path, f'Only_Names/pretrain_onlynames.txt')
    docs_path = os.path.join(pretrain_file_path, f'Docstrings/pretrain_docstrings.txt')

    if not pretrain_file_path:
        raise ValueError("path must be provided when ast_type is 'jdt'.")
    
    source_lines = load_lines(source_path)
    code_lines = load_lines(code_path)
    ast_lines = load_lines(ast_path)
    nl_lines = load_lines(nl_path)
    codes_wo_name_lines = load_lines(codes_wo_name_path)
    nl_wo_name_lines = load_lines(nl_wo_name_path)
    only_name_lines = load_lines(only_names_path)
    docs_lines = load_lines(docs_path)
    #ast_lines, nl_lines,nl_wo_name_lines = ast_lines[:100], nl_lines[:100], nl_wo_name_lines[:100]
    #sources, codes, asts, names, codes_wo_name, names_wo_name, only_name, docs  = source_lines, code_lines, ast_lines, nl_lines, codes_wo_name_lines, nl_wo_name_lines, only_name_lines, docs_lines
    sources, codes, asts, names, codes_wo_name, names_wo_name, only_name, docs  = source_lines[:100], code_lines[:100], ast_lines[:100], nl_lines[:100], codes_wo_name_lines[:100], nl_wo_name_lines[:100], only_name_lines[:100], docs_lines[:100]
    assert len(sources) == len(codes) == len(asts) == len(names) == len(codes_wo_name) == len(names_wo_name) == len(only_name) == len(docs)

    return sources, codes, asts, names, codes_wo_name, names_wo_name, only_name, docs


def load_files_for_pretrain(pretrain_file_path):

    source_path = os.path.join(pretrain_file_path, f'pretrain-fun-mask.txt')

    if not pretrain_file_path:
        raise ValueError("path must be provided when ast_type is 'jdt'.")
    
    source_lines = load_lines(source_path)
    sources  = source_lines[:100]

    return sources

def load_lines_from_file(file_path):
    lines = []
    with open(file_path, 'r') as file:
        for line in file:
            lines.append(line.strip())
    return lines
