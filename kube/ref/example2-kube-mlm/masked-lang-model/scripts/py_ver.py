import platform
import transformers
import torch
import datasets

import os
import multiprocessing

num_cores = os.cpu_count()
print(f"Number of CPU cores: {num_cores}")
print('-------------------------------------------------------')

# import psutil

# total_ram = psutil.virtual_memory().total
# total_ram_gb = total_ram / (1024**3)
# print(f"Total RAM: {total_ram_gb:.2f} GB")
# print('-------------------------------------------------------')

print(f"Transformers version: {transformers.__version__}")
print(f"Torch version: {torch.__version__}")
print(f"Datasets version: {datasets.__version__}")
version = platform.python_version()
print(f"Python version: {version}")
print('-------------------------------------------------------')
print('torch.cuda.is_available(): ', torch.cuda.is_available())
print('torch.cuda.device_count(): ', torch.cuda.device_count())
print('-------------------------------------------------------')

# sudo docker run --rm --name my-env-mlm-container env_mlm:v1