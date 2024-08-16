# GPU Monitoring Setup with Prometheus and DCGM Exporter

This guide will help to set up GPU monitoring using Prometheus and the DCGM exporter using kubectl.

## Steps to Deploy Components

1. Deploy the DCGM exporter:
    ```sh
    kubectl apply -f dcgm_exporter.yaml
    ```

2. Deploy the Prometheus configuration:
    ```sh
    kubectl apply -f prometheus_configmap.yaml
    ```

3. Deploy Prometheus:
    ```sh
    kubectl apply -f prometheus_deployment.yaml
    ```

4. Deploy the causal model job:
    ```sh
    kubectl apply -f causal_model_job.yaml
    ```

5. Deploy the pod_pvc:
    ```sh
    kubectl apply -f pod_pvc.yaml
    ```

## Port forwarding and monitoring

1. Set up port forwarding to access the Prometheus UI:
    ```sh
    kubectl port-forward svc/prometheus -n gp-engine-unoselab01 9090:9090
    ```

2. Open your browser and navigate to:
    ```
    http://localhost:9090
    ```