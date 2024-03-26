#!/usr/bin/env python
# coding: utf-8

# In[1]:


from collections import defaultdict
from tqdm import tqdm
from datasets import Dataset, load_dataset, DatasetDict
from transformers import AutoTokenizer, GPT2LMHeadModel, AutoConfig, DataCollatorForLanguageModeling
from transformers import Trainer, TrainingArguments
import torch
from transformers import pipeline
from torch.nn import CrossEntropyLoss
from torch.utils.data.dataloader import DataLoader
from torch.optim import AdamW
from accelerate import Accelerator,notebook_launcher
from transformers import get_scheduler
from huggingface_hub import Repository, get_full_repo_name
from transformers import AutoModelForMaskedLM
from transformers import default_data_collator
import collections
import numpy as np
import math
import time
import argparse
import pandas as pd
import logging
import os
import sys
from prettytable import PrettyTable  # Import PrettyTable


start_time= time.time()
# Setup logging
logger = logging.getLogger()
logger.setLevel(logging.DEBUG)
logging.basicConfig(level=logging.DEBUG,
                    format='%(asctime)s - %(levelname)s - %(message)s',
                    filename='MLM_logs20000.log',
                    filemode='w')

# Console handler
console = logging.StreamHandler()
console.setLevel(logging.INFO)
logger.addHandler(console)

# File handler
file = logging.FileHandler('info.log')
file.setLevel(logging.DEBUG)
formatter = logging.Formatter('[%(asctime)s | %(filename)s | line %(lineno)d] - %(levelname)s: %(message)s')
file.setFormatter(formatter)
logger.addHandler(file)

codesearchnet_dataset = load_dataset("code_search_net", "java")
codesearchnet_dataset

sample = codesearchnet_dataset["train"].shuffle(seed=42).select(range(3))

for row in sample:
    print(f"\n'>>> code: {row['whole_func_string']}'")


model_checkpoint = "microsoft/codebert-base-mlm"
tokenizer = AutoTokenizer.from_pretrained(model_checkpoint)


codesearchnet_dataset


def tokenize_function(examples):
    result = tokenizer(examples["whole_func_string"])
    if tokenizer.is_fast:
        result["word_ids"] = [result.word_ids(i) for i in range(len(result["input_ids"]))]
    return result


# Use batched=True to activate fast multithreading!
tokenized_datasets = codesearchnet_dataset.map(
    tokenize_function, batched=True, remove_columns=['repository_name', 'func_path_in_repository', 'func_name', 'whole_func_string', 'language', 'func_code_string', 'func_code_tokens', 'func_documentation_string', 'func_documentation_tokens', 'split_name', 'func_code_url']
)
tokenized_datasets


tokenized_samples = tokenized_datasets["train"][:3]

for idx, sample in enumerate(tokenized_samples["input_ids"]):
    print(f"'>>> code {idx} length: {len(sample)}'")


concatenated_examples = {
    k: sum(tokenized_samples[k], []) for k in tokenized_samples.keys()
}
total_length = len(concatenated_examples["input_ids"])
print(f"'>>> Concatenated code length: {total_length}'")


chunk_size = 128
chunks = {
    k: [t[i : i + chunk_size] for i in range(0, total_length, chunk_size)]
    for k, t in concatenated_examples.items()
}

for chunk in chunks["input_ids"]:
    print(f"'>>> Chunk length: {len(chunk)}'")


