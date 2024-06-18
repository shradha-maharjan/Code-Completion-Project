from collections import defaultdict
from tqdm import tqdm
from datasets import Dataset, load_dataset, DatasetDict
from transformers import AutoTokenizer, GPT2LMHeadModel, AutoConfig, DataCollatorForLanguageModeling
from transformers import Trainer, TrainingArguments
import torch
from torch.nn import CrossEntropyLoss
from torch.utils.data.dataloader import DataLoader
from torch.optim import AdamW
from accelerate import Accelerator,notebook_launcher
from transformers import get_scheduler
from huggingface_hub import Repository, get_full_repo_name
from transformers import AutoModelForMaskedLM
from transformers import default_data_collator
import collections
import numpy as np
import math
import time
import argparse
import logging
import os
import sys
from prettytable import PrettyTable 


from datasets import config
cache_dir = config.HF_DATASETS_CACHE
print(f"Dataset cache directory: {cache_dir}")
# -----------------------------------------------------------

import shutil
import os
dataset_name = "code_search_net"
dataset_cache_dir = os.path.join(cache_dir, dataset_name)
shutil.rmtree(dataset_cache_dir, ignore_errors=True)
print(f"Delete dataset cache directory: {cache_dir}")
# -----------------------------------------------------------

print('[DBG] Begin load_dataset code_search_net')
codesearchnet_dataset = load_dataset("code_search_net", "java")

print('[DBG] End load_dataset code_search_net')
