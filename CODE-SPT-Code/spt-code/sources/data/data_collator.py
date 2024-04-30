
import torch

from typing import List
import itertools

from data.vocab import Vocab
import enums

#collate_fn preprocesses batches of data for input to the model during training, evaluation, or testing.
def collate_fn(batch, args, task, code_vocab, nl_vocab, ast_vocab):
    """
    Data collator function.

    Args:
        batch (list):
        args (argparse.Namespace):
        task (str):
        code_vocab (Vocab):
        nl_vocab (Vocab):
        ast_vocab (Vocab):

    Returns:
        dict: Model inputs

    """
    # print("Collating batch...")
    # print("Task:", task)
    model_inputs = {}
    #Concatenates code, AST, and NL inputs. Converts labels to tensor format.
    # cap
    if task == enums.TASK_CODE_AST_PREDICTION:
        
        # print("Task: Code AST Prediction")
        code_raw, ast_raw, name_raw, is_ast = map(list, zip(*batch))
        # print("Batch Sizes:")
        # print("code_raw:", len(code_raw))
        # print("ast_raw:", len(ast_raw))
        # print("name_raw:", len(name_raw))
        # print("is_ast:", len(is_ast))

        model_inputs['input_ids'], model_inputs['attention_mask'] = get_concat_batch_inputs(
            code_raw=code_raw,
            code_vocab=code_vocab,
            max_code_len=args.max_code_len,
            ast_raw=ast_raw,
            ast_vocab=ast_vocab,
            max_ast_len=args.max_ast_len,
            nl_raw=name_raw,
            nl_vocab=nl_vocab,
            max_nl_len=args.max_nl_len,
            no_ast=args.no_ast,
            no_nl=args.no_nl
        )
        # Updated to add an extra dimension to model_inputs['labels'], myoungkyu song, 3/30
        # model_inputs['labels'] = torch.tensor(is_ast, dtype=torch.long)
        model_inputs['labels'] = torch.tensor(is_ast, dtype=torch.long).unsqueeze(-1)
        # print("Model Inputs:")
        # print("input_ids shape:", model_inputs['input_ids'].shape)
        # print("attention_mask shape:", model_inputs['attention_mask'].shape)
        # print("labels shape:", model_inputs['labels'].shape)
    #Concatenates code and AST inputs, and generates decoder inputs and labels for MASS task.
    # mass
    elif task == enums.TASK_MASS:

        # print("Task: MASS")
        code_raw, ast_raw, name_raw, target_raw = map(list, zip(*batch))
        # print("Batch Sizes:")
        # print("code_raw:", len(code_raw))
        # print("ast_raw:", len(ast_raw))
        # print("name_raw:", len(name_raw))
        # print("target_raw:", len(target_raw))

        model_inputs['input_ids'], model_inputs['attention_mask'] = get_concat_batch_inputs(
            code_raw=code_raw,
            code_vocab=code_vocab,
            max_code_len=args.max_code_len,
            ast_raw=ast_raw,
            ast_vocab=ast_vocab,
            max_ast_len=args.max_ast_len,
            nl_raw=name_raw,
            nl_vocab=nl_vocab,
            max_nl_len=args.max_nl_len,
            no_ast=args.no_ast,
            no_nl=args.no_nl
        )
        # print("Model Inputs:")
        # print("input_ids shape:", model_inputs['input_ids'].shape)
        # print("attention_mask shape:", model_inputs['attention_mask'].shape)

        model_inputs['decoder_input_ids'], model_inputs['decoder_attention_mask'] = get_batch_inputs(
            batch=target_raw,
            vocab=code_vocab,
            processor=Vocab.sos_processor,
            max_len=int(args.mass_mask_ratio * args.max_code_len)
        )
        
        # print("input_ids shape:", model_inputs['decoder_input_ids'].shape)
        # print("attention_mask shape:", model_inputs['decoder_attention_mask'].shape)
        model_inputs['labels'], _ = get_batch_inputs(batch=target_raw,
                                                     vocab=code_vocab,
                                                     processor=Vocab.eos_processor,
                                                     max_len=int(args.mass_mask_ratio * args.max_code_len))
        
        # print("labels shape:", model_inputs['labels'].shape)
    #Concatenates code, AST, and NL inputs. Generates decoder inputs and labels for predicting method names.
    # mnp
    elif task == enums.TASK_METHOD_NAME_PREDICTION:

        code_raw, ast_raw, nl_raw, name_raw = map(list, zip(*batch))

        model_inputs['input_ids'], model_inputs['attention_mask'] = get_concat_batch_inputs(
            code_raw=code_raw,
            code_vocab=code_vocab,
            max_code_len=args.max_code_len,
            ast_raw=ast_raw,
            ast_vocab=ast_vocab,
            max_ast_len=args.max_ast_len,
            nl_raw=nl_raw,
            nl_vocab=nl_vocab,
            max_nl_len=args.max_nl_len,
            no_ast=args.no_ast,
            no_nl=args.no_nl
        )

        model_inputs['decoder_input_ids'], model_inputs['decoder_attention_mask'] = get_batch_inputs(
            batch=name_raw,
            vocab=nl_vocab,
            processor=Vocab.sos_processor,
            max_len=args.max_nl_len
        )
        model_inputs['labels'], _ = get_batch_inputs(batch=name_raw,
                                                     vocab=nl_vocab,
                                                     processor=Vocab.eos_processor,
                                                     max_len=args.max_nl_len)
    # summarization
    #Concatenates code, AST, and NL inputs. Generates decoder inputs and labels for summarization.
    elif task == enums.TASK_SUMMARIZATION:

        code_raw, ast_raw, name_raw, nl_raw = map(list, zip(*batch))

        model_inputs['input_ids'], model_inputs['attention_mask'] = get_concat_batch_inputs(
            code_raw=code_raw,
            code_vocab=code_vocab,
            max_code_len=args.max_code_len,
            ast_raw=ast_raw,
            ast_vocab=ast_vocab,
            max_ast_len=args.max_ast_len,
            nl_raw=name_raw,
            nl_vocab=nl_vocab,
            max_nl_len=args.max_nl_len,
            no_ast=args.no_ast,
            no_nl=args.no_nl
        )

        model_inputs['decoder_input_ids'], model_inputs['decoder_attention_mask'] = get_batch_inputs(
            batch=nl_raw,
            vocab=nl_vocab,
            processor=Vocab.sos_processor,
            max_len=args.max_nl_len,
        )

        model_inputs['labels'], _ = get_batch_inputs(
            batch=nl_raw,
            vocab=nl_vocab,
            processor=Vocab.eos_processor,
            max_len=args.max_nl_len,
        )

    # translation
    # Concatenates code, AST, and NL inputs. Generates decoder inputs and labels for translation.
    elif task == enums.TASK_TRANSLATION:

        code_raw, ast_raw, name_raw, target_raw = map(list, zip(*batch))

        model_inputs['input_ids'], model_inputs['attention_mask'] = get_concat_batch_inputs(
            code_raw=code_raw,
            code_vocab=code_vocab,
            max_code_len=args.max_code_len,
            ast_raw=ast_raw,
            ast_vocab=ast_vocab,
            max_ast_len=args.max_ast_len,
            nl_raw=name_raw,
            nl_vocab=nl_vocab,
            max_nl_len=args.max_nl_len,
            no_ast=args.no_ast,
            no_nl=args.no_nl
        )

        model_inputs['decoder_input_ids'], model_inputs['decoder_attention_mask'] = get_batch_inputs(
            batch=target_raw,
            vocab=code_vocab,
            processor=Vocab.sos_processor,
            max_len=args.max_code_len
        )
        model_inputs['labels'], _ = get_batch_inputs(batch=target_raw,
                                                     vocab=code_vocab,
                                                     processor=Vocab.eos_processor,
                                                     max_len=args.max_code_len)
    # search
    #Processes data based on the split ('codebase' or 'train'). Concatenates code, AST, and NL inputs.
    elif task == enums.TASK_SEARCH:

        batch_raw = map(list, zip(*batch))
        split = next(batch_raw)[0]

        if split == 'codebase':
            url_raw, code_raw, ast_raw, name_raw = batch_raw

            model_inputs['input_ids'], model_inputs['attention_mask'] = get_concat_batch_inputs(
                code_raw=code_raw,
                code_vocab=code_vocab,
                max_code_len=args.max_code_len,
                ast_raw=ast_raw,
                ast_vocab=ast_vocab,
                max_ast_len=args.max_ast_len,
                nl_raw=name_raw,
                nl_vocab=nl_vocab,
                max_nl_len=args.max_nl_len,
                no_ast=args.no_ast,
                no_nl=args.no_nl
            )

            model_inputs['urls'] = url_raw

        elif split == 'train':
            code_raw, ast_raw, name_raw, nl_raw = batch_raw

            model_inputs['input_ids'], model_inputs['attention_mask'] = get_concat_batch_inputs(
                code_raw=code_raw,
                code_vocab=code_vocab,
                max_code_len=args.max_code_len,
                ast_raw=ast_raw,
                ast_vocab=ast_vocab,
                max_ast_len=args.max_ast_len,
                nl_raw=name_raw,
                nl_vocab=nl_vocab,
                max_nl_len=args.max_nl_len,
                no_ast=args.no_ast,
                no_nl=args.no_nl
            )

            model_inputs['nl_input_ids'], model_inputs['nl_attention_mask'] = get_batch_inputs(
                batch=nl_raw,
                vocab=nl_vocab,
                processor=Vocab.eos_processor,
                max_len=args.max_nl_len
            )
            # # neg_nl_input_ids, neg_nl_attention_mask
            # model_inputs['neg_nl_input_ids'], model_inputs['neg_nl_attention_mask'] = get_batch_inputs(
            #     batch=neg_nl_raw,
            #     vocab=nl_vocab,
            #     processor=Vocab.eos_processor,
            #     max_len=args.max_nl_len
            # )

        else:
            url_raw, nl_raw = batch_raw

            model_inputs['input_ids'], model_inputs['attention_mask'] = get_batch_inputs(
                batch=nl_raw,
                vocab=nl_vocab,
                processor=Vocab.eos_processor,
                max_len=args.max_nl_len
            )

            model_inputs['urls'] = url_raw
    # clone detection
    #Concatenates pairs of code, AST, and NL inputs. Converts labels to tensor format.
    elif task == enums.TASK_CLONE_DETECTION:
        code_1_raw, ast_1_raw, name_1_raw, code_2_raw, ast_2_raw, name_2_raw, labels = map(list, zip(*batch))

        model_inputs['input_ids'], model_inputs['attention_mask'] = get_concat_batch_inputs(
            code_raw=code_1_raw,
            code_vocab=code_vocab,
            max_code_len=args.max_code_len,
            ast_raw=ast_1_raw,
            ast_vocab=ast_vocab,
            max_ast_len=args.max_ast_len,
            nl_raw=name_1_raw,
            nl_vocab=nl_vocab,
            max_nl_len=args.max_nl_len,
            no_ast=args.no_ast,
            no_nl=args.no_nl
        )
        model_inputs['decoder_input_ids'], model_inputs['decoder_attention_mask'] = get_concat_batch_inputs(
            code_raw=code_2_raw,
            code_vocab=code_vocab,
            max_code_len=args.max_code_len,
            ast_raw=ast_2_raw,
            ast_vocab=ast_vocab,
            max_ast_len=args.max_ast_len,
            nl_raw=name_2_raw,
            nl_vocab=nl_vocab,
            max_nl_len=args.max_nl_len,
            no_ast=args.no_ast,
            no_nl=args.no_nl
        )
        model_inputs['labels'] = torch.tensor(labels, dtype=torch.long)
    # code completion
    # Concatenates code, AST, and NL inputs. Generates decoder inputs and labels for code completion.
    elif task == enums.TASK_COMPLETION:
    #It unpacks the batch into four lists: code_raw, ast_raw, name_raw, and target_raw. Each list contains data corresponding to the code, AST, method name, and target completion, respectively.
        code_raw, ast_raw, name_raw, target_raw = map(list, zip(*batch))
    #It calls get_concat_batch_inputs to concatenate the code, AST, and method name inputs and create input IDs and attention masks.
        model_inputs['input_ids'], model_inputs['attention_mask'] = get_concat_batch_inputs(
            code_raw=code_raw,
            code_vocab=code_vocab,
            max_code_len=args.max_code_len,
            ast_raw=ast_raw,
            ast_vocab=ast_vocab,
            max_ast_len=args.max_ast_len,
            nl_raw=name_raw,
            nl_vocab=nl_vocab,
            max_nl_len=args.max_nl_len,
            no_ast=args.no_ast,
            no_nl=args.no_nl
        )
        # print("Model Inputs:")
        # print("input_ids shape:", model_inputs['input_ids'].shape)
        # print("attention_mask shape:", model_inputs['attention_mask'].shape)
        # print("labels shape:", model_inputs['labels'].shape)

