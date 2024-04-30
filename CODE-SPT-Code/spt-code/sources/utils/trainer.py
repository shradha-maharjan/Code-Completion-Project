
from torch.utils.data.dataloader import DataLoader
from torch.utils.data.dataset import Dataset
from transformers import Seq2SeqTrainer, Trainer

import argparse
from typing import Optional

from data.data_collator import collate_fn

#CodeTrainer and CodeCLSTrainer inherit from Seq2SeqTrainer and Trainer respectively, which are part of the Hugging Face transformers library 
class CodeTrainer(Seq2SeqTrainer):

#Initializes the trainer, accepting several arguments including the main arguments, code vocabulary, abstract syntax tree (AST) vocabulary, natural language (NL) vocabulary, and the task.
    def __init__(self, main_args: argparse.Namespace, code_vocab, ast_vocab, nl_vocab, task, **kwargs):
        super(CodeTrainer, self).__init__(**kwargs)
        self.main_args = main_args
        self.code_vocab = code_vocab
        self.ast_vocab = ast_vocab
        self.nl_vocab = nl_vocab
        self.task = task

#Returns the training dataloader. It shuffles the data and uses a custom collate function collate_fn for processing the batches.
    def get_train_dataloader(self) -> DataLoader:
        """
        Returns the training :class:`~torch.utils.data.DataLoader`.

        Will use no sampler if :obj:`self.train_dataset` does not implement :obj:`__len__`, a random sampler (adapted
        to distributed training if necessary) otherwise.

        Subclass and override this method if you want to inject some custom behavior.
        """
        # print("Getting training dataloader")
        # print("Batch Size:", self.main_args.batch_size)
        # print("Task:", self.task)
        # print("Code Vocabulary:", self.code_vocab)
        # print("AST Vocabulary:", self.ast_vocab)
        # print("NL Vocabulary:", self.nl_vocab)
        return DataLoader(dataset=self.train_dataset,
                          batch_size=self.main_args.batch_size,
                          shuffle=True,
                          collate_fn=lambda batch: collate_fn(batch,
                                                              args=self.main_args,
                                                              task=self.task,
                                                              code_vocab=self.code_vocab,
                                                              nl_vocab=self.nl_vocab,
                                                              ast_vocab=self.ast_vocab))

#Returns the evaluation dataloader. It also uses the custom collate function.
    def get_eval_dataloader(self, eval_dataset: Optional[Dataset] = None) -> DataLoader:
        # print("Getting evaluation dataloader")
        # print("Batch Size:", self.main_args.eval_batch_size)
        # print("Task:", self.task)
        # print("Code Vocabulary:", self.code_vocab)
        # print("AST Vocabulary:", self.ast_vocab)
        # print("NL Vocabulary:", self.nl_vocab)
        if eval_dataset:
            self.eval_dataset = eval_dataset
        return DataLoader(dataset=self.eval_dataset,
                          batch_size=self.main_args.eval_batch_size,
                          collate_fn=lambda batch: collate_fn(batch,
                                                              args=self.main_args,
                                                              task=self.task,
                                                              code_vocab=self.code_vocab,
                                                              nl_vocab=self.nl_vocab,
                                                              ast_vocab=self.ast_vocab))

#Returns the test dataloader. Similar to the evaluation dataloader, it uses the custom collate function.
    def get_test_dataloader(self, test_dataset: Dataset) -> DataLoader:
        # print("Getting test dataloader")
        # print("Batch Size:", self.main_args.eval_batch_size)
        # print("Task:", self.task)
        # print("Code Vocabulary:", self.code_vocab)
        # print("AST Vocabulary:", self.ast_vocab)
        # print("NL Vocabulary:", self.nl_vocab)
        return DataLoader(dataset=test_dataset,
                          batch_size=self.main_args.eval_batch_size,
                          collate_fn=lambda batch: collate_fn(batch,
                                                              args=self.main_args,
                                                              task=self.task,
                                                              code_vocab=self.code_vocab,
                                                              nl_vocab=self.nl_vocab,
                                                              ast_vocab=self.ast_vocab))
