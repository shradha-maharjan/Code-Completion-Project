from transformers import BartConfig, Seq2SeqTrainingArguments, EarlyStoppingCallback, \
    IntervalStrategy, SchedulerType
import torch

import logging
from typing import Union, Tuple
import os
from tqdm import tqdm
import pandas as pd

import enums
from models.bart import BartForClassificationAndGeneration
from data.vocab import Vocab, load_vocab, init_vocab
from data.dataset import init_dataset
from utils.general import count_params, human_format, layer_wise_parameters, save_log_history
from eval.metrics import bleu, meteor, rouge_l, avg_ir_metrics, accuracy_for_sequence, accuracy_top_k_for_sequence
from utils.callbacks import LogStateCallBack
from utils.trainer import CodeTrainer
from data.data_collator import collate_fn
#from huggingface_hub import login

logger = logging.getLogger(__name__)


# def authenticate_huggingface():
#     login(token="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")
#     print("Authenticated with Hugging Face successfully.")

# authenticate_huggingface()

def run_completion(
        args,
        trained_model: Union[BartForClassificationAndGeneration, str] = None,
        trained_vocab: Union[Tuple[Vocab, Vocab, Vocab], str] = None,
        only_test=False):
    """
    Fine-tuning from given pre-trained model and vocabs, or training from scratch.

    Args:
        args (argparse.Namespace): Arguments
        trained_model (Union[BartForClassificationAndGeneration, str]): Optional,
            instance or directory of ``BartForClassificationAndGeneration``, must given when ``only_test`` is True
        trained_vocab (Union[Tuple[Vocab, Vocab, Vocab], str]): Optional, Tuple of instances or directory of three
            vocabularies, must given when ``only_test`` is True
        only_test (bool): True when only need to test, default to False

    """
    logger.info('-' * 100)
    logger.info(f'Code completion')
    # --------------------------------------------------
    # datasets
    # --------------------------------------------------
    logger.info('-' * 100)
    # Loads datasets for training, validation, and testing.
    logger.info('Loading datasets')
    datasets = dict()
    splits = ['test'] if only_test else ['train', 'valid', 'test']
    for split in splits:
        datasets[split] = init_dataset(args=args,
                                       mode=enums.TRAINING_MODE_FINE_TUNE,
                                       task=enums.TASK_COMPLETION,
                                       split=split,
                                       language='java')  # added language
        logger.info(f'The size of {split} set: {len(datasets[split])}')
    # If necessary, subsets the training dataset based on a specified ratio.
    if args.train_subset_ratio and 'train' in datasets:
        datasets['train'] = datasets['train'].subset(args.train_subset_ratio)
        logger.info(f'The train is trimmed to subset due to the argument: train_subset_ratio={args.train_subset_ratio}')
        logger.info('The size of trimmed train set: {}'.format(len(datasets['train'])))
    logger.info('Datasets loaded successfully')

    # --------------------------------------------------
    # vocabs
    # --------------------------------------------------
    # Loads existing vocabularies from files if provided.
    logger.info('-' * 100)
    if trained_vocab:
        if isinstance(trained_vocab, tuple):
            logger.info('Vocabularies are passed through parameter')
            assert len(trained_vocab) == 3
            code_vocab, ast_vocab, nl_vocab = trained_vocab
        else:
            logger.info('Loading vocabularies from files')
            code_vocab = load_vocab(vocab_root=trained_vocab, name=args.code_vocab_name)
            ast_vocab = load_vocab(vocab_root=trained_vocab, name=args.ast_vocab_name)
            nl_vocab = load_vocab(vocab_root=trained_vocab, name=args.nl_vocab_name)

            logger.info(f'The size of code vocabulary: {len(code_vocab)}')
            logger.info(f'The size of nl vocabulary: {len(nl_vocab)}')
            logger.info(f'The size of ast vocabulary: {len(ast_vocab)}')
            logger.info('The size of loaded vocabulary')

            # logger.info('Updating vocabularies from the dataset')

            # code_vocab = init_vocab(vocab_save_dir=args.vocab_save_dir,
            #                         name=args.code_vocab_name,
            #                         method=args.code_tokenize_method,
            #                         vocab_size=args.code_vocab_size,
            #                         datasets=[datasets['train'].codes, datasets['train'].targets],
            #                         ignore_case=True,
            #                         save_root=args.vocab_root)
            # nl_vocab = init_vocab(vocab_save_dir=args.vocab_save_dir,
            #                       name=args.nl_vocab_name,
            #                       method=args.nl_tokenize_method,
            #                       vocab_size=args.nl_vocab_size,
            #                       datasets=[datasets['train'].names],
            #                       ignore_case=True,
            #                       save_root=args.vocab_root,
            #                       index_offset=len(code_vocab))
            # ast_vocab = init_vocab(vocab_save_dir=args.vocab_save_dir,
            #                        name=args.ast_vocab_name,
            #                        method='word',
            #                        datasets=[datasets['train'].asts],
            #                        save_root=args.vocab_root,
            #                        index_offset=len(code_vocab) + len(nl_vocab))
    else:
        # Otherwise, builds vocabularies for code, abstract syntax tree (AST), and natural language (NL).
        logger.info('Building vocabularies')
        code_vocab = init_vocab(vocab_save_dir=args.vocab_save_dir,
                                name=args.code_vocab_name,
                                method=args.code_tokenize_method,
                                vocab_size=args.code_vocab_size,
                                datasets=[datasets['train'].codes, datasets['train'].targets],
                                ignore_case=True,
                                save_root=args.vocab_root)
        nl_vocab = init_vocab(vocab_save_dir=args.vocab_save_dir,
                              name=args.nl_vocab_name,
                              method=args.nl_tokenize_method,
                              vocab_size=args.nl_vocab_size,
                              datasets=[datasets['train'].names],
                              ignore_case=True,
                              save_root=args.vocab_root,
                              index_offset=len(code_vocab))
        ast_vocab = init_vocab(vocab_save_dir=args.vocab_save_dir,
                               name=args.ast_vocab_name,
                               method='word',
                               datasets=[datasets['train'].asts],
                               save_root=args.vocab_root,
                               index_offset=len(code_vocab) + len(nl_vocab))

    logger.info(f'The size of code vocabulary: {len(code_vocab)}')
    logger.info(f'The size of nl vocabulary: {len(nl_vocab)}')
    logger.info(f'The size of ast vocabulary: {len(ast_vocab)}')
    logger.info('Vocabularies built and updated successfully')

    # --------------------------------------------------
    # model
    # --------------------------------------------------
    logger.info('-' * 100)
    # Loads a pre-trained model if provided.
    if trained_model:
        if isinstance(trained_model, BartForClassificationAndGeneration):
            logger.info('Model is passed through parameter')
            model = trained_model
        else:
            logger.info('Loading the model from file')
            config = BartConfig.from_json_file(os.path.join(trained_model, 'config.json'))
            model = BartForClassificationAndGeneration.from_pretrained(os.path.join(trained_model, 'model.safetensors'),#'pytorch_model.bin'),
                                                                       config=config)
    else:
        # Otherwise, builds a new BART model for code completion.
        logger.info('Building the model')
        # Configures the training arguments, including batch size, learning rate, and number of epochs.
        config = BartConfig(vocab_size=len(code_vocab) + len(ast_vocab) + len(nl_vocab),
                            max_position_embeddings=1024,
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
                            max_length=args.max_code_len,
                            min_length=1,
                            num_beams=args.beam_width,
                            num_labels=2)
        model = BartForClassificationAndGeneration(config)
    model.set_model_mode(enums.MODEL_MODE_GEN)
    # log model statistic
    logger.info('Trainable parameters: {}'.format(human_format(count_params(model))))
    table = layer_wise_parameters(model)
    logger.debug('Layer-wised trainable parameters:\n{}'.format(table))
    logger.info('Model built successfully')

    # --------------------------------------------------
    # trainer
    # --------------------------------------------------
    logger.info('-' * 100)
    logger.info('Initializing the running configurations')

    # decode_preds decodes the predictions and labels obtained from evaluation.
    def decode_preds(preds):
        preds, labels = preds
        decoded_preds = code_vocab.decode_batch(preds)
        decoded_labels = code_vocab.decode_batch(labels)
        return decoded_labels, decoded_preds

    # compute_valid_metrics computes evaluation metrics for the validation set, including BLEU and accuracy.
    # compute metrics
    def compute_valid_metrics(eval_preds):
        decoded_labels, decoded_preds = decode_preds(eval_preds)
        refs = [ref.strip().split() for ref in decoded_labels]
        cans = [can.strip().split() for can in decoded_preds]
        result = {}
        result.update(bleu(references=refs, candidates=cans))
        result.update(accuracy_for_sequence(references=refs, candidates=cans))
        return result

    # compute_test_metrics computes evaluation metrics for the test set, including BLEU, METEOR, ROUGE-L, and accuracy.
    def compute_test_metrics(eval_preds):
        decoded_labels, decoded_preds = decode_preds(eval_preds)
        result = {'references': decoded_labels, 'candidates': decoded_preds}
        refs = [ref.strip().split() for ref in decoded_labels]
        cans = [can.strip().split() for can in decoded_preds]
        result.update(bleu(references=refs, candidates=cans))
        try:
            result.update(meteor(references=refs, candidates=cans))
        except Exception:
            pass
        result.update(rouge_l(references=refs, candidates=cans))
        result.update(avg_ir_metrics(references=refs, candidates=cans))
        result.update(accuracy_for_sequence(references=refs, candidates=cans))
        return result

    # Sets up the training arguments using Seq2SeqTrainingArguments.
    training_args = Seq2SeqTrainingArguments(output_dir=os.path.join(args.checkpoint_root, enums.TASK_COMPLETION),
                                            #  hub_model_id="shradha01/code-completion-01",
                                            #  push_to_hub=True,
                                             overwrite_output_dir=True,
                                             do_train=True,
                                             do_eval=True,
                                             do_predict=True,
                                             evaluation_strategy=IntervalStrategy.STEPS,#IntervalStrategy.EPOCH,
                                             prediction_loss_only=False,
                                             per_device_train_batch_size=args.batch_size, #128
                                             per_device_eval_batch_size=args.eval_batch_size, #128-fails
                                             gradient_accumulation_steps=args.gradient_accumulation_steps,
                                             learning_rate=args.learning_rate,
                                             weight_decay=args.lr_decay_rate,
                                             max_grad_norm=args.grad_clipping_norm,
                                             num_train_epochs=args.n_epoch,
                                             lr_scheduler_type=SchedulerType.LINEAR,
                                             warmup_steps=args.warmup_steps,
                                             logging_dir=os.path.join(args.tensor_board_root, enums.TASK_COMPLETION),
                                             logging_strategy=IntervalStrategy.STEPS,
                                             logging_steps=args.logging_steps,
                                             save_strategy=IntervalStrategy.STEPS,#IntervalStrategy.EPOCH,
                                             save_total_limit=2,
                                             seed=args.random_seed,
                                             fp16=args.fp16,
                                             dataloader_drop_last=False,
                                             run_name=args.model_name,
                                             load_best_model_at_end=True,
                                             metric_for_best_model='accuracy',
                                             greater_is_better=True,
                                             ignore_data_skip=False,
                                             label_smoothing_factor=args.label_smoothing,
                                             report_to=['tensorboard'],
                                             dataloader_pin_memory=True,
                                             predict_with_generate=True)
    # Initializes a CodeTrainer object with the specified configurations.
    trainer = CodeTrainer(main_args=args,
                          code_vocab=code_vocab,
                          ast_vocab=ast_vocab,
                          nl_vocab=nl_vocab,
                          task=enums.TASK_COMPLETION,
                          model=model,
                          args=training_args,
                          data_collator=None,
                          train_dataset=datasets['train'] if 'train' in datasets else None,
                          eval_dataset=datasets['valid'] if 'valid' in datasets else None,
                          tokenizer=nl_vocab,
                          model_init=None,
                          compute_metrics=compute_valid_metrics,
                          callbacks=[
                              EarlyStoppingCallback(early_stopping_patience=args.early_stop_patience),
                              LogStateCallBack()])
    logger.info('Running configurations initialized successfully')

    
    log_dir = 'logs'
    os.makedirs(log_dir, exist_ok=True)

    # --------------------------------------------------
    # train
    # --------------------------------------------------
    # If not in test mode, trains the model.
    if not only_test:
        logger.info('-' * 100)
        logger.info('Start training')
        train_result = trainer.train()
        task = "Completion_train"
        save_log_history(trainer, log_dir, task)
        logger.info('Training finished')
        #Saves the trained model and its state.
        trainer.save_model(args.model_root)
        trainer.save_state()
        metrics = train_result.metrics
        # Logs and saves the training metrics.
        trainer.log_metrics(split='train', metrics=metrics)
        trainer.save_metrics(split='train', metrics=metrics)

        # Push the model to Hugging Face Hub
        # logger.info('-' * 100)
        # logger.info('Pushing model to Hugging Face Hub')
        # trainer.push_to_hub(commit_message="Add fine-tuned code completion model")
        # logger.info('Model successfully pushed to Hugging Face Hub')
       # save_metrics(metrics,split='train')
    # --------------------------------------------------
    # predict
    # --------------------------------------------------
    logger.info('-' * 100)
    logger.info('Start testing')
    # Computes evaluation metrics for the test set.
    trainer.compute_metrics = compute_test_metrics
    # small_test_dataset = torch.utils.data.Subset(datasets['test'], range(100))
    # predict_results = trainer.predict(test_dataset=small_test_dataset, #datasets['test'],
    #                                 metric_key_prefix='test',
    #                                 max_length=args.max_code_len,
    #                                 num_beams=args.beam_width)
    predict_results = trainer.predict(test_dataset=datasets['test'],
                                      metric_key_prefix='test',
                                      max_length=args.max_code_len,
                                      num_beams=args.beam_width)
    predict_metrics = predict_results.metrics
    references = predict_metrics.pop('test_references')
    candidates = predict_metrics.pop('test_candidates')
    trainer.log_metrics(split='test', metrics=predict_metrics)
    # Saves the testing results and metrics.
    trainer.save_metrics(split='test', metrics=predict_metrics)
    # save testing results
    with open(os.path.join(args.output_root, f'{enums.TASK_COMPLETION}_test_results.txt'),
              mode='w', encoding='utf-8') as result_f, \
            open(os.path.join(args.output_root, f'{enums.TASK_COMPLETION}_test_refs.txt'),
                 mode='w', encoding='utf-8') as refs_f, \
            open(os.path.join(args.output_root, f'{enums.TASK_COMPLETION}_test_cans.txt'),
                 mode='w', encoding='utf-8') as cans_f:
        sample_id = 0
        for reference, candidate in zip(references, candidates):
            result_f.write(f'sample {sample_id}:\n')
            sample_id += 1
            result_f.write(f'reference: {reference}\n')
            result_f.write(f'candidate: {candidate}\n')
            result_f.write('\n')
            refs_f.write(reference + '\n')
            cans_f.write(candidate + '\n')
        for name, score in predict_metrics.items():
            result_f.write(f'{name}: {score}\n')
    logger.info('Testing finished')
    for name, score in predict_metrics.items():
        logger.info(f'{name}: {score}')

    # class UserInputDataset(datasets):
    #     """
    #     A simple dataset for wrapping user-provided input code snippets.
    #     """
    #     def __init__(self, input_code, code_vocab, max_length):
    #         # Ensure the vocabulary has a token-to-ID mapping
    #         if not hasattr(code_vocab, 'token_to_id'):
    #             raise AttributeError("The 'Vocab' object must have a 'token_to_id' attribute or similar for token mapping.")

    #         # Tokenize input and map tokens to IDs
    #         self.input_ids = [code_vocab.token_to_id.get(token, Vocab.START_VOCAB.index(Vocab.UNK_TOKEN))
    #                         for token in input_code.split()]

    #         # Generate attention mask (1 for valid tokens, 0 for padding)
    #         self.attention_mask = [1] * len(self.input_ids)

    #         # Pad to max_length
    #         pad_id = Vocab.START_VOCAB.index(Vocab.PAD_TOKEN)
    #         self.input_ids += [pad_id] * (max_length - len(self.input_ids))
    #         self.attention_mask += [0] * (max_length - len(self.attention_mask))

    #     def __len__(self):
    #         return 1  # Single input case

    #     def __getitem__(self, idx):
    #         return {
    #             "input_ids": torch.tensor(self.input_ids, dtype=torch.long),
    #             "attention_mask": torch.tensor(self.attention_mask, dtype=torch.long)
    #         }

    # while True:
    #     logger.info("Enter an incomplete code snippet for prediction (or type 'exit' to quit):")
    #     input_code = input("Input Code: ").strip()
    #     if input_code.lower() == 'exit':
    #         logger.info("Exiting user input loop.")
    #         break

    #     logger.info(f"Processing input: {input_code}")
    #     try:
    #         # Wrap the user input into a temporary dataset
    #         temp_dataset = UserInputDataset(input_code=input_code, code_vocab=code_vocab, max_length=args.max_code_len)

    #         # Use the trainer's predict API
    #         predict_results = trainer.predict(test_dataset=temp_dataset,
    #                                         metric_key_prefix='user_input',
    #                                         max_length=args.max_code_len,
    #                                         num_beams=args.beam_width)

    #         # Extract predictions
    #         generated_predictions = predict_results.predictions

    #         # Decode and log generated completions
    #         logger.info("Generated code completions:")
    #         for idx, sequence in enumerate(generated_predictions):
    #             decoded_sequence = code_vocab.decode(sequence)
    #             logger.info(f"Candidate {idx + 1}: {decoded_sequence}")
    #     except Exception as e:
    #         logger.error(f"Error during prediction: {e}")

    # Tests the accuracy of the model's predictions against the ground truth at the top K predictions.
    logger.info('-' * 100)
    logger.info('Start testing accuracy at 5')
    model.eval()
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    model = model.to(device)
    torch.cuda.empty_cache()
    test_dataloader = torch.utils.data.DataLoader(dataset=datasets['test'],
                                                  batch_size=args.eval_batch_size,
                                                  collate_fn=lambda batch: collate_fn(batch,
                                                                                      args=args,
                                                                                      task=enums.TASK_COMPLETION,
                                                                                      code_vocab=code_vocab,
                                                                                      nl_vocab=nl_vocab,
                                                                                      ast_vocab=ast_vocab))
    predictions = []
    references = []
    for step, batch in enumerate(tqdm(test_dataloader)):
        batch_size = batch['input_ids'].size(0)
        batch_outputs = model.generate(
            input_ids=batch['input_ids'].to(device),
            attention_mask=batch['attention_mask'].to(device),
            max_length=args.completion_max_len,
            min_length=3,
            early_stopping=True,
            num_beams=args.beam_width,
            num_return_sequences=5
        )
        batch_outputs = batch_outputs.view(batch_size, -1, batch_outputs.size(-1))
        for outputs in batch_outputs:
            decoded = code_vocab.decode_batch(outputs.cpu().numpy())
            predictions.append(decoded)

        labels = code_vocab.decode_batch(batch['labels'].numpy())
        references += labels

    assert len(predictions) == len(references)
    scores = accuracy_top_k_for_sequence(references=references, candidates=predictions)
    for name, score in scores.items():
        logger.info(f'{name}: {score}')

    # Saves the top K testing results.
    with open(os.path.join(args.output_root, f'{enums.TASK_COMPLETION}_test_top_k_results.txt'),
              mode='w',
              encoding='utf-8') as f:
        sample_id = 0
        for reference, candidate in zip(references, predictions):
            f.write(f'sample {sample_id}:\n')
            f.write(f'reference: {reference}\n')
            for idx, can in enumerate(candidate):
                f.write(f'candidate {idx}: {can}\n')
            f.write('\n')
            sample_id += 1
        for name, score in scores.items():
            f.write(f'{name}: {score}')


