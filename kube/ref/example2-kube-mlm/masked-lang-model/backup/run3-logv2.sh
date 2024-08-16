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
kubectl logs "$selected_pod" -f
