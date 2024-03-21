#!/usr/bin/env python
# coding: utf-8

# # Training a causal language model from scratch (PyTorch)

# Install the Transformers, Datasets, and Evaluate libraries to run this notebook.

# In[10]:





# import subprocess

# Install required packages
# subprocess.run(['pip', 'install', 'datasets', 'evaluate', 'transformers[sentencepiece]'])
# subprocess.run(['pip', 'install', 'accelerate'])
# Uncomment the following line to install TPU dependencies
# subprocess.run(['pip', 'install', 'cloud-tpu-client==0.10', 'torch==1.9.0', 'https://storage.googleapis.com/tpu-pytorch/wheels/torch_xla-1.9-cp37-cp37m-linux_x86_64.whl'])

# Install git-lfs
# subprocess.run(['apt', 'install', 'git-lfs'])

# Set up git config
# subprocess.run(['git', 'config', '--global', 'user.email', '"rshakywar@unomaha.edu"'])
# subprocess.run(['git', 'config', '--global', 'user.name', '"Rajul Shakywar"'])




# get_ipython().system('pip install datasets evaluate transformers[sentencepiece]')
# get_ipython().system('pip install accelerate')
# To run the training on TPU, you will need to uncomment the following line:
# !pip install cloud-tpu-client==0.10 torch==1.9.0 https://storage.googleapis.com/tpu-pytorch/wheels/torch_xla-1.9-cp37-cp37m-linux_x86_64.whl
# get_ipython().system('apt install git-lfs')


# You will need to setup git, adapt your email and name in the following cell.

# In[12]:


# get_ipython().system('git config --global user.email "rshakywar@unomaha.edu"')
# get_ipython().system('git config --global user.name "Rajul Shakywar"')


# You will also need to be logged in to the Hugging Face Hub. Execute the following and enter your credentials.

# In[13]:

import os

# Define the shell commands
commands = [
    "apt update",
    "apt install -y python3-pip",
    "pip3 install tqdm",
    "apt install -y git-lfs",
    "python -m pip install scikit-learn transformers datasets sentencepiece sacremoses accelerate",
    "pip3 install --upgrade huggingface_hub",
    "pip3 install 'huggingface_hub[cli,torch]'",
    "pip3 install ipywidgets",
    "pip3 install numpy pandas matplotlib",
    "pip3 install ipykernel",
    "echo 'Installation completed'",
]

# Run each command
for cmd in commands:
    os.system(cmd)



from collections import defaultdict
from tqdm import tqdm
from datasets import Dataset, load_dataset, DatasetDict
from transformers import AutoTokenizer, GPT2LMHeadModel, AutoConfig, DataCollatorForLanguageModeling
# from transformers import Trainer, TrainingArguments
import torch
# from transformers import pipeline
from torch.nn import CrossEntropyLoss
from torch.utils.data.dataloader import DataLoader
from torch.optim import AdamW
from accelerate import Accelerator
from transformers import get_scheduler
from huggingface_hub import Repository, login, get_full_repo_name


# In[ ]:

# from huggingface_hub import login
access_token_write = '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'
login(token = access_token_write)


# In[ ]:


def any_keyword_in_string(string, keywords):
    for keyword in keywords:
        if keyword in string:
            return True
    return False


# In[ ]:


filters = ["pandas", "sklearn", "matplotlib", "seaborn"]
example_1 = "import numpy as np"
example_2 = "import pandas as pd"

print(
    any_keyword_in_string(example_1, filters), any_keyword_in_string(example_2, filters)
)


# In[ ]:


def filter_streaming_dataset(dataset, filters):
    filtered_dict = defaultdict(list)
    total = 0
    for sample in tqdm(iter(dataset)):
        total += 1
        if any_keyword_in_string(sample["content"], filters):
            for k, v in sample.items():
                filtered_dict[k].append(v)
    print(f"{len(filtered_dict['content'])/total:.2%} of data after filtering.")
    return Dataset.from_dict(filtered_dict)