# def save_metrics(metrics, split):
#     """Save the metrics to a CSV file."""
#     output_root = 'logss'
#     os.makedirs(output_root, exist_ok=True)
#     df = pd.DataFrame([metrics])
#     metrics_file = os.path.join(output_root, f'{enums.TASK_COMPLETION}_{split}_metrics.csv')

#     if not os.path.exists(metrics_file):
#         df.to_csv(metrics_file, index=False)
#     else:
#         df.to_csv(metrics_file, mode='a', header=False, index=False)

     # Save log history

    # log_history = trainer.state.log_history
    # if log_history:
    #     pd.DataFrame(log_history).to_csv(os.path.join(args.output_root, f'{enums.TASK_COMPLETION}_log_history.csv'), index=False)

# def run_completion(
#         args,
#         trained_model: Union[BartForClassificationAndGeneration, str] = None,
#         trained_vocab: Union[Tuple[Vocab, Vocab, Vocab], str] = None,
#         only_test=False):
#     """
#     Fine-tuning from given pre-trained model and vocabs, or training from scratch.

#     Args:
#         args (argparse.Namespace): Arguments
#         trained_model (Union[BartForClassificationAndGeneration, str]): Optional,
#             instance or directory of ``BartForClassificationAndGeneration``, must given when ``only_test`` is True
#         trained_vocab (Union[Tuple[Vocab, Vocab, Vocab], str]): Optional, Tuple of instances or directory of three
#             vocabularies, must given when ``only_test`` is True
#         only_test (bool): True when only need to test, default to False

