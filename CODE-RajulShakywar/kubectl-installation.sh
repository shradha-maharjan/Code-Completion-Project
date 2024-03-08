#!/bin/bash

# Download kubectl binary
curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl

sleep 7
# Make kubectl executable
chmod +x ./kubectl

sleep 2
# Move kubectl to /usr/local/bin
sudo mv ./kubectl /usr/local/bin/kubectl


#Install K9s tool
# curl -sS https://webi.sh/k9s | sh
# export PATH="$HOME/.local/bin:$PATH"


