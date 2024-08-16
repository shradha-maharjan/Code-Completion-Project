#!/bin/bash

# Namespace to clean up
NAMESPACE="gp-engine-unoselab01"

echo "Deleting all jobs in namespace $NAMESPACE..."
kubectl delete jobs --all -n $NAMESPACE

echo "Deleting all pods in namespace $NAMESPACE..."
kubectl delete pods --all -n $NAMESPACE

# Delete all PVCs in the namespace
echo "Deleting all PVCs in namespace $NAMESPACE..."
kubectl delete pvc --all -n $NAMESPACE

# Uncomment the following lines if you also want to delete PVs
# echo "Deleting all PVs..."
# kubectl delete pv --all

echo "Cleanup completed."

kubectl get ns | grep uno
echo "----------------------------------------------------------------------------"
kubectl apply -f 1-kube-pvc.yml
kubectl get pvc
echo "----------------------------------------------------------------------------"
kubectl apply -f 2-kube-pod.yml
kubectl get pods
echo "----------------------------------------------------------------------------"