#     """
#     logger.info('-' * 100)
#     logger.info(f'Code completion')
#     # --------------------------------------------------
#     # datasets
#     # --------------------------------------------------
#     logger.info('-' * 100)
#     #Loads datasets for training, validation, and testing.
#     logger.info('Loading datasets')
#     datasets = dict()
#     splits = ['test'] if only_test else ['train', 'valid', 'test']
#     for split in splits:
#         datasets[split] = init_dataset(args=args,
#                                        mode=enums.TRAINING_MODE_FINE_TUNE,
#                                        task=enums.TASK_COMPLETION,
#                                        split=split,
#                                        language='java') #added language
#         logger.info(f'The size of {split} set: {len(datasets[split])}')
#     #If necessary, subsets the training dataset based on a specified ratio.
#     if args.train_subset_ratio and 'train' in datasets:
#         datasets['train'] = datasets['train'].subset(args.train_subset_ratio)
#         logger.info(f'The train is trimmed to subset due to the argument: train_subset_ratio={args.train_subset_ratio}')
#         logger.info('The size of trimmed train set: {}'.format(len(datasets['train'])))
#     logger.info('Datasets loaded successfully')

#     # --------------------------------------------------
#     # vocabs
#     # --------------------------------------------------
#     #Loads existing vocabularies from files if provided.
#     logger.info('-' * 100)
#     if trained_vocab:
#         if isinstance(trained_vocab, tuple):
#             logger.info('Vocabularies are passed through parameter')
#             assert len(trained_vocab) == 3
#             code_vocab, ast_vocab, nl_vocab = trained_vocab
#         else:
#             logger.info('Loading vocabularies from files')
#             code_vocab = load_vocab(vocab_root=trained_vocab, name=args.code_vocab_name)
#             ast_vocab = load_vocab(vocab_root=trained_vocab, name=args.ast_vocab_name)
#             nl_vocab = load_vocab(vocab_root=trained_vocab, name=args.nl_vocab_name)

