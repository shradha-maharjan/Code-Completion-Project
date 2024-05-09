#!/usr/bin/env python
# coding: utf-8

# # Training a causal language model from scratch (PyTorch)

# In[3]:


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
import evaluate
from evaluate import load
import time

start_time= time.time()

# In[4]:


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
tokenizer = AutoTokenizer.from_pretrained("huggingface-course/code-search-net-tokenizer")
raw_datasets


# In[4]:


# print(raw_datasets["valid"][0]["whole_func_string"])


# # In[5]:


# valid_dataset = raw_datasets["valid"]

# for i in range(20):
#     print(f"Index {i}: {valid_dataset['whole_func_string'][i]}")


# In[7]:


for key in raw_datasets["train"][0]:
    print(f"{key.upper()}: {raw_datasets['train'][0][key][:1000]}")


# In[8]:


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


# In[9]:


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


# In[10]:


config = AutoConfig.from_pretrained(
    "gpt2",
    vocab_size=len(tokenizer),
    n_ctx=context_length,
    bos_token_id=tokenizer.bos_token_id,
    eos_token_id=tokenizer.eos_token_id,
)


# In[11]:


model = GPT2LMHeadModel(config)
model_size = sum(t.numel() for t in model.parameters())  # num of elements
print(f"GPT-2 size: {model_size/1000**2:.1f}M parameters")


# In[12]:


tokenizer.pad_token = tokenizer.eos_token
data_collator = DataCollatorForLanguageModeling(tokenizer, mlm=False)


# In[13]:


out = data_collator([tokenized_datasets["train"][i] for i in range(5)])
for key in out:
    print(f"{key} shape: {out[key].shape}")


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


# In[16]:


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


# In[17]:


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


model_name = "codeparrot-ds-accelerate"
repo_name = get_full_repo_name(model_name)
repo_name


# In[24]:


output_dir = "codeparrot-ds-accelerate"
repo = Repository(output_dir, clone_from=repo_name)


# In[ ]:


import os

# Disable tokenizers parallelism
os.environ["TOKENIZERS_PARALLELISM"] = "false"


# In[26]:


def training_function():
    model = GPT2LMHeadModel(config)

    optimizer = AdamW(get_grouped_params(model), lr=5e-4)

    accelerator = Accelerator(mixed_precision="fp16")

    batch_sz = 32

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

            #Calculate entropy
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
                    repo.push_to_hub(
                        commit_message=f"Training in progress step {step}", blocking=False
                    )
                accelerator.print(f'epoch {epoch}: accuracy - {100 * accuracy:.2f}%')
notebook_launcher(training_function, num_processes= 2)


# In[ ]:


model_checkpoint = "shradha01/codeparrot-ds-accelerate"
text_generation = pipeline('text-generation', model=model_checkpoint, tokenizer=tokenizer)

code_example = "public Evaluation create(SimpleNode node, Object source)\n    {\n        return"
ground_truth = ["public Evaluation create(SimpleNode node, Object source)\n    {\n        return create(node, source, false);\n    }"]

outputs = text_generation(code_example)
print(outputs)
print(ground_truth)
generated_text = outputs[0]['generated_text']
print(generated_text)
generated_text_list = [generated_text]
bleu = evaluate.load("bleu")
results = bleu.compute(predictions=generated_text_list, references=ground_truth)
print(results)

rouge = evaluate.load('rouge')

results = rouge.compute(predictions=generated_text_list, references=ground_truth)
print(results)

end_time= time.time()

elapsed_time = end_time - start_time

# Convert elapsed time to minutes and seconds
minutes = int(elapsed_time // 60)
seconds = int(elapsed_time % 60)

print("Elapsed time:", minutes, "minutes", seconds, "seconds")