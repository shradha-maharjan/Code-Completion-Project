import sys
import os

# Add the 'sources' directory to the sys.path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '../..')))

# Debugging: Print sys.path after modification
print("sys.path after modification:", sys.path)

import re
import tokenize
from io import StringIO
import json
import logging
from tqdm import tqdm
from antlr4 import InputStream
import nltk

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

def regular_tokenize(source: str):
    source = re.sub(r'(\S)[.=](\S)', r'\1 . \2', source)
    return ' '.join(nltk.word_tokenize(source))

def trim_spaces(string):
    return re.sub(r'\s+', ' ', string).strip()

def tokenize_source(source, lang='enums.LANG_JAVA', use_regular=False):
    if use_regular:
        code = regular_tokenize(source)
        return trim_spaces(code)

    if lang in [enums.LANG_JAVA, enums.LANG_JAVASCRIPT, enums.LANG_PHP, enums.LANG_GO]:
        input_stream = InputStream(source)
        lexer = MAPPING_LANG_LEXER[lang](input_stream)
        tokens = [token.text for token in lexer.getAllTokens()]
        code = ' '.join(tokens)
        return trim_spaces(code)
    else:
        code = regular_tokenize(source)
        return trim_spaces(code)

def read_and_tokenize_file(input_file_path, output_file_path):
    with open(input_file_path, 'r') as infile, open(output_file_path, 'w') as outfile:
        for line in infile:
            tokenized_line = tokenize_source(line.strip())
            outfile.write(tokenized_line + '\n')

if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Tokenize source code file.")
    parser.add_argument("file_path", type=str, help="Path to the source code file.")
    #parser.add_argument("lang", type=str, help="Language of the source code.")
    parser.add_argument("output_file", type=str, help="Path to the output file.")
    args = parser.parse_args()

    try:
        #read_and_tokenize_file(args.file_path, args.lang, args.output_file)
        read_and_tokenize_file(args.file_path, args.output_file)
        print(f"Tokenized code saved to {args.output_file}")
    except Exception as e:
        logger.error(f"An error occurred: {e}")
        sys.exit(1)

# import re
# import tokenize
# from io import StringIO
# import json
# import os
# import sys
# import logging
# from tqdm import tqdm
# from antlr4 import InputStream
# import nltk

# from asts.ast_parser import generate_single_ast_nl, split_identifier, parse_ast, extract_nl_from_code
# import enums

# from data.vocab import Vocab
# from data.antlr_parsers.go.GoLexer import GoLexer
# from data.antlr_parsers.java.Java8Lexer import Java8Lexer
# from data.antlr_parsers.python3.Python3Lexer import Python3Lexer
# from data.antlr_parsers.php.PhpLexer import PhpLexer
# from data.antlr_parsers.javascript.JavaScriptLexer import JavaScriptLexer
# from data.code_tokenizers.ruby.ruby_tokenizer import RubyTokenizer

# logger = logging.getLogger(__name__)

# STRING_MATCHING_PATTERN = re.compile(r'([bruf]*)(\"\"\"|\'\'\'|\"|\')(?:(?!\2)(?:\\.|[^\\]))*\2')
# NON_SPACE_MATCHING_PATTERN = re.compile(r'\S')

# MAPPING_LANG_LEXER = {
#     enums.LANG_GO: GoLexer,
#     enums.LANG_JAVA: Java8Lexer,
#     enums.LANG_PYTHON: Python3Lexer,
#     enums.LANG_PHP: PhpLexer,
#     enums.LANG_JAVASCRIPT: JavaScriptLexer,
#     enums.LANG_RUBY: RubyTokenizer()
# }

# def regular_tokenize(source: str):
#     """
#     NLTK word tokenize with simple adoptions for source code.

#     Args:
#         source (str): Source code string.

#     Returns:
#         str: Tokenized code string
#     """
#     source = re.sub(r'(\S)[.=](\S)', r'\1 . \2', source)
#     return ' '.join(nltk.word_tokenize(source))

# def trim_spaces(string):
#     """
#     Replace consecutive spaces with a single whitespace.

#     Args:
#         string (str): String

#     Returns:
#         str: Replaced string
#     """
#     return re.sub(r'\s+', ' ', string).strip()

# def tokenize_source(source, lang, use_regular=False):
#     """
#     Tokenize the source code into tokens.

#     Args:
#         source (str): Source in string
#         lang (str): Language of source code
#         use_regular (bool): Whether to use regular tokenize method, default to False

#     Returns:
#         str: Tokenized code, delimited by whitespace

#     """
#     if use_regular:
#         code = regular_tokenize(source)
#         return trim_spaces(code)
#     # if lang == enums.LANG_PYTHON:
#     #     tokens = tokenize.generate_tokens(StringIO(source).readline)
#     #     code = ' '.join([token.string for token in tokens])
        
#     #     return trim_spaces(code)
#     if lang in [enums.LANG_JAVA, enums.LANG_JAVASCRIPT, enums.LANG_PHP, enums.LANG_GO]:
#         input_stream = InputStream(source)
#         lexer = MAPPING_LANG_LEXER[lang](input_stream)
#         tokens = [token.text for token in lexer.getAllTokens()]
#         code = ' '.join(tokens)
#         return trim_spaces(code)
#     # elif lang == enums.LANG_RUBY:
#     #     tokens = MAPPING_LANG_LEXER[lang].get_pure_tokens(source)
#     #     code = ' '.join([token[0] for token in tokens])
#     #     return trim_spaces(code)
#     # else:
#     #     # TODO: c# tokenize
#     #     code = regular_tokenize(source)
#     #     return trim_spaces(code)

# def read_and_tokenize_file(file_path, lang):
#     """
#     Read a source code file and tokenize its content.

#     Args:
#         file_path (str): Path to the source code file.
#         lang (str): Language of the source code.

#     Returns:
#         str: Tokenized code.
#     """
#     with open(file_path, 'r') as file:
#         source_code = file.read()
#     return tokenize_source(source_code, lang)

# if __name__ == "__main__":
#     import argparse

#     parser = argparse.ArgumentParser(description="Tokenize source code file.")
#     parser.add_argument("file_path", type=str, help="Path to the source code file.")
#     args = parser.parse_args()


#     tokenized_code = read_and_tokenize_file(args.file_path)
#     print(tokenized_code)
