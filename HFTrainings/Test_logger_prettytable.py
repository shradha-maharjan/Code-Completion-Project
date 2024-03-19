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
import pandas as pd
import math
import time
import argparse
import os
import logging
from prettytable import PrettyTablelogging.StreamHandler

# In[2]:

logging.basicConfig(level=logging.DEBUG,
                    format='%(asctime)s - %(levelname)s - %(message)s',
                    filename='logfile.log',
                    filemode='w')
console = logging.StreamHandler()
console.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
console.setFormatter(formatter)
logging.getLogger('').addHandler(console)

start_time= time.time()

codesearchnet_dataset = load_dataset("code_search_net", "java")
codesearchnet_dataset


# In[3]:


sample = codesearchnet_dataset["train"].shuffle(seed=42).select(range(3))

for row in sample:
    print(f"\n'>>> code: {row['whole_func_string']}'")


# In[4]:


# use bert model checkpoint tokenizer
model_checkpoint = "microsoft/codebert-base-mlm"
tokenizer = AutoTokenizer.from_pretrained(model_checkpoint)



# In[6]:


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


# In[7]:


tokenized_samples = tokenized_datasets["train"][:3]

for idx, sample in enumerate(tokenized_samples["input_ids"]):
    print(f"'>>> code {idx} length: {len(sample)}'")


# In[8]:


concatenated_examples = {
    k: sum(tokenized_samples[k], []) for k in tokenized_samples.keys()
}
total_length = len(concatenated_examples["input_ids"])
print(f"'>>> Concatenated code length: {total_length}'")


# In[9]:


chunk_size = 128
chunks = {
    k: [t[i : i + chunk_size] for i in range(0, total_length, chunk_size)]
    for k, t in concatenated_examples.items()
}

for chunk in chunks["input_ids"]:
    print(f"'>>> Chunk length: {len(chunk)}'")


# In[10]:


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


# In[11]:


lm_datasets = tokenized_datasets.map(group_texts, batched=True)
lm_datasets


# In[12]:


tokenizer.decode(lm_datasets["train"][1]["input_ids"])


# In[13]:


tokenizer.decode(lm_datasets["train"][1]["labels"])


# In[14]:


data_collator = DataCollatorForLanguageModeling(tokenizer=tokenizer, mlm_probability=0.15)


# In[15]:


samples = [lm_datasets["train"][i] for i in range(2)]
for sample in samples:
    _ = sample.pop("word_ids")

for chunk in data_collator(samples)["input_ids"]:
    print(f"\n'>>> {tokenizer.decode(chunk)}'")


# In[16]:

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


# In[17]:


samples = [lm_datasets["train"][i] for i in range(2)]
batch = whole_word_masking_data_collator(samples)

for chunk in batch["input_ids"]:
    print(f"\n'>>> {tokenizer.decode(chunk)}'")


# In[ ]:


import argparse

# Parse command-line arguments
parser = argparse.ArgumentParser(description="Data Splitting")
parser.add_argument("-train", type=int, help="Size of the training dataset")
parser.add_argument("-test", type=int, help="Size of the test dataset")
parser.add_argument("-valid", type=int, help="Size of the validation dataset")
args = parser.parse_args()

train_size = args.train
test_size = args.test
valid_size = args.valid


# Create PrettyTable to display sizes of each split
table = PrettyTable()
table.field_names = ["Split", "Size"]
# In[22]:


# Split the dataset into train, validation, and test sets
train_dataset = lm_datasets["train"].shuffle(seed=42).select(range(train_size))
remaining_dataset = lm_datasets["train"].shuffle(seed=42).select(range(train_size, len(lm_datasets["train"])))
valid_dataset = remaining_dataset.shuffle(seed=42).select(range(valid_size))
test_dataset = remaining_dataset.shuffle(seed=42).select(range(valid_size, valid_size + test_size))

# # Print sizes of each split
# print(f"Train dataset size: {len(train_dataset)}")
# print(f"Validation dataset size: {len(valid_dataset)}")
# print(f"Test dataset size: {len(test_dataset)}")
# Add sizes to the PrettyTable
table.add_row(["Train", len(train_dataset)])
table.add_row(["Validation", len(valid_dataset)])
table.add_row(["Test", len(test_dataset)])

# Print the PrettyTable
print(table)

# Log information about the data splitting
logging.info("Data splitting information:")
logging.info(table)

# In[24]:


