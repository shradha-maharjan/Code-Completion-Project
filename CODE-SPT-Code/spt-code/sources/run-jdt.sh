# Check if DATE is provided as a command-line argument
if [ $# -ne 1 ]; then
    echo "Usage: $0 <date> (e.g., ./run.sh 12-25-13-10PM)"
    exit 1
fi

DATE="$1"

# Run the command with the provided date
nohup python main.py --do-pre-train --pre-train-tasks cap --batch-size 16 --eval-batch-size 32 --cuda-visible-devices 0,1 --fp16 --model-name pre_train --n-epoch 1 --n-epoch-pre-train 1 --pre-train-subset-ratio 0.1 --task completion --remove-existing-saved-file pre_train:fine_tune --ast-type "jdt" > output-spt-code-2024-$DATE.log 2>&1
#--parse-subset-ratio 0.01