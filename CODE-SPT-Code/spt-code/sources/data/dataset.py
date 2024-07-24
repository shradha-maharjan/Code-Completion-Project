import torch.utils.data
from torch.utils.data.dataset import Dataset

import os
import random
import logging
import pickle
import shutil

import enums
from .data_utils import load_dataset_from_dir, set_args, \
    parse_for_summarization, parse_for_translation, parse_for_search, parse_for_clone, parse_for_completion, \
    parse_for_bug_fix
from .data_util_jdt import load_lines_from_file, load_ast_from_file_jdt, compare_and_save_sources, compare_similarity #, compare_and_save_sources_completion
from eval.bleu.google_bleu import avg_bleu
from data.vocab import Vocab

logger = logging.getLogger(__name__)


class CodeDataset(Dataset):

    def __init__(self, args, dataset_name, mode, task=None, language=None, split=None, clone_mapping=None):
        """
        Initialization definition.

        Args:
            args (argparse.Namespace): Arguments
            dataset_name (str): Name of the dataset
            mode (str): Training mode, ``pre_train`` or ``fine_tune``
            task (str): Dataset mode, support pre-training tasks: ['cap', 'mass', 'mnp'],
                and downstream fine-tuning task: ['summarization', 'translation'],
                future support ['completion', 'search', 'clone', 'summarization', 'translation']
            language (str): Only for downstream fine-tuning
            split (str): Only for downstream fine-tuning, support ['train', 'valid', 'test', 'codebase']
            clone_mapping (dict[int, str]): Mapping from code id to source code string, use only for clone detection

        """
        super(CodeDataset, self).__init__()
        self.args = args
        self.dataset_name = dataset_name
        self.task = task
        self.mode = mode
        self.split = split
        self.paths = {}

        # dataset dir for files, all files in this dir meeting the filename will be used as dataset files
        self.dataset_dir = os.path.join(args.dataset_root, self.mode)
            
        # load pre-training dataset
        if self.mode == 'pre_train':
            set_args(args=args)
            print("Loading dataset from directory:", self.dataset_dir)
            self.paths, self.languages, self.sources, self.codes, self.asts, self.names, self.codes_wo_name, \
                self.names_wo_name, self.only_names, self.docs = load_dataset_from_dir(dataset_dir=self.dataset_dir)
            self.size = len(self.codes)

            # with open('pre_train_sources.txt', 'w') as src_file:
            #     for source in self.sources:
            #         formatted_source = source.replace('\n', ' ').replace('\t', ' ').replace('\r', ' ')
            #         src_file.write(f"{formatted_source}\n")
            
            if args.ast_type == "jdt":
                self.jdt_file_path = args.jdt_file_path
                print("JDT flag is set, loading ASTs from:", self.jdt_file_path)
                # Unpacking both sources and ASTs returned by the method
                sources_from_file, self.asts = load_ast_from_file_jdt(self.jdt_file_path, context = 'load_dataset')

                # # Save loaded sources and ASTs to a file on a single line
                # with open('loaded_data.txt', 'w') as file:
                #     for source, ast in zip(sources_from_file, self.asts):
                #         file.write(f"Loaded source: {source}, Loaded AST: {ast}\n")
                    
                # compare_and_save_sources(self,sources_from_file, self.asts)

                compare_and_save_sources(self, sources_from_file, self.asts, 'sources', 'mismatched_sources.txt')

            # # Optional saving of source-AST pairs for verification
            # print("Saving source-AST pairs for verification...")
            # self.save_source_ast_pairs()

            # comparing two results b/w JDT result and self.sourses
            # for i in jdt_result, self.sources:
            #   code, ast = jdt_result[i]
            #   src = self.sources[i]
            #   code = remove_whitespaces(lowercase(code))
            #   src = remove_whitespaces(lowercase(src))
            #   if compare(code, src) == False:
            #      save(code, src, ast)
            
            # sys.exit(0)

        # load fine-tuning dataset
        else:
            assert split
            logger.info(f'  Loading {split} set')
            self.dataset_dir = os.path.join(self.dataset_dir, task)
            # code summarization
            if task == enums.TASK_SUMMARIZATION:
                assert language, '\'Language\' must be specific if downstream task is code summarization'
                assert split in ['train', 'valid', 'test']
                self.dataset_dir = os.path.join(self.dataset_dir, language, split)

                self.source_path = os.path.join(self.dataset_dir, 'source.code')
                self.code_path = os.path.join(self.dataset_dir, 'token.code')
                self.nl_path = os.path.join(self.dataset_dir, 'token.docstring')

                self.paths, self.codes, self.asts, self.names, self.nls = parse_for_summarization(
                    source_path=self.source_path,
                    code_path=self.code_path,
                    nl_path=self.nl_path,
                    lang=language)
                assert len(self.codes) == len(self.asts) == len(self.names) == len(self.nls)
                self.size = len(self.codes)
            # code translation
            elif task == enums.TASK_TRANSLATION:
                assert split in ['train', 'valid', 'test']
                assert language in ['java-c_sharp', 'c_sharp-java']
                source_lang, target_lang = language.split('-')
                java_path = f'{split}.java-cs.txt.java'
                c_sharp_path = f'{split}.java-cs.txt.cs'
                source_path = os.path.join(self.dataset_dir,
                                           c_sharp_path if source_lang == 'c_sharp' else java_path)
                target_path = os.path.join(self.dataset_dir,
                                           c_sharp_path if target_lang == 'c_sharp' else java_path)
                self.paths['source'] = source_path
                self.paths['target'] = target_path
                self.codes, self.asts, self.names, self.targets = parse_for_translation(
                    source_path=source_path,
                    source_lang=args.translation_source_language,
                    target_path=target_path,
                    target_lang=args.translation_target_language)

                assert len(self.codes) == len(self.asts) == len(self.names) == len(self.targets)
                self.size = len(self.codes)
            # code search
            elif task == enums.TASK_SEARCH:
                assert language, '``Language`` must be specific if downstream task is code search'
                assert split in ['codebase', 'train', 'valid', 'test']
                self.dataset_dir = os.path.join(self.dataset_dir, language)
                self.paths['file'] = os.path.join(self.dataset_dir, f'{split}.jsonl')

                if split == 'codebase':
                    set_args(args=args) # Added to pass args, myoungkyu song, 03/24/2024
                    self.urls, self.codes, self.asts, self.names = parse_for_search(dataset_dir=self.dataset_dir,
                                                                                    lang=language,
                                                                                    split=split)
                    assert len(self.urls), len(self.codes) == len(self.asts) == len(self.names)
                    self.size = len(self.urls)
                elif split == 'train':
                    self.codes, self.asts, self.names, self.nls = parse_for_search(dataset_dir=self.dataset_dir,
                                                                                   lang=language,
                                                                                   split=split)
                    assert len(self.codes) == len(self.asts) == len(self.names) == len(self.nls)
                    self.size = len(self.codes)
                else:
                    self.urls, self.nls = parse_for_search(dataset_dir=self.dataset_dir, lang=language, split=split)
                    self.size = len(self.urls)
            # code clone detection
            elif task == enums.TASK_CLONE_DETECTION:
                assert split in ['train', 'valid', 'test']
                assert clone_mapping
                path = os.path.join(self.dataset_dir, f'{split}.txt')
                self.paths['file'] = path
                self.codes_1, self.asts_1, self.names_1, \
                    self.codes_2, self.asts_2, self.names_2, self.labels = parse_for_clone(path=path,
                                                                                           mapping=clone_mapping)
                assert len(self.codes_1) == len(self.asts_1) == len(self.names_1) \
                       == len(self.codes_2) == len(self.asts_2) == len(self.names_2) == len(self.labels)
                self.size = len(self.codes_1)
            #completion old
            # elif task == enums.TASK_COMPLETION:
            #     assert split in ['train', 'valid', 'test']
            #     source_path = os.path.join(self.dataset_dir, f'data.TargetType.seq.{split}.source.txt')
            #     target_path = os.path.join(self.dataset_dir, f'data.TargetType.seq.{split}.target.txt')
            #     self.paths['source'] = source_path
            #     self.paths['target'] = target_path
            #     set_args(args=args) # Added to pass args, myoungkyu song, 03/31/2024
            #     self.codes, self.asts, self.names, self.targets = parse_for_completion(source_path=source_path,
            #                                                                         target_path=target_path)
            #     assert len(self.codes) == len(self.asts) == len(self.names) == len(self.targets)
            #     self.size = len(self.codes)
            elif task == enums.TASK_COMPLETION:
                assert split in ['train', 'valid', 'test']
                source_path = os.path.join(self.dataset_dir, f'source_tokenized_methods_{split}.txt')
                target_path = os.path.join(self.dataset_dir, f'target_tokenized_methods_{split}.txt')
                self.paths['source'] = source_path
                self.paths['target'] = target_path
                ast_path = os.path.join(self.dataset_dir, f'raw_methods_asts_{split}.txt')
                nl_path = os.path.join(self.dataset_dir, f'NL_methods_{split}.txt')
                set_args(args=args) # Added to pass args, myoungkyu song, 03/31/2024
                self.codes, self.asts, self.names, self.targets = parse_for_completion(source_path=source_path, target_path=target_path, ast_path=ast_path, nl_path=nl_path)
                assert len(self.codes) == len(self.asts) == len(self.names) == len(self.targets)
                self.size = len(self.codes)
                print("size:", self.size)
                print("Codes:", self.codes)
                print("Targets:", self.targets)
                print("Asts:", self.asts)
                print("Names:", self.names)

            #     print("parsing for debug:")
            #     source_path_debug = os.path.join(self.dataset_dir, f'source_methods_valid.txt')
            #     target_path_debug = os.path.join(self.dataset_dir, f'target_methods_valid.txt')
            #     self.paths['source'] = source_path
            #     self.paths['target'] = target_path
            #     set_args(args=args) # Added to pass args, myoungkyu song, 03/31/2024
            #     self.codes, self.asts, self.names, self.targets = parse_for_completion(source_path=source_path_debug,
            #                                                                            target_path=target_path_debug)
            #     assert len(self.codes) == len(self.asts) == len(self.names) == len(self.targets)
            #     self.size = len(self.codes)
            #     print("size:",self.size)
            #     print("Codes:", self.codes)
            #     print("Targets debug:", self.targets)

            # elif task == enums.TASK_COMPLETION:
            #     assert split in ['train', 'valid', 'test']
            #     source_path = os.path.join(self.dataset_dir, f'data.TargetType.seq.{split}.source.txt')
            #     target_path = os.path.join(self.dataset_dir, f'data.TargetType.seq.{split}.target.txt')
            #     self.paths['source'] = source_path
            #     self.paths['target'] = target_path
            #     set_args(args=args)  # Passing args
            #     self.codes, _, self.names, self.targets = parse_for_completion(source_path=source_path,
            #                                                                 target_path=target_path)
                
            #     if args.ast_type == "jdt":
            #         self.ast_file_path = os.path.join(args.ast_file_path, f'finetune_methods_{split}.txt')
            #         print(f"JDT flag is set, loading ASTs from: {self.ast_file_path} for {split} split")
            #         # Compare and get indices of matches
            #         matched_indices = compare_similarity(self.ast_file_path, self.codes)
            #         # Load ASTs only for matched indices
            #         self.asts = load_ast_from_file_jdt(self.ast_file_path, matched_indices, context='parse_for_completion')
            #         print(len(self.codes), len(self.asts), len(self.names), len(self.targets), "**")
            #         assert len(self.codes) == len(self.asts) == len(self.names) == len(self.targets)

