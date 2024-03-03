# Step 1 : Install the kubectl and k9s tool

**I.Download kubectl binary**
- curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl

**II. Make kubectl executable**
- chmod +x ./kubectl

**III. Move kubectl to /usr/local/bin**
- sudo mv ./kubectl /usr/local/bin/kubectl

**IV. Upload config**
- Upload the config file from [NRP portal](https://portal.nrp-nautilus.io/) and execute NautalisConfigSetup.ipny setup

**V. Install K9s tool**
- curl -sS https://webi.sh/k9s | sh
- export PATH="$HOME/.local/bin:$PATH"
- k9s
