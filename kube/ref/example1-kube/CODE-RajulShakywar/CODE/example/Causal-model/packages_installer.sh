#!/bin/bash

pip3 install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu118
# conda install numpy pandas matplotlib
# conda install conda-forge::git-lfs
python -m pip install scikit-learn transformers datasets sentencepiece sacremoses accelerate
pip install --upgrade huggingface_hub
pip install 'huggingface_hub[cli,torch]'
pip install ipywidgets
# conda install ipykernel

# from collections import defaultdict
# from tqdm import tqdm
# from datasets import Dataset, load_dataset, DatasetDict
# from transformers import AutoTokenizer, GPT2LMHeadModel, AutoConfig, DataCollatorForLanguageModeling
# # from transformers import Trainer, TrainingArguments
# import torch
# # from transformers import pipeline
# from torch.nn import CrossEntropyLoss
# from torch.utils.data.dataloader import DataLoader
# from torch.optim import AdamW
# from accelerate import Accelerator
# from transformers import get_scheduler
# from huggingface_hub import Repository, get_full_repo_name


# In[ ]:


from huggingface_hub import login

python -m ipykernel install --user --name "$CONDA_DEFAULT_ENV" --display-name "Python ($CONDA_DEFAULT_ENV)" --env PATH $PATH​