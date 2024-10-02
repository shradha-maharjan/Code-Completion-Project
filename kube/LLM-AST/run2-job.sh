#!/bin/bash


POD_NAME="shradha-llmast-gp-engine-unoselab02-pod01"
SCRIPT_PATH="/data"
JOB_YAML="./3-kube-job-llm-ast-v2.yml"
#JOB_YAML="./3-kube-job-llm-ast-v3.yml"
TAR_File="/home/user1-selab3/Documents/research-shradha/llm_ast_code_nongen_PR.tar.gz"
# PYTHON_PROGRAM=""
# PYTHON_PROGRAM="py_ver.py"

echo "----------------------------------------------------------------------------"
echo "Copying $TAR_File to the pod $POD_NAME"
kubectl cp $TAR_File $POD_NAME:$SCRIPT_PATH/ --kubeconfig ~/kube2/config
#kubectl exec -it $POD_NAME -- tar -xzf $SCRIPT_PATH/llm_ast_sptcode.tar.gz -C $SCRIPT_PATH
# kubectl cp ./scripts/$PYTHON_PROGRAM $POD_NAME:$SCRIPT_PATH/$PYTHON_PROGRAM
echo "----------------------------------------------------------------------------"
echo "----------------------------------------------------------------------------"
echo "Creating Kubernetes job from $JOB_YAML"
kubectl create -f $JOB_YAML --kubeconfig ~/kube2/config
echo "----------------------------------------------------------------------------"
echo "----------------------------------------------------------------------------"
echo "Done."
echo "----------------------------------------------------------------------------"


# kubectl exec  --stdin --tty shradha-llmast-gp-engine-unoselab01-pod02 -- /bin/bash

#!/bin/bash


# POD_NAME="shradha-llmast-gp-engine-unoselab02-pod02"
# SCRIPT_PATH="/datamid"
# JOB_YAML="./3-kube-job-llm-ast.yml"
# #JOB_YAML="./3-kube-job-llm-ast-v3.yml"
# TAR_File="/home/user1-selab3/Documents/research-shradha/llm_ast_code_nongen_withjava.tar.gz"
# # PYTHON_PROGRAM=""
# # PYTHON_PROGRAM="py_ver.py"

# echo "----------------------------------------------------------------------------"
# echo "Copying $TAR_File to the pod $POD_NAME"
# kubectl cp $TAR_File $POD_NAME:$SCRIPT_PATH/ --kubeconfig ~/kube2/config
# #kubectl exec -it $POD_NAME -- tar -xzf $SCRIPT_PATH/llm_ast_sptcode.tar.gz -C $SCRIPT_PATH
# # kubectl cp ./scripts/$PYTHON_PROGRAM $POD_NAME:$SCRIPT_PATH/$PYTHON_PROGRAM
# echo "----------------------------------------------------------------------------"
# echo "----------------------------------------------------------------------------"
# echo "Creating Kubernetes job from $JOB_YAML"
# kubectl create -f $JOB_YAML --kubeconfig ~/kube2/config
# echo "----------------------------------------------------------------------------"
# echo "----------------------------------------------------------------------------"
# echo "Done."
# echo "----------------------------------------------------------------------------"