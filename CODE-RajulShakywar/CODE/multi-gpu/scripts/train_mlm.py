
from collections import defaultdict
from tqdm import tqdm
from datasets import Dataset, load_dataset, DatasetDict
from transformers import AutoTokenizer, GPT2LMHeadModel, AutoConfig, DataCollatorForLanguageModeling
from transformers import Trainer, TrainingArguments
import torch
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
import logging
import os
import sys
from prettytable import PrettyTable 

# Command line arguments
parser = argparse.ArgumentParser(description="Data Splitting")
parser.add_argument("-train", type=int, required=True, help="Size of the training dataset (e.g., 10000)")
parser.add_argument("-test", type=int, required=True, help="Size of the test dataset (e.g., 1000)")
parser.add_argument("-ngpu", type=int, required=True, help="Number of GPUs (e.g., 1 or 2)")
parser.add_argument("-logfile", type=str, required=True, help="Log file name (e.g., logfile.log)")
args = parser.parse_args()

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
file = logging.FileHandler(args.logfile)
file.setLevel(logging.DEBUG)
formatter = logging.Formatter('[%(asctime)s | %(filename)s | line %(lineno)d] - %(levelname)s: %(message)s')
file.setFormatter(formatter)
logger.addHandler(file)

codesearchnet_dataset = load_dataset("code_search_net", "java")

model_checkpoint = "microsoft/codebert-base-mlm"
tokenizer = AutoTokenizer.from_pretrained(model_checkpoint)

def tokenize_function(examples):
    result = tokenizer(examples["whole_func_string"])
    if tokenizer.is_fast:
        result["word_ids"] = [result.word_ids(i) for i in range(len(result["input_ids"]))]
    return result

tokenized_datasets = codesearchnet_dataset.map(
    tokenize_function, batched=True, remove_columns=['repository_name', 'func_path_in_repository', 'func_name', 'whole_func_string', 'language', 'func_code_string', 'func_code_tokens', 'func_documentation_string', 'func_documentation_tokens', 'split_name', 'func_code_url']
)

chunk_size = 128

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

data_collator = DataCollatorForLanguageModeling(tokenizer=tokenizer, mlm_probability=0.15)

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


# Define a function to create a pretty table for configurations
def create_config_table(config_dict):
    config_table = PrettyTable()
    config_table.field_names = ["Configuration", "Value"]
    config_table.align["Configuration"] = "l"
    config_table.align["Value"] = "l"
    for config, value in config_dict.items():
        config_table.add_row([config, str(value)])
    return config_table

train_size = args.train
test_size = args.test

# Display configurations in a pretty table
config_dict = {'train_size': train_size, 'test_size': test_size}
config_table = create_config_table(config_dict)
logger.info('Configurations:\n{}'.format(config_table))

downsampled_dataset = lm_datasets["train"].train_test_split(
    train_size=train_size, test_size=test_size, seed=42
)

def insert_random_mask(batch):
    features = [dict(zip(batch, t)) for t in zip(*batch.values())]
    masked_inputs = whole_word_masking_data_collator(features)
    # Create a new "masked" column for each column in the dataset
    return {"masked_" + k: v.numpy() for k, v in masked_inputs.items()}

eval_dataset = downsampled_dataset["test"].map(
    insert_random_mask,
    batched=True,
    remove_columns=downsampled_dataset["test"].column_names,
)
eval_dataset = eval_dataset.rename_columns(
    {
        "masked_input_ids": "input_ids",
        "masked_attention_mask": "attention_mask",
        "masked_labels": "labels",
    }
)

model_name = "MLM_FinetunedModel_accel"
# repo_name = get_full_repo_name(model_name)

output_dir = model_name
# repo = Repository(output_dir, clone_from=repo_name)