def group_texts(examples):
    # Concatenate all texts
    concatenated_examples = {k: sum(examples[k], []) for k in examples.keys()}
    # Compute length of concatenated texts
    total_length = len(concatenated_examples[list(examples.keys())[0]])
    # We drop the last chunk if it's smaller than chunk_size
    total_length = (total_length // chunk_size) * chunk_size
    # Split by chunks of max_len
    result = {
        k: [t[i : i + chunk_size] for i in range(0, total_length, chunk_size)]
        for k, t in concatenated_examples.items()
    }
    # Create a new labels column
    result["labels"] = result["input_ids"].copy()
    return result


lm_datasets = tokenized_datasets.map(group_texts, batched=True)
lm_datasets


tokenizer.decode(lm_datasets["train"][1]["input_ids"])


tokenizer.decode(lm_datasets["train"][1]["labels"])


data_collator = DataCollatorForLanguageModeling(tokenizer=tokenizer, mlm_probability=0.15)

samples = [lm_datasets["train"][i] for i in range(2)]
for sample in samples:
    _ = sample.pop("word_ids")

for chunk in data_collator(samples)["input_ids"]:
    print(f"\n'>>> {tokenizer.decode(chunk)}'")

wwm_probability = 0.2


def whole_word_masking_data_collator(features):
    for feature in features:
        word_ids = feature.pop("word_ids")

        # Create a map between words and corresponding token indices
        mapping = collections.defaultdict(list)
        current_word_index = -1
        current_word = None
        for idx, word_id in enumerate(word_ids):
            if word_id is not None:
                if word_id != current_word:
                    current_word = word_id
                    current_word_index += 1
                mapping[current_word_index].append(idx)

        # Randomly mask words
        mask = np.random.binomial(1, wwm_probability, (len(mapping),))
        input_ids = feature["input_ids"]
        labels = feature["labels"]
        new_labels = [-100] * len(labels)
        for word_id in np.where(mask)[0]:
            word_id = word_id.item()
            for idx in mapping[word_id]:
                new_labels[idx] = labels[idx]
                input_ids[idx] = tokenizer.mask_token_id
        feature["labels"] = new_labels

    return default_data_collator(features)


samples = [lm_datasets["train"][i] for i in range(2)]
batch = whole_word_masking_data_collator(samples)

for chunk in batch["input_ids"]:
    print(f"\n'>>> {tokenizer.decode(chunk)}'")

# Define a function to create a pretty table for configurations
def create_config_table(config_dict):
    config_table = PrettyTable()
    config_table.field_names = ["Configuration", "Value"]
    config_table.align["Configuration"] = "l"
    config_table.align["Value"] = "l"
    for config, value in config_dict.items():
        config_table.add_row([config, str(value)])
    return config_table

parser = argparse.ArgumentParser(description="Data Splitting")
parser.add_argument("-train", type=int, help="Size of the training dataset")
parser.add_argument("-valid", type=int, help="Size of the validation dataset")
args = parser.parse_args()

train_size = args.train
valid_size = args.valid

# Display configurations in a pretty table
config_dict = {'train_size': train_size, 'valid_size': valid_size}
config_table = create_config_table(config_dict)
logger.debug('Configurations:\n{}'.format(config_table))

train_dataset = lm_datasets["train"].shuffle(seed=42).select(range(train_size))
valid_dataset = lm_datasets["validation"].shuffle(seed=42).select(range(valid_size))

# Print sizes of each split
print(f"Train dataset size: {len(train_dataset)}")
print(f"Validation dataset size: {len(valid_dataset)}")


# In[23]:

batch_size = 64
logging_steps = len(train_dataset) // batch_size

training_args = TrainingArguments(
    output_dir="MLM_FinetunedModel",
    overwrite_output_dir=True,
    evaluation_strategy="epoch",
    learning_rate=2e-5,
    weight_decay=0.01,
    per_device_train_batch_size=batch_size,
    per_device_eval_batch_size=batch_size,
    push_to_hub=True,
    fp16=True,
    logging_steps=logging_steps,
)


# In[24]:


from transformers import Trainer

trainer = Trainer(
    model= AutoModelForMaskedLM.from_pretrained(model_checkpoint),
    args=training_args,
    train_dataset=train_dataset,
    eval_dataset=valid_dataset,
    data_collator=data_collator,
    tokenizer=tokenizer,
)

print (train_dataset)
print (valid_dataset)


os.environ['WANDB_NOTEBOOK_NAME'] = 'MLM_wholewordmask.ipynb'
os.environ['WANDB_MODE'] = 'disabled'

# Evaluate the model
eval_results = trainer.evaluate()

# Calculate perplexity
perplexity = math.exp(eval_results['eval_loss'])


# Calculate loss
loss = eval_results['eval_loss']

logger.info(f"Perplexity: {perplexity:.2f}")
logger.info(f"Loss: {loss:.2f}")


# In[27]:


trainer.train()


# In[28]:


# Evaluate the model
eval_results = trainer.evaluate()

# Calculate perplexity
perplexity = math.exp(eval_results['eval_loss'])

# Calculate loss
loss = eval_results['eval_loss']

logger.info(f"Perplexity: {perplexity:.2f}")
logger.info(f"Loss: {loss:.2f}")


# ------------------------------------------------------------------------
# Remove the above and start the evaluation from the following code.
# ------------------------------------------------------------------------


# Iterate through each masked text and its corresponding ground truth text
pred_model = pipeline("fill-mask", model=model_checkpoint)

# Path to the file containing masked texts and ground truth texts
# masked_texts_file = "/home/user1-selab3/shradha_test/jsoninput/whole_func_strings2.txt"
# ground_truth_file = "/home/user1-selab3/shradha_test/jsoninput/ground_truth.txt"
masked_texts_file = "/home/user1-selab3/shradha_test/jsoninput/outputs.txt"
ground_truth_file = "/home/user1-selab3/shradha_test/jsoninput/output_java.txt"

# List to store reciprocal ranks for each masked text
reciprocal_ranks = []

# Read the masked texts and ground truth texts from their respective files
with open(masked_texts_file, "r") as masked_file, open(ground_truth_file, "r") as truth_file:
    masked_texts = masked_file.readlines()
    ground_truth_texts = truth_file.readlines()
    
# Initialize an empty list to store predictions
all_preds = []

# Initialize a counter for masked token IDs
masked_token_id_counter = 0

# Initialize a counter for ground truth text line IDs
ground_truth_line_id_counter = 0

# Initialize an empty list to store ground truth texts and their IDs
ground_truth_data = []

# Iterate through masked texts and ground truth texts
for masked_text, truth_text in zip(masked_texts, ground_truth_texts):
    # Increment the masked token ID counter for each new masked token
    masked_token_id_counter += 1
    
    # Get predictions for the current masked text
    preds = pred_model(masked_text, top_k= 10)
    logging.info("Predictions:")
    for pred in preds:
        logging.info(pred)
    
    # Initialize an empty list to store predictions for the current masked text
    masked_text_preds = []
    
    # Iterate through predictions for the current masked text
    for rank, pred in enumerate(sorted(preds, key=lambda x: x['score'], reverse=True), start=1):
        # Create a dictionary for each prediction with required fields
        pred_dict = {
            'token_id': masked_token_id_counter,
            'rank': rank,
            'score': pred['score'],
            'token': pred['token'],
            'token_str': pred['token_str'],
            'sequence': pred['sequence']
        }
        # Append the prediction dictionary to the list of predictions for the current masked text
        masked_text_preds.append(pred_dict)
    
    # Append the list of predictions for the current masked text to the list of all predictions
    all_preds.extend(masked_text_preds)
    
    # Increment the ground truth line ID counter
    ground_truth_line_id_counter += 1
    
    # Store ground truth text and its ID
    ground_truth_data.append({'ground_truth_text': truth_text, 'token_id': ground_truth_line_id_counter})

# Convert the list of predictions into a DataFrame
preds_df = pd.DataFrame(all_preds)

# Convert the list of ground truth data into a DataFrame
ground_truth_df = pd.DataFrame(ground_truth_data)

# Print the DataFrame containing predictions
print("Predictions DataFrame:")
print(preds_df)

# Print the DataFrame containing ground truth text
print("\nGround Truth DataFrame:")
print(ground_truth_df)


# In[37]:


results = preds_df.merge(ground_truth_df, how='left', on=['token_id'])

# Function to return the sequence if it matches the ground truth sequence, or fill null otherwise
def fill_sequence(row):
    if row['sequence'] == row['ground_truth_text']:
        return row['sequence']
    else:
        return None

# Apply the function to the 'sequence' column
results['match_sequence'] = results.apply(fill_sequence, axis=1)

# Display the results DataFrame
print(results['match_sequence'])


# In[38]:


results['match_sequence'].fillna('None', inplace=True)

# Group by 'token_id' and 'match_sequence', taking the minimum rank
relevances_rank = results.groupby(['token_id', 'match_sequence'])['rank'].min()

print(relevances_rank)


# In[39]:


ranks = relevances_rank[relevances_rank.index.get_level_values('match_sequence') != 'None']

print(ranks)


# In[40]:


# # Filter results for rows where match_sequence is True
# match_sequence_results = results[results['match_sequence'].notna()]

# # Access the ranks corresponding to the match sequence
# ranks = match_sequence_results['rank']

# # Print the ranks
# print(ranks)


# In[40]:


reciprocal_ranks = 1 / (ranks)
reciprocal_ranks


# In[41]:


mean_reciprocal_rank = reciprocal_ranks.mean()

logger.info(f"Mean Reciprocal Rank: {mean_reciprocal_rank:.2f}")


# In[47]:


# relevant_docs = results[results['score'] > 0.9]
# relevant_docs


# Function to return the sequence if it matches the ground truth sequence, or fill null otherwise
def fill_sequence(row):
    # Convert score to the desired format
    score_formatted = '{:.2f}'.format(row['score'] * 100)  # Convert to percentage format with 2 decimal places
    
    if row['score'] > 0.0006:
        return f"{score_formatted}%: {row['sequence']}"  # Include the score in the specified format
    else:
        return None

# Apply the function to the 'sequence' column
results['match_sequence'] = results.apply(fill_sequence, axis=1)

# Display the results DataFrame
results.head(10)


# In[49]:


results['match_sequence'].fillna('None', inplace=True)

# Group by 'token_id' and 'match_sequence', taking the minimum rank
relevances_rank = results.groupby(['token_id', 'match_sequence'])['rank'].min()

print(relevances_rank)


# In[66]:


# # Filter results for rows where match_sequence is True
# match_sequence_results = results[results['match_sequence'].notna()]
# match_sequence_results.head(10)

# Access the ranks corresponding to the match sequence
# ranks = match_sequence_results['rank']
# ranks.head(10)
# Print the ranks
#print(ranks)


# In[50]:


ranks = relevances_rank[relevances_rank.index.get_level_values('match_sequence') != 'None']

print(ranks)


# In[51]:


reciprocal_ranks = 1 / (ranks)
reciprocal_ranks


# In[52]:


mean_reciprocal_rank = reciprocal_ranks.mean()


logger.info(f"Mean Reciprocal Rank: {mean_reciprocal_rank:.2f}")

end_time= time.time()

elapsed_time = end_time - start_time

# Convert elapsed time to minutes and seconds
minutes = int(elapsed_time // 60)
seconds = int(elapsed_time % 60)


logger.info(f"Elapsed time: {minutes} minutes {seconds} seconds")





