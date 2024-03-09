#!/usr/bin/env python
# coding: utf-8

# # Training a causal language model from scratch (PyTorch)

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
from evaluate import load
import evaluate

import time

start_time= time.time()

# In[4]:


# Iterator for Training
def batch_iterator(batch_size=10):
    for _ in tqdm(range(0, 32768, batch_size)): #For each iteration, it uses next(iter_dataset) to get the next item from the dataset and then extracts the "content" field from it.
        yield [next(iter_dataset)["whole_func_string"] for _ in range(batch_size)]


# In[5]:


# Base tokenizer
#from arguments import TokenizerTrainingArguments
from transformers import AutoTokenizer
from transformers.models.gpt2.tokenization_gpt2 import bytes_to_unicode

# Base tokenizer
tokenizer = AutoTokenizer.from_pretrained("gpt2") #loads the pre-trained tokenizer associated with the GPT-2 model.
base_vocab = list(bytes_to_unicode().values()) #returns a mapping from bytes to Unicode characters.
#This list is assigned to base_vocab, presumably containing the base vocabulary used by the tokenizer.


# In[6]:


from datasets import load_dataset
from tqdm import tqdm

# Load dataset
dataset = load_dataset("code_search_net", "java",split="train", streaming=True) #loads a dataset named "codeparrot-clean" with the split "train"
iter_dataset = iter(dataset)

tokenizer_name = "codesearchnet"

#Training and saving
new_tokenizer = tokenizer.train_new_from_iterator( #passed as the iterator providing data for training.
    batch_iterator(), vocab_size=50000, initial_alphabet=base_vocab #batch_iterator(), vocab_size=200_000, initial_alphabet=base_vocab
)
new_tokenizer.save_pretrained(tokenizer_name, push_to_hub=True)


# In[7]:


from transformers import AutoConfig, AutoModelForCausalLM, HfArgumentParser

# Load codeparrot tokenizer trained for Python code tokenization
tokenizer = AutoTokenizer.from_pretrained(tokenizer_name) 
print(len(tokenizer))

# Config: "scale_attn_by_layer_idx" and "reorder_and_upcast_attn" are Mistral stability tweaks
config_kwargs = { #is a dictionary containing configuration arguments for the model.
    "vocab_size": len(tokenizer),
    "scale_attn_by_inverse_layer_idx": True,
    "reorder_and_upcast_attn": True,
}

# Load model config (GPT-2 large in this case)
config = AutoConfig.from_pretrained('gpt2-large', **config_kwargs) #loads the configuration for the GPT-2 large model and applying the configuration arguments specified in config_kwargs.

# Initialize new model with config
model = AutoModelForCausalLM.from_config(config) # initializes a new model for causal language modeling using the configuration obtained earlier.


# In[8]:


ds_train = load_dataset("code_search_net", "java", split="train")
ds_test = load_dataset("code_search_net", "java", split="test")
ds_valid = load_dataset("code_search_net", "java", split="validation")
raw_datasets = DatasetDict(
    {
        "train": ds_train.shuffle().select(range(20000)), # "train": ds_train,  # .shuffle().select(range(50000)),
        "test": ds_test.shuffle().select(range(2500)),
        "valid": ds_valid.shuffle().select(range(2500)) # "valid": ds_valid,  # .shuffle().select(range(500))
    }
)
context_length = 128
raw_datasets


# In[9]:


tokenizer


# In[10]:


len(tokenizer)


# In[11]:


outputs = tokenizer(
    raw_datasets["train"][:2]["whole_func_string"],
    truncation=True,
    max_length=context_length,
    return_overflowing_tokens=True,
    return_length=True,
)

print(f"Input IDs length: {len(outputs['input_ids'])}")
print(f"Input chunk lengths: {(outputs['length'])}")
print(f"Chunk mapping: {outputs['overflow_to_sample_mapping']}")


# In[12]:


