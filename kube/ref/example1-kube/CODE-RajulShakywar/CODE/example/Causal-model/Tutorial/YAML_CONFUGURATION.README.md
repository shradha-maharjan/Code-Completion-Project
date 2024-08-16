# Understanding PVC, Pod with PVC, and Job in Kubernetes

This README provides an explanation of PVC (PersistentVolumeClaim), Pod with PVC, and Job in Kubernetes, along with detailed explanations of the YAML templates provided.

---

## 1. What is PVC, Pod with PVC, and Job?

- **PersistentVolumeClaim (PVC):** A PVC is a request for storage by a user. It allows users to request storage resources without having to know the details of the underlying storage infrastructure. PVCs are used to mount storage volumes to Pods.

- **Pod with PVC:** A Pod with PVC is a Kubernetes pod that uses PersistentVolumeClaims to attach storage volumes to it. This allows applications running in the pod to access persistent storage.

- **Job:** A Job in Kubernetes is a workload that runs a specific task to completion. It ensures that a specified number of pod replicas successfully terminate before considering the job as complete.

---

## 2. Explanation of YAML Templates:

### pvc.yaml

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: {{pvc_name}}
spec:
  storageClassName: rook-cephfs-central
  accessModes:
  - ReadWriteMany
  resources:
    requests:
      storage: 50Gi
```
- **Explanation:**
  - Defines a PersistentVolumeClaim with the given name.
  - Requests storage of 50Gi with ReadWriteMany access mode.
  - Uses the storage class \`rook-cephfs-central\`.

### pod.yaml

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: {{pod_name}}
spec:
  automountServiceAccountToken: false
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: topology.kubernetes.io/region
            operator: In
            values:
            - us-central 
  containers:
  - name: causal-model-gp-engine-unoselab01-pod1
    image: ubuntu:20.04
    command: ["sh", "-c", "echo 'Im a new pod' && sleep infinity"]
    resources:
      limits:
        memory: 25Gi
        cpu: 2
      requests:
        memory: 22Gi
        cpu: 2
    volumeMounts:
    - mountPath: /data
      name: {{persistentVolume_name}}
  volumes:
  - name: {{persistentVolume_name}}
    persistentVolumeClaim:
      claimName: {{persistentVolume_name}}
```
- **Explanation:**
  - Defines a Pod with the given name.
  - Specifies node affinity for scheduling to nodes in \`us-central\` region.
  - Uses an Ubuntu 20.04 image.
  - Requests 22Gi of memory and 2 CPU units.
  - Mounts a persistent volume to \`/data\` directory.

### causal-model.job.yaml

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ job_name }}
spec:
  ttlSecondsAfterFinished: 86400 # a day
  template:
    spec:
      automountServiceAccountToken: false
      affinity:
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:
              - matchExpressions:
                  - key: topology.kubernetes.io/region
                    operator: In
                    values:
                      - us-central 
      containers:
        - name: job-casual-model-train-container
          image: gitlab-registry.nrp-nautilus.io/gp-engine/jupyter-stacks/bigdata-2023:latest
          workingDir: /data
          command: ["sh", "-c", "bash run_install.sh && python3 /data/causal-model-a.py"]
          volumeMounts:
            - name: {{ pvc_name }}
              mountPath: /data
          resources:
            limits:
              memory: 21Gi
              cpu: "8"
              nvidia.com/gpu: 1
            requests:
              memory: 20Gi
              cpu: "8"    
              nvidia.com/gpu: 1
          securityContext:
            allowPrivilegeEscalation: false
            runAsUser: {{runAsUserID}}
      volumes:
        - name: {{ pvc_name }}
          persistentVolumeClaim:
            claimName: {{ pvc_name }}
      restartPolicy: Never
  backoffLimit: 1
```
- **Explanation:**
  - Defines a Job with the given name.
  - Specifies a TTL of 86400 seconds (1 day) after the job is finished.
  - Defines pod template with the same node affinity as the pod template above.
  - Uses a container with specified resource limits and requests, including GPU resources.
  - Mounts a PVC to \`/data\` directory for persistent storage.
  - Sets security context to disallow privilege escalation and specifies the user ID to run as.
  - Restarts the pod only once if it fails.

---

## 3. Explanation of Important Attributes:

- **apiVersion:** Specifies the Kubernetes API version used by the object.
- **kind:** Specifies the type of Kubernetes object being created.
- **metadata:** Contains metadata such as name, labels, etc.
- **spec:** Defines the desired state of the object.
- **affinity:** Specifies scheduling constraints.
- **containers:** Specifies the containers running in the pod or job.
- **volumeMounts:** Mounts storage volumes to containers.
- **resources:** Specifies resource limits and requests for CPU, memory, and optionally GPU.
- **securityContext:** Defines security settings for the container.
- **volumes:** Defines the volumes available to the pod or job.
- **restartPolicy:** Specifies the restart policy for the pod in the job.
- **backoffLimit:** Specifies the number of retries before considering a job as failed.