# In[ ]:


# This cell will take a very long time to execute, so you should skip it and go to
# the next one!
"""
from datasets import load_dataset

split = "train"  # "valid"
filters = ["pandas", "sklearn", "matplotlib", "seaborn"]

data = load_dataset(f"transformersbook/codeparrot-{split}", split=split, streaming=True)
filtered_data = filter_streaming_dataset(data, filters)
"""


# In[ ]:


ds_train = load_dataset("huggingface-course/codeparrot-ds-train", split="train")
ds_valid = load_dataset("huggingface-course/codeparrot-ds-valid", split="validation")
raw_datasets = DatasetDict(
    {
        "train": ds_train.shuffle().select(range(5_000)), # "train": ds_train,  # .shuffle().select(range(50000)),
        "valid": ds_valid.shuffle().select(range(50)) # "valid": ds_valid,  # .shuffle().select(range(500))
    }
)
context_length = 128
tokenizer = AutoTokenizer.from_pretrained("huggingface-course/code-search-net-tokenizer")
raw_datasets


# In[ ]:


for key in raw_datasets["train"][0]:
    print(f"{key.upper()}: {raw_datasets['train'][0][key][:1000]}")


# In[ ]:


outputs = tokenizer(
    raw_datasets["train"][:2]["content"],
    truncation=True,
    max_length=context_length,
    return_overflowing_tokens=True,
    return_length=True,
)

print(f"Input IDs length: {len(outputs['input_ids'])}")
print(f"Input chunk lengths: {(outputs['length'])}")
print(f"Chunk mapping: {outputs['overflow_to_sample_mapping']}")


# In[ ]:


def tokenize(element):
    outputs = tokenizer(
        element["content"],
        truncation=True,
        max_length=context_length,
        return_overflowing_tokens=True,
        return_length=True,
    )
    input_batch = []
    for length, input_ids in zip(outputs["length"], outputs["input_ids"]):
        if length == context_length:
            input_batch.append(input_ids)
    return {"input_ids": input_batch}


tokenized_datasets = raw_datasets.map(
    tokenize, batched=True, remove_columns=raw_datasets["train"].column_names
)
print(f'[DBG] tokenized_datasets: {tokenized_datasets}')
print(f'[DBG] len(tokenizer): {len(tokenizer)}')
print(f'[DBG] tokenizer.bos_token_id: {tokenizer.bos_token_id}')
print(f'[DBG] tokenizer.eos_token_id: {tokenizer.eos_token_id}')


# In[ ]:


config = AutoConfig.from_pretrained(
    "gpt2",
    vocab_size=len(tokenizer),
    n_ctx=context_length,
    bos_token_id=tokenizer.bos_token_id,
    eos_token_id=tokenizer.eos_token_id,
)


# In[ ]:


model = GPT2LMHeadModel(config)
model_size = sum(t.numel() for t in model.parameters())  # num of elements
print(f"GPT-2 size: {model_size/1000**2:.1f}M parameters")


# In[ ]:


tokenizer.pad_token = tokenizer.eos_token
data_collator = DataCollatorForLanguageModeling(tokenizer, mlm=False)


# In[ ]:


out = data_collator([tokenized_datasets["train"][i] for i in range(5)])
for key in out:
    print(f"{key} shape: {out[key].shape}")


# In[ ]:


device = torch.device("cuda") if torch.cuda.is_available() else torch.device("cpu")
n_gpu = torch.cuda.device_count()
device, n_gpu


# In[ ]:


keytoken_ids = []
for keyword in [
    "plt",
    "pd",
    "sk",
    "fit",
    "predict",
    " plt",
    " pd",
    " sk",
    " fit",
    " predict",
    "testtest",
]:
    ids = tokenizer([keyword]).input_ids[0]
    idss = tokenizer([keyword]).input_ids
    idsss = tokenizer(keyword)
    if len(ids) == 1:
        keytoken_ids.append(ids[0])
    else:
        print(f"Keyword has not single token: {keyword} {len(ids)} {ids} {tokenizer.tokenize(keyword)}")
