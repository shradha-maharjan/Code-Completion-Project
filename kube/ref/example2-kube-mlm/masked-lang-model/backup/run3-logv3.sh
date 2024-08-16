#!/bin/bash

# Function to display the available pods with index numbers
display_pods() {
  echo "Available pods:"
  pods=($(kubectl get pods --no-headers -o custom-columns=":metadata.name"))
  for i in "${!pods[@]}"; do
    echo "[$i] ${pods[$i]}"
  done
}

# Function to prompt the user for a pod selection by index
prompt_for_pod() {
  echo "Please select a pod by entering the corresponding index number:"
  read -r selected_index
}

# Function to validate the selected index
validate_index() {
  if [[ $selected_index =~ ^[0-9]+$ ]] && [ "$selected_index" -ge 0 ] && [ "$selected_index" -lt "${#pods[@]}" ]; then
    return 0
  else
    return 1
  fi
}

# Function to check the current status of the selected pod
check_pod_status() {
  selected_pod="${pods[$selected_index]}"
  status=$(kubectl get pod "$selected_pod" -o jsonpath='{.status.phase}')

  case $status in
    "Running")
      echo "Pod $selected_pod is in Running state."
      return 0
      ;;
    "Pending")
      echo "Pod $selected_pod is in Pending state. Waiting for it to start..."
      return 1
      ;;
    "ContainerCreating")
      echo "Pod $selected_pod is still creating containers. Waiting for it to be ready..."
      return 1
      ;;
    *)
      echo "Unexpected status '$status' for pod $selected_pod. Exiting..."
      exit 1
      ;;
  esac
}

# Display the available pods
display_pods

# Prompt the user for a pod selection
prompt_for_pod

# Validate the selected index
while ! validate_index; do
  echo "Invalid selection. Please enter a valid index number:"
  prompt_for_pod
done

# Fetch logs for the selected pod
selected_pod="${pods[$selected_index]}"
echo "Fetching logs for pod: $selected_pod"

# Function to fetch logs with retry logic
fetch_logs() {
  local retries=0
  local max_retries=999
  local wait_time=30

  while [ $retries -lt $max_retries ]; do
    check_pod_status
    status_code=$?

    if [ $status_code -eq 0 ]; then
      # log_output=$(kubectl logs "$selected_pod" -f 2>&1)
      # echo "$log_output"
      # echo "Logs fetched successfully."
      kubectl logs "$selected_pod" -f
      return 0
    elif [ $status_code -eq 1 ]; then
      echo "Waiting for $wait_time seconds before checking status again..."
      sleep $wait_time
    else
      echo "Error checking status. Exiting..."
      exit 1
    fi

    retries=$((retries + 1))
  done

  echo "Max retries exceeded. Could not fetch logs for pod: $selected_pod"
  exit 1
}

# Fetch the logs with retry logic
fetch_logs
