from accelerate import Accelerator, notebook_launcher
import torch.utils.data
from transformers import BartConfig, Seq2SeqTrainingArguments, IntervalStrategy, SchedulerType, TrainingArguments

import logging
import os
from typing import Union, Tuple

import enums
from data.dataset import init_dataset
from data.vocab import Vocab, init_vocab, load_vocab
from utils.general import count_params, human_format, layer_wise_parameters
from utils.trainer import CodeTrainer, CodeCLSTrainer
from utils.callbacks import LogStateCallBack
from models.bart import BartForClassificationAndGeneration

logger = logging.getLogger(__name__)

def pre_train(args,
              trained_model: Union[BartForClassificationAndGeneration, str] = None,
              trained_vocab: Union[Tuple[Vocab, Vocab, Vocab], str] = None):
    
    # Initialize the Accelerator
    accelerator = Accelerator()

    tasks = args.pre_train_tasks
    #Determines the tasks to be pre-trained on. If no specific tasks are provided, it defaults to pre-training tasks defined in enums.PRE_TRAIN_TASKS
    if tasks is None:
        logger.warning('Was specified for pre-training, but got pre-training tasks to None, '
                       'will default to {}'.format(','.join(enums.PRE_TRAIN_TASKS)))
        tasks = enums.PRE_TRAIN_TASKS
    else:
        supported_tasks = []
        for task in tasks.split(','):
            task = task.strip().lower()
            if task in enums.PRE_TRAIN_TASKS:
                supported_tasks.append(task)
            else:
                logger.warning(f'Pre-training task {task} is not supported and will be ignored.')
        tasks = supported_tasks
#If a pre-trained model is provided, it loads it. If not, it initializes a new BART model.
    assert not trained_model or \
        isinstance(trained_model, str) or \
        isinstance(trained_model, BartForClassificationAndGeneration), \
        f'The model type is not supported, expect Bart model or string of model dir, got {type(trained_model)}'

