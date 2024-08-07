from torch.utils.data.dataloader import DataLoader
from torch.utils.data.dataset import Dataset
from transformers import Seq2SeqTrainer, Trainer

import argparse
from typing import Optional

from data.data_collator import collate_fn


class CodeTrainer(Seq2SeqTrainer):

    def __init__(self, main_args: argparse.Namespace, code_vocab, ast_vocab, nl_vocab, task, **kwargs):
        super(CodeTrainer, self).__init__(**kwargs)
        self.main_args = main_args
        self.code_vocab = code_vocab
        self.ast_vocab = ast_vocab
        self.nl_vocab = nl_vocab
        self.task = task

    def get_train_dataloader(self) -> DataLoader:
        """
        Returns the training :class:`~torch.utils.data.DataLoader`.

        Will use no sampler if :obj:`self.train_dataset` does not implement :obj:`__len__`, a random sampler (adapted
        to distributed training if necessary) otherwise.

        Subclass and override this method if you want to inject some custom behavior.
        """

        return DataLoader(dataset=self.train_dataset,
                          batch_size=self.main_args.batch_size,
                          shuffle=True,
                          collate_fn=lambda batch: collate_fn(batch,
                                                              args=self.main_args,
                                                              task=self.task,
                                                              code_vocab=self.code_vocab,
                                                              nl_vocab=self.nl_vocab,
                                                              ast_vocab=self.ast_vocab))

    def get_eval_dataloader(self, eval_dataset: Optional[Dataset] = None) -> DataLoader:
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

    def get_test_dataloader(self, test_dataset: Dataset) -> DataLoader:
        return DataLoader(dataset=test_dataset,
                          batch_size=self.main_args.eval_batch_size,
                          collate_fn=lambda batch: collate_fn(batch,
                                                              args=self.main_args,
                                                              task=self.task,
                                                              code_vocab=self.code_vocab,
                                                              nl_vocab=self.nl_vocab,
                                                              ast_vocab=self.ast_vocab))

    def set_task(self, task):
        self.task = task
        
# from data.data_collator import collate_fn
# import argparse
# from typing import Optional, Dict
# import logging
# import torch
# from torch.utils.data import DataLoader, Dataset
# from transformers import Seq2SeqTrainer, Trainer, TrainerCallback, TrainingArguments, TrainerState, TrainerControl, EvalPrediction
# from data.data_collator import collate_fn

# logger = logging.getLogger(__name__)

# class CodeTrainer(Seq2SeqTrainer):

#     def __init__(self, main_args: argparse.Namespace, code_vocab, ast_vocab, nl_vocab, task, **kwargs):
#         super(CodeTrainer, self).__init__(**kwargs)
#         self.main_args = main_args
#         self.code_vocab = code_vocab
#         self.ast_vocab = ast_vocab
#         self.nl_vocab = nl_vocab
#         self.task = task
#         self.eval_losses = []  # List to store evaluation losses

#         # Ensure optimizer and lr_scheduler are initialized
#         if self.optimizer is None or self.lr_scheduler is None:
#             self.create_optimizer_and_scheduler(num_training_steps=self.args.max_steps)
#             self.optimizer = self.optimizer
#             self.lr_scheduler = self.lr_scheduler

#     def get_train_dataloader(self) -> DataLoader:
#         return DataLoader(dataset=self.train_dataset,
#                           batch_size=self.main_args.batch_size,
#                           shuffle=True,
#                           collate_fn=lambda batch: collate_fn(batch,
#                                                               args=self.main_args,
#                                                               task=self.task,
#                                                               code_vocab=self.code_vocab,
#                                                               nl_vocab=self.nl_vocab,
#                                                               ast_vocab=self.ast_vocab))

#     def get_eval_dataloader(self, eval_dataset: Optional[Dataset] = None) -> DataLoader:
#         if eval_dataset:
#             self.eval_dataset = eval_dataset
#         return DataLoader(dataset=self.eval_dataset,
#                           batch_size=self.main_args.eval_batch_size,
#                           collate_fn=lambda batch: collate_fn(batch,
#                                                               args=self.main_args,
#                                                               task=self.task,
#                                                               code_vocab=self.code_vocab,
#                                                               nl_vocab=self.nl_vocab,
#                                                               ast_vocab=self.ast_vocab))