#recent modifications
            # elif task == enums.TASK_COMPLETION:
            #     assert split in ['train', 'valid', 'test']
            #     source_path = os.path.join(self.dataset_dir, f'source_methods_{split}.txt')
            #     target_path = os.path.join(self.dataset_dir, f'target_methods_{split}.txt')
            #     self.paths['source'] = source_path
            #     self.paths['target'] = target_path
            #     set_args(args=args) # Added to pass args, myoungkyu song, 03/31/2024
            #     self.codes, self.asts, self.names, self.targets = parse_for_completion(source_path=source_path,
            #                                                                            target_path=target_path)
                
            #     print(len(self.codes))
            #     print(len(self.asts))
            #     print(len(self.names))
            #     print(len(self.targets))
            #     print("**")
            #     if args.ast_type == "jdt":
            #         self.ast_file_path = os.path.join(args.ast_file_path, f'raw_methods_asts_{split}.txt')  # Customize path as needed
            #         print(f"JDT flag is set, loading ASTs from: {self.ast_file_path} for {split} split")
            #         #sources_from_file, self.asts = load_ast_from_file_jdt(self.ast_file_path, context = 'parse_for_completion')
            #         self.asts = load_lines_from_file(self.ast_file_path)
                    
            #         #compare_and_save_sources(self, sources_from_file, self.asts, 'codes', 'mismatched_sources_completion.txt')
            #     assert len(self.codes) == len(self.asts) == len(self.names) == len(self.targets)
            #     self.size = len(self.codes)

            #     # JDT's AST expression with 6 text files in CODE-SPT-Code/dataset/fine_tune/completion
            #     # Given 'split', self.asts can be updated.
            #     # To parse data.TargetType.seq.test.source.txt (INPUT), find the corresponding raw java methods.
            #     # HOW
            #     # for method_raw in methods_of_raw_data:
            #     #    for input_method in methods_of_INPUT:
            #     #        when comparing 2 strings, use similarity values (threshold .7 .8 .9 ...) https://www.baeldung.com/java-apache-commons-text
            #     #        compare (remove_whitecase_undersc(lowercases(method_raw)), remove_whitecase_undersc(input_method)) == true
            #     #           return jdt_ast(method_raw)
            #     #  
            #     # self.asts = self.load_ast_from_file_jdt(self.jdt_file_path)
            #     # Do the similar logic.

            #     print(len(self.codes))
            #     print(len(self.asts))
            #     print(len(self.names))
            #     print(len(self.targets))
            #     # assert len(self.codes) == len(self.asts) == len(self.names) == len(self.targets)
            #     self.size = len(self.codes)
            # bug fix
            elif task == enums.TASK_BUG_FIX:
                assert split in ['train', 'valid', 'test']
                # language here stands for dataset scale
                assert language in ['small', 'medium']
                self.dataset_dir = os.path.join(self.dataset_dir, language)
                buggy_path = os.path.join(self.dataset_dir, f'{split}.buggy-fixed.buggy')
                fixed_path = os.path.join(self.dataset_dir, f'{split}.buggy-fixed.fixed')
                self.paths['buggy'] = buggy_path
                self.paths['fixed'] = fixed_path
                self.codes, self.asts, self.names, self.targets = parse_for_bug_fix(buggy_path=buggy_path,
                                                                                    fixed_path=fixed_path)
                assert len(self.codes) == len(self.asts) == len(self.names) == len(self.targets)
                self.size = len(self.codes)

    def __getitem__(self, index):
        # cap
        if self.task == enums.TASK_CODE_AST_PREDICTION:
            # print(f'[DBG] index: {index}')
            # org_ast = self.asts[index]
            # if index == 184215:
            #     self.keep_ast_index = 184215
            # elif index == 59442:
            #     print(f'[DBG] returned index: {self.keep_ast_index}')
            #     return self.codes[self.keep_ast_index], self.asts[self.keep_ast_index], self.names[self.keep_ast_index], 1

            is_ast = random.random() < 0.5
            if is_ast:
                return self.codes[index], self.asts[index], self.names[index], 1
            else:
                other_ast = self.asts[random.randint(0, self.size - 1)]
                while other_ast == self.asts[index]:
                    other_ast = self.asts[random.randint(0, self.size - 1)]
                return self.codes[index], other_ast, self.names[index], 0
        # mass
        elif self.task == enums.TASK_MASS:
            # print(f'[DBG] index: {index}')

            code_tokens = self.codes[index].split()
            mask_len = int(self.args.mass_mask_ratio * len(code_tokens))
            mask_start = random.randint(0, len(code_tokens) - mask_len)
            mask_tokens = code_tokens[mask_start: mask_start + mask_len]
            input_tokens = code_tokens[:mask_start] + [Vocab.MSK_TOKEN] + code_tokens[mask_start + mask_len:]
            # print(f'[DBG] code {code_tokens}')
            # print(f'[DBG] input {input_tokens}')
            # print(f'[DBG] mask {mask_tokens}')
            return ' '.join(input_tokens), self.asts[index], self.names[index], ' '.join(mask_tokens)
        # mnp
        elif self.task == enums.TASK_METHOD_NAME_PREDICTION:
            return self.codes_wo_name[index], self.asts[index], self.names_wo_name[index], self.names[index]
        # summarization
        elif self.task == enums.TASK_SUMMARIZATION:
            return self.codes[index], self.asts[index], self.names[index], self.nls[index]
            # return self.codes[index], None, None, self.nls[index]
        # translation
        elif self.task == enums.TASK_TRANSLATION:
            return self.codes[index], self.asts[index], self.names[index], self.targets[index]
        # search
        elif self.task == enums.TASK_SEARCH:
            if self.split == 'codebase':
                return self.split, self.urls[index], self.codes[index], self.asts[index], self.names[index]
            elif self.split == 'train':
                pos_nl = self.nls[index]
                # while True:
                #     neg_index = random.randint(0, self.size - 1)
                #     neg_nl = self.nls[neg_index]
                #     if avg_bleu(references=[pos_nl.split()], candidates=[neg_nl.split()]) < 0.5:
                #         break
                # return self.split, self.codes[index], self.asts[index], self.names[index], pos_nl, neg_nl
                return self.split, self.codes[index], self.asts[index], self.names[index], pos_nl
            else:
                return self.split, self.urls[index], self.nls[index]
        # clone detection
        elif self.task == enums.TASK_CLONE_DETECTION:
            return self.codes_1[index], self.asts_1[index], self.names_1[index], \
                   self.codes_2[index], self.asts_2[index], self.names_2[index], self.labels[index]
        # code completion
        elif self.task == enums.TASK_COMPLETION:
            return self.codes[index], self.asts[index], self.names[index], self.targets[index]
        # bug fix
        elif self.task == enums.TASK_BUG_FIX:
            return self.codes[index], self.asts[index], self.names[index], self.targets[index]

    def __len__(self):
        return self.size

    def set_task(self, task):
        self.task = task

    def save(self):
        """Save to binary pickle file"""
        path = os.path.join(self.args.dataset_save_dir, f'{self.dataset_name}.pk')
        with open(path, mode='wb') as f:
            pickle.dump(self, f)
        logger.info(f'Dataset saved to {path}')

    def subset(self, ratio):
        """
        Return a subset of self.

        Args:
            ratio (float): The ratio of size, must greater than 0 and less than/equal to 1

        Returns:
            Dataset: the subset

        """
        assert 0 < ratio <= 1, f'The subset ratio supposed to be 0 < ratio <= 1, but got ratio={ratio}'
        if ratio == 1:
            return self
        indices = random.sample(range(self.size), int(self.size * ratio))
        return torch.utils.data.Subset(self, indices)


