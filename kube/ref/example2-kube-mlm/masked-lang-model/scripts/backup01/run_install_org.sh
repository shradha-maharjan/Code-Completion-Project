#!/bin/bash

pip3 install tqdm
conda install -y -c conda-forge git-lfs
git config --global user.email 'myoungkyu@unomaha.edu'
git config --global user.name 'Myoungkyu Song'
python -m pip install scikit-learn transformers datasets sentencepiece sacremoses accelerate
pip3 install --upgrade huggingface_hub
pip3 install 'huggingface_hub[cli,torch]'
pip3 install ipywidgets
pip3 install numpy pandas matplotlib
pip3 install ipykernel
pip3 install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu118
pip3 install prettytable
echo '--------------------------------------------------------'
echo 'Installation completed'
echo '--------------------------------------------------------'

# This script should be updated to use a ``conda virtual environment``.