## Training large language models with multiple GPUs on Kubernetes

- [Update1](#update1)


| Index    | # GPU     | # Training Data Size | # Testing Data Size | # Epoch   | Time       |
|----------|-----------|----------------------|---------------------|-----------|------------|
| 1        | 1         | 100,000              | 10,000              | 30        | Incomplete |
| 2        | 5         | 100,000              | 10,000              | 30        | 673 m 58 s (11:13:58) |
| 3        | 10        | 100,000              | 10,000              | 30        | 96 m 49 s (01:36:49) |
| 4        | 15        | 100,000              | 10,000              | 30        | 89 m 45 s (01:29:45) |


| Index | # GPU | # Training Data Size | # Testing Data Size | # Epoch | Time    |
|-------|-------|----------------------|---------------------|---------|---------|
| 1     | 1     | 20,000               | 2,000               | 5       | 22m 43s |
| 2     | 2     | 20,000               | 2,000               | 5       | 5m 21s  |



## How to run a python script train_mlm.py as a kubernetes job 

**I. Create a persistent volume claim (PVC)**
- `kubectl apply -f pvc.yml`
- `kubectl get pvc`

**II. Create pod with volume mount as a recent pvc**
- `kubectl apply -f pod_pvc.yml`
- `kubectl get pods`

**III. Copy run_install.sh script for dependencies installation and train_mlm python script scripts to the volume mounted by pod once pod starts running**
- `kubectl cp ./scripts/run_install.sh gp-engine-unoselab01-pod1:/data/run_install.sh`
- `kubectl cp ./scripts/train_mlm.py gp-engine-unoselab01-pod1:/data/train_mlm.py`
- `kubectl cp ./scripts/env_mlm.yml gp-engine-unoselab01-pod1:/data/env_mlm.yml`

**IV. Replace the {number_of_gpus} with number of desired gpus to run the job in train_mlm_job.yml**

**IV. Create train_mlm job**
- `kubectl create -f ./train_mlm_job.yml`

**V. Check the logs of the newly created pod and resolve any errors or exceptions if there are any, and wait for the job to be completed**
- `k9s`
- `kubectl logs <job-pod-name>`

**VI. Open k9s, select the namespace, job and monitor job container logs**
- `k9s`

## Update1

**1. The docker file to create an image for CUDA and Conda environment**

- `sudo docker image build -t env_mlm:v1 -f Dockerfile_mlm .`

The command builds a Docker image named `env_mlm:v1` using the `Dockerfile_mlm` located in the current directory (`.`). It tags the resulting image with `env_mlm:v1`, to be identified and used later for running containers based on this image.

**2. Deployment for other Container Registry users**

- `sudo docker login ghcr.io -u unose`

The command initiates a login process to the GitHub Container Registry (GHCR) using Docker. It prompts for your GitHub username and password a personal access token to authenticate your Docker client with GHCR. This authentication is necessary to push and pull Docker images from repositories hosted on GHCR.

- `sudo docker tag env_mlm:v1 ghcr.io/unose/env_mlm:v1`

The command creates a new tag for the Docker image `env_mlm:v1` with the repository location `ghcr.io/unose/env_mlm:v1`. This allows you to associate the existing local image (`env_mlm:v1`) with a new repository and tag (`ghcr.io/unose/env_mlm:v1`) within the GitHub Container Registry (GHCR). This step is typically done before pushing the image to GHCR to ensure it is correctly identified within the registry.

- `sudo docker image push ghcr.io/unose/env_mlm:v1`

The command pushes the Docker image tagged as `ghcr.io/unose/env_mlm:v1` to the GitHub Container Registry (GHCR). This operation makes the image available for deployment and use by others who have appropriate access permissions to the repository `unose/env_mlm` on GHCR. 

**3. The docker file for the version 2**

- Updated to execute the program `train_mlm.py`.

<pre>
Before:

ENTRYPOINT ["/bin/bash", "-c", "source activate env_mlm && python /py_ver.py"]

After:

ENTRYPOINT ["/bin/bash", "-c", "source activate env_mlm && accelerate launch --multi_gpu --num_processes 1 train_mlm.py -train 1000 -test 100 -ngpu 1 -epoch 3 -logfile log.txt"]
</pre>

- The `sudo docker image build -t env_mlm:v2 -f Dockerfile_mlm .` command failed in MacOS. Switched to the server (selab2) to run the command, which was completed successfully.

- `sudo docker image ls`

- `docker login gitlab-registry.nrp-nautilus.io -u msong`

- `sudo docker tag env_mlm:v2 gitlab-registry.nrp-nautilus.io/msong/research/env_mlm:v2`

- `sudo docker push gitlab-registry.nrp-nautilus.io/msong/research/env_mlm:v2`

The command failed. Switched to GHCR.

- `docker login ghcr.io -u unose`

- `sudo docker tag env_mlm:v2 ghcr.io/unose/env_mlm:v2`

- `sudo docker push ghcr.io/unose/env_mlm:v2`

Switch to MacOS

- `sudo docker pull ghcr.io/unose/env_mlm:v2`

This command downloads the Docker image `env_mlm` with the version tag `v2` from the GitHub Container Registry (`ghcr.io/unose`).

- `sudo docker tag ghcr.io/unose/env_mlm:v2 gitlab-registry.nrp-nautilus.io/msong/research/env_mlm:v2`

This command creates a new tag for the downloaded Docker image, renaming it so it can be pushed to another registry, in this case, the GitLab Container Registry (`gitlab-registry.nrp-nautilus.io/msong/research`). The new tag is also `v2`.

- `docker login gitlab-registry.nrp-nautilus.io -u msong`

- `docker push gitlab-registry.nrp-nautilus.io/msong/research/env_mlm:v2`

Do not use the prefix `sudo`. Otherwise, the command fails.