batch_size = 64
# Show the training loss with every epoch
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


# In[26]:

trainer = Trainer(
    model= AutoModelForMaskedLM.from_pretrained(model_checkpoint),
    args=training_args,
    train_dataset=train_dataset,
    eval_dataset=test_dataset,
    data_collator=data_collator,
    tokenizer=tokenizer,
)


# In[27]:

os.environ['WANDB_NOTEBOOK_NAME'] = 'test copy.ipynb'
os.environ['WANDB_MODE'] = 'disabled'


# In[28]:

# Evaluate the model
eval_results = trainer.evaluate()

# Calculate perplexity
perplexity = math.exp(eval_results['eval_loss'])

# Calculate loss
loss = eval_results['eval_loss']

# Calculate entropy

eval_dataset_size = len(trainer.eval_dataset)

entropy = eval_results['eval_loss']
# Log evaluation results
logging.info(f"Entropy: {entropy:.4f}")
logging.info(f"Perplexity: {perplexity:.2f}")
logging.info(f"Loss: {loss:.2f}")
# In[29]:


trainer.train()


# In[30]:

# Evaluate the model
eval_results = trainer.evaluate()

# Calculate perplexity
perplexity = math.exp(eval_results['eval_loss'])

# Calculate loss
loss = eval_results['eval_loss']

# Calculate entropy

entropy = eval_results['eval_loss']

# Log evaluation results
logging.info(f"Entropy: {entropy:.4f}")
logging.info(f"Perplexity: {perplexity:.2f}")
logging.info(f"Loss: {loss:.2f}")

# In[31]:


trainer.push_to_hub()


# In[32]:


model = "MLM_FinetunedModel"

pred_model = pipeline("fill-mask", model = "MLM_FinetunedModel")

text = "public Evaluation create(SimpleNode node, Object source)\n    {\n        return <mask>(node, source, false);\n    }"

preds = pred_model(text)
print(preds)


# In[ ]:

model = "MLM_FinetunedModel"
pred_model = pipeline("fill-mask", model=model)
text = "public Evaluation create(SimpleNode node, Object source)\n    {\n        return <mask>(node, source, false);\n    }"

# Get predictions
preds = pred_model(text)
print(preds)

# Log predictions
logging.info("Predictions:")
for pred in preds:
    logging.info(pred)
    
# Sort predictions by score in descending order
sorted_preds = sorted(preds, key=lambda x: x['score'], reverse=True)
print(sorted_preds)
# Determine the rank of the correct answer
correct_answer = preds[0]['token_str']
print(correct_answer)
correct_rank = next(i+1 for i, pred in enumerate(sorted_preds) if pred['token_str'] == correct_answer)
print(correct_rank)

# Compute the reciprocal ranks
reciprocal_ranks = [1 / rank for rank in range(1, len(sorted_preds) + 1)]
print(reciprocal_ranks)

# Calculate Mean Reciprocal Rank
mrr = sum(reciprocal_ranks) / len(reciprocal_ranks)

logging.info(f"Mean Reciprocal Rank (MRR): {mrr}")

# In[ ]:

# Get the correct answer (assuming it's the first mask prediction)
correct_answer = preds[0]['token_str']
print(correct_answer)

# Create a DataFrame from the predictions
df = pd.DataFrame(preds)

# Sort the DataFrame by score in descending order
df_sorted = df.sort_values(by='score', ascending=False)
print(df_sorted)

# Reset the index of the sorted DataFrame
df_sorted.reset_index(drop=True, inplace=True)

# Determine the rank of the correct answer
correct_rank = df_sorted.index[df_sorted['token_str'] == correct_answer].tolist()[0] + 1  # Add 1 to start ranks from 1
print(correct_rank)

# Calculate the reciprocal ranks
df_sorted['rank'] = df_sorted.index + 1
print(df_sorted['rank'])
df_sorted['reciprocal_rank'] = 1 / df_sorted['rank']

# Calculate Mean Reciprocal Rank (MRR)
mrr = df_sorted['reciprocal_rank'].mean()

logging.info(f"Mean Reciprocal Rank (MRR): {mrr}")

# In[ ]:

end_time= time.time()


elapsed_time = end_time - start_time

# Convert elapsed time to minutes and seconds
minutes = int(elapsed_time // 60)
seconds = int(elapsed_time % 60)

# Log elapsed time
logging.info(f"Elapsed time: {minutes} minutes {seconds} seconds")