#             logger.info(f'The size of code vocabulary: {len(code_vocab)}')
#             logger.info(f'The size of nl vocabulary: {len(nl_vocab)}')
#             logger.info(f'The size of ast vocabulary: {len(ast_vocab)}')
#             logger.info('The size of loaded vocabulary')

#             logger.info('Updating vocabularies from the dataset')

#             code_vocab = init_vocab(vocab_save_dir=args.vocab_save_dir,
#                                 name=args.code_vocab_name,
#                                 method=args.code_tokenize_method,
#                                 vocab_size=args.code_vocab_size,
#                                 datasets=[datasets['train'].codes, datasets['train'].targets],
#                                 ignore_case=True,
#                                 save_root=args.vocab_root)
#             nl_vocab = init_vocab(vocab_save_dir=args.vocab_save_dir,
#                                 name=args.nl_vocab_name,
#                                 method=args.nl_tokenize_method,
#                                 vocab_size=args.nl_vocab_size,
#                                 datasets=[datasets['train'].names],
#                                 ignore_case=True,
#                                 save_root=args.vocab_root,
#                                 index_offset=len(code_vocab))
#             ast_vocab = init_vocab(vocab_save_dir=args.vocab_save_dir,
#                                 name=args.ast_vocab_name,
#                                 method='word',
#                                 datasets=[datasets['train'].asts],
#                                 save_root=args.vocab_root,
#                                 index_offset=len(code_vocab) + len(nl_vocab))
#     else:
#         #Otherwise, builds vocabularies for code, abstract syntax tree (AST), and natural language (NL).
#         logger.info('Building vocabularies')
#         code_vocab = init_vocab(vocab_save_dir=args.vocab_save_dir,
#                                 name=args.code_vocab_name,
#                                 method=args.code_tokenize_method,
#                                 vocab_size=args.code_vocab_size,
#                                 datasets=[datasets['train'].codes, datasets['train'].targets],
#                                 ignore_case=True,
#                                 save_root=args.vocab_root)
#         nl_vocab = init_vocab(vocab_save_dir=args.vocab_save_dir,
#                               name=args.nl_vocab_name,
#                               method=args.nl_tokenize_method,
#                               vocab_size=args.nl_vocab_size,
#                               datasets=[datasets['train'].names],
#                               ignore_case=True,
#                               save_root=args.vocab_root,
#                               index_offset=len(code_vocab))
#         ast_vocab = init_vocab(vocab_save_dir=args.vocab_save_dir,
#                                name=args.ast_vocab_name,
#                                method='word',
#                                datasets=[datasets['train'].asts],
#                                save_root=args.vocab_root,
#                                index_offset=len(code_vocab) + len(nl_vocab))