keytoken_ids


# In[ ]:


def keytoken_weighted_loss(inputs, logits, keytoken_ids, alpha=1.0):
    # Shift so that tokens < n predict n
    shift_labels = inputs[..., 1:].contiguous()
    shift_logits = logits[..., :-1, :].contiguous()
    # Calculate per-token loss
    loss_fct = CrossEntropyLoss(reduce=False)
    loss = loss_fct(shift_logits.view(-1, shift_logits.size(-1)), shift_labels.view(-1))
    # Resize and average loss per sample
    loss_per_sample = loss.view(shift_logits.size(0), shift_logits.size(1)).mean(axis=1)
    # Calculate and scale weighting
    weights = torch.stack([(inputs == kt).float() for kt in keytoken_ids]).sum(
        axis=[0, 2]
    )
    weights = alpha * (1.0 + weights)
    # Calculate weighted average
    weighted_loss = (loss_per_sample * weights).mean()
    return weighted_loss


# In[ ]:


batch_sz = 32

tokenized_datasets.set_format("torch")
train_dataloader = DataLoader(tokenized_datasets["train"], batch_size=batch_sz, shuffle=True)
eval_dataloader = DataLoader(tokenized_datasets["valid"], batch_size=batch_sz)


# In[ ]:


weight_decay = 0.1

def get_grouped_params(model, no_decay=["bias", "LayerNorm.weight"]):
    params_with_wd, params_without_wd = [], []
    for n, p in model.named_parameters():
        if any(nd in n for nd in no_decay):
            params_without_wd.append(p)
        else:
            params_with_wd.append(p)
    return [
        {"params": params_with_wd, "weight_decay": weight_decay},
        {"params": params_without_wd, "weight_decay": 0.0},
    ]


# In[ ]:


# def evaluate():
#     model.eval()
#     losses = []
#     for step, batch in enumerate(eval_dataloader):
#         with torch.no_grad():
#             outputs = model(batch["input_ids"], labels=batch["input_ids"])

#         losses.append(accelerator.gather(outputs.loss))
#     # loss = torch.mean(torch.cat(losses))
#     loss = torch.mean(torch.stack(losses))
#     try:
#         perplexity = torch.exp(loss)
#     except OverflowError:
#         perplexity = float("inf")
#     return loss.item(), perplexity.item()
# 
# Updated by "https://huggingface.co/blog/codeparrot"
def evaluate():
    model.eval()
    losses = []
    for step, batch in enumerate(eval_dataloader):
        with torch.no_grad():
            outputs = model(batch["input_ids"], labels=batch["input_ids"])
        loss = outputs.loss.repeat(batch_sz) # <===== Added.
        losses.append(accelerator.gather(loss))
    loss = torch.mean(torch.cat(losses))
    # loss = torch.mean(torch.stack(losses))
    try:
        perplexity = torch.exp(loss)
    except OverflowError:
        perplexity = float("inf")
    return loss.item(), perplexity.item()


# In[ ]:


model = GPT2LMHeadModel(config)
model.to(device)


# In[ ]:


optimizer = AdamW(get_grouped_params(model), lr=5e-4)


# In[ ]:


# accelerator = Accelerator(fp16=True)
accelerator = Accelerator(mixed_precision="fp16")

model, optimizer, train_dataloader, eval_dataloader = accelerator.prepare(
    model, optimizer, train_dataloader, eval_dataloader
)


# In[ ]:


num_train_epochs = 1
num_update_steps_per_epoch = len(train_dataloader)
num_training_steps = num_train_epochs * num_update_steps_per_epoch

lr_scheduler = get_scheduler(
    name="linear",
    optimizer=optimizer,
    num_warmup_steps=1_000,
    num_training_steps=num_training_steps,
)


# In[ ]:


