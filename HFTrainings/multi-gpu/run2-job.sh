#!/bin/bash

POD_NAME="gp-engine-unoselab01-pod2"
SCRIPT_PATH="/data"
JOB_YAML="./train_mlm_job_8gpu.yml"
PYTHON_PROGRAM="train_mlm.py"
# PYTHON_PROGRAM="py_ver.py"

echo "Copying run_install.sh to the pod..."
kubectl cp ./scripts/run_install.sh $POD_NAME:$SCRIPT_PATH/run_install.sh
echo "Copying $PYTHON_PROGRAM to the pod..."
kubectl cp ./scripts/$PYTHON_PROGRAM $POD_NAME:$SCRIPT_PATH/$PYTHON_PROGRAM
echo "Copying env_mlm.yml to the pod..."
kubectl cp ./scripts/env_mlm.yml $POD_NAME:$SCRIPT_PATH/env_mlm.yml

echo "Creating Kubernetes job from train_mlm_job_8gpu.yml..."
kubectl create -f $JOB_YAML

echo "All commands executed successfully."
