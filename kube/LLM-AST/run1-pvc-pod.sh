# # !/bin/bash

# Namespace to clean up
# NAMESPACE="pvc-shradha-llmast-gp-engine-unoselab02"

# echo "Deleting all jobs in namespace $NAMESPACE..."
# kubectl delete jobs --all -n $NAMESPACE

# echo "Deleting all pods in namespace $NAMESPACE..."
# kubectl delete pods --all -n $NAMESPACE

# Delete all PVCs in the namespace
# echo "Deleting all PVCs in namespace $NAMESPACE..."
# kubectl delete pvc --all -n $NAMESPACE

# Uncomment the following lines if you also want to delete PVs
# echo "Deleting all PVs..."
# kubectl delete pv --all

# echo "Cleanup completed."

# kubectl get ns | grep uno
# echo "----------------------------------------------------------------------------"
# # kubectl apply -f 1-kube-pvc-llm-ast.yml
# kubectl apply -f 1-kube-pvc-llm-ast.yml --kubeconfig ~/kube2/config
# kubectl get pvc
# echo "----------------------------------------------------------------------------"
# kubectl apply -f 2-kube-pod-llm-ast.yml --kubeconfig ~/kube2/config
# kubectl get pods
# echo "----------------------------------------------------------------------------"

# kubectl exec  --stdin --tty <POD NAME> -- /bin/bash
# (myenv_python3_11) user1-selab3@oisit-selab3:~/kube2$ kubectl get pods --kubeconfig ~/kube2/config 
# No resources found in gp-engine-unoselab02 namespace.
# (myenv_python3_11) user1-selab3@oisit-selab3:~/kube2$ k9s --kubeconfig ~/kube2/config 


# !/bin/bash

Namespace to clean up
NAMESPACE="pvc-shradha-llmast-gp-engine-unoselab02a"

# echo "Deleting all jobs in namespace $NAMESPACE..."
# kubectl delete jobs --all -n $NAMESPACE

# echo "Deleting all pods in namespace $NAMESPACE..."
# kubectl delete pods --all -n $NAMESPACE

# Delete all PVCs in the namespace
# echo "Deleting all PVCs in namespace $NAMESPACE..."
# kubectl delete pvc --all -n $NAMESPACE

Uncomment the following lines if you also want to delete PVs
echo "Deleting all PVs..."
kubectl delete pv --all

echo "Cleanup completed."

kubectl get ns | grep uno
echo "----------------------------------------------------------------------------"
# kubectl apply -f 1-kube-pvc-llm-ast.yml
kubectl apply -f 1-kube-pvc-llm-ast.yml --kubeconfig ~/kube2/config
kubectl get pvc
echo "----------------------------------------------------------------------------"
kubectl apply -f 2-kube-pod-llm-ast.yml --kubeconfig ~/kube2/config
kubectl get pods
echo "----------------------------------------------------------------------------"
