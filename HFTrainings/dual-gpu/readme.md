# Dual GPUs
### Command to run the program `train_mlm.py`
```
$ python train_mlm.py -train 10000 -test 1000 -ngpu 2 -logfile logfile-may19.log
```
### Elapsed time
<pre>
Elapsed time: 4 minutes 38 seconds
</pre>
### Program log for training and validing a model
```
$ python train_mlm.py -train 10000 -test 1000 -ngpu 2 -logfile logfile-may19.log
Map: 100%|█████████████████████████████████| 26909/26909 [00:06<00:00, 4098.27 examples/s]
Map: 100%|█████████████████████████████████| 26909/26909 [00:45<00:00, 593.07 examples/s]
Configurations:
+---------------+-------+
| Configuration | Value |
+---------------+-------+
| train_size    | 10000 |
| test_size     | 1000  |
+---------------+-------+
Map: 100%|███████████████████████████████████████████████████| 1000/1000 [00:00<00:00, 3359.94 examples/s]
Launching training on 2 GPUs.
 20%|████████████████████                                    | 157/785 [00:50<03:22,  3.11it/s]
>>> Epoch 0: Loss: 0.9816863536834717
>>> Epoch 0: Loss: 0.9816863536834717
>>> Epoch 0: Perplexity: 2.6689531803131104
>>> Epoch 0: Perplexity: 2.6689531803131104
>>> Epoch 0: Entropy: 6.920124053955078
>>> Epoch 0: Entropy: 6.920124053955078
 40%|████████████████████████████████████████                | 314/785 [01:45<02:36,  3.02it/s]
>>> Epoch 1: Loss: 0.9523355960845947
>>> Epoch 1: Loss: 0.9523355960845947
>>> Epoch 1: Perplexity: 2.5917561054229736
>>> Epoch 1: Perplexity: 2.5917561054229736
>>> Epoch 1: Entropy: 6.921031951904297
>>> Epoch 1: Entropy: 6.921031951904297
 60%|██████████████████████████████████████████████          | 471/785 [02:41<01:44,  3.00it/s]
>>> Epoch 2: Loss: 0.9277446269989014
>>> Epoch 2: Loss: 0.9277446269989014
>>> Epoch 2: Perplexity: 2.528799533843994
>>> Epoch 2: Perplexity: 2.528799533843994
>>> Epoch 2: Entropy: 6.917446136474609
>>> Epoch 2: Entropy: 6.917446136474609
 80%|█████████████████████████████████████████████████       | 628/785 [03:37<00:52,  2.99it/s]
>>> Epoch 3: Loss: 0.9115557670593262
>>> Epoch 3: Loss: 0.9115557670593262
>>> Epoch 3: Perplexity: 2.4881906509399414
>>> Epoch 3: Perplexity: 2.4881906509399414
>>> Epoch 3: Entropy: 6.916872978210449
>>> Epoch 3: Entropy: 6.916872978210449
100%|████████████████████████████████████████████████████████| 785/785 [04:33<00:00,  3.00it/s]
>>> Epoch 4: Loss: 0.9038363695144653
>>> Epoch 4: Loss: 0.9038363695144653
>>> Epoch 4: Perplexity: 2.469057083129883
>>> Epoch 4: Perplexity: 2.469057083129883
>>> Epoch 4: Entropy: 6.92379093170166
>>> Epoch 4: Entropy: 6.92379093170166
100%|████████████████████████████████████████████████████████| 785/785 [04:36<00:00,  2.84it/s]
100%|████████████████████████████████████████████████████████| 785/785 [04:36<00:00,  2.84it/s]
Elapsed time: 4 minutes 38 seconds
```
### GPU usage with 2 GPUs
<pre>
Every 2.0s: nvidia-smi                                  oisit-selab3: Mon May 20 10:49:32 2024

