#!/bin/bash

POD_NAME="gp-engine-unoselab01-pod2"
SCRIPT_PATH="/data"
JOB_YAML="./3-kube-job-mlm.yml"
PYTHON_PROGRAM="train_mlm.py"
# PYTHON_PROGRAM="py_ver.py"

echo "----------------------------------------------------------------------------"
echo "Copying $PYTHON_PROGRAM to the pod $POD_NAME"
kubectl cp ./scripts/$PYTHON_PROGRAM $POD_NAME:$SCRIPT_PATH/$PYTHON_PROGRAM
echo "----------------------------------------------------------------------------"
echo "----------------------------------------------------------------------------"
echo "Creating Kubernetes job from $JOB_YAML"
kubectl create -f $JOB_YAML
echo "----------------------------------------------------------------------------"
echo "----------------------------------------------------------------------------"
echo "Done."
echo "----------------------------------------------------------------------------"