#     logger.info(f'The size of code vocabulary: {len(code_vocab)}')
#     logger.info(f'The size of nl vocabulary: {len(nl_vocab)}')
#     logger.info(f'The size of ast vocabulary: {len(ast_vocab)}')
#     logger.info('Vocabularies built and updated successfully')

#     # --------------------------------------------------
#     # model
#     # --------------------------------------------------
#     logger.info('-' * 100)
#     #Loads a pre-trained model if provided.
#     if trained_model:
#         if isinstance(trained_model, BartForClassificationAndGeneration):
#             logger.info('Model is passed through parameter')
#             model = trained_model
#         else:
#             logger.info('Loading the model from file')
#             config = BartConfig.from_json_file(os.path.join(trained_model, 'config.json'))
#             model = BartForClassificationAndGeneration.from_pretrained(os.path.join(trained_model, 'pytorch_model.bin'),
#                                                                        config=config)
#     else:
#         #Otherwise, builds a new BART model for code completion.
#         logger.info('Building the model')
#         #Configures the training arguments, including batch size, learning rate, and number of epochs.
#         config = BartConfig(vocab_size=len(code_vocab) + len(ast_vocab) + len(nl_vocab),
#                             max_position_embeddings=1024,
#                             encoder_layers=args.n_layer,
#                             encoder_ffn_dim=args.d_ff,
#                             encoder_attention_heads=args.n_head,
#                             decoder_layers=args.n_layer,
#                             decoder_ffn_dim=args.d_ff,
#                             decoder_attention_heads=args.n_head,
#                             activation_function='gelu',
#                             d_model=args.d_model,
#                             dropout=args.dropout,
#                             use_cache=True,
#                             pad_token_id=Vocab.START_VOCAB.index(Vocab.PAD_TOKEN),
#                             bos_token_id=Vocab.START_VOCAB.index(Vocab.SOS_TOKEN),
#                             eos_token_id=Vocab.START_VOCAB.index(Vocab.EOS_TOKEN),
#                             is_encoder_decoder=True,
#                             decoder_start_token_id=Vocab.START_VOCAB.index(Vocab.SOS_TOKEN),
#                             forced_eos_token_id=Vocab.START_VOCAB.index(Vocab.EOS_TOKEN),
#                             max_length=args.max_code_len,
#                             min_length=1,
#                             num_beams=args.beam_width,
#                             num_labels=2)
#         model = BartForClassificationAndGeneration(config)
#     model.set_model_mode(enums.MODEL_MODE_GEN)
#     # log model statistic
#     logger.info('Trainable parameters: {}'.format(human_format(count_params(model))))
#     table = layer_wise_parameters(model)
#     logger.debug('Layer-wised trainable parameters:\n{}'.format(table))
#     logger.info('Model built successfully')