model_name = "codeparrot-ds-accelerate"
repo_name = get_full_repo_name(model_name)

print("------------Repo Name---------")
print(repo_name)


# In[ ]:


output_dir = "codeparrot-ds-accelerate"
repo = Repository(output_dir, clone_from=repo_name)

print("------------Repo---------")
print(repo)


# In[ ]:


evaluate()


# In[ ]:


"""
Version 2 without weight
"""
from tqdm.notebook import tqdm

gradient_accumulation_steps = 8
eval_steps = 100 # 5_000

model.train()
completed_steps = 0
for epoch in range(num_train_epochs):
    for step, batch in tqdm(
        enumerate(train_dataloader, start=1), total=num_training_steps
    ):
        batch["input_ids"].to(device)
        # Consider "weight"
        # logits = model(batch["input_ids"]).logits
        # loss = keytoken_weighted_loss(batch["input_ids"], logits, keytoken_ids)
        # ########################################################################
        # Do not consider "weight"
        loss = model(batch["input_ids"], labels=batch["input_ids"]).loss
        n_gpu = torch.cuda.device_count()
        samples_per_step = batch_sz * n_gpu
        if step % 100 == 0:
            accelerator.print(
                {
                    "lr": lr_scheduler.get_last_lr()[0],
                    "samples": step * samples_per_step,
                    "steps": completed_steps,
                    "loss/train": loss.item() * gradient_accumulation_steps,
                }
            )
        loss = loss / gradient_accumulation_steps
        accelerator.backward(loss)
        if step % gradient_accumulation_steps == 0:
            accelerator.clip_grad_norm_(model.parameters(), 1.0)
            optimizer.step()
            lr_scheduler.step()
            optimizer.zero_grad()
            completed_steps += 1
        if (step % (eval_steps * gradient_accumulation_steps)) == 0:
            eval_loss, perplexity = evaluate()
            accelerator.print({"loss/eval": eval_loss, "perplexity": perplexity})
            model.train()
            accelerator.wait_for_everyone()
            unwrapped_model = accelerator.unwrap_model(model)
            unwrapped_model.save_pretrained(output_dir, save_function=accelerator.save)
            if accelerator.is_main_process:
                tokenizer.save_pretrained(output_dir)
                repo.push_to_hub(
                    commit_message=f"Training in progress step {step}", blocking=False
                )


# In[ ]:


from tqdm.notebook import tqdm

gradient_accumulation_steps = 8
eval_steps = 100 # 5_000

model.train()
completed_steps = 0
for epoch in range(num_train_epochs):
    for step, batch in tqdm(
        enumerate(train_dataloader, start=1), total=num_training_steps
    ):
        logits = model(batch["input_ids"]).logits
        loss = keytoken_weighted_loss(batch["input_ids"], logits, keytoken_ids)
        if step % 100 == 0:
            accelerator.print(
                {
                    # "lr": get_lr(),
                    # "samples": step * samples_per_step,
                    "steps": completed_steps,
                    "loss/train": loss.item() * gradient_accumulation_steps,
                }
            )
        loss = loss / gradient_accumulation_steps
        accelerator.backward(loss)
        if step % gradient_accumulation_steps == 0:
            accelerator.clip_grad_norm_(model.parameters(), 1.0)
            optimizer.step()
            lr_scheduler.step()
            optimizer.zero_grad()
            completed_steps += 1
        if (step % (eval_steps * gradient_accumulation_steps)) == 0:
            eval_loss, perplexity = evaluate()
            accelerator.print({"loss/eval": eval_loss, "perplexity": perplexity})
            model.train()
            accelerator.wait_for_everyone()
            unwrapped_model = accelerator.unwrap_model(model)
            unwrapped_model.save_pretrained(output_dir, save_function=accelerator.save)
            if accelerator.is_main_process:
                tokenizer.save_pretrained(output_dir)
                repo.push_to_hub(
                    commit_message=f"Training in progress step {step}", blocking=False
                )

