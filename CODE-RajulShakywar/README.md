# Step 1 : Install the kubectl and k9s tool

**I.Download kubectl binary**
- `curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl`

**II. Make kubectl executable**
- `chmod +x ./kubectl`

**III. Move kubectl to /usr/local/bin**
- `sudo mv ./kubectl /usr/local/bin/kubectl`

**IV. Upload config**
- Upload the config file from [NRP portal](https://portal.nrp-nautilus.io/) and execute NautalisConfigSetup.ipny setup

**V. Install K9s tool**
- `curl -sS https://webi.sh/k9s | sh`
- `export PATH="$HOME/.local/bin:$PATH"`
- `k9s`

# Step 2 : Run the examples

**I. Ensure that any existing resources are deleted if they are already running**
- `kubectl delete pods --all && kubectl delete jobs --all`
- `kubectl delete pvc --all`

**II. Create a persistent volume claim (PVC)**
- `kubectl apply -f .pvc.yml`
- `kubectl get pvc`

**III. Create pod with volume mount as a recent pvc**
- `kubectl apply -f pod.yml`
- `kubectl get pods`

**IV. Copy randomforest.py and VitCifar.py scripts to the volume mounted by pod once it starts running**
- `kubectl cp ./RandomForestMNIST.py <pod-name>:/data/RandomForestMNIST.py`
- `kubectl cp ../scripts/ViTCifar10.py <pod-name>:/data/ViTCifar10.py`
- `kubectl exec gp-engine-unoselab01-pod1 -- cat /data RandomForestMNIST.py`
- `kubectl exec <pod-name> -- cat /data/ViTCifar10.py`

**V. Create jobs**
- `kubectl create -f ./sklearn_job.yml`
- `kubectl create -f ./vit_cifar10_job.yml`

**VI. Check logs**
- `kubectl get pods`
- `kubectl logs <pod-name> --tail=1`
- `kubectl logs <pod-name> --tail=5`

# Problem 1 # Running Causal-model python script as a kubernetes job 

**I. Ensure that any existing resources are deleted if they are already running**
- `kubectl delete pods --all && kubectl delete jobs --all`
- `kubectl delete pvc --all`

**II. Create a persistent volume claim (PVC)**
- `kubectl apply -f .pvc.yml`
- `kubectl get pvc`

**III. Create pod with volume mount as a recent pvc**
- `kubectl apply -f pod_pvc.yml`
- `kubectl get pods`

**IV. Copy run_install.sh script for dependencies installation and causal-model python script scripts to the volume mounted by pod once it starts running**
- `!kubectl cp ./scripts/causal-model-a.py gp-engine-unoselab01-pod1:/data/causal-model-a.py`
- `!kubectl cp ./scripts/run_install.sh gp-engine-unoselab01-pod1:/data/run_install.sh `

**VI._Note_ - We need to configure causal-model job with root permissions unless no File I/O opertation can be performed inside job container**

**VII.Configure causal_model_job.yaml so that it runs the container with root permissions to perform write operations within container, run job container with infinite sleep (sleep infinity) bash command in causal_model_job.yaml**
- `!kubectl create -f ./causal_model_job.yml`

**VIII.Find the security context parameters of pod once job starts running**
- `!kubectl exec job-casual-model-gp-engine-unoselab01`
- `id`

**IX.Find runAsUser parameter id and add security context configuration in causal_model_job.yaml**
- `runAsUser: id`
- `allowPrivilegeEscalation: false`

**X.Delete existing job and create a new one with updated security context along with command for running run_install.sh and causal-model-a.py scripts**
- `kubectl delete job <job-name>`
- `!kubectl create -f ./causal_model_job.yml`

**XI. Open k9s, select the namespace, job and monitor job container logs**
- `k9s`