#     # --------------------------------------------------
#     # trainer
#     # --------------------------------------------------
#     logger.info('-' * 100)
#     logger.info('Initializing the running configurations')

#     #decode_preds decodes the predictions and labels obtained from evaluation.
#     def decode_preds(preds):
#         preds, labels = preds
#         decoded_preds = code_vocab.decode_batch(preds)
#         decoded_labels = code_vocab.decode_batch(labels)
#         return decoded_labels, decoded_preds

#     #compute_valid_metrics computes evaluation metrics for the validation set, including BLEU and accuracy.
#     # compute metrics
#     def compute_valid_metrics(eval_preds):
#         decoded_labels, decoded_preds = decode_preds(eval_preds)
#         refs = [ref.strip().split() for ref in decoded_labels]
#         cans = [can.strip().split() for can in decoded_preds]
#         result = {}
#         result.update(bleu(references=refs, candidates=cans))
#         result.update(accuracy_for_sequence(references=refs, candidates=cans))
#         return result

#     #compute_test_metrics computes evaluation metrics for the test set, including BLEU, METEOR, ROUGE-L, and accuracy.
#     def compute_test_metrics(eval_preds):
#         decoded_labels, decoded_preds = decode_preds(eval_preds)
#         result = {'references': decoded_labels, 'candidates': decoded_preds}
#         refs = [ref.strip().split() for ref in decoded_labels]
#         cans = [can.strip().split() for can in decoded_preds]
#         result.update(bleu(references=refs, candidates=cans))
#         try:
#             result.update(meteor(references=refs, candidates=cans))
#         except Exception:
#             pass
#         result.update(rouge_l(references=refs, candidates=cans))
#         result.update(avg_ir_metrics(references=refs, candidates=cans))
#         result.update(accuracy_for_sequence(references=refs, candidates=cans))
#         return result

