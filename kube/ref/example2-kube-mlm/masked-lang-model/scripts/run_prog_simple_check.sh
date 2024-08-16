CMD="accelerate launch --multi_gpu --num_processes 2 train_mlm.py -train 1000 -test 100 -ngpu 2 -epoch 3 -logfile log.txt"
eval $CMD
echo $CMD