#If pre-trained vocabularies are provided, it loads them. If not, it initializes new vocabularies.
    if trained_vocab is None and args.trained_vocab is not None:
        trained_vocab = args.trained_vocab
    assert not trained_vocab or isinstance(trained_vocab, str), \
        f'The vocab type is not supported, expect string of vocab dir, got {type(trained_vocab)}'

    logger.info('*' * 100)
    logger.info('Initializing pre-training environments')

    # --------------------------------------------------
    # datasets
    # --------------------------------------------------
    logger.info('-' * 100)
    logger.info('Loading and parsing datasets')
    #Initializes and loads the pre-training dataset.
    dataset = init_dataset(args=args, mode=enums.TRAINING_MODE_PRE_TRAIN)
    logger.info(f'The size of pre_training set: {len(dataset)}')
    if args.pre_train_subset_ratio:
        logger.info(f'The pre-train dataset is trimmed to subset due to the argument: '
                    f'pre_train_subset_ratio={args.pre_train_subset_ratio}')
        dataset = dataset.subset(args.pre_train_subset_ratio)
        logger.info('The size of trimmed pre-train set: {}'.format(len(dataset)))
        # ######################################################
        # Updated for bug-fix, myoungkyu song, 03/23/2024
        """
        if isinstance(dataset, torch.utils.data.Subset):
            logger.info('The size of trimmed pre-train set (dataset.dataset): {}'.format(len(dataset.dataset)))
            dataset = dataset.dataset
        """
        logger.info('The size of trimmed pre-train set: {}'.format(len(dataset)))

    logger.info('Datasets loaded and parsed successfully')

    # ##################################################
    # Edited to force to initialize vocab, myoungkyu song, 03/23/2024
    if trained_vocab == 'None':
        trained_vocab = None
    # ##################################################

    # --------------------------------------------------
    # vocabs
    # --------------------------------------------------
    logger.info('-' * 100)
    #If trained_vocab is provided, it loads existing vocabularies from files.
    if trained_vocab:
        logger.info('Loading vocabularies from files')
        code_vocab = load_vocab(vocab_root=trained_vocab, name=args.code_vocab_name)
        ast_vocab = load_vocab(vocab_root=trained_vocab, name=args.ast_vocab_name)
        nl_vocab = load_vocab(vocab_root=trained_vocab, name=args.nl_vocab_name)
    else:
        # else it builds vocabularies for code, abstract syntax tree (AST), and natural language (NL).
        logger.info('Building vocabularies')
        # code vocab
        code_vocab = init_vocab(vocab_save_dir=args.vocab_save_dir,
                                name=args.code_vocab_name,
                                method=args.code_tokenize_method,
                                vocab_size=args.code_vocab_size,
                                datasets=[dataset.codes],
                                ignore_case=True,
                                save_root=args.vocab_root)
        # nl vocab
        nl_vocab = init_vocab(vocab_save_dir=args.vocab_save_dir,
                              name=args.nl_vocab_name,
                              method=args.nl_tokenize_method,
                              vocab_size=args.nl_vocab_size,
                              datasets=[dataset.names, dataset.docs] if hasattr(dataset, 'docs') else [dataset.names],
                              ignore_case=True,
                              save_root=args.vocab_root,
                              index_offset=len(code_vocab))
        # ast vocab
        ast_vocab = init_vocab(vocab_save_dir=args.vocab_save_dir,
                               name=args.ast_vocab_name,
                               method='word',
                               datasets=[dataset.asts],
                               save_root=args.vocab_root,
                               index_offset=len(code_vocab) + len(nl_vocab))
    logger.info(f'The size of code vocabulary: {len(code_vocab)}')
    logger.info(f'The size of nl vocabulary: {len(nl_vocab)}')
    logger.info(f'The size of ast vocabulary: {len(ast_vocab)}')
    logger.info('Vocabularies built successfully')

    # --------------------------------------------------
    # Model
    # --------------------------------------------------
    #Configures the BART model based on the vocabularies and the number of layers, hidden units, etc.
    logger.info('-' * 100)
    logger.info('Building model')
    config = BartConfig(vocab_size=len(code_vocab) + len(ast_vocab) + len(nl_vocab),
                        max_position_embeddings=512,
                        encoder_layers=args.n_layer,
                        encoder_ffn_dim=args.d_ff,
                        encoder_attention_heads=args.n_head,
                        decoder_layers=args.n_layer,
                        decoder_ffn_dim=args.d_ff,
                        decoder_attention_heads=args.n_head,
                        activation_function='gelu',
                        d_model=args.d_model,
                        dropout=args.dropout,
                        use_cache=True,
                        pad_token_id=Vocab.START_VOCAB.index(Vocab.PAD_TOKEN),
                        bos_token_id=Vocab.START_VOCAB.index(Vocab.SOS_TOKEN),
                        eos_token_id=Vocab.START_VOCAB.index(Vocab.EOS_TOKEN),
                        is_encoder_decoder=True,
                        decoder_start_token_id=Vocab.START_VOCAB.index(Vocab.SOS_TOKEN),
                        forced_eos_token_id=Vocab.START_VOCAB.index(Vocab.EOS_TOKEN),
                        max_length=100,
                        min_length=1,
                        num_beams=args.beam_width,
                        num_labels=2)
    model = BartForClassificationAndGeneration(config)

    model, optimizer, train_dataloader = accelerator.prepare(model, None, dataset)  # Assuming no optimizer is set yet
    # log model statistic
    logger.info('Model trainable parameters: {}'.format(human_format(count_params(model))))
    table = layer_wise_parameters(model)
    logger.debug('Layer-wised trainable parameters:\n{}'.format(table))
    logger.info('Model built successfully')

    # --------------------------------------------------
    # pre-train
    # --------------------------------------------------
    #It iterates over different pre-training tasks, such as code AST prediction, MASS (Masked Sequence to Sequence), and method name prediction.
    for task in tasks:
        logger.info('-' * 100)
        logger.info(f'Pre-training task: {task.upper()}')
        accelerator.print(f'Pre-training task: {task.upper()}')

        if isinstance(dataset, torch.utils.data.Subset):
            dataset.dataset.set_task(task)
        else:
            dataset.set_task(task)

        #It sets up the appropriate mode for the model (classification or generation).
        if task == enums.TASK_CODE_AST_PREDICTION:
            logger.info('-' * 100)
            if args.n_epoch_pre_train != 30:
                print(f'n_epoch_pre_train is updated to {args.n_epoch_pre_train}')
            # set model mode
            logger.info('-' * 100)
            # Updated to use 'GEN' instead of 'CLS', myoungkyu song, 3/29/2024
            model.set_model_mode(enums.MODEL_MODE_GEN) # model.set_model_mode(enums.MODEL_MODE_CLS)
            # --------------------------------------------------
            # trainer
            # --------------------------------------------------
            logger.info('-' * 100)
            logger.info('Initializing the running configurations')
            # Updated n_epoch to reduce training time, myoungkyu song, 3/20/2024
            #   File "/.../envs/spt-code/lib/python3.8/site-packages/transformers/trainer_pt_utils.py", line 510, in __call__
            #     nll_loss = log_probs.gather(dim=-1, index=labels)
            # RuntimeError: Index tensor must have the same number of dimensions as input tensor
            # Initializes training configurations, such as batch size, learning rate, and number of epochs.
            training_args = TrainingArguments(output_dir=os.path.join(args.pre_train_output_root, task),
                                              overwrite_output_dir=True,
                                              do_train=True,
                                              per_device_train_batch_size=args.batch_size,
                                              gradient_accumulation_steps=1,
                                              learning_rate=args.learning_rate,
                                              weight_decay=args.lr_decay_rate,
                                              max_grad_norm=args.grad_clipping_norm,
                                              num_train_epochs=args.n_epoch_pre_train, # updated
                                              lr_scheduler_type=SchedulerType.LINEAR,
                                              warmup_steps=args.warmup_steps,
                                              logging_dir=os.path.join(args.tensor_board_root, task),
                                              logging_strategy=IntervalStrategy.STEPS,
                                              logging_steps=args.logging_steps,
                                              save_strategy=IntervalStrategy.NO,
                                              seed=args.random_seed,
                                              fp16=args.fp16,
                                              dataloader_drop_last=False,
                                              run_name=args.model_name,
                                              load_best_model_at_end=True,
                                              ignore_data_skip=False,
                                              label_smoothing_factor=args.label_smoothing,
                                              report_to=['tensorboard'],
                                              dataloader_pin_memory=True)
        #Initializes a trainer object with the model, training arguments, dataset, and callbacks.
            trainer = CodeCLSTrainer(main_args=args,
                                     code_vocab=code_vocab,
                                     ast_vocab=ast_vocab,
                                     nl_vocab=nl_vocab,
                                     task=task,
                                     model=model,
                                     args=training_args,
                                     data_collator=None,
                                     train_dataset=dataset,
                                     tokenizer=nl_vocab,
                                     model_init=None,
                                     compute_metrics=None,
                                     callbacks=[LogStateCallBack()])
            logger.info('Running configurations initialized successfully')

            # --------------------------------------------------
            # train
            # --------------------------------------------------
            logger.info('-' * 100)
            logger.info(f'Start pre-training task: {task}')
            cap_result = trainer.train()
            logger.info(f'Pre-training task {task} finished')
            #Saves the trained model.
            trainer.save_model(os.path.join(args.model_root, task))

        elif task == enums.TASK_MASS:
            logger.info('-' * 100)
            if args.n_epoch_pre_train != 30:
                print(f'n_epoch_pre_train is updated to {args.n_epoch_pre_train}')
            # set model mode
            logger.info('-' * 100)
            model.set_model_mode(enums.MODEL_MODE_GEN)
            # --------------------------------------------------
            # trainer
            # --------------------------------------------------
            # Updated n_epoch to reduce training time, myoungkyu song, 3/20/2024
            logger.info('-' * 100)
            logger.info('Initializing the running configurations')
            training_args = Seq2SeqTrainingArguments(output_dir=os.path.join(args.pre_train_output_root, task),
                                                     overwrite_output_dir=True,
                                                     do_train=True,
                                                     per_device_train_batch_size=args.batch_size,
                                                     gradient_accumulation_steps=1,
                                                     learning_rate=args.learning_rate,
                                                     weight_decay=args.lr_decay_rate,
                                                     max_grad_norm=args.grad_clipping_norm,
                                                     num_train_epochs=args.n_epoch_pre_train, # updated
                                                     lr_scheduler_type=SchedulerType.LINEAR,
                                                     warmup_steps=args.warmup_steps,
                                                     logging_dir=os.path.join(args.tensor_board_root, task),
                                                     logging_strategy=IntervalStrategy.STEPS,
                                                     logging_steps=args.logging_steps,
                                                     save_strategy=IntervalStrategy.NO,
                                                     seed=args.random_seed,
                                                     fp16=args.fp16,
                                                     dataloader_drop_last=False,
                                                     run_name=args.model_name,
                                                     load_best_model_at_end=True,
                                                     ignore_data_skip=False,
                                                     label_smoothing_factor=args.label_smoothing,
                                                     report_to=['tensorboard'],
                                                     dataloader_pin_memory=True)
            trainer = CodeTrainer(main_args=args,
                                  code_vocab=code_vocab,
                                  ast_vocab=ast_vocab,
                                  nl_vocab=nl_vocab,
                                  task=task,
                                  model=model,
                                  args=training_args,
                                  data_collator=None,
                                  train_dataset=dataset,
                                  tokenizer=nl_vocab,
                                  model_init=None,
                                  compute_metrics=None,
                                  callbacks=[LogStateCallBack()])
            logger.info('Running configurations initialized successfully')

            # --------------------------------------------------
            # train
            # --------------------------------------------------
            logger.info('-' * 100)
            logger.info(f'Start pre-training task: {task}')
            # model device
            logger.info('Device: {}'.format(next(model.parameters()).device))
            mass_result = trainer.train()
            logger.info(f'Pre-training task {task} finished')
            trainer.save_model(os.path.join(args.model_root, task))

        elif task == enums.TASK_METHOD_NAME_PREDICTION:
            logger.info('-' * 100)
            if args.n_epoch_pre_train != 30:
                print(f'n_epoch_pre_train is updated to {args.n_epoch_pre_train}')
            # set model mode
            logger.info('-' * 100)
            model.set_model_mode(enums.MODEL_MODE_GEN)
            # --------------------------------------------------
            # trainer
            # --------------------------------------------------
            logger.info('-' * 100)
            logger.info('Initializing the running configurations')
            training_args = Seq2SeqTrainingArguments(output_dir=os.path.join(args.pre_train_output_root, task),
                                                     overwrite_output_dir=True,
                                                     do_train=True,
                                                     per_device_train_batch_size=args.batch_size,
                                                     gradient_accumulation_steps=1,
                                                     learning_rate=args.learning_rate,
                                                     weight_decay=args.lr_decay_rate,
                                                     max_grad_norm=args.grad_clipping_norm,
                                                     num_train_epochs=args.n_epoch_pre_train, # updated
                                                     lr_scheduler_type=SchedulerType.LINEAR,
                                                     warmup_steps=args.warmup_steps,
                                                     logging_dir=os.path.join(args.tensor_board_root, task),
                                                     logging_strategy=IntervalStrategy.STEPS,
                                                     logging_steps=args.logging_steps,
                                                     save_strategy=IntervalStrategy.NO,
                                                     seed=args.random_seed,
                                                     fp16=args.fp16,
                                                     dataloader_drop_last=False,
                                                     run_name=args.model_name,
                                                     load_best_model_at_end=True,
                                                     ignore_data_skip=False,
                                                     label_smoothing_factor=args.label_smoothing,
                                                     report_to=['tensorboard'],
                                                     dataloader_pin_memory=True)
            trainer = CodeTrainer(main_args=args,
                                  code_vocab=code_vocab,
                                  ast_vocab=ast_vocab,
                                  nl_vocab=nl_vocab,
                                  task=task,
                                  model=model,
                                  args=training_args,
                                  data_collator=None,
                                  train_dataset=dataset,
                                  tokenizer=nl_vocab,
                                  model_init=None,
                                  compute_metrics=None,
                                  callbacks=[LogStateCallBack()])
            logger.info('Running configurations initialized successfully')

            # --------------------------------------------------
            # train
            # --------------------------------------------------
            logger.info('-' * 100)
            logger.info(f'Start pre-training task: {task}')
            mnp_result = trainer.train()
            logger.info(f'Pre-training task {task} finished')
            #After pre-training for all tasks, it saves the trained model and vocabularies to the specified directories.
            trainer.save_model(os.path.join(args.model_root, task))

    logger.info('Pre-training finished')

#then, it returns the trained model and the three vocabularies (code, AST, NL).
    return model, (code_vocab, ast_vocab, nl_vocab)

def run_pre_train():
    # Configure arguments or load from somewhere
    pre_train(args=args)

# Use notebook_launcher to run the training function
notebook_launcher(run_pre_train, num_processes= 2)
# if __name__ == "__main__":
#     import pickle
#     file_path = "/home/user1-selab3/Documents/research-shradha/CODE-SPT-Code/dataset/dataset_saved/fine_tune.search.java.train.pk"

#     with open(file_path, "rb") as f:
#         load_data = pickle.load(f)

#     for data in load_data:
#         print(data)
#         break
