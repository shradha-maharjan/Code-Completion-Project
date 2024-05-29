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
