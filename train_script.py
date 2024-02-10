from datasets import load_dataset, DatasetDict
from transformers import AutoTokenizer
from transformers import AutoTokenizer, GPT2LMHeadModel, AutoConfig
from transformers import DataCollatorForLanguageModeling
from transformers import Trainer, TrainingArguments


def create_dataset():
    ds_train = load_dataset("huggingface-course/codeparrot-ds-train", split="train")
    ds_valid = load_dataset("huggingface-course/codeparrot-ds-valid", split="validation")

    raw_datasets = DatasetDict(
        {
            "train": ds_train.shuffle().select(range(10000)), # 50000
            "valid": ds_valid.shuffle().select(range(100)) # 500
        }
    )

    return raw_datasets


def create_tokenizer(raw_datasets):
    context_length = 128
    tokenizer = AutoTokenizer.from_pretrained("huggingface-course/code-search-net-tokenizer")

    # outputs = tokenizer(
    #     raw_datasets["train"][:2]["content"],
    #     truncation=True,
    #     max_length=context_length,
    #     return_overflowing_tokens=True,
    #     return_length=True,
    # )

    config = AutoConfig.from_pretrained(
        "gpt2",
        vocab_size=len(tokenizer),
        n_ctx=context_length,
        bos_token_id=tokenizer.bos_token_id,
        eos_token_id=tokenizer.eos_token_id,
    )

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

    return config, tokenizer, tokenized_datasets


def train(config, tokenizer, tokenized_datasets):
    tokenizer.pad_token = tokenizer.eos_token
    data_collator = DataCollatorForLanguageModeling(tokenizer, mlm=False)

    args = TrainingArguments(
        output_dir="codeparrot-ds",
        per_device_train_batch_size=32,
        per_device_eval_batch_size=32,
        evaluation_strategy="steps",
        eval_steps=2_000, # 5_000,
        logging_steps=2_000, # 5_000,
        gradient_accumulation_steps=8,
        num_train_epochs=1,
        weight_decay=0.1,
        warmup_steps=1_000, # 1_000,
        lr_scheduler_type="cosine",
        learning_rate=5e-4,
        save_steps=2_000, # 5_000,
        fp16=True,
        push_to_hub=True,
    )

    model = GPT2LMHeadModel(config)
    # model_size = sum(t.numel() for t in model.parameters())

    trainer = Trainer(
        model=model,
        tokenizer=tokenizer,
        args=args,
        data_collator=data_collator,
        train_dataset=tokenized_datasets["train"],
        eval_dataset=tokenized_datasets["valid"],
        # use_wandb=False
    )

    trainer.train()


def main():
    raw_datasets = create_dataset()
    config, tokenizer, tokenized_datasets = create_tokenizer(raw_datasets)
    train(config, tokenizer, tokenized_datasets)


if __name__ == "__main__":
    main()