#     def get_test_dataloader(self, test_dataset: Dataset) -> DataLoader:
#         return DataLoader(dataset=test_dataset,
#                           batch_size=self.main_args.eval_batch_size,
#                           collate_fn=lambda batch: collate_fn(batch,
#                                                               args=self.main_args,
#                                                               task=self.task,
#                                                               code_vocab=self.code_vocab,
#                                                               nl_vocab=self.nl_vocab,
#                                                               ast_vocab=self.ast_vocab))

#     def set_task(self, task):
#         self.task = task

#     def training_step(self, model, inputs):
#         model.train()
#         inputs = self._prepare_inputs(inputs)
#         loss = self.compute_loss(model, inputs)
#         loss = loss / self.args.gradient_accumulation_steps
#         loss.backward()

#         lr = self.lr_scheduler.get_last_lr()[0] if self.lr_scheduler is not None else 0.0

#         self.log({
#             'train_loss': loss.item(),
#             'learning_rate': lr,
#             'epoch': self.state.epoch,
#             'step': self.state.global_step
#         })

#         return loss
    
#     def evaluation_step(self, model, inputs, prediction_loss_only):
#         model.eval()
#         inputs = self._prepare_inputs(inputs)
#         with torch.no_grad():
#             outputs = model(**inputs)
#             if prediction_loss_only:
#                 loss = outputs[0]
#             else:
#                 loss, logits = outputs[:2]

#         lr = self.lr_scheduler.get_last_lr()[0] if self.lr_scheduler is not None else 0.0

#         self.log({
#             'eval_loss': loss.item(),
#             'learning_rate': lr,
#             'epoch': self.state.epoch,
#             'step': self.state.global_step
#         })

#         # Append loss to eval_losses list
#         self.eval_losses.append({'eval_loss': loss.item(), 'epoch': self.state.epoch, 'step': self.state.global_step})

#         return loss if prediction_loss_only else (loss, logits)

#     def evaluate(self, eval_dataset: Optional[Dataset] = None):
#         eval_dataloader = self.get_eval_dataloader(eval_dataset)
#         eval_loss = 0
#         all_preds = []
#         all_labels = []
#         self.eval_losses = []  # Reset eval_losses before evaluation
#         for step, batch in enumerate(eval_dataloader):
#             loss, logits = self.evaluation_step(self.model, batch, prediction_loss_only=False)
#             eval_loss += loss.item()
#             all_preds.extend(logits.cpu().numpy())
#             all_labels.extend(batch['labels'].cpu().numpy())

#         eval_loss /= len(eval_dataloader)
#         logger.info(f"Evaluation Loss: {eval_loss}")

#         self.log({
#             'eval_loss': eval_loss,
#             'learning_rate': self.lr_scheduler.get_last_lr()[0] if self.lr_scheduler is not None else 0.0,
#             'epoch': self.state.epoch,
#             'step': self.state.global_step
#         })

#         # Log each evaluation step loss
#         for eval_step in self.eval_losses:
#             logger.info(f"Step {eval_step['step']} Eval Loss: {eval_step['eval_loss']} (Epoch: {eval_step['epoch']})")

#         return EvalPrediction(predictions=all_preds, label_ids=all_labels)

#     def training_epoch(self):
#         for epoch in range(int(self.args.num_train_epochs)):
#             for step, batch in enumerate(self.get_train_dataloader()):
#                 loss = self.training_step(self.model, batch)
#                 self.optimizer.step()
#                 if self.lr_scheduler is not None:
#                     self.lr_scheduler.step()
#                 self.optimizer.zero_grad()
#                 self.state.global_step += 1  # Ensure global_step is updated

#             # Call evaluate at the end of each epoch
#             if self.args.do_eval:
#                 self.evaluate()
#             self.state.epoch += 1  # Increment epoch after evaluation

#     def train(self):
#         self.state.global_step = 0
#         self.state.epoch = 0
#         self.training_epoch()