def init_dataset(args, mode, task=None, language=None, split=None, clone_mapping=None,
                 load_if_saved=True) -> CodeDataset:
    """
    Find dataset, if the dataset is saved, load and return, else initialize and return.

    Args:
        args (argparse.Namespace): Arguments
        mode (str): Training mode, ``pre_train`` or ``fine_tune``
        task (str): Dataset mode, support pre-training tasks: ['cap', 'mass', 'mnp'],
            and downstream fine-tuning task: ['summarization', 'translation'],
            future support ['completion', 'search', 'clone', 'summarization', 'translation']
        language (str): Only for downstream fine-tuning
        split (str): Only for downstream fine-tuning, support ['train', 'valid', 'test', 'codebase(only for search)']
        clone_mapping (dict[int, str]): Mapping from code id to source code string, use only for clone detection
        load_if_saved (bool): Whether to load the saved instance if it exists, default to True

    Returns:
        CodeDataset: Loaded or initialized dataset

    """
    name = '.'.join([sub_name for sub_name in [mode, task, language, split] if sub_name is not None])
    if load_if_saved:
        path = os.path.join(args.dataset_save_dir, f'{name}.pk') # '../../dataset/dataset_saved/pre_train.pk'
        path_org = os.path.join(args.dataset_save_dir, f'{name}_org.pk') # '../../dataset/dataset_saved/pre_train_org.pk'

        # #######################################################################
        # Updated it with an argument `remove_existing_saved_file`, myoungkyu song, 03/23/2024
        if os.path.exists(path) and \
                (args.remove_existing_saved_file is not None and 'fine_tune' in args.remove_existing_saved_file) and \
                ('fine_tune' in path):
            logger.info(f'Removing the existing file: {path}')
            os.remove(path)
        if os.path.exists(path) and \
                (args.remove_existing_saved_file is not None and 'pre_train' in args.remove_existing_saved_file) and \
                ('pre_train' in path):
            logger.info(f'Removing the existing file: {path}')
            os.remove(path)
        if os.path.exists(path_org) and (args.copy_existing_saved_file is not None and 'pre_train_org' in args.copy_existing_saved_file) and ('pre_train' in path):
            logger.info(f'Copying the existing file: {path_org}')
            shutil.copy(path_org, path)
            
        # #######################################################################

        if os.path.exists(path) and os.path.isfile(path):
            logger.info(f'Trying to load saved binary pickle file from: {path}')
            with open(path, mode='rb') as f:
                obj = pickle.load(f)
            assert isinstance(obj, CodeDataset)
            obj.args = args
            logger.info(f'Dataset instance loaded from: {path}')
            print_paths(obj.paths)
            return obj
    dataset = CodeDataset(args=args,
                          dataset_name=name,
                          mode=mode,
                          task=task,
                          language=language,
                          split=split,
                          clone_mapping=clone_mapping)
    dataset.save()
    return dataset


