

POD_NAME="shradha-llmast-gp-engine-unoselab02-pod02-new-01"
SCRIPT_PATH="/datamid"
JOB_YAML="./3-kube-job-llm-ast.yml"
#JOB_YAML="./3-kube-job-llm-ast.yml"
TAR_File="/home/user1-selab3/Documents/llm_ast_nongen_PR_test.tar.gz"
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


# # kubectl exec  --stdin --tty shradha-llmast-gp-engine-unoselab01-pod02 -- /bin/bash

#!/bin/bash



# POD_NAME="shradha-llmast-gp-engine-unoselab02-pod02-new-ea"
# SCRIPT_PATH="/datalarge"
# JOB_YAML="./3-kube-job-llm-ast-v2.yml"
# #JOB_YAML="./3-kube-job-llm-ast-v3.yml"
# TAR_File="/home/user1-selab3/Documents/llm_ast_nongen_SB_test.tar.gz"
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



# POD_NAME="shradha-llmast-gp-engine-unoselab02-pod02-new-03"
# SCRIPT_PATH="/datasmall"
# JOB_YAML="./3-kube-job-llm-ast-v3.yml"
# #JOB_YAML="./3-kube-job-llm-ast-v3.yml"
# TAR_File="/home/user1-selab3/Documents/llm_ast_nongen_PR_test.tar.gz"
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