#It generates decoder inputs and labels for the code completion task.
#The decoder_input_ids and decoder_attention_mask are generated using get_batch_inputs with a maximum length specified by args.completion_max_len.
#The labels are also generated using get_batch_inputs, with the same maximum length.
        model_inputs['decoder_input_ids'], model_inputs['decoder_attention_mask'] = get_batch_inputs(
            batch=target_raw,
            vocab=code_vocab,
            processor=Vocab.sos_processor,
            max_len=args.completion_max_len
        )
        model_inputs['labels'], _ = get_batch_inputs(batch=target_raw,
                                                     vocab=code_vocab,
                                                     processor=Vocab.eos_processor,
                                                     max_len=args.completion_max_len)
        
        # print("input_ids shape:", model_inputs['decoder_input_ids'].shape)
        # print("attention_mask shape:", model_inputs['decoder_attention_mask'].shape)
        # print("labels shape:", model_inputs['labels'].shape)

    # bug fix
    #  Concatenates code, AST, and NL inputs. Generates decoder inputs and labels for bug fix.
    elif task == enums.TASK_BUG_FIX:
        code_raw, ast_raw, name_raw, target_raw = map(list, zip(*batch))
        max_code_len = 55 if args.bug_fix_scale == 'small' else 105
        model_inputs['input_ids'], model_inputs['attention_mask'] = get_concat_batch_inputs(
            code_raw=code_raw,
            code_vocab=code_vocab,
            max_code_len=max_code_len,
            ast_raw=ast_raw,
            ast_vocab=ast_vocab,
            max_ast_len=args.max_ast_len,
            nl_raw=name_raw,
            nl_vocab=nl_vocab,
            max_nl_len=args.max_nl_len,
            no_ast=args.no_ast,
            no_nl=args.no_nl
        )
        # print("Model Inputs:")
        # print("input_ids shape:", model_inputs['input_ids'].shape)
        # print("attention_mask shape:", model_inputs['attention_mask'].shape)
        # print("labels shape:", model_inputs['labels'].shape)
        
        model_inputs['decoder_input_ids'], model_inputs['decoder_attention_mask'] = get_batch_inputs(
            batch=target_raw,
            vocab=code_vocab,
            processor=Vocab.sos_processor,
            max_len=max_code_len
        )
        model_inputs['labels'], _ = get_batch_inputs(batch=target_raw,
                                                     vocab=code_vocab,
                                                     processor=Vocab.eos_processor,
                                                     max_len=max_code_len)
        
        # print("input_ids shape:", model_inputs['decoder_input_ids'].shape)
        # print("attention_mask shape:", model_inputs['decoder_attention_mask'].shape)
        # print("labels shape:", model_inputs['labels'].shape)

    return model_inputs

