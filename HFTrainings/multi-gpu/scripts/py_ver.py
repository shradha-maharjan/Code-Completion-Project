import platform
import transformers
import torch
import datasets

print(f"Transformers version: {transformers.__version__}")
print(f"Torch version: {torch.__version__}")
print(f"Datasets version: {datasets.__version__}")
version = platform.python_version()
print(f"Python version: {version}")