#Allows setting the task for the trainer
    def set_task(self, task):
        self.task = task

#Inherits from Trainer, which is a general-purpose trainer.Shares similar methods and functionalities with CodeTrainer.Primarily used for training models for code classification tasks.
class CodeCLSTrainer(Trainer):

#Initializes the trainer.
    def __init__(self, main_args: argparse.Namespace, code_vocab, ast_vocab, nl_vocab, task, **kwargs):
        super(CodeCLSTrainer, self).__init__(**kwargs)
        self.main_args = main_args
        self.code_vocab = code_vocab
        self.ast_vocab = ast_vocab
        self.nl_vocab = nl_vocab
        self.task = task

#Returns the training dataloader.
    def get_train_dataloader(self) -> DataLoader:
        """
        Returns the training :class:`~torch.utils.data.DataLoader`.

        Will use no sampler if :obj:`self.train_dataset` does not implement :obj:`__len__`, a random sampler (adapted
        to distributed training if necessary) otherwise.

        Subclass and override this method if you want to inject some custom behavior.
        """

        # print("Getting training dataloader")
        # print("Batch Size:", self.main_args.batch_size)
        # print("Task:", self.task)
        # print("Code Vocabulary:", self.code_vocab)
        # print("AST Vocabulary:", self.ast_vocab)
        # print("NL Vocabulary:", self.nl_vocab)
        return DataLoader(dataset=self.train_dataset,
                          batch_size=self.main_args.batch_size,
                          shuffle=True,
                          collate_fn=lambda batch: collate_fn(batch,
                                                              args=self.main_args,
                                                              task=self.task,
                                                              code_vocab=self.code_vocab,
                                                              nl_vocab=self.nl_vocab,
                                                              ast_vocab=self.ast_vocab))
#Returns the evaluation dataloader
    def get_eval_dataloader(self, eval_dataset: Optional[Dataset] = None) -> DataLoader:
        # print("Getting evaluation dataloader")
        # print("Batch Size:", self.main_args.eval_batch_size)
        # print("Task:", self.task)
        # print("Code Vocabulary:", self.code_vocab)
        # print("AST Vocabulary:", self.ast_vocab)
        # print("NL Vocabulary:", self.nl_vocab)
        if eval_dataset:
            self.eval_dataset = eval_dataset
        return DataLoader(dataset=self.eval_dataset,
                          batch_size=self.main_args.eval_batch_size,
                          collate_fn=lambda batch: collate_fn(batch,
                                                              args=self.main_args,
                                                              task=self.task,
                                                              code_vocab=self.code_vocab,
                                                              nl_vocab=self.nl_vocab,
                                                              ast_vocab=self.ast_vocab))

#Returns the test dataloader
    def get_test_dataloader(self, test_dataset: Dataset) -> DataLoader:
        # print("Getting test dataloader")
        # print("Batch Size:", self.main_args.eval_batch_size)
        # print("Task:", self.task)
        # print("Code Vocabulary:", self.code_vocab)
        # print("AST Vocabulary:", self.ast_vocab)
        # print("NL Vocabulary:", self.nl_vocab)
        return DataLoader(dataset=test_dataset,
                          batch_size=self.main_args.eval_batch_size,
                          collate_fn=lambda batch: collate_fn(batch,
                                                              args=self.main_args,
                                                              task=self.task,
                                                              code_vocab=self.code_vocab,
                                                              nl_vocab=self.nl_vocab,
                                                              ast_vocab=self.ast_vocab))
#Allows setting the task for the trainer
    def set_task(self, task):
        self.task = task


# Example usage

# if __name__ == "__main__":
#     # Assuming arguments and vocabularies are defined elsewhere
#     main_args = argparse.Namespace(batch_size=16, eval_batch_size=8)
#     code_vocab = {}
#     ast_vocab = {}
#     nl_vocab = {}
#     task = ("cap", "completion")

#     code_trainer = CodeTrainer(main_args, code_vocab, ast_vocab, nl_vocab, task)
#     eval_dataloader = code_trainer.get_eval_dataloader()