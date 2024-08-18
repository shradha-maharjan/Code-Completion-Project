#!/bin/bash

#!/bin/bash
set -e
cd /data

echo "Activating environment..."
eval "$(conda shell.bash hook)"
conda activate env_mlm

echo "Installing dependencies..."
pip install psutil gputil;
pip install scikit-learn;
pip install prettytable;
pip install dataclasses;
pip install chardet;
pip install rouge==1.0.0;
pip install accelerate;
pip install typing;
pip install antlr4-tools;
conda install -c conda-forge tensorboard;
pip install tree_sitter==0.19.0;
pip install antlr4-python3-runtime==4.9.2;
pip install nltk;
echo '--------------------------------------------------------'
echo 'Installation completed'
echo '--------------------------------------------------------'

cd /data/deploy-spt-code/spt-code/sources;
python main.py --do-pre-train --pre-train-tasks cap --batch-size 16 --eval-batch-size 32 --cuda-visible-devices 0,1 --fp16 --model-name pre_train --n-epoch 1 --n-epoch-pre-train 1 --pre-train-subset-ratio 0.1 --parse-subset-ratio 0.01 --task completion --remove-existing-saved-file pre_train:fine_tune --ast-type "jdt";

# conda env create -f env_mlm.yml
# echo $SHELL
# conda init bash
# # source .bashrc
# conda activate env_mlm