class CodeCLSTrainer(Trainer):

    def __init__(self, main_args: argparse.Namespace, code_vocab, ast_vocab, nl_vocab, task, **kwargs):
        super(CodeCLSTrainer, self).__init__(**kwargs)
        self.main_args = main_args
        self.code_vocab = code_vocab
        self.ast_vocab = ast_vocab
        self.nl_vocab = nl_vocab
        self.task = task

    def get_train_dataloader(self) -> DataLoader:
        """
        Returns the training :class:`~torch.utils.data.DataLoader`.

        Will use no sampler if :obj:`self.train_dataset` does not implement :obj:`__len__`, a random sampler (adapted
        to distributed training if necessary) otherwise.

        Subclass and override this method if you want to inject some custom behavior.
        """

        return DataLoader(dataset=self.train_dataset,
                          batch_size=self.main_args.batch_size,
                          shuffle=True,
                          collate_fn=lambda batch: collate_fn(batch,
                                                              args=self.main_args,
                                                              task=self.task,
                                                              code_vocab=self.code_vocab,
                                                              nl_vocab=self.nl_vocab,
                                                              ast_vocab=self.ast_vocab))

    def get_eval_dataloader(self, eval_dataset: Optional[Dataset] = None) -> DataLoader:
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

    def get_test_dataloader(self, test_dataset: Dataset) -> DataLoader:
        return DataLoader(dataset=test_dataset,
                          batch_size=self.main_args.eval_batch_size,
                          collate_fn=lambda batch: collate_fn(batch,
                                                              args=self.main_args,
                                                              task=self.task,
                                                              code_vocab=self.code_vocab,
                                                              nl_vocab=self.nl_vocab,
                                                              ast_vocab=self.ast_vocab))

    def set_task(self, task):
        self.task = task

# import os
# import argparse
# import matplotlib.pyplot as plt
# from torch.utils.data.dataloader import DataLoader
# from torch.utils.data.dataset import Dataset
# from transformers import Seq2SeqTrainer, Trainer
# from data.data_collator import collate_fn

# import logging
# from typing import Optional

# logger = logging.getLogger(__name__)

# class CodeTrainer(Seq2SeqTrainer):
#     def __init__(self, main_args: argparse.Namespace, code_vocab, ast_vocab, nl_vocab, task, model, **kwargs):
#         super(CodeTrainer, self).__init__(model=model, **kwargs)
#         self.main_args = main_args
#         self.code_vocab = code_vocab
#         self.ast_vocab = ast_vocab
#         self.nl_vocab = nl_vocab
#         self.task = task
#         self.losses = []
#         self.steps = []

#     def get_train_dataloader(self) -> DataLoader:
#         return DataLoader(dataset=self.train_dataset,
#                           batch_size=self.main_args.batch_size,
#                           shuffle=True,
#                           collate_fn=lambda batch: collate_fn(batch,
#                                                               args=self.main_args,
#                                                               task=self.task,
#                                                               code_vocab=self.code_vocab,
#                                                               nl_vocab=self.nl_vocab,
#                                                               ast_vocab=self.ast_vocab))

#     def get_eval_dataloader(self, eval_dataset: Optional[Dataset] = None) -> DataLoader:
#         if eval_dataset:
#             self.eval_dataset = eval_dataset
#         return DataLoader(dataset=self.eval_dataset,
#                           batch_size=self.main_args.eval_batch_size,
#                           collate_fn=lambda batch: collate_fn(batch,
#                                                               args=self.main_args,
#                                                               task=self.task,
#                                                               code_vocab=self.code_vocab,
#                                                               nl_vocab=self.nl_vocab,
#                                                               ast_vocab=self.ast_vocab))

#     def get_test_dataloader(self, test_dataset: Dataset) -> DataLoader:
#         return DataLoader(dataset=test_dataset,
#                           batch_size=self.main_args.eval_batch_size,
#                           collate_fn=lambda batch: collate_fn(batch,
#                                                               args=self.main_args,
#                                                               task=self.task,
#                                                               code_vocab=self.code_vocab,
#                                                               nl_vocab=self.nl_vocab,
#                                                               ast_vocab=self.ast_vocab))

#     def set_task(self, task):
#         self.task = task

#     def training_step(self, model, inputs):
#         model.train()
#         inputs = self._prepare_inputs(inputs)
#         outputs = model(**inputs)
#         loss = outputs.loss
#         return loss

#     def train(self):
#         self.losses = []
#         self.steps = []
#         total_steps = 0
        
#         logger.info('-' * 100)
#         logger.info(f'Start pre-training task: {self.task}')
        
#         for epoch in range(int(self.args.num_train_epochs)):
#             logger.debug(f'Start epoch {epoch}')
#             for step, batch in enumerate(self.get_train_dataloader()):
#                 loss = self.training_step(self.model, batch)
                
#                 # Store loss and step information
#                 self.losses.append(loss.item())
#                 self.steps.append(total_steps)
#                 total_steps += 1
                
