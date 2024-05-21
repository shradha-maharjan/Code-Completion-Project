import torch.utils.data
from transformers import BartConfig, Seq2SeqTrainingArguments, IntervalStrategy, SchedulerType, TrainingArguments

import logging
import os
from typing import Union, Tuple

import enums
from data.dataset import init_dataset
from data.vocab import Vocab, init_vocab, load_vocab
from utils.general import count_params, human_format, layer_wise_parameters
from utils.trainer import CodeTrainer, CodeCLSTrainer
from utils.callbacks import LogStateCallBack
from models.bart import BartForClassificationAndGeneration

import pickle
file_path = "/home/user1-selab3/Documents/research-shradha/CODE-SPT-Code/dataset/dataset_saved/fine_tune.search.java.train.pk"
with open(file_path, "rb") as f:
    load_data = pickle.load(f)
for data in load_data:
    print(data)
    break

"""
('train', '@ Override public ImageSource apply ( ImageSource input ) { final int [ ] [ ] pixelMatrix = new int [ 3 ] [ 3 ] ; int w = input . getWidth ( ) ; int h = input . getHeight ( ) ; int [ ] [ ] output = new int [ h ] [ w ] ; for ( int j = 1 ; j < h - 1 ; j ++ ) { for ( int i = 1 ; i < w - 1 ; i ++ ) { pixelMatrix [ 0 ] [ 0 ] = input . getR ( i - 1 , j - 1 ) ; pixelMatrix [ 0 ] [ 1 ] = input . getRGB ( i - 1 , j ) ; pixelMatrix [ 0 ] [ 2 ] = input . getRGB ( i - 1 , j + 1 ) ; pixelMatrix [ 1 ] [ 0 ] = input . getRGB ( i , j - 1 ) ; pixelMatrix [ 1 ] [ 2 ] = input . getRGB ( i , j + 1 ) ; pixelMatrix [ 2 ] [ 0 ] = input . getRGB ( i + 1 , j - 1 ) ; pixelMatrix [ 2 ] [ 1 ] = input . getRGB ( i + 1 , j ) ; pixelMatrix [ 2 ] [ 2 ] = input . getRGB ( i + 1 , j + 1 ) ; int edge = ( int ) convolution ( pixelMatrix ) ; int rgb = ( edge << 16 | edge << 8 | edge ) ; output [ j ] [ i ] = rgb ; } } MatrixSource source = new MatrixSource ( output ) ; return source ; }', 'local_variable_declaration__ array_creation_expression __local_variable_declaration local_variable_declaration local_variable_declaration local_variable_declaration__ array_creation_expression __local_variable_declaration for_statement__ local_variable_declaration binary_expression__ binary_expression __binary_expression update_expression for_statement__ local_variable_declaration binary_expression__ binary_expression __binary_expression update_expression expression_statement__ assignment_expression__ binary_expression binary_expression __assignment_expression __expression_statement expression_statement__ assignment_expression__ binary_expression __assignment_expression __expression_statement expression_statement__ assignment_expression__ binary_expression binary_expression __assignment_expression __expression_statement expression_statement__ assignment_expression__ binary_expression __assignment_expression __expression_statement expression_statement__ assignment_expression__ binary_expression __assignment_expression __expression_statement expression_statement__ assignment_expression__ binary_expression binary_expression __assignment_expression __expression_statement expression_statement__ assignment_expression__ binary_expression __assignment_expression __expression_statement expression_statement__ assignment_expression__ binary_expression binary_expression __assignment_expression __expression_statement local_variable_declaration__ cast_expression __local_variable_declaration local_variable_declaration__ parenthesized_expression__ binary_expression__ binary_expression__ binary_expression binary_expression __binary_expression __binary_expression __parenthesized_expression __local_variable_declaration expression_statement__ assignment_expression __expression_statement __for_statement __for_statement local_variable_declaration__ object_creation_expression __local_variable_declaration return_statement', 'apply get width get height get r get rgb get rgb get rgb get rgb get rgb get rgb get rgb convolution', 'Expects a height mat as input param input A grayscale height map return edges')
('train', 'public < L extends Listener > void popEvent ( Event < ? , L > expected ) { synchronized ( this . stack ) { final Event < ? , ? > actual = this . stack . pop ( ) ; if ( actual != expected ) { throw new IllegalStateException ( String . format ( ___STR , expected . getListenerClass ( ) , actual ) ) ; } } }', 'synchronized_statement__ parenthesized_expression local_variable_declaration if_statement__ parenthesized_expression__ binary_expression __parenthesized_expression throw_statement__ object_creation_expression __throw_statement __if_statement __synchronized_statement', 'pop event pop format get listener class', 'Pops the top event off the current event stack This action has to be performed immediately after the event has been dispatched to all listeners param Type of the listener param expected The Event which is expected at the top of the stack pushEvent')
('train', 'protected void modify ( Transaction t ) { try { this . lock . writeLock ( ) . lock ( ) ; t . perform ( ) ; } finally { this . lock . writeLock ( ) . unlock ( ) ; } }', 'try_statement__ expression_statement expression_statement expression_statement __try_statement', 'modify write lock lock perform write lock unlock', 'Executes the given transaction within the context of a write lock param t The transaction to execute')
...
"""

import pickle

# Replace 'path_to_your_pickle_file.pkl' with the actual path to your pickle file
filename = '/home/user1-selab3/Documents/pre_train.pk'
# filename = '/home/user1-selab3/Documents/research-shradha/CODE-SPT-Code/dataset/dataset_saved/pre_train.pk'

# Load data from the pickle file
with open(filename, 'rb') as file:
    dataset = pickle.load(file)

if hasattr(dataset, 'asts'):
    print("Type of 'asts':", type(dataset.asts))
    if isinstance(dataset.asts, list) and len(dataset.asts) > 0:
        print("Sample of 'asts':", dataset.asts[0])
    else:
        print("The 'asts' list is empty.")
else:
    print("The 'asts' attribute does not exist.")


if hasattr(dataset, 'asts') and dataset.asts:
    # Convert all entries to strings (if necessary) and join into a single string for processing
    extracted_data = ' '.join(str(ast) for ast in dataset.asts)
    components = extracted_data.split()  # Split by space, adjust if your data uses a different delimiter
    unique_components = list(set(components))  # Remove duplicates
    unique_components.sort()  # Optional: sort the components

    print("Unique components in 'asts':")
    for component in unique_components:
        print(component)
else:
    print("The 'asts' attribute is empty or does not exist.")