def training_function():

    # set batch size to 32, a larger bacth size when using a more powerful gpu
    batch_size = 32

    train_dataloader = DataLoader(downsampled_dataset["train"], shuffle=True, batch_size=batch_size, collate_fn=whole_word_masking_data_collator)
    eval_dataloader = DataLoader(downsampled_dataset["test"], batch_size=batch_size, collate_fn=whole_word_masking_data_collator)

    # initialize pretrained bert model
    model = AutoModelForMaskedLM.from_pretrained(model_checkpoint)
    # set the optimizer
    optimizer = AdamW(model.parameters(), lr=5e-5)

    # initialize accelerator for training
    accelerator = Accelerator()
    model, optimizer, train_dataloader, eval_dataloader = accelerator.prepare(model, optimizer, train_dataloader, eval_dataloader)

    # set the number of epochs which is set to 30
    num_train_epochs = 30
    num_update_steps_per_epoch = len(train_dataloader)
    num_training_steps = num_train_epochs * num_update_steps_per_epoch

    # define the learning rate scheduler for training
    lr_scheduler = get_scheduler("linear",optimizer=optimizer,num_warmup_steps=0,num_training_steps=num_training_steps)

    progress_bar = tqdm(range(num_training_steps))

    # counter = 0

    for epoch in range(num_train_epochs):
        # Training
        model.train()
        for batch_idx, batch in enumerate(train_dataloader):
            outputs = model(**batch)
            loss = outputs.loss
            accelerator.backward(loss)
            optimizer.step()
            lr_scheduler.step()
            optimizer.zero_grad()
            progress_bar.update(1)
            if epoch == 0 and batch_idx == 0:
                stream = os.popen('nvidia-smi')
                output = stream.read()
                print("---------------Nvidia GPU---------------")
                print(output)               
                # Show a bash command like 'nvidia-smi'
                # IF the result of 'nvidia-smi' shows 2 GPU usage, then
                # If '2-GPU program' uses 2 GPUs for building a model and no difference from 1 GPU program result, then
                #   Test the program train_mlm with training data size, 20000 and testing data size 2000.
                
                # IF the result of 'nvidia-smi' shows 1 GPU usage, then
                # If '2-GPU program' does not use 2 GPUs, then
                #   Need to explore the solution to let the program/environment determine 2 GPUs specified in YML.
                
                # counte += 1

        # Evaluation
        model.eval()
        losses = []
        for step, batch in enumerate(eval_dataloader):
            with torch.no_grad():
                outputs = model(**batch)
            loss = outputs.loss # <===== Added.
            losses.append(accelerator.gather(loss.repeat(batch_size)))
        loss = torch.mean(torch.cat(losses))
        logger.info(f">>> Epoch {epoch}: Loss: {loss.item()}")

        # perplexity metric used for mask language model training
        try:
            perplexity = torch.exp(torch.tensor(loss))
        except OverflowError:
            perplexity = float("inf")
        logger.info(f">>> Epoch {epoch}: Perplexity: {perplexity.item()}")

        # Calculate probabilities
        losses_tensor = torch.cat(losses)  
        probabilities = torch.nn.functional.softmax(-losses_tensor, dim=0)  # Taking negative of losses_tensor to ensure proper softmax calculation

        # Calculate entropy
        entropy = -torch.sum(probabilities * torch.log(probabilities + 1e-20)) 
        logger.info(f">>> Epoch {epoch}: Entropy: {entropy.item()}")  # Print entropy

        # Save model
        accelerator.wait_for_everyone()
        unwrapped_model = accelerator.unwrap_model(model)
        unwrapped_model.save_pretrained(output_dir, save_function=accelerator.save)
        if accelerator.is_main_process:
            tokenizer.save_pretrained(output_dir)
            # repo.push_to_hub(
            #     commit_message=f"Training in progress epoch {epoch}", blocking=False
            # )

start_time= time.time()
notebook_launcher(training_function, num_processes = args.ngpu)
end_time= time.time()
elapsed_time = end_time - start_time
minutes = int(elapsed_time // 60)
seconds = int(elapsed_time % 60)
logger.info(f"Elapsed time: {minutes} minutes {seconds} seconds")
