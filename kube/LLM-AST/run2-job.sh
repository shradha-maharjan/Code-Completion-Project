#!/bin/bash

POD_NAME="shradha-llmast-gp-engine-unoselab01-pod"
SCRIPT_PATH="/data"
JOB_YAML="./3-kube-job-llm-ast-v2.yml"
LLM_AST_DIR="/home/user1-selab3/Documents/research-shradha/deploy-spt-code"
# PYTHON_PROGRAM=""
# PYTHON_PROGRAM="py_ver.py"

echo "----------------------------------------------------------------------------"
echo "Copying $LLM_AST_DIR to the pod $POD_NAME"
kubectl cp $LLM_AST_DIR $POD_NAME:$SCRIPT_PATH/
# kubectl cp ./scripts/$PYTHON_PROGRAM $POD_NAME:$SCRIPT_PATH/$PYTHON_PROGRAM
echo "----------------------------------------------------------------------------"
echo "----------------------------------------------------------------------------"
echo "Creating Kubernetes job from $JOB_YAML"
kubectl create -f $JOB_YAML
echo "----------------------------------------------------------------------------"
echo "----------------------------------------------------------------------------"
echo "Done."
echo "----------------------------------------------------------------------------"
