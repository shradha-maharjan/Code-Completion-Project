`Studying large lanuguage models with train_mlm.py using different numbers of GPUs on Kubernetes.`

| Index    | # GPU     | # Training Data Size | # Testing Data Size | # Epoch   | Time       |
|----------|-----------|----------------------|---------------------|-----------|------------|
| 1        | 1         | 100,000              | 10,000              | 30        | Incomplete |
| 2        | 5         | 100,000              | 10,000              | 30        | 673 m 58 s |
| 3        | 10        | 100,000              | 10,000              | 30        | 96 m 49 s  |
| 4        | 15        | 100,000              | 10,000              | 30        | 89 m 45 s  |


| Index | # GPU | # Training Data Size | # Testing Data Size | # Epoch | Time    |
|-------|-------|----------------------|---------------------|---------|---------|
| 1     | 1     | 20,000               | 2,000               | 5       | 22m 43s |
| 2     | 2     | 20,000               | 2,000               | 5       | 5m 21s  |


```
Please add a description how to perform this experiment study with 1 python script (train_mlm.py) and 3 kubernate YML files.
```

# Running mlm model python script as a kubernetes job 

**I. Create a persistent volume claim (PVC)**
- `kubectl apply -f .pvc.yml`
- `kubectl get pvc`

**II. Create pod with volume mount as a recent pvc**
- `kubectl apply -f pod_pvc.yml`
- `kubectl get pods`

**III. Copy run_install.sh script for dependencies installation and causal-model python script scripts to the volume mounted by pod once pod starts running**
- `!kubectl cp ./scripts/run_install.sh gp-engine-unoselab01-pod1:/data/run_install.sh`
- `!kubectl cp ./scripts/train_mlm.py gp-engine-unoselab01-pod1:/data/train_mlm.py`

**IV. Create train_mlm job**
- `!kubectl create -f ./train_mlm_job.yml`

**V. Check for logs of the newly created pod and resolve any erros or exceptions if there is any and wait for the job to be completed**
- `k9s`
- `!kubectl logs <job-pod-name>`

**VI. Open k9s, select the namespace, job and monitor job container logs**
- `k9s`

