import sys
import os
import re
import tokenize
from io import StringIO
import json
import logging
from tqdm import tqdm
from antlr4 import InputStream

import enums

from data.antlr_parsers.go.GoLexer import GoLexer
from data.antlr_parsers.java.Java8Lexer import Java8Lexer
from data.antlr_parsers.python3.Python3Lexer import Python3Lexer
from data.antlr_parsers.php.PhpLexer import PhpLexer
from data.antlr_parsers.javascript.JavaScriptLexer import JavaScriptLexer
from data.code_tokenizers.ruby.ruby_tokenizer import RubyTokenizer

logger = logging.getLogger(__name__)

STRING_MATCHING_PATTERN = re.compile(r'([bruf]*)(\"\"\"|\'\'\'|\"|\')(?:(?!\2)(?:\\.|[^\\]))*\2')
NON_SPACE_MATCHING_PATTERN = re.compile(r'\S')

MAPPING_LANG_LEXER = {
    enums.LANG_GO: GoLexer,
    enums.LANG_JAVA: Java8Lexer,
    enums.LANG_PYTHON: Python3Lexer,
    enums.LANG_PHP: PhpLexer,
    enums.LANG_JAVASCRIPT: JavaScriptLexer,
    enums.LANG_RUBY: RubyTokenizer()
}

def replace_string_literal(source):
    return re.sub(pattern=STRING_MATCHING_PATTERN, repl='___STR', string=source)

def trim_spaces(string):
    return re.sub(r'\s+', ' ', string).strip()

def trim_method_name(full_name):
    point_pos = full_name.rfind('.')
    if point_pos != -1:
        return full_name[point_pos + 1:]
    else:
        return full_name

# Multi-character operators that should be treated as single tokens
MULTI_CHAR_OPERATORS = {">>>", "<<=", ">>=", ">>>=", "<<", ">>"}

def reassemble_tokens(tokens):
    """Reassemble multi-character operators that were split during tokenization."""
    reassembled_tokens = []
    i = 0
    
    while i < len(tokens):
        if i + 2 < len(tokens) and tokens[i] + tokens[i+1] + tokens[i+2] in MULTI_CHAR_OPERATORS:
            reassembled_tokens.append(tokens[i] + tokens[i+1] + tokens[i+2])
            i += 3
        elif i + 1 < len(tokens) and tokens[i] + tokens[i+1] in MULTI_CHAR_OPERATORS:
            reassembled_tokens.append(tokens[i] + tokens[i+1])
            i += 2
        else:
            reassembled_tokens.append(tokens[i])
            i += 1

    return reassembled_tokens

def tokenize_source(source, lang):
    """Tokenize source code based on language, skipping specific operators and reassembling multi-character operators."""
    if lang not in MAPPING_LANG_LEXER:
        logger.error(f"Lexer for language '{lang}' not found.")
        return ""
    
    input_stream = InputStream(source)
    lexer_class = MAPPING_LANG_LEXER[lang]
    lexer = lexer_class(input_stream)
    
    tokens = [token.text for token in lexer.getAllTokens()]
    
    tokens = reassemble_tokens(tokens)
    
    code = ' '.join(tokens)
    #code = replace_string_literal(' '.join(tokens))
    return trim_spaces(code)

def read_and_tokenize_file(input_file_path, method_names_file_path, tokenized_output_file_path, code_wo_name_output_file_path):
    with open(input_file_path, 'r') as infile, \
         open(method_names_file_path, 'r') as namefile, \
         open(tokenized_output_file_path, 'w') as tokenized_outfile, \
         open(code_wo_name_output_file_path, 'w') as code_wo_name_outfile:

        # Load all method names into a list
        method_names = [trim_method_name(name.strip()) for name in namefile]

        for idx, line in enumerate(infile):
            tokenized_line = tokenize_source(line.strip(), lang=enums.LANG_JAVA)
            
            tokenized_outfile.write(tokenized_line + '\n')
            
            if idx < len(method_names):
                method_name = method_names[idx]
                code_wo_name = tokenized_line.replace(method_name, 'f', 1)
            else:
                code_wo_name = tokenized_line
            
            code_wo_name_outfile.write(code_wo_name + '\n')

if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Tokenize source code file and replace method names.")
    parser.add_argument("file_path", type=str, help="Path to the source code file.")
    parser.add_argument("method_names_file", type=str, help="Path to the file containing method names.")
    parser.add_argument("tokenized_output_file", type=str, help="Path to the tokenized output file.")
    parser.add_argument("code_wo_name_output_file", type=str, help="Path to the output file with method names replaced.")
    args = parser.parse_args()

    try:
        read_and_tokenize_file(
            args.file_path,
            args.method_names_file,
            args.tokenized_output_file,
            args.code_wo_name_output_file
        )
        print(f"Tokenized code saved to {args.tokenized_output_file}")
        print(f"Code with method names replaced saved to {args.code_wo_name_output_file}")
    except Exception as e:
        logger.error(f"An error occurred: {e}")
        sys.exit(1)


# def read_and_tokenize_file(input_file_path, output_file_path):
#     with open(input_file_path, 'r') as infile, open(output_file_path, 'w') as outfile:
#         for line in infile:
#             tokenized_line = tokenize_source(line.strip(),lang=enums.LANG_JAVA)
#             outfile.write(tokenized_line + '\n')

# if __name__ == "__main__":
#     import argparse

#     parser = argparse.ArgumentParser(description="Tokenize source code file.")
#     parser.add_argument("file_path", type=str, help="Path to the source code file.")
#     parser.add_argument("output_file", type=str, help="Path to the output file.")
#     args = parser.parse_args()

#     try:
#         read_and_tokenize_file(args.file_path, args.output_file)
#         print(f"Tokenized code saved to {args.output_file}")
#     except Exception as e:
#         logger.error(f"An error occurred: {e}")
#         sys.exit(1)