#     #Sets up the training arguments using Seq2SeqTrainingArguments.
#     training_args = Seq2SeqTrainingArguments(output_dir=os.path.join(args.checkpoint_root, enums.TASK_COMPLETION),
#                                              overwrite_output_dir=True,
#                                              do_train=True,
#                                              do_eval=True,
#                                              do_predict=True,
#                                              evaluation_strategy=IntervalStrategy.EPOCH,
#                                              prediction_loss_only=False,
#                                              per_device_train_batch_size=args.batch_size,
#                                              per_device_eval_batch_size=args.eval_batch_size,
#                                              gradient_accumulation_steps=args.gradient_accumulation_steps,
#                                              learning_rate=args.learning_rate,
#                                              weight_decay=args.lr_decay_rate,
#                                              max_grad_norm=args.grad_clipping_norm,
#                                              num_train_epochs=args.n_epoch,
#                                              lr_scheduler_type=SchedulerType.LINEAR,
#                                              warmup_steps=args.warmup_steps,
#                                              logging_dir=os.path.join(args.tensor_board_root, enums.TASK_COMPLETION),
#                                              logging_strategy=IntervalStrategy.STEPS,
#                                              logging_steps=args.logging_steps,
#                                              save_strategy=IntervalStrategy.EPOCH,
#                                              save_total_limit=2,
#                                              seed=args.random_seed,
#                                              fp16=args.fp16,
#                                              dataloader_drop_last=False,
#                                              run_name=args.model_name,
#                                              load_best_model_at_end=True,
#                                              metric_for_best_model='accuracy',
#                                              greater_is_better=True,
#                                              ignore_data_skip=False,
#                                              label_smoothing_factor=args.label_smoothing,
#                                              report_to=['tensorboard'],
#                                              dataloader_pin_memory=True,
#                                              predict_with_generate=True)
#     #Initializes a CodeTrainer object with the specified configurations.
#     trainer = CodeTrainer(main_args=args,
#                           code_vocab=code_vocab,
#                           ast_vocab=ast_vocab,
#                           nl_vocab=nl_vocab,
#                           task=enums.TASK_COMPLETION,
#                           model=model,
#                           args=training_args,
#                           data_collator=None,
#                           train_dataset=datasets['train'] if 'train' in datasets else None,
#                           eval_dataset=datasets['valid'] if 'valid' in datasets else None,
#                           tokenizer=nl_vocab,
#                           model_init=None,
#                           compute_metrics=compute_valid_metrics,
#                           callbacks=[
#                               EarlyStoppingCallback(early_stopping_patience=args.early_stop_patience),
#                               LogStateCallBack()])
#     logger.info('Running configurations initialized successfully')