#                 logger.debug(f'Epoch {epoch} / step {step} finished, loss: {loss.item()}')
        
#         logger.info(f'Pre-training task {self.task} finished')
#         self.plot_training_trend()

#         self.save_model(os.path.join(self.main_args.model_root, self.task))
#         return {"train_runtime": total_steps, "train_loss": self.losses[-1], "metrics": {"loss": self.losses[-1]}}

#     def plot_training_trend(self):
#         plt.figure(figsize=(10, 6))
#         plt.plot(self.steps, self.losses, label='Training Loss')
#         plt.xlabel('Training Steps')
#         plt.ylabel('Loss')
#         plt.title('Training Loss Trend')
#         plt.legend()
#         plt.grid(True)
#         plt.show()

# class CodeCLSTrainer(Trainer):
#     def __init__(self, main_args: argparse.Namespace, code_vocab, ast_vocab, nl_vocab, task, model, **kwargs):
#         super(CodeCLSTrainer, self).__init__(model=model, **kwargs)
#         self.main_args = main_args
#         self.code_vocab = code_vocab
#         self.ast_vocab = ast_vocab
#         self.nl_vocab = nl_vocab
#         self.task = task
#         self.losses = []
#         self.steps = []

#     def get_train_dataloader(self) -> DataLoader:
#         return DataLoader(dataset=self.train_dataset,
#                           batch_size=self.main_args.batch_size,
#                           shuffle=True,
#                           collate_fn=lambda batch: collate_fn(batch,
#                                                               args=self.main_args,
#                                                               task=self.task,
#                                                               code_vocab=self.code_vocab,
#                                                               nl_vocab=self.nl_vocab,
#                                                               ast_vocab=self.ast_vocab))

#     def get_eval_dataloader(self, eval_dataset: Optional[Dataset] = None) -> DataLoader:
#         if eval_dataset:
#             self.eval_dataset = eval_dataset
#         return DataLoader(dataset=self.eval_dataset,
#                           batch_size=self.main_args.eval_batch_size,
#                           collate_fn=lambda batch: collate_fn(batch,
#                                                               args=self.main_args,
#                                                               task=self.task,
#                                                               code_vocab=self.code_vocab,
#                                                               nl_vocab=self.nl_vocab,
#                                                               ast_vocab=self.ast_vocab))

#     def get_test_dataloader(self, test_dataset: Dataset) -> DataLoader:
#         return DataLoader(dataset=test_dataset,
#                           batch_size=self.main_args.eval_batch_size,
#                           collate_fn=lambda batch: collate_fn(batch,
#                                                               args=self.main_args,
#                                                               task=self.task,
#                                                               code_vocab=self.code_vocab,
#                                                               nl_vocab=self.nl_vocab,
#                                                               ast_vocab=self.ast_vocab))

#     def set_task(self, task):
#         self.task = task

#     def training_step(self, model, inputs):
#         model.train()
#         inputs = self._prepare_inputs(inputs)
#         outputs = model(**inputs)
#         loss = outputs.loss
#         return loss

#     def train(self):
#         self.losses = []
#         self.steps = []
#         total_steps = 0
        
#         logger.info('-' * 100)
#         logger.info(f'Start pre-training task: {self.task}')
        
#         for epoch in range(int(self.args.num_train_epochs)):
#             logger.debug(f'Start epoch {epoch}')
#             for step, batch in enumerate(self.get_train_dataloader()):
#                 loss = self.training_step(self.model, batch)
                
#                 # Store loss and step information
#                 self.losses.append(loss.item())
#                 self.steps.append(total_steps)
#                 total_steps += 1
                
#                 logger.debug(f'Epoch {epoch} / step {step} finished, loss: {loss.item()}')
        
#         logger.info(f'Pre-training task {self.task} finished')
#         self.plot_training_trend()

#         self.save_model(os.path.join(self.main_args.model_root, self.task))
#         return {"train_runtime": total_steps, "train_loss": self.losses[-1], "metrics": {"loss": self.losses[-1]}}

#     def plot_training_trend(self):
#         plt.figure(figsize=(10, 6))
#         plt.plot(self.steps, self.losses, label='Training Loss')
#         plt.xlabel('Training Steps')
#         plt.ylabel('Loss')
#         plt.title('Training Loss Trend')
#         plt.legend()
#         plt.grid(True)
#         plt.show()


