#!/bin/bash

# ANSI color codes
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to display the available pods with index numbers
display_pods() {
  echo -e "${GREEN}Available pods:${NC}"
  pods=($(kubectl get pods --no-headers -o custom-columns=":metadata.name"))
  for i in "${!pods[@]}"; do
    echo -e "[$i] ${YELLOW}${pods[$i]}${NC}"
  done
}

# Function to prompt the user for a pod selection by index
prompt_for_pod() {
  echo -e "${GREEN}Please select a pod by entering the corresponding index number:${NC}"
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
      echo -e "${GREEN}Pod $selected_pod is in Running state.${NC}"
      return 0
      ;;
    "Pending")
      echo -e "${YELLOW}Pod $selected_pod is in Pending state. Waiting for it to start...${NC}"
      return 1
      ;;
    "ContainerCreating")
      echo -e "${YELLOW}Pod $selected_pod is still creating containers. Waiting for it to be ready...${NC}"
      return 1
      ;;
    "Succeeded")
      echo "Pod $selected_pod has completed."
      return 2
      ;;
    "Failed")
      echo "Pod $selected_pod has failed."
      return 2
      ;;
    *)
      echo -e "${RED}Unexpected status '$status' for pod $selected_pod. Exiting...${NC}"
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
  echo -e "${RED}Invalid selection. Please enter a valid index number:${NC}"
  prompt_for_pod
done

# Fetch logs for the selected pod
selected_pod="${pods[$selected_index]}"
echo -e "${GREEN}Fetching logs for pod: $selected_pod${NC}"

# Function to fetch logs and save if pod status is "Completed"
fetch_logs() {
  local retries=0
  local max_retries=999
  local wait_time=30

  while [ $retries -lt $max_retries ]; do
    check_pod_status
    status_code=$?

    if [ $status_code -eq 0 ]; then
      echo -e "${GREEN}Logs fetched successfully.${NC}"
      kubectl logs "$selected_pod" -f
      return 0
    elif [ $status_code -eq 1 ]; then
      echo -e "${YELLOW}Waiting for $wait_time seconds before checking status again...${NC}"
      sleep $wait_time
    elif [ $status_code -eq 2 ]; then
      echo "Pod $selected_pod has completed. Saving logs..."

      # Prompt user for log file name
      echo "Enter the filename to save logs (e.g., logs.txt):"
      read -r log_file

      # Save logs to file
      kubectl logs "$selected_pod" > "$log_file"

      echo "Logs saved to $log_file"
      return 0
    elif [ $status_code -eq 1 ]; then
      echo "Waiting for $wait_time seconds before checking status again..."
      sleep $wait_time
    else
      echo -e "${RED}Error checking status. Exiting...${NC}"
      exit 1
    fi

    retries=$((retries + 1))
  done

  echo -e "${RED}Max retries exceeded. Could not fetch logs for pod: $selected_pod${NC}"
  exit 1
}

# Fetch the logs with retry logic
fetch_logs