#     # --------------------------------------------------
#     # train
#     # --------------------------------------------------
#     #If not in test mode, trains the model.
#     if not only_test:
#         logger.info('-' * 100)
#         logger.info('Start training')
#         train_result = trainer.train()
#         logger.info('Training finished')
#         #Saves the trained model and its state.
#         trainer.save_model(args.model_root)
#         trainer.save_state()
#         metrics = train_result.metrics
#         #Logs and saves the training metrics.
#         trainer.log_metrics(split='train', metrics=metrics)
#         trainer.save_metrics(split='train', metrics=metrics)

#     # --------------------------------------------------
#     # predict
#     # --------------------------------------------------
#     logger.info('-' * 100)
#     logger.info('Start testing')
#     #Computes evaluation metrics for the test set.
#     trainer.compute_metrics = compute_test_metrics
#     predict_results = trainer.predict(test_dataset=datasets['test'],
#                                       metric_key_prefix='test',
#                                       max_length=args.max_code_len,
#                                       num_beams=args.beam_width)
#     predict_metrics = predict_results.metrics
#     references = predict_metrics.pop('test_references')
#     candidates = predict_metrics.pop('test_candidates')
#     trainer.log_metrics(split='test', metrics=predict_metrics)
#     #Saves the testing results and metrics.
#     trainer.save_metrics(split='test', metrics=predict_metrics)
#     # save testing results
#     with open(os.path.join(args.output_root, f'{enums.TASK_COMPLETION}_test_results.txt'),
#               mode='w', encoding='utf-8') as result_f, \
#             open(os.path.join(args.output_root, f'{enums.TASK_COMPLETION}_test_refs.txt'),
#                  mode='w', encoding='utf-8') as refs_f, \
#             open(os.path.join(args.output_root, f'{enums.TASK_COMPLETION}_test_cans.txt'),
#                  mode='w', encoding='utf-8') as cans_f:
#         sample_id = 0
#         for reference, candidate in zip(references, candidates):
#             result_f.write(f'sample {sample_id}:\n')
#             sample_id += 1
#             result_f.write(f'reference: {reference}\n')
#             result_f.write(f'candidate: {candidate}\n')
#             result_f.write('\n')
#             refs_f.write(reference + '\n')
#             cans_f.write(candidate + '\n')
#         for name, score in predict_metrics.items():
#             result_f.write(f'{name}: {score}\n')
#     logger.info('Testing finished')
#     for name, score in predict_metrics.items():
#         logger.info(f'{name}: {score}')

#     #Tests the accuracy of the model's predictions against the ground truth at the top K predictions.
#     logger.info('-' * 100)
#     logger.info('Start testing accuracy at 5')
#     model.eval()
#     device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
#     model = model.to(device)
#     torch.cuda.empty_cache()
#     test_dataloader = torch.utils.data.DataLoader(dataset=datasets['test'],
#                                                   batch_size=args.eval_batch_size,
#                                                   collate_fn=lambda batch: collate_fn(batch,
#                                                                                       args=args,
#                                                                                       task=enums.TASK_COMPLETION,
#                                                                                       code_vocab=code_vocab,
#                                                                                       nl_vocab=nl_vocab,
#                                                                                       ast_vocab=ast_vocab))
#     predictions = []
#     references = []
#     for step, batch in enumerate(tqdm(test_dataloader)):
#         batch_size = batch['input_ids'].size(0)
#         batch_outputs = model.generate(
#             input_ids=batch['input_ids'].to(device),
#             attention_mask=batch['attention_mask'].to(device),
#             max_length=args.completion_max_len,
#             min_length=3,
#             early_stopping=True,
#             num_beams=args.beam_width,
#             num_return_sequences=5
#         )
#         batch_outputs = batch_outputs.view(batch_size, -1, batch_outputs.size(-1))
#         for outputs in batch_outputs:
#             decoded = code_vocab.decode_batch(outputs.cpu().numpy())
#             predictions.append(decoded)

#         labels = code_vocab.decode_batch(batch['labels'].numpy())
#         references += labels

#     assert len(predictions) == len(references)
#     scores = accuracy_top_k_for_sequence(references=references, candidates=predictions)
#     for name, score in scores.items():
#         logger.info(f'{name}: {score}')

# #Saves the top K testing results.
#     with open(os.path.join(args.output_root, f'{enums.TASK_COMPLETION}_test_top_k_results.txt'),
#               mode='w',
#               encoding='utf-8') as f:
#         sample_id = 0
#         for reference, candidate in zip(references, predictions):
#             f.write(f'sample {sample_id}:\n')
#             f.write(f'reference: {reference}\n')
#             for idx, can in enumerate(candidate):
#                 f.write(f'candidate {idx}: {can}\n')
#             f.write('\n')
#             sample_id += 1
#         for name, score in scores.items():
#             f.write(f'{name}: {score}')
