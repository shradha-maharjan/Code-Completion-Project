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
import math
import time

start_time = time.time()


# In[2]:


ds_train = load_dataset("code_search_net", "java", split="train")
ds_test = load_dataset("code_search_net", "java", split="test")
ds_valid = load_dataset("code_search_net", "java", split="validation")
raw_datasets = DatasetDict(
    {
        "train": ds_train.shuffle().select(range(12000)), # "train": ds_train,  # .shuffle().select(range(50000)),
        "test": ds_test.shuffle().select(range(1500)),
        "valid": ds_valid.shuffle().select(range(1500)) # "valid": ds_valid,  # .shuffle().select(range(500))
    }
)
raw_datasets


# In[9]:


print(raw_datasets["test"][0]["whole_func_string"])


# In[10]:


for key in raw_datasets["train"][0]:
    print(f"{key.upper()}: {raw_datasets['train'][0][key][:1000]}")


# In[11]:


# use bert model checkpoint tokenizer
model_checkpoint = "distilbert-base-uncased"
# word piece tokenizer
tokenizer = AutoTokenizer.from_pretrained(model_checkpoint)

#define tokenize function to tokenize the dataset
def tokenize_function(data):
    result = tokenizer(data["whole_func_string"])
    return result

# batched is set to True to activate fast multithreading!
tokenize_dataset = raw_datasets.map(tokenize_function, batched = True, remove_columns = raw_datasets["train"].column_names)

print(f'[DBG] tokenized_dataset: {tokenize_dataset}')
print(f'[DBG] len(tokenizer): {len(tokenizer)}')
print(f'[DBG] tokenizer.bos_token_id: {tokenizer.bos_token_id}')
print(f'[DBG] tokenizer.eos_token_id: {tokenizer.eos_token_id}')


# In[12]:


def concat_chunk_dataset(data):
    chunk_size = 128
    # concatenate texts
    concatenated_sequences = {k: sum(data[k], []) for k in data.keys()}
    #compute length of concatenated texts
    total_concat_length = len(concatenated_sequences[list(data.keys())[0]])

    # drop the last chunk if is smaller than the chunk size
    total_length = (total_concat_length // chunk_size) * chunk_size

    # split the concatenated sentences into chunks using the total length
    result = {k: [t[i: i + chunk_size] for i in range(0, total_length, chunk_size)]
    for k, t in concatenated_sequences.items()}

    '''we create a new labels column which is a copy of the input_ids of the processed text data,the labels column serve as 
    ground truth for our masked language model to learn from. '''
    
    result["labels"] = result["input_ids"].copy()

    return result

processed_dataset = tokenize_dataset.map(concat_chunk_dataset, batched = True)


# In[13]:


from transformers import DataCollatorForLanguageModeling

''' Apply random masking once on the whole test data, then uses the default data collector to handle the test dataset in batches '''

data_collator = DataCollatorForLanguageModeling(tokenizer = tokenizer, mlm_probability = 0.15)

# Function to insert random mask
def insert_random_mask(batch):
    features = [dict(zip(batch, t)) for t in zip(*batch.values())]
    masked_inputs = data_collator(features)
    return {"masked_" + k: v.numpy() for k, v in masked_inputs.items()}

# Map insert_random_mask function to test dataset
eval_dataset = processed_dataset["test"].map(insert_random_mask,batched=True,remove_columns=processed_dataset["test"].column_names
)

# Rename columns
eval_dataset = eval_dataset.rename_columns({
    "masked_input_ids": "input_ids",
    "masked_attention_mask": "attention_mask",
    "masked_labels": "labels"
})


# In[14]:


import os

# Disable tokenizers parallelism
os.environ["TOKENIZERS_PARALLELISM"] = "false"


# In[16]:


def training_function():

    # set batch size to 32, a larger bacth size when using a more powerful gpu
    batch_size = 32

    train_dataloader = DataLoader(processed_dataset["train"], shuffle=True, batch_size=batch_size, collate_fn=data_collator)
    eval_dataloader = DataLoader(processed_dataset["test"], batch_size=batch_size, collate_fn=default_data_collator)

    # initialize pretrained bert model
    model = AutoModelForMaskedLM.from_pretrained(model_checkpoint)

    # set the optimizer
    optimizer = AdamW(model.parameters(), lr=5e-5)

    # initialize accelerator for training
    accelerator = Accelerator()
    model, optimizer, train_dataloader, eval_dataloader = accelerator.prepare(model, optimizer, train_dataloader, eval_dataloader)

    # set the number of epochs which is set to 30
    num_train_epochs = 5
    num_update_steps_per_epoch = len(train_dataloader)
    num_training_steps = num_train_epochs * num_update_steps_per_epoch

    # define the learning rate scheduler for training
    lr_scheduler = get_scheduler("linear",optimizer=optimizer,num_warmup_steps=0,num_training_steps=num_training_steps)


    progress_bar = tqdm(range(num_training_steps))

    # directory to save the models
    output_dir = "MLP_TrainedModels"

    for epoch in range(num_train_epochs):
        # Training
        model.train()
        for batch in train_dataloader:
            outputs = model(**batch)
            loss = outputs.loss
            accelerator.backward(loss)
            optimizer.step()
            lr_scheduler.step()
            optimizer.zero_grad()
            progress_bar.update(1)

        # Evaluation
        model.eval()
        losses = []
        for step, batch in enumerate(eval_dataloader):
            with torch.no_grad():
                outputs = model(**batch)
            loss = outputs.loss
            losses.append(accelerator.gather(loss.repeat(batch_size)))

        losses = torch.cat(losses)
        losses = losses[: len(eval_dataset)]
        print(losses)

        # perplexity metric used for mask language model training
        try:
            perplexity = math.exp(torch.mean(losses))
        except OverflowError:
            perplexity = float("inf")
        print(f">>> Epoch {epoch}: Perplexity: {perplexity}")

        # Calculate probabilities
        losses = losses.cpu().numpy()  # Convert losses to NumPy array
        probabilities = torch.nn.functional.softmax(torch.tensor(losses), dim=0)

        # Calculate entropy
        entropy = -torch.sum(probabilities * torch.log(probabilities))
        print(f">>> Epoch {epoch}: Entropy: {entropy}")

        # Save model
        accelerator.wait_for_everyone()
        unwrapped_model = accelerator.unwrap_model(model)
        unwrapped_model.save_pretrained(output_dir, save_function=accelerator.save)
        if accelerator.is_main_process:
            tokenizer.save_pretrained(output_dir)

notebook_launcher(training_function, num_processes= 2)


# In[17]:


model = "MLP_TrainedModels"

pred_model = pipeline("fill-mask", model = "MLP_TrainedModels")

text = "public FileWatcher register(final Path path, final Class<? extends FileEventHandler> handler) {\n    return [MASK](path, handler, EMPTY);\n  }"

preds = pred_model(text)
print(preds)

end_time= time.time()

Total_time = end_time - start_time

print(Total_time)