def tokenize(element):
    outputs = tokenizer(
        element["whole_func_string"],
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


# In[14]:


config = AutoConfig.from_pretrained(
    "gpt2",
    vocab_size=len(tokenizer),
    n_ctx=context_length,
    bos_token_id=tokenizer.bos_token_id,
    eos_token_id=tokenizer.eos_token_id,
)


# In[15]:


model = GPT2LMHeadModel(config)
model_size = sum(t.numel() for t in model.parameters())  # num of elements
print(f"GPT-2 size: {model_size/1000**2:.1f}M parameters")


# In[16]:


model = GPT2LMHeadModel(config)


# In[18]:


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


# In[19]:


from torch.nn import CrossEntropyLoss
import torch


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


# In[20]:


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

lr_scheduler_type = "cosine"
learning_rate = 2e-4

optimizer = AdamW(get_grouped_params(model), lr=learning_rate)
lr_scheduler = get_scheduler(name=lr_scheduler_type, optimizer=optimizer,
                             num_warmup_steps=750,
                             num_training_steps=50000)


# In[21]:


import os

# Disable tokenizers parallelism
os.environ["TOKENIZERS_PARALLELISM"] = "false"


# In[22]:


def training_function():
    model = GPT2LMHeadModel(config)

    output_dir = "CLM_TrainedModels"
    
    optimizer = AdamW(get_grouped_params(model), lr=5e-4)

    accelerator = Accelerator(mixed_precision="fp16")

    batch_sz = 2

    tokenized_datasets.set_format("torch")
    train_dataloader = DataLoader(tokenized_datasets["train"], batch_size=batch_sz, shuffle=True)
    eval_dataloader = DataLoader(tokenized_datasets["valid"], batch_size=batch_sz)

    model, optimizer, train_dataloader, eval_dataloader = accelerator.prepare(
        model, optimizer, train_dataloader, eval_dataloader
    )

    num_train_epochs = 1
    num_update_steps_per_epoch = len(train_dataloader)
    num_training_steps = num_train_epochs * num_update_steps_per_epoch

    lr_scheduler = get_scheduler(
        name="linear",
        optimizer=optimizer,
        num_warmup_steps=1_000,
        num_training_steps=num_training_steps,

    )

    def evaluate():
        model.eval()
        losses = []
        total_correct = 0
        total_samples = 0
        total_entropy = 0

        for step, batch in enumerate(eval_dataloader):
            with torch.no_grad():
                outputs = model(batch["input_ids"], labels=batch["input_ids"])
            loss = outputs.loss.repeat(batch_sz) # <===== Added.
            losses.append(accelerator.gather(loss))
            
            # Calculate accuracy
            logits = outputs.logits
            predictions = torch.argmax(logits, dim=-1).to('cuda')
            labels = batch["input_ids"].to('cuda')
            correct = (predictions == labels).sum().item()
            total_correct += correct
            total_samples += labels.numel()

            softmax_probs = torch.nn.functional.softmax(logits, dim=-1)
            entropy = -torch.sum(softmax_probs * torch.log(softmax_probs), dim=-1)
            total_entropy += entropy.sum().item()

        loss = torch.mean(torch.cat(losses))
        accuracy = total_correct/ total_samples
        entropy = total_entropy / total_samples

        try:
            perplexity = torch.exp(torch.tensor(loss))
        except OverflowError:
            perplexity = float("inf")

        return loss.item(), perplexity.item(), accuracy, entropy
        
    gradient_accumulation_steps = 1
    eval_steps = 20 # 5_000

    model.train()
    completed_steps = 0
    for epoch in range(num_train_epochs):
        for step, batch in tqdm(
            enumerate(train_dataloader, start=1), total=num_training_steps
        ):
            logits = model(batch["input_ids"]).logits
            loss = keytoken_weighted_loss(batch["input_ids"], logits, keytoken_ids)
            if step % 20 == 0:
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
                eval_loss, perplexity, accuracy, entropy = evaluate()
                accelerator.print({"loss/eval": eval_loss, "perplexity": perplexity, "accuracy": accuracy, "entropy": entropy})
                model.train()
                accelerator.wait_for_everyone()
                unwrapped_model = accelerator.unwrap_model(model)
                unwrapped_model.save_pretrained(output_dir, save_function=accelerator.save)
                if accelerator.is_main_process:
                    tokenizer.save_pretrained(output_dir)
                accelerator.print(f'epoch {epoch}: accuracy - {100 * accuracy:.2f}%')
notebook_launcher(training_function, num_processes= 2)


# In[23]:


from transformers import pipeline

model_checkpoint = "shradha01/codeparrot-ds-accelerate"

code_example = "public static Quaterniond lerp(Quaterniond a,"
text_generation = pipeline('text-generation', model=model_checkpoint, tokenizer=tokenizer)

outputs = text_generation(code_example)
print(outputs)


# In[26]:


model_checkpoint = "CLM_TrainedModels"
text_generation = pipeline('text-generation', model=model_checkpoint, tokenizer=tokenizer)

code_example = "public Evaluation create(SimpleNode node, Object source)\n    {\n        return"
ground_truth = ["public Evaluation create(SimpleNode node, Object source)\n    {\n        return create(node, source, false);\n    }"]

outputs = text_generation(code_example)
print(outputs)

generated_text = outputs[0]['generated_text']
generated_text_list = [generated_text]
print (generated_text_list)
print (ground_truth)
bleu = evaluate.load("bleu")
results = bleu.compute(predictions=generated_text_list, references=ground_truth)
print(results)

rouge = evaluate.load('rouge')

results = rouge.compute(predictions=generated_text_list, references=ground_truth)
print(results)

# In[ ]:

end_time= time.time()


elapsed_time = end_time - start_time

# Convert elapsed time to minutes and seconds
minutes = int(elapsed_time // 60)
seconds = int(elapsed_time % 60)

print("Elapsed time:", minutes, "minutes", seconds, "seconds")



