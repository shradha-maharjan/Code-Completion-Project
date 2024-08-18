#!/bin/bash

POD_NAME="shradha-mlm-gp-engine-unoselab01-pod"
SCRIPT_PATH="/data"
JOB_YAML="./3-kube-job-mlm.yml"
PYTHON_PROGRAM="train_mlm.py"
# PYTHON_PROGRAM="py_ver.py"

echo "----------------------------------------------------------------------------"
echo "Copying $PYTHON_PROGRAM to the pod $POD_NAME"
#kubectl cp $LLM_AST_DIR $POD_NAME:$SCRIPT_PATH/deploy-spt-code
kubectl cp $PYTHON_PROGRAM $POD_NAME:$SCRIPT_PATH/$PYTHON_PROGRAM
echo "----------------------------------------------------------------------------"
echo "----------------------------------------------------------------------------"
echo "Creating Kubernetes job from $JOB_YAML"
kubectl create -f $JOB_YAML
echo "----------------------------------------------------------------------------"
echo "----------------------------------------------------------------------------"
echo "Done."
echo "----------------------------------------------------------------------------"

# kubectl logs -f pod-job-id (e.g., job03-mlm-shradha-gp-engine-unoselab01-bsdcd)

# Access the pod with a shell.
# kubectl exec --stdin --tty shradha-mlm-gp-engine-unoselab01-pod -- /bin/bash