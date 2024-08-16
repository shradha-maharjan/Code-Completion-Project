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
pip3 install torch torchvision torchaudio
conda install --yes --file requirements.txt
pip install rouge==1.0.0
pip install accelerate
pip install typing
pip install antlr4-tools
conda install -c conda-forge tensorboard
pip install tree_sitter==0.19.0
pip install --upgrade tree-sitter
pip install antlr4-python3-runtime==4.9.2
python -m pip install transformers
echo '--------------------------------------------------------'
echo 'Installation completed'
echo '--------------------------------------------------------'

# conda env create -f env_mlm.yml
# echo $SHELL
# conda init bash
# # source .bashrc
# conda activate env_mlm