Mon May 20 10:49:32 2024
+---------------------------------------------------------------------------------------+
| NVIDIA-SMI 535.154.05             Driver Version: 535.154.05   CUDA Version: 12.2     |
|-----------------------------------------+----------------------+----------------------+
| GPU  Name                 Persistence-M | Bus-Id        Disp.A | Volatile Uncorr. ECC |
| Fan  Temp   Perf          Pwr:Usage/Cap |         Memory-Usage | GPU-Util  Compute M. |
|                                         |                      |               MIG M. |
|=========================================+======================+======================|
|   0  NVIDIA RTX A5000               Off | 00000000:17:00.0 Off |                  Off |
| 30%   47C    P2             208W / 230W |   9238MiB / 24564MiB |     90%      Default |
|                                         |                      |                  N/A |
+-----------------------------------------+----------------------+----------------------+
|   1  NVIDIA RTX A5000               Off | 00000000:65:00.0 Off |                  Off |
| 30%   49C    P2             206W / 230W |   9498MiB / 24564MiB |    100%      Default |
|                                         |                      |                  N/A |
+-----------------------------------------+----------------------+----------------------+
</pre>


# Single GPU
### Command
```
$ python train_mlm.py -train 10000 -test 1000 -ngpu 1 -logfile logfile-may19.log
```
### Elapsed time
<pre>
Elapsed time: 8 minutes 15 seconds
</pre>
### Program log for training and validing a model
```
$ python train_mlm.py -train 10000 -test 1000 -ngpu 1 -logfile logfile-may19.log
Map: 100%|████████████████████████████████████████| 15328/15328 [00:03<00:00, 4451.58 examples/s]
Map: 100%|█████████████████████████████████████████| 15328/15328 [00:21<00:00, 697.52 examples/s]
Configurations:
+---------------+-------+
| Configuration | Value |
+---------------+-------+
| train_size    | 10000 |
| test_size     | 1000  |
+---------------+-------+
Launching training on one GPU.
 20%|███████████████████▊                                                | 313/1565 [01:32<05:24,  3.86it/s]
>>> Epoch 0: Loss: 1.013965368270874
>>> Epoch 0: Perplexity: 2.756510019302368
>>> Epoch 0: Entropy: 6.909434795379639
 40%|████████████████████████▌                                           | 625/1565 [03:11<04:44,  3.30it/s]
>>> Epoch 1: Loss: 1.0084915161132812
>>> Epoch 1: Perplexity: 2.741462469100952
>>> Epoch 1: Entropy: 6.909267425537109
 60%|████████████████████████████▎                                       | 938/1565 [04:51<03:09,  3.30it/s]
>>> Epoch 2: Loss: 0.9056450724601746
>>> Epoch 2: Perplexity: 2.473526954650879
>>> Epoch 2: Entropy: 6.913277626037598
 80%|████████████████████████████████████████████████▎                   | 1251/1565 [06:30<01:35,  3.30it/s]
>>> Epoch 3: Loss: 0.9363067746162415
>>> Epoch 3: Perplexity: 2.550544500350952
>>> Epoch 3: Entropy: 6.911128520965576
100%|███████████████████████████████████████████████████████████████████▉| 1564/1565 [08:10<00:00,  3.31it/s]
>>> Epoch 4: Loss: 0.8990395069122314
>>> Epoch 4: Perplexity: 2.4572417736053467
>>> Epoch 4: Entropy: 6.918934345245361
100%|████████████████████████████████████████████████████████████████████| 1565/1565 [08:14<00:00,  3.16it/s]
Elapsed time: 8 minutes 15 seconds
```
### GPU usage with 1 GPU
<pre>
Every 2.0s: nvidia-smi                                  oisit-selab3: Mon May 20 10:50:16 2024

Mon May 20 10:50:17 2024
+---------------------------------------------------------------------------------------+
| NVIDIA-SMI 535.154.05             Driver Version: 535.154.05   CUDA Version: 12.2     |
|-----------------------------------------+----------------------+----------------------+
| GPU  Name                 Persistence-M | Bus-Id        Disp.A | Volatile Uncorr. ECC |
| Fan  Temp   Perf          Pwr:Usage/Cap |         Memory-Usage | GPU-Util  Compute M. |
|                                         |                      |               MIG M. |
|=========================================+======================+======================|
|   0  NVIDIA RTX A5000               Off | 00000000:17:00.0 Off |                  Off |
| 30%   63C    P2             211W / 230W |   8656MiB / 24564MiB |     87%      Default |
|                                         |                      |                  N/A |
+-----------------------------------------+----------------------+----------------------+
|   1  NVIDIA RTX A5000               Off | 00000000:65:00.0 Off |                  Off |
| 30%   50C    P8              18W / 230W |    318MiB / 24564MiB |      0%      Default |
|                                         |                      |                  N/A |
+-----------------------------------------+----------------------+----------------------+
</pre>