#This function encodes a batch of sequences into model inputs. It utilizes the given vocabulary and optional post-processor to tokenize and encode the sequences
def get_batch_inputs(batch: List[str], vocab: Vocab, processor=None, max_len=None):
    """
    Encode the given batch to input to the model.

    Args:
        batch (list[str]): Batch of sequence,
            each sequence is represented by a string or list of tokens
        vocab (Vocab): Vocab of the batch
        processor (tokenizers.processors.PostProcessor): Optional, post-processor method
        max_len (int): Optional, the maximum length of each sequence

    Returns:
        (torch.LongTensor, torch.LongTensor): Tensor of batch and mask, [B, T]

    """
    # set post processor
    vocab.tokenizer.post_processor = processor
    # set truncation
    if max_len:
        vocab.tokenizer.enable_truncation(max_length=max_len)
    else:
        vocab.tokenizer.no_truncation()
    # encode batch
    inputs, padding_mask = vocab.encode_batch(batch, pad=True, max_length=max_len)
    # to tensor
    inputs = torch.tensor(inputs, dtype=torch.long)
    padding_mask = torch.tensor(padding_mask, dtype=torch.long)

    return inputs, padding_mask

#This function concatenates inputs from code, abstract syntax tree (AST), and natural language (NL) into a single tensor along with their respective padding masks.
def get_concat_batch_inputs(code_raw, code_vocab, max_code_len,
                            ast_raw, ast_vocab, max_ast_len,
                            nl_raw, nl_vocab, max_nl_len,
                            no_ast=False, no_nl=False):
    """
    Return the concat tensor and mask for input.

    Args:
        code_raw:
        code_vocab:
        max_code_len:
        ast_raw:
        ast_vocab:
        max_ast_len:
        nl_raw:
        nl_vocab:
        max_nl_len:
        no_ast:
        no_nl:

    Returns:
        (torch.Tensor, torch.Tensor):
            - Concat inputs
            - concat attention mask

    """
    # print("Concatenating inputs...")
    # print("Code raw:", code_raw)
    # print("AST raw:", ast_raw)
    # print("NL raw:", nl_raw)
    code_inputs, code_padding_mask = get_batch_inputs(batch=code_raw,
                                                      vocab=code_vocab,
                                                      processor=Vocab.sep_processor,
                                                      max_len=max_code_len)

    if not no_ast:
        ast_inputs, ast_padding_mask = get_batch_inputs(batch=ast_raw,
                                                        vocab=ast_vocab,
                                                        processor=Vocab.sep_processor,
                                                        max_len=max_ast_len)
        # print("AST inputs shape:", ast_inputs.shape)
        # print("AST padding mask shape:", ast_padding_mask.shape)
    else:
        ast_inputs, ast_padding_mask = None, None

    if not no_nl:
        nl_inputs, nl_padding_mask = get_batch_inputs(batch=nl_raw,
                                                      vocab=nl_vocab,
                                                      processor=Vocab.eos_processor,
                                                      max_len=max_nl_len)
        print("NL inputs shape:", nl_inputs.shape)
        print("NL padding mask shape:", nl_padding_mask.shape)
    else:
        nl_inputs, nl_padding_mask = None, None

    inputs = torch.cat([inputs for inputs in [code_inputs, ast_inputs, nl_inputs] if inputs is not None], dim=-1)
    padding_mask = torch.cat([mask for mask in [code_padding_mask, ast_padding_mask, nl_padding_mask]
                              if mask is not None], dim=-1)

    # code_inputs, code_padding_mask = get_batch_inputs(batch=code_raw,
    #                                                   vocab=code_vocab,
    #                                                   processor=Vocab.sep_processor,
    #                                                   max_len=max_code_len)
    # ast_inputs, ast_padding_mask = get_batch_inputs(batch=ast_raw,
    #                                                 vocab=ast_vocab,
    #                                                 processor=Vocab.sep_processor,
    #                                                 max_len=max_ast_len)
    # nl_inputs, nl_padding_mask = get_batch_inputs(batch=nl_raw,
    #                                               vocab=nl_vocab,
    #                                               processor=Vocab.eos_processor,
    #                                               max_len=max_nl_len)
    #
    # inputs = torch.cat([code_inputs, ast_inputs, nl_inputs], dim=-1)
    # padding_mask = torch.cat([code_padding_mask, ast_padding_mask, nl_padding_mask], dim=-1)

    # print("Concatenated inputs shape:", inputs.shape)
    # print("Concatenated padding mask shape:", padding_mask.shape)
    return inputs, padding_mask

#this function pads a list of sequences to create a padded 2D tensor.
def pad_batch(batch, pad_value=0):
    """
    Pad a list of sequence to a padded 2d tensor.

    Args:
        batch (list[list[int]]): List of sequence
        pad_value (int): Optional, fill value, default to 0.

    Returns:
        torch.Tensor: Padded tensor. [B, T].

    # """
    # print("Padding batch...")
    # print("Input batch:", batch)
    # print("Pad value:", pad_value)
    batch = list(zip(*itertools.zip_longest(*batch, fillvalue=pad_value)))
    # print("After padding:", batch)
    
    return torch.tensor([list(b) for b in batch]).long()
