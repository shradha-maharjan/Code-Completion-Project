import torch
from transformers import TrainerCallback, TrainingArguments, TrainerState, TrainerControl
import logging
from typing import Dict

from .timer import Timer

logger = logging.getLogger(__name__)

class LogStateCallBack(TrainerCallback):
    """
    Custom callback to provide detailed logging during training and evaluation.
    """

    epoch_timer = Timer()
    map_step_epoch = {-1: -1}

    def on_epoch_begin(self,
                       args: TrainingArguments,
                       state: TrainerState,
                       control: TrainerControl,
                       **kwargs):
        """
        Called at the beginning of an epoch.
        Resets the epoch timer and logs the start of the epoch.
        """
        self.epoch_timer.reset()
        logger.debug('-' * 100)
        logger.debug(f'Start epoch {state.epoch}')

    def on_epoch_end(self,
                     args: TrainingArguments,
                     state: TrainerState,
                     control: TrainerControl,
                     optimizer: torch.optim.Optimizer,
                     **kwargs):
        """
        Called at the end of an epoch.
        Logs the end of the epoch, time taken, and the learning rate.
        """
        epoch = state.epoch - 1
        self.map_step_epoch[state.global_step] = epoch
        logger.debug('Epoch {} / step {} finished, time: {:.2f}s'.format(epoch,
                                                                         state.global_step,
                                                                         self.epoch_timer.time()))
        logger.debug('Learning rate: {}'.format(optimizer.param_groups[0]['lr']))

        # Check if log_history is not empty before accessing
        if state.log_history:
            if 'train_loss' in state.log_history[-1]:
                logger.debug('Train loss: {}'.format(state.log_history[-1]['train_loss']))
            if 'eval_loss' in state.log_history[-1]:
                logger.debug('Eval loss: {}'.format(state.log_history[-1]['eval_loss']))

    def on_log(self,
               args: TrainingArguments,
               state: TrainerState,
               control: TrainerControl,
               **kwargs):
        """
        Called at each logging step.
        Logs the current state including loss and learning rate.
        """
        if state.log_history:
            current_log = state.log_history[-1]
            if 'loss' in current_log:
                logger.info(f"Step {state.global_step}: loss = {current_log['loss']}")
            if 'learning_rate' in current_log:
                logger.info(f"Step {state.global_step}: learning_rate = {current_log['learning_rate']}")
            if 'epoch' in current_log:
                logger.info(f"Step {state.global_step}: epoch = {current_log['epoch']}")

    def on_evaluate(self,
                    args: TrainingArguments,
                    state: TrainerState,
                    control: TrainerControl,
                    metrics: Dict[str, float],
                    **kwargs):
        """
        Called after evaluation.
        Logs the evaluation metrics and the best model checkpoint.
        """
        epoch = metrics.pop('epoch', None)
        if epoch is not None:
            logger.debug(f'Evaluation after epoch {epoch - 1} finished')
        else:
            logger.debug('Evaluation finished')

        for metric, score in metrics.items():
            logger.debug(f'{metric}: {score}')

        try:
            best_steps = int(state.best_model_checkpoint.split('-')[-1])
            best_epoch = self.map_step_epoch.get(best_steps, "unknown")
            logger.info(f'Best model at epoch {best_epoch} / step {best_steps}, '
                        f'scores: {state.best_metric}')
        except Exception:
            logger.info('Best model checkpoint not found or best model has not been set.')
# import torch
# from transformers import TrainerCallback, TrainingArguments, TrainerState, TrainerControl

# import logging
# from typing import Dict

# from models.bart import BartForClassificationAndGeneration
# from .timer import Timer
# from .early_stopping import EarlyStopping
# import enums


# logger = logging.getLogger(__name__)


# class LogStateCallBack(TrainerCallback):

#     epoch_timer = Timer()
#     map_step_epoch = {-1: -1}

#     def on_epoch_begin(self,
#                        args: TrainingArguments,
#                        state: TrainerState,
#                        control: TrainerControl,
#                        **kwargs):
#         self.epoch_timer.reset()
#         logger.debug('-' * 100)
#         logger.debug(f'Start epoch {state.epoch}')

#     def on_epoch_end(self,
#                      args: TrainingArguments,
#                      state: TrainerState,
#                      control: TrainerControl,
#                      optimizer: torch.optim.Optimizer,
#                      **kwargs):
#         epoch = state.epoch - 1
#         self.map_step_epoch[state.global_step] = epoch
#         logger.debug('Epoch {} / step {} finished, time: {:.2f}s'.format(epoch,
#                                                                          state.global_step,
#                                                                          self.epoch_timer.time()))
#         logger.debug('learning rate: {}'.format(optimizer.param_groups[0]['lr']))

#     # def on_evaluate(self,
#     #                 args: TrainingArguments,
#     #                 state: TrainerState,
#     #                 control: TrainerControl,
#     #                 metrics: Dict[str, float],
#     #                 **kwargs):
#     #     epoch = metrics.pop('epoch') - 1
#     #     logger.debug(f'Evaluation after epoch {epoch} finished')
#     #     for metric, score in metrics.items():
#     #         logger.debug(f'{metric}: {score}')
#     #     try:
#     #         best_steps = int(state.best_model_checkpoint.split('-')[-1])
#     #     except Exception:
#     #         best_steps = -1
#     #     logger.info(f'Best model at epoch {self.map_step_epoch[best_steps]} / step {best_steps}, '
#     #                 f'scores: {state.best_metric}')

#     def on_evaluate(self,
#                     args: TrainingArguments,
#                     state: TrainerState,
#                     control: TrainerControl,
#                     metrics: Dict[str, float],
#                     **kwargs):
#         """
#         Called after evaluation.
#         Logs the evaluation metrics and the best model checkpoint.
#         """
#         epoch = metrics.pop('epoch', None)
#         if epoch is not None:
#             logger.debug(f'Evaluation after epoch {epoch - 1} finished')
#         else:
#             logger.debug('Evaluation finished')

#         for metric, score in metrics.items():
#             logger.debug(f'{metric}: {score}')

#         try:
#             best_steps = int(state.best_model_checkpoint.split('-')[-1])
#             best_epoch = self.map_step_epoch.get(best_steps, "unknown")
#             logger.info(f'Best model at epoch {best_epoch} / step {best_steps}, '
#                         f'scores: {state.best_metric}')
#         except Exception:
#             logger.info('Best model checkpoint not found or best model has not been set.')