def print_paths(paths):
    """
    Print paths.

    Args:
        paths (dict): Dict mapping path group to path string or list of path strings.

    """
    logger.info('Dataset loaded from these files:')
    for key, value in paths.items():
        if isinstance(value, list):
            for v in value:
                logger.info(f'  {key}: {v}')
        else:
            logger.info(f'  {key}: {value}')


def save_all_datasets(args):
    # logger.info('*' * 100)
    # logger.info('Pre-training dataset')
    # _ = init_dataset(args=args,
    #                  mode=enums.TRAINING_MODE_PRE_TRAIN,
    #                  load_if_saved=False)
    # summarization
    for lang in [enums.LANG_JAVA, enums.LANG_GO, enums.LANG_PHP, enums.LANG_PYTHON, enums.LANG_RUBY,
                 enums.LANG_JAVASCRIPT]:
        for split in ['train', 'valid', 'test']:
            logger.info('*' * 100)
            logger.info(f'Summarization - {lang} - {split}')
            _ = init_dataset(args=args,
                             mode=enums.TRAINING_MODE_FINE_TUNE,
                             task=enums.TASK_SUMMARIZATION,
                             language=lang,
                             split=split,
                             load_if_saved=False)
    # # translation
    # for lang in ['java-c_sharp', 'c_sharp-java']:
    #     for split in ['train', 'valid', 'test']:
    #         logger.info('*' * 100)
    #         logger.info(f'Translation - {lang} - {split}')
    #         _ = init_dataset(args=args,
    #                          mode=enums.TRAINING_MODE_FINE_TUNE,
    #                          task=enums.TASK_TRANSLATION,
    #                          language=lang,
    #                          split=split,
    #                          load_if_saved=False)
    # # clone
    # from .data_utils import load_clone_mapping
    # code_mapping = load_clone_mapping(args.dataset_root)
    # for split in ['train', 'valid', 'test']:
    #     logger.info('*' * 100)
    #     logger.info(f'Clone - {split}')
    #     _ = init_dataset(args=args,
    #                      mode=enums.TRAINING_MODE_FINE_TUNE,
    #                      task=enums.TASK_CLONE_DETECTION,
    #                      split=split,
    #                      clone_mapping=code_mapping,
    #                      load_if_saved=False)
    # # # search
    # for lang in [enums.LANG_JAVA, enums.LANG_GO, enums.LANG_PHP, enums.LANG_PYTHON, enums.LANG_RUBY,
    #              enums.LANG_JAVASCRIPT]:
    #     for split in ['codebase', 'train', 'valid', 'test']:
    #         logger.info('*' * 100)
    #         logger.info(f'Search - {lang} - {split}')
    #         _ = init_dataset(args=args,
    #                          mode=enums.TRAINING_MODE_FINE_TUNE,
    #                          task=enums.TASK_SEARCH,
    #                          language=lang,
    #                          split=split,
    #                          load_if_saved=False)
    # # completion
    # for split in ['train', 'valid', 'test']:
    #     logger.info('*' * 100)
    #     logger.info(f'Completion - {split}')
    #     _ = init_dataset(args=args,
    #                      mode=enums.TRAINING_MODE_FINE_TUNE,
    #                      task=enums.TASK_COMPLETION,
    #                      split=split,
    #                      load_if_saved=False)
